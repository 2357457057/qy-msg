package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import top.yqingyu.qymsg.QyMsg;

/**
 * 样例消息处理样例，接收数据  进行响应处理。
 */
@ChannelHandler.Sharable
public class DemoMsgHandler extends QyMsgServerHandler {
    @Override
    protected QyMsg handle(ChannelHandlerContext ctx, QyMsg msg) {
        //无条件回显。
        return msg;
    }
}
