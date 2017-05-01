package com.xiaojing.distributed;

import com.xiaojing.distributed.server.SimpleTransfer;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

  private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

  public static void main(String[] args) throws Exception {

    int i = 0;
    while (true) {
      Thread.sleep(10);

      LOGGER.debug("" + (i++));

      try {
        TTransport transport = new TSocket("localhost", 9999);
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);//设置传输协议
        SimpleTransfer.Client client = new SimpleTransfer.Client(protocol);
        client.transfer("1", "2", 1);//10ms
        LOGGER.info("success");
      } catch (Exception e) {
        LOGGER.error("error", e);
      }
    }
  }


}
