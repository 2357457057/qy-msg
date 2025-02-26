package top.yqingyu.qymsg.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import top.yqingyu.qymsg.Dict;
import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * QyMsg 转Byte发送出去。
 */
public class QyMsgEncodeBytes extends MessageToMessageEncoder<QyMsg> {
    private final MsgTransfer transfer;

    private final QyMsgExceptionHandler exceptionHandler;

    public QyMsgEncodeBytes(MsgTransfer transfer, QyMsgExceptionHandler exceptionHandler) {
        this.transfer = transfer;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, QyMsg msg, List<Object> out) throws Exception {
        ArrayList<byte[]> msgBytes;
        try {
            msgBytes = transfer.msgEncoder.encode(msg);
        } catch (Exception e) {
            if (exceptionHandler != null)
                exceptionHandler.exceptionCaught(ctx, e);
            QyMsg clone = msg.clone();
            clone.setMsgType(MsgType.ERR_MSG);
            clone.putMsg("msg encodeException");
            clone.putMsgData(Dict.ERR_MSG_EXCEPTION, e);
            msgBytes = transfer.msgEncoder.encode(clone);
        }
        for (byte[] bytes : msgBytes) {
            ByteBuf buffer = ctx.alloc().buffer(bytes.length);
            buffer.writeBytes(bytes);
            out.add(buffer);
        }

    }
}
