package top.yqingyu.qymsg.netty;

import io.netty.channel.*;
import top.yqingyu.qymsg.QyMsg;

public class QyMsgClientHandler extends SimpleChannelInboundHandler<QyMsg> {

    private final ConnectionPool pool;

    public QyMsgClientHandler(ConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        pool.pushConnection(ctx);
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