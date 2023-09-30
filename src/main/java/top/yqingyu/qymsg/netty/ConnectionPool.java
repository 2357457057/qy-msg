package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.exception.QyException;
import top.yqingyu.qymsg.DataType;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPool {
    public static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    MsgClient client;
    ConnectionConfig config;
    private final ConcurrentLinkedQueue<Connection> CONNECT_QUEUE;
    private final ConcurrentHashMap<Integer, Connection> CONNECT_MAP;
    private final ReentrantLock genConnectionLock = new ReentrantLock();
    final CyclicBarrier connBarrier = new CyclicBarrier(2);
    private volatile boolean init = false;

    ConnectionPool(MsgClient client) {
        this.config = client.config;
        this.client = client;
        CONNECT_QUEUE = new ConcurrentLinkedQueue<>();
        CONNECT_MAP = new ConcurrentHashMap<>(config.poolMax);
    }

    private void init() throws Exception {
        if (!init) {
            try {
                genConnectionLock.lock();
                if (!init) {
                    QyMsg qyMsg = new QyMsg(MsgType.NORM_MSG, DataType.JSON);
                    qyMsg.putMsg("connection closed");
                    for (int i = 0; i < config.poolMin; i++) {
                        connect0();
                    }
                    keep.start(this);
                    init = true;
                }
            } finally {
                genConnectionLock.unlock();
            }
        }
    }

    void pushConnection(ChannelHandlerContext context) throws Exception {
        Connection connection = new Connection(context);
        CONNECT_MAP.put(connection.getHash(), connection);
        CONNECT_QUEUE.add(connection);
        connBarrier.await();
    }

    void putMsg(ChannelHandlerContext context, QyMsg msg) throws Exception {
        Connection connection = CONNECT_MAP.get(context.hashCode());
        connection.put(msg);
    }

    public Connection getConnection() throws Exception {
        init();
        Connection take;
        if (CONNECT_MAP.size() < config.poolMax) {
            take = getConnection0();
        } else {
            take = CONNECT_QUEUE.poll();
        }
        if (take != null && take.isClosed()) {
            CONNECT_MAP.clear();
            CONNECT_QUEUE.clear();
            take = null;
        }
        while (take == null) {
            Thread.sleep(0);
            take = getConnection0();
            if (take != null && !CONNECT_MAP.containsValue(take)) {
                take = null;
            }
            if (take != null && take.isClosed()) {
                CONNECT_MAP.clear();
                CONNECT_QUEUE.clear();
                take = null;
            }
        }
        CONNECT_QUEUE.add(take);
        return take;
    }

    private Connection getConnection0() throws Exception {
        try {
            genConnectionLock.lock();
            if (CONNECT_MAP.size() < config.poolMax) {
                connect0();
            }
            return CONNECT_QUEUE.poll();
        } finally {
            genConnectionLock.unlock();
        }

    }

    private void connect0() throws Exception {
        client.bootstrap.connect(config.host, config.port);
        try {
            connBarrier.await(5, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException){
            throw new QyException("connect time out");
        }
    }

    static class keep implements Runnable {
        private final ConcurrentHashMap<Integer, Connection> CONNECT_MAP;
        private final QyMsg HEART;
        private final long sleepTime;

        keep(ConnectionPool pool) {
            this.CONNECT_MAP = pool.CONNECT_MAP;
            sleepTime = Constants.noOpMaxTime / 1000_000L / 4 * 3;
            HEART = pool.client.HEART_BEAT;
        }

        static void start(ConnectionPool pool) {
            Thread thread = new Thread(new keep(pool));
            thread.setName("keep-live");
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(sleepTime);
                    CONNECT_MAP.forEach((i, c) -> {
                        if (c.needKeep()) {
                            try {
                                c.getLock.lock();
                                if (c.needKeep()) {
                                    c.write(HEART);
                                }
                            } catch (Exception e) {
                                CONNECT_MAP.remove(i);
                            } finally {
                                c.getLock.unlock();
                            }
                        }
                    });
                } catch (Exception ignore) {
                }
            }
        }
    }
}
