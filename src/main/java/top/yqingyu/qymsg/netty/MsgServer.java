package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.yqingyu.common.utils.ThreadUtil;
import top.yqingyu.qymsg.MsgTransfer;

public class MsgServer {

    private ServerBootstrap serverBootstrap;
    private int pool = Runtime.getRuntime().availableProcessors() * 2;
    private int radix = 32;
    private long clearTime = 30 * 60 * 1000;
    private String serverName = "QyMsg";
    private String threadName = "handle";
    private Class<? extends QyMsgServerHandler> handler;
    private ChannelFuture future;

    private MsgServer() {
    }

    public static class Builder {
        public final MsgServer msgServer;

        public Builder() {
            msgServer = new MsgServer();
        }


        public Builder Pool(int pool) {
            msgServer.pool = pool;
            return this;
        }

        public Builder Radix(int radix) {
            msgServer.radix = radix;
            return this;
        }

        public Builder ClearTime(int clearTime) {
            msgServer.clearTime = clearTime;
            return this;
        }

        public Builder ServerName(String serverName) {
            msgServer.serverName = serverName;
            return this;
        }

        public Builder ThreadName(String threadName) {
            msgServer.threadName = threadName;
            return this;
        }

        public Builder handler(Class<? extends QyMsgServerHandler> handler) {
            msgServer.handler = handler;
            return this;
        }

        public MsgServer build() throws Exception {
            NioEventLoopGroup serverGroup = new NioEventLoopGroup(1, ThreadUtil.createThFactoryC("BOSS", "Th"));
            NioEventLoopGroup clientGroup = new NioEventLoopGroup(msgServer.pool, ThreadUtil.createThFactoryC(msgServer.serverName, msgServer.threadName));
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(serverGroup, clientGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.childHandler(new QyMsgServerInitializer(msgServer.handler, MsgTransfer.init(msgServer.radix, msgServer.clearTime)));
            msgServer.serverBootstrap = serverBootstrap;
            return msgServer;
        }
    }

    public void start(int port) throws Exception {
        future = serverBootstrap.bind(port).sync();
    }

    public void block() throws InterruptedException {
        future.channel().closeFuture().sync();
    }

}
