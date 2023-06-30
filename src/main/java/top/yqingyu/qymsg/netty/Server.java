package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.yqingyu.common.utils.ThreadUtil;
import top.yqingyu.qymsg.MsgTransfer;

public class Server {

    private ServerBootstrap serverBootstrap;
    private int port = 4729;
    private int pool = Runtime.getRuntime().availableProcessors() * 2;
    private int radix = 32;
    private long clearTime = 30 * 60 * 1000;
    private String serverName = "QyMsg";
    private String threadName = "handle";
    private Class<? extends QyMsgServerHandler> handler;
    private ChannelFuture future;

    private Server() {
    }

    public static class Builder {
        public final Server server;

        public Builder() {
            server = new Server();
        }

        public Builder Port(int port) {
            server.port = port;
            return this;
        }

        public Builder Pool(int pool) {
            server.pool = pool;
            return this;
        }

        public Builder Radix(int radix) {
            server.radix = radix;
            return this;
        }

        public Builder ClearTime(int clearTime) {
            server.clearTime = clearTime;
            return this;
        }

        public Builder ServerName(String serverName) {
            server.serverName = serverName;
            return this;
        }

        public Builder ThreadName(String threadName) {
            server.threadName = threadName;
            return this;
        }

        public Builder handler(Class<? extends QyMsgServerHandler> handler) {
            server.handler = handler;
            return this;
        }

        public Server build() throws Exception {
            NioEventLoopGroup serverGroup = new NioEventLoopGroup(1, ThreadUtil.createThFactoryC("BOSS", "Th"));
            NioEventLoopGroup clientGroup = new NioEventLoopGroup(server.pool, ThreadUtil.createThFactoryC(server.serverName, server.threadName));
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(serverGroup, clientGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.childHandler(new QyMsgServerInitializer(server.handler, MsgTransfer.init(server.radix, server.clearTime)));
            server.serverBootstrap = serverBootstrap;
            return server;
        }
    }

    public void start() throws Exception {
        future = serverBootstrap.bind(port).sync();
    }

    public void block() throws InterruptedException {
        future.channel().closeFuture().sync();
    }

}
