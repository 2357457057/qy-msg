package top.yqingyu.qymsg.bean;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author YYJ
 * @version 1.0.0
 * @ClassNameextra.bean.StringKey
 * @description
 * @createTime 2023年02月14日 23:26:00
 */
public class StringKey extends KeyValue<String> implements Serializable {
    @Serial
    private static final long serialVersionUID = 704921303243464809L;

    public StringKey() {
        super(DataType.STRING);
    }

    public static StringKey add(String key, String val) {
        return createKeyCmd(key, val, OperatingState.ADD);
    }

    public static StringKey get(String key) {
        return createKeyCmd(key, null, OperatingState.GET);
    }

    public static StringKey rm(String key) {
        return createKeyCmd(key, null, OperatingState.REMOVE);
    }

    public static StringKey createKeyCmd(String key, String val, OperatingState opeState) {
        StringKey stringKey = new StringKey();
        stringKey.setOperatingState(opeState);
        stringKey.setKey(key);
        stringKey.setVal(val);
        return stringKey;
    }
}
