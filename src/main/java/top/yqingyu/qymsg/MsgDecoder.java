package top.yqingyu.qymsg;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.qydata.DataMap;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.RadixUtil;
import top.yqingyu.qymsg.exception.IllegalQyMsgException;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.yqingyu.qymsg.Dict.*;

/**
 * 从socket、SocketChannel读取并解码QyMsg
 *
 * @author YYJ
 * @version 1.0.0
 * @Date 2022年09月06日 10:36:00
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
     * 消息解码
     *
     * @author YYJ
     */
    public QyMsg decode(Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        byte[] headerBytes = IoUtil.readBytes3(socket, HEADER_LENGTH, runFlag);
        if (!runFlag.get()) {
            return null;
        }
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(headerBytes[SEGMENTATION_IDX]);
        } catch (Exception e) {
            throw handleException(e, "非法分片标识", headerBytes);
        }
        if (segmentation) {
            int msgLength = getMsgLength(headerBytes);     //后五位
            QyMsg parse = createMsg(headerBytes);
            headerBytes = IoUtil.readBytes3(socket, SEGMENTATION_INFO_LENGTH, runFlag);
            setSegmentInfo(parse, headerBytes);
            headerBytes = IoUtil.readBytes3(socket, msgLength, runFlag);
            parse.putMsg(headerBytes);
            log.debug("PartMsgId: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
            return connector.merger(parse);
        } else {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE(headerBytes[MSG_TYPE_IDX]);
            } catch (Exception e) {
                throw handleException(e, "非法的消息类型", headerBytes);
            }
            switch (msgType) {
                case AC -> {
                    return AC_Decode(headerBytes, socket, runFlag);
                }
                case HEART_BEAT -> {
                    return HEART_BEAT_Decode(headerBytes, socket, runFlag);
                }
                case ERR_MSG -> {
                    return ERR_MSG_Decode(headerBytes, socket, runFlag);
                }
                default -> {
                    return NORM_MSG_Decode(headerBytes, socket, runFlag);
                }
            }
        }
    }

    /**
     * 消息解码
     *
     * @author YYJ
     */
    public QyMsg decode(SocketChannel socketChannel, long sleep) throws Exception {

        byte[] header = IoUtil.readBytes(socketChannel, HEADER_LENGTH);
        Thread.sleep(sleep);
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(header[SEGMENTATION_IDX]);
        } catch (Exception e) {
            throw handleException(e, "非法分片标识", header);
        }

        if (segmentation) {
            int length = getMsgLength(header);
            QyMsg parse = createMsg(header);
            header = IoUtil.readBytes(socketChannel, SEGMENTATION_INFO_LENGTH);
            setSegmentInfo(parse, header);
            header = IoUtil.readBytes(socketChannel, length);
            parse.putMsg(header);
            log.debug("PartMsgId: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
            return connector.merger(parse);
        } else {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE(header[MSG_TYPE_IDX]);
            } catch (Exception e) {
                throw handleException(e, "非法消息标识", header);
            }
            switch (msgType) {
                case AC -> {
                    return AC_Decode(header, socketChannel);
                }
                case HEART_BEAT -> {
                    return HEART_BEAT_Decode(header, socketChannel);
                }
                case ERR_MSG -> {
                    return ERR_MSG_Decode(header, socketChannel);
                }
                default -> {
                    return NORM_MSG_Decode(header, socketChannel);
                }
            }
        }
    }


    /**
     * 认证消息解码
     *
     * @param socket socket
     */
    private QyMsg AC_Decode(byte[] header, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = createMsg(header);
        if (Objects.requireNonNull(qyMsg.getDataType()) == DataType.OBJECT) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(IoUtil.readBytes3(socket, getMsgLength(header), runFlag), DataMap.class));
        }
        return qyMsg.setDataMap(JSON.parseObject(IoUtil.readBytes3(socket, getMsgLength(header), runFlag), DataMap.class));
    }

    /**
     * 心跳消息组装
     */
    private QyMsg HEART_BEAT_Decode(byte[] header, Socket socket, AtomicBoolean runFlag) throws IOException {
        QyMsg qyMsg = createMsg(header);
        byte[] bytes = IoUtil.readBytes3(socket, getMsgLength(header), runFlag);
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        return qyMsg;
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Decode(byte[] header, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = createMsg(header);
        switch (qyMsg.getDataType()) {
            case STRING -> {
                byte[] bytes = IoUtil.readBytes3(socket, getMsgLength(header), runFlag);
                String s = new String(bytes, StandardCharsets.UTF_8);
                String from = s.substring(0, CLIENT_ID_LENGTH);
                String msg = s.substring(CLIENT_ID_LENGTH);
                qyMsg.setFrom(from);
                qyMsg.putMsg(msg.getBytes(StandardCharsets.UTF_8));
                return qyMsg;
            }
            case OBJECT -> {
                return qyMsg.setDataMap(IoUtil.deserializationObj(IoUtil.readBytes3(socket, getMsgLength(header), runFlag), DataMap.class));
            }
            case STREAM -> {
                return streamDeal(header, socket, runFlag);
            }
            default -> { //JSON\FILE
                return qyMsg.setDataMap(JSON.parseObject(IoUtil.readBytes3(socket, getMsgLength(header), runFlag), DataMap.class));
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Decode(byte[] header, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = createMsg(header);
        if (DataType.JSON.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(JSON.parseObject(IoUtil.readBytes3(socket, getMsgLength(header), runFlag), DataMap.class));
        } else if (DataType.OBJECT.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(IoUtil.readBytes3(socket, getMsgLength(header), runFlag), DataMap.class));
        } else {
            return streamDeal(header, socket, runFlag);
        }
    }

    /**
     * 流类型数据处理
     *
     * @author YYJ
     */
    private QyMsg streamDeal(byte[] header, Socket socket, AtomicBoolean runFlag) throws IOException {
        QyMsg qyMsg = createMsg(header);
        byte[] bytes = IoUtil.readBytes3(socket, CLIENT_ID_LENGTH, runFlag);
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        qyMsg.putMsg(IoUtil.readBytes3(socket, getMsgLength(header) - CLIENT_ID_LENGTH, runFlag));
        return qyMsg;
    }

    /**
     * 认证消息解码
     */
    private QyMsg AC_Decode(byte[] header, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = createMsg(header);
        if (Objects.requireNonNull(qyMsg.getDataType()).equals(DataType.OBJECT)) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(IoUtil.readBytes(socketChannel, getMsgLength(header)), DataMap.class));
        }
        return qyMsg.setDataMap(JSON.parseObject(IoUtil.readBytes(socketChannel, getMsgLength(header)), DataMap.class));
    }

    /**
     * 心跳消息组装
     */
    private QyMsg HEART_BEAT_Decode(byte[] header, SocketChannel socketChannel) throws IOException {
        QyMsg qyMsg = createMsg(header);
        byte[] bytes = IoUtil.readBytes(socketChannel, getMsgLength(header));
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        return qyMsg;
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Decode(byte[] header, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = createMsg(header);
        switch (qyMsg.getDataType()) {
            case STRING -> {
                byte[] bytes = IoUtil.readBytes(socketChannel, getMsgLength(header));
                String s = new String(bytes, StandardCharsets.UTF_8);
                String from = s.substring(0, CLIENT_ID_LENGTH);
                String msg = s.substring(CLIENT_ID_LENGTH);
                qyMsg.setFrom(from);
                qyMsg.putMsg(msg);
                return qyMsg;
            }
            case OBJECT -> {
                return qyMsg.setDataMap(IoUtil.deserializationObj(IoUtil.readBytes(socketChannel, getMsgLength(header)), DataMap.class));
            }
            case STREAM -> {
                return streamDeal(header, socketChannel);
            }
            default -> { //JSON FILE
                return qyMsg.setDataMap(JSON.parseObject(IoUtil.readBytes(socketChannel, getMsgLength(header)), DataMap.class));
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Decode(byte[] header, SocketChannel socketChannel) throws Exception {
        QyMsg qyMsg = createMsg(header);
        if (DataType.JSON.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(JSON.parseObject(IoUtil.readBytes(socketChannel, getMsgLength(header)), DataMap.class));
        } else if (DataType.OBJECT.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(IoUtil.readBytes(socketChannel, getMsgLength(header)), DataMap.class));
        } else {
            return streamDeal(header, socketChannel);
        }
    }

    /**
     * 流类型数据处理
     *
     * @author YYJ
     */
    private QyMsg streamDeal(byte[] header, SocketChannel socketChannel) throws IOException {
        QyMsg qyMsg = createMsg(header);
        byte[] bytes = IoUtil.readBytes(socketChannel, CLIENT_ID_LENGTH);
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        qyMsg.putMsg(IoUtil.readBytes(socketChannel, getMsgLength(header) - CLIENT_ID_LENGTH));
        return qyMsg;
    }

    public IllegalQyMsgException handleException(Exception e, String s, byte[] array) {
        return new IllegalQyMsgException(e, "{} arr:{} str:{}", s, Arrays.toString(array), new String(array, StandardCharsets.UTF_8));
    }

    public int getMsgLength(byte[] array) {
        try {
            return RadixUtil.byte2Radix(ArrayUtil.subarray(array, MSG_LENGTH_IDX_START, MSG_LENGTH_IDX_END), transfer.MSG_LENGTH_RADIX);
        } catch (Exception e) {
            log.error("消息长度解析报错{} {}", new String(array, StandardCharsets.UTF_8), array, e);
            throw e;
        }
    }

    public void setSegmentInfo(QyMsg msg, byte[] array) {
        try {
            msg.setPartition_id(new String(array, PARTITION_ID_IDX_START, PARTITION_ID_IDX_END, StandardCharsets.UTF_8));
            msg.setNumerator(RadixUtil.byte2Radix(ArrayUtil.subarray(array, NUMERATOR_IDX_START, NUMERATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            msg.setDenominator(RadixUtil.byte2Radix(ArrayUtil.subarray(array, DENOMINATOR_IDX_START, DENOMINATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            msg.setSegmentation(true);
        } catch (Exception e) {
            log.error("SegmentInfo解析异常 {} {}", array, new String(array, StandardCharsets.UTF_8));
        }
    }

    public QyMsg createMsg(byte[] header) {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(header[MSG_TYPE_IDX]);
        } catch (Exception e) {
            throw handleException(e, "非法的消息标识", header);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(header[DATA_TYPE_IDX]);
        } catch (Exception e) {
            throw handleException(e, "非法的数据标识", header);
        }
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(header[SEGMENTATION_IDX]);
        } catch (Exception e) {
            throw handleException(e, "非法分片字符", header);
        }
        QyMsg qyMsg = new QyMsg(msgType, dataType);
        qyMsg.setFrom(new String(header, MSG_FROM_IDX_START, CLIENT_ID_LENGTH, StandardCharsets.UTF_8));
        qyMsg.setSegmentation(segmentation);
        return qyMsg;
    }
}
