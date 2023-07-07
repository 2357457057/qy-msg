package top.yqingyu.qymsg.netty;


public class ConnectionConfig {
    String host;
    int port;
    int poolMax;
    int poolMin;
    int radix;
    int bodyLengthMax;
    String name;
    String threadName;
    volatile long clearTime;

    private ConnectionConfig(Builder builder) {
        host = builder.host;
        port = builder.port;
        poolMax = builder.poolMax;
        poolMin = builder.poolMin;
        radix = builder.radix;
        clearTime = builder.clearTime;
        bodyLengthMax = builder.bodyLengthMax;
        name = builder.name;
        threadName = builder.threadName;
    }

    public static final class Builder {
        String host = "127.0.0.1";
        int port = 4729;
        int poolMax = 6;
        int poolMin = 2;
        int radix = 32;
        int bodyLengthMax = 1400;
        String name = "QyMsgClient";
        String threadName = "handle";
        volatile long clearTime = 30 * 60 * 1000;

        public Builder() {
        }

        public Builder host(String val) {
            host = val;
            return this;
        }

        public Builder port(int val) {
            port = val;
            return this;
        }

        public Builder poolMax(int val) {
            poolMax = val;
            return this;
        }

        public Builder poolMin(int val) {
            poolMin = val;
            return this;
        }

        public Builder radix(int val) {
            radix = val;
            return this;
        }

        public Builder clearTime(long val) {
            clearTime = val;
            return this;
        }

        public Builder bodyLengthMax(int val) {
            bodyLengthMax = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder threadName(String val) {
            threadName = val;
            return this;
        }

        public ConnectionConfig build() {
            return new ConnectionConfig(this);
        }
    }

    public void setClearTime(long clearTime) {
        this.clearTime = clearTime;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getPoolMax() {
        return poolMax;
    }

    public int getPoolMin() {
        return poolMin;
    }

    public int getRadix() {
        return radix;
    }

    public long getClearTime() {
        return clearTime;
    }

    public int getBodyLengthMax() {
        return bodyLengthMax;
    }

    public String getName() {
        return name;
    }

    public String getThreadName() {
        return threadName;
    }
}
