namespace java com.xiaojing.distributed

service SimpleTransfer{
    bool transfer(1:string fromId,2:string toId,  3:i64 amount)
}