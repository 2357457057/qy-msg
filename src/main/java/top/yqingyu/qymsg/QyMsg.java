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

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof QyMsg)) {
            return false;
        } else {
            QyMsg other = (QyMsg) o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.isSegmentation() != other.isSegmentation()) {
                return false;
            } else {
                label109:
                {
                    Object this$numerator = this.getNumerator();
                    Object other$numerator = other.getNumerator();
                    if (this$numerator == null) {
                        if (other$numerator == null) {
                            break label109;
                        }
                    } else if (this$numerator.equals(other$numerator)) {
                        break label109;
                    }

                    return false;
                }

                label102:
                {
                    Object this$denominator = this.getDenominator();
                    Object other$denominator = other.getDenominator();
                    if (this$denominator == null) {
                        if (other$denominator == null) {
                            break label102;
                        }
                    } else if (this$denominator.equals(other$denominator)) {
                        break label102;
                    }

                    return false;
                }

                Object this$msgType = this.getMsgType();
                Object other$msgType = other.getMsgType();
                if (this$msgType == null) {
                    if (other$msgType != null) {
                        return false;
                    }
                } else if (!this$msgType.equals(other$msgType)) {
                    return false;
                }

                label88:
                {
                    Object this$dataType = this.getDataType();
                    Object other$dataType = other.getDataType();
                    if (this$dataType == null) {
                        if (other$dataType == null) {
                            break label88;
                        }
                    } else if (this$dataType.equals(other$dataType)) {
                        break label88;
                    }

                    return false;
                }

                Object this$from = this.getFrom();
                Object other$from = other.getFrom();
                if (this$from == null) {
                    if (other$from != null) {
                        return false;
                    }
                } else if (!this$from.equals(other$from)) {
                    return false;
                }

                label74:
                {
                    Object this$to = this.getTo();
                    Object other$to = other.getTo();
                    if (this$to == null) {
                        if (other$to == null) {
                            break label74;
                        }
                    } else if (this$to.equals(other$to)) {
                        break label74;
                    }

                    return false;
                }

                Object this$partition_id = this.getPartition_id();
                Object other$partition_id = other.getPartition_id();
                if (this$partition_id == null) {
                    if (other$partition_id != null) {
                        return false;
                    }
                } else if (!this$partition_id.equals(other$partition_id)) {
                    return false;
                }

                Object this$dataMap = this.getDataMap();
                Object other$dataMap = other.getDataMap();
                if (this$dataMap == null) {
                    if (other$dataMap != null) {
                        return false;
                    }
                } else if (!this$dataMap.equals(other$dataMap)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof QyMsg;
    }

    public int hashCode() {
        boolean PRIME = true;
        int result = 1;
        result = result * 59 + (this.isSegmentation() ? 79 : 97);
        Object $numerator = this.getNumerator();
        result = result * 59 + ($numerator == null ? 43 : $numerator.hashCode());
        Object $denominator = this.getDenominator();
        result = result * 59 + ($denominator == null ? 43 : $denominator.hashCode());
        Object $msgType = this.getMsgType();
        result = result * 59 + ($msgType == null ? 43 : $msgType.hashCode());
        Object $dataType = this.getDataType();
        result = result * 59 + ($dataType == null ? 43 : $dataType.hashCode());
        Object $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : $from.hashCode());
        Object $to = this.getTo();
        result = result * 59 + ($to == null ? 43 : $to.hashCode());
        Object $partition_id = this.getPartition_id();
        result = result * 59 + ($partition_id == null ? 43 : $partition_id.hashCode());
        Object $dataMap = this.getDataMap();
        result = result * 59 + ($dataMap == null ? 43 : $dataMap.hashCode());
        return result;
    }

    public String toString() {
        MsgType var10000 = this.getMsgType();
        return "QyMsg(msgType=" + var10000 + ", dataType=" + this.getDataType() + ", from=" + this.getFrom() + ", to=" + this.getTo() + ", segmentation=" + this.isSegmentation() + ", partition_id=" + this.getPartition_id() + ", numerator=" + this.getNumerator() + ", denominator=" + this.getDenominator() + ", dataMap=" + this.getDataMap() + ")";
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
