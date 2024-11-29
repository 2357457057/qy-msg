package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import top.yqingyu.common.utils.ThreadUtil;
import top.yqingyu.common.utils.UUIDUtil;
import top.yqingyu.qymsg.DataType;
import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    public void returnConnection(Connection connection) {
        pool.returnConnection(connection);
    }
}
