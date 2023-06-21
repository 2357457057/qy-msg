package top.yqingyu.qymsg;


import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.yqingyu.qymsg.Dict.*;

/**
 * 各种消息类型的处理
 *
 * @author YYJ
 * @version 1.0.0
 * @ClassNameMsgTransfer
 * @createTime 2022年09月01日 23:03:00
 */

@SuppressWarnings("all")
public class MsgTransfer {
    //消息长度占位长度
    private static final Logger log = LoggerFactory.getLogger(MsgTransfer.class);

//    private static final int MSG_LENGTH_MAX = 33_554_432;


    public int MSG_LENGTH_RADIX;
    //默认为 大部分网卡的一帧的长度
    public int BODY_LENGTH_MAX = 1400;
    public MsgConnector connector;
    public MsgEncoder msgEncoder;
    public MsgDecoder msgDecoder;

    private static Hashtable<DataType, Character> DATA_TYPE_2_CHAR;
    private static Hashtable<Character, DataType> CHAR_2_DATA_TYPE;

    private static Hashtable<MsgType, Character> MSG_TYPE_2_CHAR;
    private static Hashtable<Character, MsgType> CHAR_2_MSG_TYPE;
    private static Hashtable<Character, Boolean> SEGMENTATION_2_BOOLEAN;
    private static Hashtable<Boolean, Character> BOOLEAN_2_SEGMENTATION;


    protected static final String DICT = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";

    static {
        //消息类型映射
        {
            MSG_TYPE_2_CHAR = new Hashtable<>();
            MSG_TYPE_2_CHAR.put(MsgType.AC, '#');
            MSG_TYPE_2_CHAR.put(MsgType.HEART_BEAT, '&');
            MSG_TYPE_2_CHAR.put(MsgType.NORM_MSG, '%');
            MSG_TYPE_2_CHAR.put(MsgType.ERR_MSG, '=');

            CHAR_2_MSG_TYPE = new Hashtable<>();
            CHAR_2_MSG_TYPE.put('#', MsgType.AC);
            CHAR_2_MSG_TYPE.put('&', MsgType.HEART_BEAT);
            CHAR_2_MSG_TYPE.put('%', MsgType.NORM_MSG);
            CHAR_2_MSG_TYPE.put('=', MsgType.ERR_MSG);

        }
        //数据类型映射
        {
            DATA_TYPE_2_CHAR = new Hashtable<>();
            DATA_TYPE_2_CHAR.put(DataType.OBJECT, '=');
            DATA_TYPE_2_CHAR.put(DataType.JSON, '%');
            DATA_TYPE_2_CHAR.put(DataType.STRING, '&');
            DATA_TYPE_2_CHAR.put(DataType.STREAM, '#');
            DATA_TYPE_2_CHAR.put(DataType.FILE, ']');

            CHAR_2_DATA_TYPE = new Hashtable<>();
            CHAR_2_DATA_TYPE.put('=', DataType.OBJECT);
            CHAR_2_DATA_TYPE.put('%', DataType.JSON);
            CHAR_2_DATA_TYPE.put('&', DataType.STRING);
            CHAR_2_DATA_TYPE.put('#', DataType.STREAM);
            CHAR_2_DATA_TYPE.put(']', DataType.FILE);
        }
        //消息分片映射
        {
            SEGMENTATION_2_BOOLEAN = new Hashtable<>();
            SEGMENTATION_2_BOOLEAN.put('+', true);
            SEGMENTATION_2_BOOLEAN.put('-', false);

            BOOLEAN_2_SEGMENTATION = new Hashtable<>();
            BOOLEAN_2_SEGMENTATION.put(true, '+');
            BOOLEAN_2_SEGMENTATION.put(false, '-');

        }
    }


    public static MsgTransfer init(int radix, int body_length_max, long clearTime) {
        MsgTransfer msgTransfer = new MsgTransfer();
        msgTransfer.MSG_LENGTH_RADIX = radix;
        msgTransfer.BODY_LENGTH_MAX = body_length_max;
        msgTransfer.connector = new MsgConnector(clearTime);
        msgTransfer.msgEncoder = new MsgEncoder(msgTransfer);
        msgTransfer.msgDecoder = new MsgDecoder(msgTransfer);
        return msgTransfer;
    }

    public static MsgTransfer init(int radix, long clearTime) {
        MsgTransfer msgTransfer = new MsgTransfer();
        msgTransfer.MSG_LENGTH_RADIX = radix;
        msgTransfer.connector = new MsgConnector(clearTime);
        msgTransfer.msgEncoder = new MsgEncoder(msgTransfer);
        msgTransfer.msgDecoder = new MsgDecoder(msgTransfer);
        return msgTransfer;
    }

    protected static char DATA_TYPE_2_CHAR(DataType dataType) {
        return DATA_TYPE_2_CHAR.get(dataType);
    }

    public static DataType CHAR_2_DATA_TYPE(char c) {
        return CHAR_2_DATA_TYPE.get(c);
    }

    protected static char MSG_TYPE_2_CHAR(MsgType msgType) {
        return MSG_TYPE_2_CHAR.get(msgType);
    }

    public static MsgType CHAR_2_MSG_TYPE(char c) {
        return CHAR_2_MSG_TYPE.get(c);
    }

    public static boolean SEGMENTATION_2_BOOLEAN(char c) {

        return SEGMENTATION_2_BOOLEAN.get(c);
    }

    protected static char BOOLEAN_2_SEGMENTATION(boolean b) {
        return BOOLEAN_2_SEGMENTATION.get(b);
    }


    private byte[] getQyMsgBytes(byte[]... bytess) {
        byte[] buf = new byte[0];

        for (byte[] bytes : bytess) {
            buf = ArrayUtil.addAll(buf, getLength(bytes).getBytes(StandardCharsets.UTF_8));
            buf = ArrayUtil.addAll(buf, bytes);
        }
        //将信息长度与信息组合
        return buf;
    }

    public String getLength(byte[] bytes) {
        //将信息长度与信息组合
        return StringUtil.leftPad(Integer.toUnsignedString(bytes.length, MSG_LENGTH_RADIX), BODY_LENGTH_LENGTH, '0');
    }

    public String getLength(long length) {
        StringBuilder msgLength = new StringBuilder();
        msgLength.append(Integer.toUnsignedString((int) length, MSG_LENGTH_RADIX));
        //长度信息不足MSG_LENGTH_LENGTH位按0补充
        while (msgLength.toString().getBytes(StandardCharsets.UTF_8).length != BODY_LENGTH_LENGTH) {
            msgLength.insert(0, '0');
        }
        //将信息长度与信息组合
        return msgLength.toString();
    }

    /**
     * @param socketChannel xxx
     * @param qyMsg         消息
     * @author YYJ
     * @version 1.0.0
     * @description 写出分片消息 或完整消息
     */
    public void writeQyMsg(SocketChannel socketChannel, QyMsg qyMsg) throws Exception {
        ArrayList<byte[]> assembly = msgEncoder.encode(qyMsg);
        for (byte[] bytes : assembly) {
            IoUtil.writeBytes(socketChannel, bytes);
        }
    }

    /**
     * @param socket xxx
     * @param qyMsg  消息
     * @author YYJ
     * @version 1.0.0
     * @description 写出分片消息 或完整消息
     */
    public void writeQyMsg(Socket socket, QyMsg qyMsg) throws Exception {
        ArrayList<byte[]> assembly = msgEncoder.encode(qyMsg);
        for (byte[] bytes : assembly) {
            IoUtil.writeBytes(socket, bytes);
        }
    }

    /**
     * @param socketChannel xxx
     * @param queue         分片队列
     * @param sleep         间隔时间
     * @return 解析的消息
     * @author YYJ
     * @version 1.0.0
     * @description 读取消息并将分片消息写入队列
     */
    public QyMsg readQyMsg(SocketChannel socketChannel, long sleep) throws IOException, ClassNotFoundException, InterruptedException {
        return msgDecoder.decode(socketChannel, sleep);
    }

    /**
     * @param socket xxx
     * @param queue  分片队列
     * @param sleep  间隔时间
     * @return 解析的消息
     * @author YYJ
     * @version 1.0.0
     * @description 读取消息并将分片消息写入队列
     */
    public QyMsg readQyMsg(Socket socket, AtomicBoolean breakFlag) throws IOException, ClassNotFoundException, InterruptedException {
        return msgDecoder.decode(socket, breakFlag);
    }


    public void writeMessage(SocketChannel socketChannel, String userId, String msg) throws Exception {
        writeQyBytes(socketChannel, getQyMsgBytes(userId.getBytes(StandardCharsets.UTF_8), msg.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * description: 通过 SocketChannel 写出杨氏消息体
     *
     * @author yqingyu
     * DATE 2022/4/22
     */
    public void writeMessage(SocketChannel socketChannel, String msg) throws Exception {
        try {
            writeQyBytes(socketChannel, msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new Exception("WriteMsgError", e);
        }
    }

//    /**
//     * description: 通过 SocketChannel 写出杨氏消息体
//     *
//     * @author yqingyu
//     * DATE 2022/4/22
//     */
//    public static void writeMessage(SocketChannel socketChannel, QyMsg msg) throws Exception {
//        try {
//            writeQyBytes(socketChannel, msg.toString().getBytes(StandardCharsets.UTF_8));
//        } catch (Exception e) {
//            throw new Exception("WriteMsgError", e);
//        }
//    }


    public void writeQyBytes(SocketChannel socketChannel, byte[] bytes) throws Exception {

        bytes = getQyMsgBytes(bytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
    }


    /**
     * 写出带有用户名的信息
     * date 2022/5/7 1:12
     * return void
     */
    public void writeMessage(Socket socket, QyMsg msg) throws Exception {


        byte[] bytes = JSON.toJSONBytes(msg);

        byte[] qyMsgBytes = getQyMsgBytes(bytes);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(qyMsgBytes);
        outputStream.flush();
    }


    public QyMsg readMsg(Socket socket) throws Exception {
        byte[] bytes = readQyBytes(socket);
        return JSON.parseObject(bytes, QyMsg.class);
    }

    public QyMsg readMsg(Socket socket, int timeout) throws Exception {
        byte[] bytes = readQyBytes(socket, timeout);
        return JSON.parseObject(bytes, QyMsg.class);
    }


    /**
     * description: 通过 Socket 写出杨氏消息体
     *
     * @author yqingyu
     * DATE 2022/4/22
     */
    public void writeMessage(Socket socket, String msg) throws Exception {
        writeQyBytes(socket, msg.getBytes(StandardCharsets.UTF_8));
    }


    public void writeQyBytes(Socket socket, byte[] bytes) throws Exception {

        OutputStream outputStream = socket.getOutputStream();

        bytes = getQyMsgBytes(bytes);

        outputStream.write(bytes);
        outputStream.flush();
    }


    public QyMsg readMessage2(SocketChannel socketChannel) throws IOException {

        QyMsg qyMsgHeader = JSON.parseObject(readQyBytes(socketChannel), QyMsg.class);
        return qyMsgHeader;
    }

    /**
     * description: 读取 SocketChannel 中的杨氏消息体
     *
     * @author yqingyu
     * DATE 2022/4/22
     */
    public String readMessage(SocketChannel socketChannel) throws IOException {

        try {
            return new String(readQyBytes(socketChannel), StandardCharsets.UTF_8);
        } catch (Exception e) {
            socketChannel.close();
            e.printStackTrace();
            throw e;
        }
    }

    public byte[] readQyBytes(SocketChannel socketChannel) throws IOException {
        String len = new String(IoUtil.readBytes(socketChannel, BODY_LENGTH_LENGTH), StandardCharsets.UTF_8);

        return IoUtil.readBytes(socketChannel, Integer.parseInt(len, MSG_LENGTH_RADIX));
    }


    /**
     * description: 读取 Socket 中的杨氏消息体
     *
     * @author yqingyu
     * DATE 2022/4/22
     */
    public String readMessage(Socket socket) throws IOException {
        String msg;
        try {
            msg = new String(readQyBytes(socket), StandardCharsets.UTF_8);
        } catch (IOException e) {
            msg = "";
            log.error("消息读取异常 ", e);
        }
        return msg;
    }

    public byte[] readQyBytes(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        byte[] buff = IoUtil.readBytes(inputStream, BODY_LENGTH_LENGTH);
        String msgLength = new String(buff, StandardCharsets.UTF_8);

        buff = IoUtil.readBytes(inputStream, Integer.parseInt(msgLength, MSG_LENGTH_RADIX));
        return buff;
    }


    /**
     * description: 读取 Socket 中的杨氏消息体
     *
     * @author yqingyu
     * DATE 2022/4/22
     */
    public String readMessage(Socket socket, int timeout) throws Exception {
        String msg;
        try {
            msg = new String(readQyBytes(socket, timeout), StandardCharsets.UTF_8);
        } catch (IOException e) {
            msg = "";
            e.printStackTrace();
            throw e;
        }
        return msg;
    }

    public byte[] readQyBytes(Socket socket, int timeout) throws Exception {
        InputStream inputStream = socket.getInputStream();
        byte[] buff = IoUtil.readBytes(inputStream, BODY_LENGTH_LENGTH);
        String msgLength = new String(buff, StandardCharsets.UTF_8);

        buff = IoUtil.readBytes(inputStream, Integer.parseInt(msgLength, MSG_LENGTH_RADIX), timeout);
        return buff;
    }

    /**
     * 序列化写出
     *
     * @author YYJ
     * @version 1.0.0
     * @description
     */
    public void writeSerializable(SocketChannel socketChannel, QyMsg qyQyMsg) throws Exception {
        writeQyBytes(socketChannel, IoUtil.objToSerializBytes(qyQyMsg));
    }

    /**
     * 序列化写出
     *
     * @author YYJ
     * @version 1.0.0
     * @description
     */
    public void writeSerializable$(Socket socket, QyMsg qyQyMsg) throws Exception {
        writeQyBytes(socket, IoUtil.objToSerializBytes(qyQyMsg));
    }


    /**
     * 序列化读取
     *
     * @author YYJ
     * @version 1.0.0
     * @description
     */
    public QyMsg readSerializable(SocketChannel socketChannel) throws Exception {
        return IoUtil.deserializationObj(readQyBytes(socketChannel), QyMsg.class);
    }

    /**
     * 序列化读取
     *
     * @author YYJ
     * @version 1.0.0
     * @description
     */
    public QyMsg readSerializable(Socket socket) throws Exception {
        return IoUtil.deserializationObj(readQyBytes(socket), QyMsg.class);
    }

}
