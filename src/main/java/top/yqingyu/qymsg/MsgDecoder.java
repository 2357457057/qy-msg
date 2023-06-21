package top.yqingyu.qymsg;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.qymsg.exception.IllegalQyMsgException;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.yqingyu.qymsg.Dict.*;

/**
 * 从socket、SocketChannel读取并解析QyMsg
 *
 * @author YYJ
 * @version 1.0.0
 * @ClassNameDecodeMsg
 * @createTime 2022年09月06日 10:36:00
 */

public class MsgDecoder {
    private static final Logger log = LoggerFactory.getLogger(MsgDecoder.class);
    private final MsgConnector connector;
    private final MsgTransfer transfer;

    public MsgDecoder(MsgTransfer transfer) {
        this.connector = transfer.connector;
        this.transfer = transfer;
    }

    /**
     * @author YYJ
     * @description 消息解析
     */
    public QyMsg decode(Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException, InterruptedException {
        InputStream inputStream = socket.getInputStream();
        byte[] readBytes = IoUtil.readBytes3(socket, HEADER_LENGTH, runFlag);
        if (!runFlag.get()) {
            return null;
        }
        String s = new String(readBytes, StandardCharsets.UTF_8);
        char $0 = s.charAt(MSG_TYPE_IDX);//msg  type
        char $1 = s.charAt(DATA_TYPE_IDX);//data type
        char $2 = s.charAt(SEGMENTATION_IDX);//是否分片
        String $3 = s.substring(MSG_LENGTH_IDX_START, MSG_LENGTH_IDX_END);     //后五位
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN($2);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法分片字符: " + $2, e);
        }
        if (segmentation) {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE($0);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的消息类型: " + s, e);
            }
            DataType dataType;
            try {
                dataType = MsgTransfer.CHAR_2_DATA_TYPE($1);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的数据类型: " + s, e);
            }

            QyMsg parse = new QyMsg(msgType, dataType);
            parse.setSegmentation(true);
            readBytes = IoUtil.readBytes3(socket, SEGMENTATION_INFO_LENGTH, runFlag);
            String segmentationInfo = new String(readBytes, StandardCharsets.UTF_8);
            parse.setPartition_id(segmentationInfo.substring(PARTITION_ID_IDX_START, PARTITION_ID_IDX_END));
            parse.setNumerator(Integer.parseInt(segmentationInfo.substring(NUMERATOR_IDX_START, NUMERATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            parse.setDenominator(Integer.parseInt(segmentationInfo.substring(DENOMINATOR_IDX_START, DENOMINATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            readBytes = IoUtil.readBytes3(socket, Integer.parseInt($3, transfer.MSG_LENGTH_RADIX), runFlag);
            parse.putMsg(readBytes);
            log.debug("part msg id: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
            return connector.merger(parse);
        } else {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE($0);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的消息类型: " + s, e);
            }
            switch (msgType) {
                case AC -> {
                    return AC_Decode($1, $3, socket, runFlag);
                }
                case HEART_BEAT -> {
                    return HEART_BEAT_Decode($0, $1, $2, $3, socket, runFlag);
                }
                case ERR_MSG -> {
                    return ERR_MSG_Decode($0, $1, $2, $3, socket, runFlag);
                }
                default -> {
                    return NORM_MSG_Decode($0, $1, $2, $3, socket, runFlag);
                }
            }
        }
    }

    /**
     * @author YYJ
     * @description 消息解析
     */
    public QyMsg decode(SocketChannel socketChannel, long sleep) throws IOException, ClassNotFoundException, InterruptedException {

        byte[] readBytes = IoUtil.readBytes(socketChannel, HEADER_LENGTH);
        Thread.sleep(sleep);
        String s = new String(readBytes, StandardCharsets.UTF_8);
        if (StringUtil.isNotBlank(s) && s.length() != HEADER_LENGTH) {
            throw new IllegalQyMsgException("消息头长度非法 消息头为：" + s);
        }
        char $0 = s.charAt(MSG_TYPE_IDX);//msg  type
        char $1 = s.charAt(DATA_TYPE_IDX);//data type
        char $2 = s.charAt(SEGMENTATION_IDX);//是否分片
        String $3 = s.substring(MSG_LENGTH_IDX_START, MSG_LENGTH_IDX_END);     //后五位

        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN($2);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法分片字符: " + $2, e);
        }

        if (segmentation) {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE($0);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的消息类型: " + s, e);
            }
            DataType dataType;
            try {
                dataType = MsgTransfer.CHAR_2_DATA_TYPE($1);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的数据类型: " + s, e);
            }

            QyMsg parse = new QyMsg(msgType, dataType);
            parse.setSegmentation(true);
            readBytes = IoUtil.readBytes(socketChannel, SEGMENTATION_INFO_LENGTH);
            String segmentationInfo = new String(readBytes, StandardCharsets.UTF_8);
            parse.setPartition_id(segmentationInfo.substring(PARTITION_ID_IDX_START, PARTITION_ID_IDX_END));
            parse.setNumerator(Integer.parseInt(segmentationInfo.substring(NUMERATOR_IDX_START, NUMERATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            parse.setDenominator(Integer.parseInt(segmentationInfo.substring(DENOMINATOR_IDX_START, DENOMINATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            readBytes = IoUtil.readBytes(socketChannel, Integer.parseInt($3, transfer.MSG_LENGTH_RADIX));
            parse.putMsg(readBytes);
            log.debug("part msg id: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
            return connector.merger(parse);
        } else {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE($0);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的消息类型: " + s, e);
            }
            switch (msgType) {
                case AC -> {
                    return AC_Decode($1, $3, socketChannel);
                }
                case HEART_BEAT -> {
                    return HEART_BEAT_Decode($0, $1, $2, $3, socketChannel);
                }
                case ERR_MSG -> {
                    return ERR_MSG_Decode($0, $1, $2, $3, socketChannel);
                }
                default -> {
                    return NORM_MSG_Decode($0, $1, $3, socketChannel);
                }
            }
        }
    }


    /**
     * @param msg_length 消息长度
     * @param socket     socket
     * @description 认证消息解析
     */
    private QyMsg AC_Decode(char data_type, String msg_length, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型:" + data_type, e);
        }

        if (Objects.requireNonNull(dataType) == DataType.OBJECT) {
            byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
            return IoUtil.deserializationObj(bytes, QyMsg.class);
        }
        byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
        return JSON.parseObject(bytes, QyMsg.class);
    }

    /**
     * 心跳消息组装
     */
    private QyMsg HEART_BEAT_Decode(char msg_type, char data_type, char segmentationC, String msg_length, Socket socket, AtomicBoolean runFlag) throws IOException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + msg_type, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + data_type, e);
        }
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(segmentationC);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法分片字符: " + segmentationC, e);
        }

        QyMsg qyMsg = new QyMsg(msgType, dataType);
        qyMsg.setSegmentation(segmentation);
        byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        return qyMsg;
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Decode(char msg_type, char data_type, char segmentation, String msg_length, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + msg_type, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型:" + data_type, e);
        }

        switch (dataType) {
            case STRING -> {
                byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
                String s = new String(bytes, StandardCharsets.UTF_8);
                String from = s.substring(0, CLIENT_ID_LENGTH);
                String msg = s.substring(CLIENT_ID_LENGTH);
                QyMsg qyMsg = new QyMsg(msgType, dataType);
                qyMsg.setFrom(from);
                qyMsg.putMsg(msg.getBytes(StandardCharsets.UTF_8));
                return qyMsg;
            }
            case OBJECT -> {
                byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
                return IoUtil.deserializationObj(bytes, QyMsg.class);
            }
            case STREAM -> {
                return streamDeal(msg_type, data_type, msg_length, socket, runFlag);
            }
            default -> { //JSON\FILE
                byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
                return JSON.parseObject(bytes, QyMsg.class);
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Decode(char msg_type, char data_type, char segmentation, String msg_length, Socket socket, AtomicBoolean runFlag) throws IOException {
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型:" + data_type, e);
        }
        if (DataType.JSON.equals(dataType)) {
            byte[] bytes = IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX), runFlag);
            return JSON.parseObject(bytes, QyMsg.class);
        } else {
            return streamDeal(msg_type, data_type, msg_length, socket, runFlag);
        }
    }

    /**
     * @author YYJ
     * @description 流类型数据处理
     */
    private QyMsg streamDeal(char msg_type, char data_type, String msg_length, Socket socket, AtomicBoolean runFlag) throws IOException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + msg_type, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + data_type, e);
        }
        QyMsg qyMsg = new QyMsg(msgType, dataType);
        byte[] bytes = IoUtil.readBytes3(socket, CLIENT_ID_LENGTH, runFlag);
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        qyMsg.putMsg(IoUtil.readBytes3(socket, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX) - CLIENT_ID_LENGTH, runFlag));
        return qyMsg;
    }

    /**
     * @param msg_length    消息长度
     * @param socketChannel 流
     * @description 认证消息解析
     */
    private QyMsg AC_Decode(char data_type, String msg_length, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型:" + data_type, e);
        }

        if (Objects.requireNonNull(dataType) == DataType.OBJECT) {
            byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
            return IoUtil.deserializationObj(bytes, QyMsg.class);
        }
        byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
        return JSON.parseObject(bytes, QyMsg.class);
    }

    /**
     * 心跳消息组装
     */
    private QyMsg HEART_BEAT_Decode(char msg_type, char data_type, char segmentationC, String msg_length, SocketChannel socketChannel) throws IOException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + msg_type, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + data_type, e);
        }
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(segmentationC);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法分片字符: " + segmentationC, e);
        }

        QyMsg qyMsg = new QyMsg(msgType, dataType);
        qyMsg.setSegmentation(segmentation);
        byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        return qyMsg;
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Decode(char msg_type, char data_type, String msg_length, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + msg_type, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + data_type, e);
        }
        switch (dataType) {
            case STRING -> {
                byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
                String s = new String(bytes, StandardCharsets.UTF_8);
                String from = s.substring(0, CLIENT_ID_LENGTH);
                String msg = s.substring(CLIENT_ID_LENGTH);
                QyMsg qyMsg = new QyMsg(msgType, dataType);
                qyMsg.setFrom(from);
                qyMsg.putMsg(msg);
                return qyMsg;
            }
            case OBJECT -> {
                byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
                return IoUtil.deserializationObj(bytes, QyMsg.class);
            }
            case STREAM -> {
                return streamDeal(msg_type, data_type, msg_length, socketChannel);
            }
            default -> { //JSON FILE
                byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
                return JSON.parseObject(bytes, QyMsg.class);
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Decode(char msg_type, char data_type, char segmentation, String msg_length, SocketChannel socketChannel) throws IOException {
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型:" + data_type, e);
        }
        if (DataType.JSON.equals(dataType)) {
            byte[] bytes = IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
            return JSON.parseObject(bytes, QyMsg.class);
        } else {
            return streamDeal(msg_type, data_type, msg_length, socketChannel);
        }
    }

    /**
     * @author YYJ
     * @description 流类型数据处理
     */
    private QyMsg streamDeal(char msg_type, char data_type, String msg_length, SocketChannel socketChannel) throws IOException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + msg_type, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + data_type, e);
        }
        QyMsg qyMsg = new QyMsg(msgType, dataType);
        byte[] bytes = IoUtil.readBytes(socketChannel, CLIENT_ID_LENGTH);
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        qyMsg.putMsg(IoUtil.readBytes(socketChannel, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX) - CLIENT_ID_LENGTH));
        return qyMsg;
    }
}
