package top.yqingyu.qymsg;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.qydata.DataMap;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.RadixUtil;
import top.yqingyu.qymsg.exception.IllegalQyMsgException;
import top.yqingyu.qymsg.serialize.KryoSerializer;

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
        QyMsg parse = createMsg(headerBytes);

        if (parse.isSegmentation()) {
            headerBytes = IoUtil.readBytes3(socket, SEGMENTATION_INFO_LENGTH, runFlag);
            setSegmentInfo(parse, headerBytes);
            parse.putMsg(IoUtil.readBytes3(socket, parse.getBodySize(), runFlag));
            log.debug("PartMsgId: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
            return connector.merger(parse);
        } else {
            switch (parse.getMsgType()) {
                case NORM_MSG -> {
                    return NORM_MSG_Decode(parse, socket, runFlag);
                }
                case AC -> {
                    return AC_Decode(parse, socket, runFlag);
                }
                case HEART_BEAT -> {
                    return parse;
                }
                default -> {
                    return ERR_MSG_Decode(parse, socket, runFlag);
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

        QyMsg parse = createMsg(header);

        if (parse.isSegmentation()) {
            header = IoUtil.readBytes(socketChannel, SEGMENTATION_INFO_LENGTH);
            setSegmentInfo(parse, header);
            header = IoUtil.readBytes(socketChannel, parse.getBodySize());
            parse.putMsg(header);
            log.debug("PartMsgId: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
            return connector.merger(parse);
        }
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(header[MSG_TYPE_IDX]);
        } catch (Exception e) {
            throw handleException(e, "非法消息标识", header);
        }
        switch (msgType) {
            case AC -> {
                return AC_Decode(parse, socketChannel);
            }
            case HEART_BEAT -> {
                return parse;
            }
            case ERR_MSG -> {
                return ERR_MSG_Decode(parse, socketChannel);
            }
            default -> {
                return NORM_MSG_Decode(parse, socketChannel);
            }
        }

    }


    /**
     * 认证消息解码
     *
     * @param socket socket
     */
    private QyMsg AC_Decode(QyMsg qyMsg, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        byte[] bytes = IoUtil.readBytes3(socket, qyMsg.getBodySize(), runFlag);
        if (DataType.KRYO5.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(KryoSerializer.INSTANCE.decode(bytes));
        }
        if (DataType.OBJECT.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
        }
        return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Decode(QyMsg qyMsg, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        DataType dataType = qyMsg.getDataType();
        byte[] bytes = IoUtil.readBytes3(socket, qyMsg.getBodySize(), runFlag);
        switch (dataType) {
            case KRYO5 -> {
                return qyMsg.setDataMap(KryoSerializer.INSTANCE.decode(bytes));
            }
            case STRING -> {
                qyMsg.putMsg(new String(bytes, StandardCharsets.UTF_8));
                return qyMsg;
            }
            case OBJECT -> {
                return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
            }
            case STREAM -> {
                qyMsg.putMsg(bytes);
                return qyMsg;
            }
            default -> { //JSON\FILE
                return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Decode(QyMsg qyMsg, Socket socket, AtomicBoolean runFlag) throws IOException, ClassNotFoundException {
        return NORM_MSG_Decode(qyMsg, socket, runFlag);
    }

    /**
     * 认证消息解码
     */
    private QyMsg AC_Decode(QyMsg qyMsg, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        byte[] bytes = IoUtil.readBytes(socketChannel, qyMsg.getBodySize());
        if (DataType.KRYO5.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(KryoSerializer.INSTANCE.decode(bytes));
        }
        if (DataType.OBJECT.equals(qyMsg.getDataType())) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
        }
        return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
    }


    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Decode(QyMsg qyMsg, SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        byte[] bytes = IoUtil.readBytes(socketChannel, qyMsg.getBodySize());
        switch (qyMsg.getDataType()) {
            case KRYO5 -> {
                return qyMsg.setDataMap(KryoSerializer.INSTANCE.decode(bytes));
            }
            case STRING -> {
                qyMsg.putMsg(new String(bytes, StandardCharsets.UTF_8));
                return qyMsg;
            }
            case OBJECT -> {
                return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
            }
            case STREAM -> {
                qyMsg.putMsg(bytes);
                return qyMsg;
            }
            default -> { //JSON FILE
                return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Decode(QyMsg qyMsg, SocketChannel socketChannel) throws Exception {
        return NORM_MSG_Decode(qyMsg, socketChannel);
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
        qyMsg.setBodySize(getMsgLength(header));
        return qyMsg;
    }
}
