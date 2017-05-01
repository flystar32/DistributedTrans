## Undo Log

        
Undo Log 是为了实现事务的原子性，主要记录的是一个操作的反操作的内容。

- 事务的原子性(Atomicity)   
  一个事务（transaction）中的所有操作，要么全部完成，要么全部不完成，不会结束在中间某个环节。   
  事务在执行过程中发生错误，会被回滚（Rollback）到事务开始前的状态，就像这个事务从来没有执行过一样。   
- 事务的持久性(Durability)   
  事务处理结束后，对数据的修改就是永久的，即便系统故障也不会丢失。   
  
- 用Undo Log实现原子性和持久化的事务的简化过程

        假设有A、B两个数据，初始值分别为1和2。现在需要执行一个事务，将A的值改为3且将B的值改为4。  
  
        A.事务开始.   
        B.记录A=1到undo log的内存buffer.   
        C.在内存中修改A=3.   
        D.记录B=2到undo log的内存buffer.   
        E.在内存中修改B=4.   
        F.将undo log的buffer写到磁盘。  
        G.将内存中修改后的数据写到磁盘。  
        H.将事务标记为已提交的状态


这整个过程中，有可能出现异常情况。在目前的系统中，我们认为异常情况有两种：    
一正常的逻辑已经无法继续下去，但是程序本身还是可以正常运行的，可以依靠程序本身的异常处理逻辑来处理这部分异常。    
第二种异常比较严重，程序本身已经无法正常工作了，比如系统突然断电。    
为了便于叙述我们将前者称为逻辑异常，后者称为宕机异常。


* 如果在**A到F**的过程中出现了逻辑异常，数据库会将此次的事务表示为失败的状态，因为G并没有执行，所以数据还是原样不动，符合一致性。   
* 如果在**G到H**的过程中出现了逻辑异常，数据库会将此次的事务表示为失败的状态，当发现G已执行后，会执行F中保存的undolog，将数据恢复。   
* 如果在**A到E**的过程中出现了宕机异常，数据库重启后会发现这个事务处于初始状态，但是没看到undolog，说明G肯定没执行，数据是一致的，可以放心地直接将事务标记为失败。  
* 如果在**F到H**的过程中出现了宕机异常，数据库重启后会发现这个事务处于初始状态，然后一看，undolog是存在的，这个时候就尴尬了，G到底执行成功了没有呢？，如果未执行成功，则数据现在就是一致的，直接将事务标记为失败就好，如果执行成功了，则需要执行undolog将数据恢复成一致的状态，这可如何是好？    

这里需要引入一个redolog，就是将之前的更新A和更新B的操作也记录下来，只要这个redolog的执行可以保证幂等性，之前苦恼的问题就解决了，也不需要猜测这个G是否真正的执行成功了，只需要将redolog重新执行一遍即可。然后就可以放心地执行undolog，将数据恢复一致性后再将事务标记成失败的状态。   
幂等是一个很好的词语，我们在设计自己的系统时候，可以很轻松地通过请求流水号等参数将幂等实现。但是对于数据库来说，因为对性能的要求比较高，所以幂等有可能不成立。(这一点我不确定，我猜测的，支持幂等最好)   

回到上文中的顺序，其实F和G这两步骤，在每一个的事务执行的过程中，都需要强行地写两次磁盘。这样会导致大量的磁盘IO，因此性能很低。

综合这两点来说，redolog是避免不掉的，而且既然已经有redolog了，是否就可以不再需要将数据实时写到磁盘这一步，大不了奔溃的时候，直接使用redolog将数据恢复。   

        A.事务开始.
        B.记录A=1到undo log的内存buffer.
        C.内存中修改A=3.
        D.记录A=3到redo log的内存buffer.
        E.记录B=2到undo log的内存buffer.
        F.内存中修改B=4.
        G.记录B=4到redo log的内存buffer.
        H.将undo log的buffer写到磁盘。
        I.将redo log的内存buffer写入磁盘。
        J.事务提交

虽然数据不需要时写磁盘了，但是undolg和redolog还是需要写，看起来并没有什么改观？   
但是有个不一样的是，数据库的数据是结构化存储的，存储位置早就确定了，而且大多数是更新请求。   
但是redolog和undolog都是新的内容，对他们来说，保存就是新增文件。   
再联想到kafka为什么写文件效率那么高，磁盘的顺序写操作其实是非常快的，并不比内存满多少。   
而且既然想要实现顺序写，就干脆把undolog也作为redolog的内容的一部分进行保存。

顺序写，就表明了这个过程中，redolog可能是逻辑无关的，很多分别属于不同事务的redolog会被一起写到磁盘上，当系统在出现宕机异常时，会找到数据保存的那个checkpoint，然后开始执行之后的redolog，将数据恢复。   
如果执行了尚未被标记为成功的事务，或者执行了已经被标记为Rollback的事务，这时候会去找到他们的undolog，执行undolog后，将数据恢复到一致的状态。   


详细的流程这里就不再讲了，涉及到mvcc的更加复杂，我也尚未完全弄清楚，可以参考以下文章：   
[InnoDB recovery详细流程](http://www.sysdb.cn/index.php/2016/01/14/innodb-recovery/)   
[MySQL · 引擎特性 · InnoDB 崩溃恢复过程](http://mysql.taobao.org/monthly/2015/06/01/)   

## 分布式事务

回到刚才的问题，之所以做了那么多的扩展，是因为遇到了前面说的那个问题，无法确定`将undo log的buffer写到磁盘`执行成功后，`将内存中修改后的数据写到磁盘`是否执行成功，如果解决了这个问题，也就没redolog啥事了。 

我们在执行分布于不同的两个数据库的操作时，数据库的事务已经无法使用了，但是对于单个数据库来说了，数据库的事务还是很有效的，而且对于我们应用层的框架来说，也不会那么纠结于性能，毕竟网络IO会占大多数。   
想到这里，我们可以使用数据库的事务来保证数据A操作的do操作和undolog的保存在同一个事务中！  

   
因为我们主要的业务是做支付，那么我们就将A转账给B这个场景来进行讨论

![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114738461-956963526.png)



如图所示，distributeJob代表了一个完整的转账场景，A转账10元给B，其中A和B的账户存储在不同的数据库中，执行的过程是先通过transferOut从A的账户中扣除10元，然后再执行transferIn给B增加10元。

undoOutSave，表明将transferOut的undolog保存起来，方便在需要rollback时将transferOut的影响撤销，在这里其实就是将钱加回来，即给A的账户增加10元。同理对于transferIn的undolog的保存也就是undoInSave。   
其中绿色的框框将两个操作框起来，是表明这两个操作是位于同一个数据库事务中。

当执行过程中出现异常时，会将之前所有已完成的操作回滚，恢复到初始状态，一个比较通用的整体的流程如下

![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114752976-1423027523.png)




## 实现  [talk is cheap,code is here](https://github.com/flystar32/DistributedTrans)   

因为我们是使用thrift框架来做服务的，整个过程使用拦截器来实现各种逻辑，最外层代码看起来如下

    @DistributeJob
    public boolean transfer(Context context, String fromId, String toId, long amount) throws TException {
      try {
        transferOut(context, fromId, toId, amount);
        transferIn(context, fromId, toId, amount);
      } catch (Exception e) {
        throw new TException(e);
      }
      return true;
    }


    @DoJob
    @GetConnection
    public boolean transferOut(Context context, @SharedKey("userId") String fromId, String toId,
                               long amount) throws Exception {
      userDao.updateBalanceById(context.getConnection(), fromId, -amount, 0L);
      undoTransferOut(context, null, fromId, toId, amount);
      return true;
    }

    @UndoJob
    @GetConnection
    public boolean undoTransferOut(Context context, com.xiaojing.distributed.model.UndoJob undoJob,
                                   @SharedKey("userId") String fromId, String toId, long amount)
      throws Exception {
      userDao.updateBalanceById(context.getConnection(), fromId, amount, Long.MIN_VALUE);
      return true;
    }


看不懂也没关系，下面有详细的流程图。
![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114931695-728300721.png)



红色的线表明，所有被抛出的逻辑异常都会触发在线的回滚。这里叫逻辑异常也不全，也有可能是网络异常导致的IO异常，这里我们换个说法，将这些异常统称为，非宕机异常。


同样的，如前文所说，还有一种异常叫做宕机异常，在解决这些异常时，没有在线回滚了，只能在服务重启后，通过扫表的方式来进行离线的回滚。离线回滚无非就是线找到未完成的事务，然后将其的undolog找出来，然后执行undolog即可。

如前文所说的，undolog执行的幂等还是很重要的，在这里我们是通过将undolog置为rollbacked和执行undolog的内容放在同一个事务中来保证，undolog只会执行一次的。


在出现异常的时候，回滚的流程图如下。
![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114942992-985239747.png)




图中的rollback fail，极少数情况下会发生，比如B账户注销了，或者是B账户的钱恰好在这一刻完全花完了，这种情况，只好交给人工处理。

#### 测试
1、代码中的test目录下，模拟了各种情况下出现的非宕机异常，验证了结果的有效性。单测中使用了h2数据库，测试前必须先将其启动。  
2、对于宕机异常，写了一个shell脚本，每1s关闭服务一次，client不断去调用server执行转账操作。当停止两个脚本后，正常启动服务，最后check金额，满足一致性。执行方法如下：
> mvn -Dmaven.test.skip=true clean package    
> nohup sh transfer_server.sh daemon &    
> nohup sh transfer_server.sh kill &   
> nohup sh transfer_client.sh start &     
> kill -9  (daemon/kill/client) 将服务端和客户端都停止   
> sh scheduler_rollback.sh start   

执行前，需要将配置文件中的数据库换成自己的地址，并执行一下init.sql中的初始化语句，还有把脚本里的路径换成你自己的。      
在正常服务中，scheduler_rollback不需要作为一个单独的服务进行启动，他只是main server的一个线程


在执行rollback之前，查看数据库中的数据，明显可以看出数据是不一致的   
![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114832726-886299461.png)


rollback服务启动一段时间后，查看数据库中账户1和2的总金额，发现是一致的。我们这里使用的是10s调度一次，通过测试数据观察，真正的回滚耗费实践是在ms级别的。 
![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114847117-1102461498.png)


rollback执行过程中的数据distribute_job和undo_job的状态如下   
![](http://images2015.cnblogs.com/blog/1081851/201705/1081851-20170501114856679-329330405.png)



#### 小细节
1、这里是先执行了do，然后再保存undo，其实因为两者属于同一个数据库事务，先后顺序其实不那么重要。但是在逻辑异常需要回滚的时候，我们还是希望能够直接从内存里拿出undolog，然后进行undo操作的，这样可以减少一次数据库查找的开销，因为这个原因，所以将undosave放在了后面，写代码的时候比较不容易弄混。   
2、小技巧，对于所有的有可能存在并发的对数据的操作，所有的读操作都是不正确的，直接使用CAS的写操作，以写代查，才是正确的做法。   
3、判断一个事务是否是因为宕机停留在初始状态是通过超时来判断的，所以执行中的distributeJob需要select for update。
4、正常的服务都是无状态扩容的，虽然我们的rollback支持并发和幂等，但是为了避免过度竞争影响效率，rollback操作还是需要制定一台的执行。

## 局限
听起来很美妙，实现了分布式事务！遗憾的是，这套代码并没有在生产环境上使用，理由有如下：

1、真实情况下，一个交易并不仅仅是只有账户的变动，还有其他服务的调用、跨服务的rpc，而不仅仅是数据库。关于这一点，下一篇我会写写跨服务的分布式事务。   
2、大多数交易出现的费宕机异常都是网络的IO异常，这种情况下完全可以通过重试解决，直接rollback的方式过于悲观，而且增加上游接入难度。   
3、“分布式事务”本身的局限性，这里只对量变比较好使，对于质变这种方式，这种rollback是不是正确的呢？况且即使是做CAS也无法解决ABA的问题。



最佳使用场景，电商购物车多件商品抢购模型。因为都是量变，而且直接在失败时迅速回归库存正好适用于此场景。

## 参考文档
[MySQL数据库InnoDB存储引擎Log漫游(1)](http://mp.weixin.qq.com/s?__biz=MzIyMTQ1NDE0MQ==&mid=2247483671&idx=1&sn=e066938c9a7571fdd6969b0d0ae3f661&chksm=e83dcb45df4a42535073eebf24537759bbbec7198ff5f6d8a1b096e5b04cf551d9d788a69191&mpshare=1&scene=1&srcid=1124iwqIPmeLLWTC4sy87ZXW#rd)    
[MySQL · 引擎特性 · InnoDB undo log 漫游](http://mysql.taobao.org/monthly/2015/04/01/)    
[MySQL · 引擎特性 · InnoDB redo log漫游](http://mysql.taobao.org/monthly/2015/05/01/)