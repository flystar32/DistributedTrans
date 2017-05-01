package com.xiaojing.distributed;

import com.google.inject.Injector;

import com.xiaojing.distributed.server.SimpleTransfer;
import com.xiaojing.distributed.server.SimpleTransferImpl;
import com.xiaojing.distributed.util.GuiceConfig;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    long begin = System.currentTimeMillis();
    LOGGER.debug("start begin at " + begin);

    Injector injector = GuiceConfig.getInstance().getInjector();
    SimpleTransferImpl simpleTransfer = injector.getInstance(SimpleTransferImpl.class);

    TServerSocket serverTransport = new TServerSocket(9999);
    TBinaryProtocol.Factory factory = new TBinaryProtocol.Factory();
    TProcessor tProcessor = new SimpleTransfer.Processor<SimpleTransfer.Iface>(simpleTransfer);
    TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport)
        .processor(tProcessor)
        .protocolFactory(factory);
    final TServer tServer = new TThreadPoolServer(serverArgs);
    tServer.serve();
  }

}
