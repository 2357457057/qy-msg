package top.yqingyu.qymsg.socket;

import top.yqingyu.qymsg.DataType;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;


public class MsgClient {
    ConnectionPool pool;
    ConnectionConfig config;
    QyMsg HEART_BEAT;

    private MsgClient() {
    }

    public static MsgClient create(ConnectionConfig config) {
        MsgClient client = new MsgClient();
        client.config = config;
        client.HEART_BEAT = new QyMsg(MsgType.HEART_BEAT, DataType.STRING);
        client.HEART_BEAT.setFrom(config.name);
        client.pool = new ConnectionPool(client);
        return client;
    }

    public void shutdown() throws InterruptedException {
        pool.shutdown();
    }

    public Connection getConnection() throws Exception {
        return pool.getConnection();
    }
}
