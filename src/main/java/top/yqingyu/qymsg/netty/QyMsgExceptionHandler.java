package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * 异常处理类
 * 持有{@linknetty.ServerExceptionHandler} 具体实现异常处理
 */
public class QyMsgExceptionHandler extends ChannelDuplexHandler {
    private final ServerExceptionHandler serverExceptionHandler;

    public QyMsgExceptionHandler() {
        this.serverExceptionHandler = new ServerExceptionHandler() {};
    }

    public QyMsgExceptionHandler(ServerExceptionHandler serverExceptionHandler) {
        this.serverExceptionHandler = serverExceptionHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        serverExceptionHandler.handle(ctx, cause);
    }
}
