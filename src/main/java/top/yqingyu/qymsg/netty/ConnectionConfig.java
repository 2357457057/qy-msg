package top.yqingyu.qymsg.netty;


public class ConnectionConfig {
    String host;
    int port;
    int poolMax;
    int poolMin;
    int radix;
    volatile long clearTime;
    volatile int bodyLengthMax;

    private ConnectionConfig(Builder builder) {
        host = builder.host;
        port = builder.port;
        poolMax = builder.poolMax;
        poolMin = builder.poolMin;
        radix = builder.radix;
        clearTime = builder.clearTime;
        bodyLengthMax = builder.bodyLengthMax;
    }

    public static final class Builder {
        String host = "127.0.0.1";
        int port = 4729;
        int poolMax = 6;
        int poolMin = 2;
        int radix = 32;
        volatile long clearTime = 30 * 60 * 1000;
        volatile int bodyLengthMax = 1400;

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

        public ConnectionConfig build() {
            return new ConnectionConfig(this);
        }
    }

    public void setClearTime(long clearTime) {
        this.clearTime = clearTime;
    }

    public void setBodyLengthMax(int bodyLengthMax) {
        this.bodyLengthMax = bodyLengthMax;
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
}
