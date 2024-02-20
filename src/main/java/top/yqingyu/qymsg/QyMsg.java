package top.yqingyu.qymsg;

import top.yqingyu.common.qydata.DataMap;
import top.yqingyu.common.utils.UUIDUtil;

import java.io.Serializable;

import static top.yqingyu.qymsg.Dict.MSG_ID;
import static top.yqingyu.qymsg.Dict.QYMSG;

/**
 * QyMsg 传输对象
 *
 * @author YYJ
 * @version 1.0.0
 * @ClassNameQyMessage
 * @createTime 2022年07月12日 01:13:00
 */
@SuppressWarnings("all")
public class QyMsg implements Serializable {
    private final static long serialVersionUID = -1854823182151532168L;
    private MsgType msgType;
    private DataType dataType;

    //来自谁的消息
    private String from;

    //发给谁的消息
    private String to;

    //是否分区
    private boolean segmentation;

    //碎片ID
    private String partition_id;

    private Integer numerator;
    //分片
    private Integer denominator;
    private DataMap dataMap = new DataMap(); //具体消息

    public QyMsg(MsgType msgType, DataType dataType) {
        this.msgType = msgType;
        this.dataType = dataType;
    }

    public void putMsgId(String msgId) {
        this.dataMap.put(MSG_ID, msgId);
    }

    public String gainMsgId() {
        return (String) this.dataMap.get(MSG_ID);
    }

    public String genMsgId() {
        String s = UUIDUtil.randomUUID().toString2();
        this.dataMap.put(MSG_ID, s);
        return s;
    }

    public void putMsg(String msg) {
        this.dataMap.put(QYMSG, msg);
    }

    public void putMsg(Object msg) {
        this.dataMap.put("MSG", msg);

    }

    public QyMsg putMsgData(String msgKey, String msgValue) {
        this.dataMap.put(msgKey, msgValue);
        return this;
    }

    public QyMsg putMsgData(String msgKey, Object msgValue) {
        this.dataMap.put(msgKey, msgValue);
        return this;
    }

    public QyMsg clone() {
        QyMsg clone = new QyMsg(this.msgType, this.dataType);
        clone.from = this.from;
        clone.to = this.to;
        return clone;
    }

    public MsgType getMsgType() {
        return this.msgType;
    }

    public DataType getDataType() {
        return this.dataType;
    }

    public String getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }

    public boolean isSegmentation() {
        return this.segmentation;
    }

    public String getPartition_id() {
        return this.partition_id;
    }

    public Integer getNumerator() {
        return this.numerator;
    }

    public Integer getDenominator() {
        return this.denominator;
    }

    public DataMap Data() {
        return this.dataMap;
    }

    public DataMap getDataMap() {
        return this.dataMap;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setSegmentation(boolean segmentation) {
        this.segmentation = segmentation;
    }

    public void setPartition_id(String partition_id) {
        this.partition_id = partition_id;
    }

    public void setNumerator(Integer numerator) {
        this.numerator = numerator;
    }

    public void setDenominator(Integer denominator) {
        this.denominator = denominator;
    }

    public QyMsg setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
        return this;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

    public QyMsg(MsgType msgType, DataType dataType, String from, String to, boolean segmentation, String partition_id, Integer numerator, Integer denominator, DataMap dataMap) {
        this.msgType = msgType;
        this.dataType = dataType;
        this.from = from;
        this.to = to;
        this.segmentation = segmentation;
        this.partition_id = partition_id;
        this.numerator = numerator;
        this.denominator = denominator;
        this.dataMap = dataMap;
    }
}
