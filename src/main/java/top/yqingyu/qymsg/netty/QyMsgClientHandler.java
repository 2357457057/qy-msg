package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import top.yqingyu.qymsg.QyMsg;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QyMsgClientHandler extends SimpleChannelInboundHandler<QyMsg> {
    public static final ConcurrentLinkedQueue<ChannelHandlerContext> CTX_QUEUE = new ConcurrentLinkedQueue<>();
    public static final LinkedBlockingQueue<QyMsg> MSG_QUEUE = new LinkedBlockingQueue<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        CTX_QUEUE.add(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, QyMsg msg) throws Exception {
        MSG_QUEUE.add(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}