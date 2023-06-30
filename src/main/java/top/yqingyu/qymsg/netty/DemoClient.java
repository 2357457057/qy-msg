package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import top.yqingyu.common.utils.UUIDUtil;
import top.yqingyu.qymsg.DataType;
import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

public class DemoClient {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup(10);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new QyMsgClientInitializer(MsgTransfer.init(32, 30 * 60 * 1000)));
        //启动客户端去连接服务器端
        bootstrap.connect("127.0.0.1", 4729).sync();
        Thread.sleep(10);
        ChannelHandlerContext poll = QyMsgClientHandler.CTX_QUEUE.poll();
        QyMsg qyMsg = new QyMsg(MsgType.AC, DataType.OBJECT);
        String s = UUIDUtil.randomUUID().toString2();
        qyMsg.setFrom(s);
        qyMsg.putMsgData("AC_STR", "123456");
        poll.writeAndFlush(qyMsg);
        QyMsg take = QyMsgClientHandler.MSG_QUEUE.take();
        System.out.println(take);
        poll.close();

    }
}
