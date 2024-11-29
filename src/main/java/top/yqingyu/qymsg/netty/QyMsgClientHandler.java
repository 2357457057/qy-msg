package top.yqingyu.qymsg.netty;

import io.netty.channel.*;
import top.yqingyu.qymsg.QyMsg;

public class QyMsgClientHandler extends SimpleChannelInboundHandler<QyMsg> {

    private final ConnectionPool pool;
    private final Channel channel;

    public QyMsgClientHandler(ConnectionPool pool, Channel channel) {
        this.pool = pool;
        this.channel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel.attr(pool.connectionAttr).set(new Connection(ctx));
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