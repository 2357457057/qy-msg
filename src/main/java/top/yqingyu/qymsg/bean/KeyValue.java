package top.yqingyu.qymsg.bean;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author YYJ
 * @version 1.0.0
 * @ClassNameextra.bean.KeyValue
 * @description
 * @createTime 2023年02月14日 22:57:00
 */
public class KeyValue<V> implements Serializable {
    @Serial
    private static final long serialVersionUID = 704921303243464809L;
    private String key;
    private V val;
    private OperatingState operatingState;
    private DataType dataType;

    public KeyValue() {
    }

    public KeyValue(DataType dataType) {
        this.dataType = dataType;
    }


    public DataType getDataType() {
        return dataType;
    }

    public OperatingState getOperatingState() {
        return operatingState;
    }

    public void setOperatingState(OperatingState operatingState) {
        this.operatingState = operatingState;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public V getVal() {
        return val;
    }

    public void setVal(V val) {
        this.val = val;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public enum DataType {
        STRING("String"),
        SET("Set"),
        QUEUE("Queue"),
        STACK("Stack"),
        OTHER("other");

        private final String name;

        DataType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum OperatingState {
        ADD,
        REMOVE,
        GET,
        CREATE,
        POP,

    }
}
