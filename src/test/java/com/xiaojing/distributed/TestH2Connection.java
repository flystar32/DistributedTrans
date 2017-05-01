package com.xiaojing.distributed;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by xiaojing on 15/9/30.
 */
public class TestH2Connection {


  @Test
  public void testH2Working(){
    try {

      /**todo execute 'java -cp h2*.jar org.h2.tools.Server' to start h2 server first*/
      Class.forName("org.h2.Driver");
      Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:test2", "sa", "");
      System.out.print(conn);
      Assert.assertNotEquals(null, conn);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}