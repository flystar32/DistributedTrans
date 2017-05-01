package com.xiaojing.distributed;

import com.google.inject.Injector;

import com.xiaojing.distributed.util.DataBaseUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


/**
 * Created by xiaojing on 15/9/30.
 *
 * init h2 database
 * todo execute 'java -cp h2*.jar org.h2.tools.Server' to start h2 server first
 *
 * memory model will delete all data when the connection is closed
 */
public abstract class BaseTest {

    public static Injector injector;
    public static Connection connection1;
    public static Connection connection2;
    public static Statement stmt1;
    public static Statement stmt2;

    @BeforeClass
    public static void beforeClass() {

        //prepare the test database
        System.out.println("---  init DataBase  ---");
        try {
            Class.forName("org.h2.Driver");
            connection1 = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:distributed_test0", "sa", "");
            connection2 = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:distributed_test1", "sa", "");
            stmt1 = connection1.createStatement();
            stmt1.addBatch("CREATE TABLE IF NOT EXISTS `distribute_job` (\n" +
                    "  `id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `state` varchar(36) NOT NULL DEFAULT '',\n" +
                    " `last_modify_timestamp` BIGINT(20) NOT NULL,\n"+
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            stmt1.addBatch("CREATE TABLE IF NOT EXISTS `undo_job` (\n" +
                    "  `id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `distribute_job_id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `state` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `content` varbinary(2048)  DEFAULT '',\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            stmt1.addBatch("CREATE TABLE IF NOT EXISTS `user` (\n" +
                    "  `id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `balance` bigint(11) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");


            stmt1.addBatch("insert into user values ('2',10000)");
            stmt1.executeBatch();
            //}
            stmt2 = connection2.createStatement();
            stmt2.addBatch("CREATE TABLE IF NOT EXISTS `distribute_job` (\n" +
                    "  `id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `state` varchar(36) NOT NULL DEFAULT '',\n" +
                    " `last_modify_timestamp` BIGINT(20) NOT NULL,\n"+
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            stmt2.addBatch("CREATE TABLE IF NOT EXISTS `undo_job` (\n" +
                    "  `id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `distribute_job_id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `state` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `content` varbinary(2048)  DEFAULT '',\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            stmt2.addBatch("CREATE TABLE IF NOT EXISTS `user` (\n" +
                    "  `id` varchar(36) NOT NULL DEFAULT '',\n" +
                    "  `balance` bigint(11) NOT NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

            stmt2.addBatch("insert into user values ('1',10000)");
            stmt2.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stmt1.close();
                stmt2.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("---init DataBase OK ---");


        DataBaseUtil.init();


    }

    @AfterClass
    public static void afterClass() {
        try {
            connection1.close();
            connection2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInitDatabase() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:tcp://localhost/mem:test1", "sa", "");
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("select * from user");
        while (rs.next()) {
            System.out.println(rs.getString(1) + "   " + rs.getLong(2));
        }
    }
}
