package top.yqingyu.qymsg.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.qymsg.QyMsg;
import top.yqingyu.qymsg.netty.Constants;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPool {
    public static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    MsgClient client;
    ConnectionConfig config;
    private final ConcurrentLinkedQueue<Connection> CONNECT_QUEUE;
    private final ConcurrentHashMap<Integer, Connection> CONNECT_MAP;
    private final ReentrantLock genConnectionLock = new ReentrantLock();
    private keep keepLive;
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
                    for (int i = 0; i < config.poolMin; i++) {
                        connect0();
                    }
                    keepLive = keep.start(this);
                    init = true;
                }
            } finally {
                genConnectionLock.unlock();
            }
        }
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
        Connection connection = new Connection(config);
        CONNECT_QUEUE.add(connection);
        CONNECT_MAP.put(connection.hashCode(), connection);
    }

    public void shutdown() {
        keepLive.shutdown();
        CONNECT_MAP.forEach((i, c) -> {
            try {
                c.close();
            } catch (IOException e) {
                logger.error("", e);
            }
        });

    }

    static class keep implements Runnable {
        private final ConcurrentHashMap<Integer, Connection> CONNECT_MAP;
        private final QyMsg HEART;
        private final long sleepTime;
        private Thread th;

        keep(ConnectionPool pool) {
            this.CONNECT_MAP = pool.CONNECT_MAP;
            sleepTime = Constants.noOpMaxTime / 1000_000L / 4 * 3;
            HEART = pool.client.HEART_BEAT;
        }

        static keep start(ConnectionPool pool) {
            keep keep = new keep(pool);
            Thread thread = new Thread(keep);
            thread.setName("keep-live");
            thread.setDaemon(true);
            keep.th = thread;
            thread.start();
            return keep;
        }

        void shutdown() {
            th.interrupt();
            th.stop();
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
