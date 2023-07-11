package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import top.yqingyu.common.utils.ThreadUtil;
import top.yqingyu.qymsg.DataType;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

public class MsgClient {
    Bootstrap bootstrap;
    ConnectionPool pool;
    ConnectionConfig config;
    private EventLoopGroup group;
    QyMsg HEART_BEAT;

    private MsgClient() {
    }

    public static MsgClient create(ConnectionConfig config) {
        MsgClient client = new MsgClient();
        client.config = config;
        client.HEART_BEAT = new QyMsg(MsgType.HEART_BEAT, DataType.STRING);
        client.HEART_BEAT.setFrom(config.name);
        client.pool = new ConnectionPool(client);

        EventLoopGroup group = new NioEventLoopGroup(config.poolMax, ThreadUtil.createThFactoryC(config.name, config.threadName));
        client.group = group;
        Bootstrap bootstrap = new Bootstrap();
        client.bootstrap = bootstrap;

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new QyMsgClientInitializer(client));
        return client;
    }

    public void shutdown() throws InterruptedException {
        group.shutdownGracefully().sync();
    }

    public Connection getConnection() throws Exception {
        return pool.getConnection();
    }
}
