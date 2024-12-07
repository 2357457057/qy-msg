package top.yqingyu.qymsg.netty;

import io.netty.channel.*;
import io.netty.util.AttributeKey;
import top.yqingyu.qymsg.QyMsg;

import java.util.concurrent.CountDownLatch;

public class QyMsgClientHandler extends SimpleChannelInboundHandler<QyMsg> {

    private final ConnectionPool pool;
    private final Channel channel;

    public QyMsgClientHandler(ConnectionPool pool, Channel channel) {
        this.pool = pool;
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CountDownLatch countDownLatch = (CountDownLatch) channel.attr(AttributeKey.valueOf("SYNC:" + channel.hashCode())).get();
        countDownLatch.countDown();
        AttributeKey<Object> objectAttributeKey;
        if (AttributeKey.exists("CONNECTION:" + channel.hashCode())) {
            objectAttributeKey = AttributeKey.valueOf("CONNECTION:" + channel.hashCode());
        } else
            objectAttributeKey = AttributeKey.newInstance("CONNECTION:" + channel.hashCode());
        channel.attr(objectAttributeKey).set(new Connection(ctx));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, QyMsg msg) throws Exception {
        pool.putMsg(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}