package top.yqingyu.qymsg.netty;

import top.yqingyu.common.utils.LocalDateTimeUtil;
import top.yqingyu.common.utils.PercentUtil;
import top.yqingyu.common.utils.UUIDUtil;
import top.yqingyu.qymsg.DataType;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;


public class DemoClient {
    public static void main(String[] args) throws Exception {

        ConnectionConfig build = new ConnectionConfig.Builder()

                .host("192.168.50.68")
                .poolMax(40)
                .build();
        MsgClient client = MsgClient.create(build);
        QyMsg qyMsg = new QyMsg(MsgType.AC, DataType.OBJECT);
        String s = UUIDUtil.randomUUID().toString2();
        qyMsg.setFrom(s);
        qyMsg.putMsgData("AC_STR", "okkkko");

        ConcurrentLinkedQueue<Long> longs = new ConcurrentLinkedQueue<>();
        CountDownLatch latch = new CountDownLatch(100000);
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    try {
                        Connection connection = client.pool.getConnection();
                        LocalDateTime now = LocalDateTime.now();
                        connection.get(qyMsg);
                        longs.add(LocalDateTimeUtil.between(now, LocalDateTime.now(), ChronoUnit.MICROS));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        latch.await();

        long to = 0;
        for (Long aLong : longs) {
            to += aLong;
        }
        System.out.println(to / longs.size());
        System.out.println(longs.size());
        client.shutdown();
    }
}
