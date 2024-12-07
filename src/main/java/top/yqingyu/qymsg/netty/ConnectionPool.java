package top.yqingyu.qymsg.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import top.yqingyu.qymsg.QyMsg;
import top.yqingyu.qymsg.exception.ConnectTimeOutException;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPool {
    public static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    MsgClient client;
    ConnectionConfig config;
    private final LinkedBlockingQueue<Connection> CONNECT_QUEUE;
    private final ConcurrentHashMap<Integer, Connection> CONNECT_MAP;
    private final ReentrantLock genConnectionLock = new ReentrantLock();
    final CyclicBarrier connBarrier = new CyclicBarrier(2);

    ConnectionPool(MsgClient client) {
        this.config = client.config;
        this.client = client;
        CONNECT_QUEUE = new LinkedBlockingQueue<>();
        CONNECT_MAP = new ConcurrentHashMap<>(config.poolMax);
        keep.start(this);
    }

    void putMsg(ChannelHandlerContext context, QyMsg msg) throws Exception {
        Connection connection = CONNECT_MAP.get(context.hashCode());
        connection.put(msg);
    }

    public Connection getConnection() throws Exception {
        Connection take;
        if (CONNECT_MAP.size() < config.poolMin) {
            take = getConnection0();
        } else if (CONNECT_MAP.size() < config.poolMax && !CONNECT_QUEUE.isEmpty()) {
            take = CONNECT_QUEUE.poll();
        } else if (CONNECT_MAP.size() < config.poolMax) {
            return getConnection0();
        } else {
            return CONNECT_QUEUE.take();
        }
        if (take != null && take.isClosed()) {
            CONNECT_MAP.clear();
            CONNECT_QUEUE.clear();
            take = null;
        }
        if (take != null)
            return take;
        return getConnection0();
    }

    public void returnConnection(Connection connection) {
        if (connection == null) return;
        if (connection.isClosed()) {
            CONNECT_MAP.remove(connection.getHash());
        } else {
            CONNECT_QUEUE.add(connection);
        }
    }

    void pushConnection(Connection connection) throws Exception {
        CONNECT_MAP.put(connection.getHash(), connection);
        CONNECT_QUEUE.add(connection);
        connBarrier.await();
    }

    private Connection getConnection0() throws Exception {
        try {
            genConnectionLock.lock();
            if (CONNECT_MAP.size() == config.poolMax) {
                return CONNECT_QUEUE.take();
            }
            client.bootstrap.connect(config.host, config.port);
            try {
                connBarrier.await(5, TimeUnit.SECONDS);
            } catch (TimeoutException timeoutException) {
                connBarrier.reset();
                throw new ConnectTimeOutException("connect time out");
            }
            return CONNECT_QUEUE.take();
        } finally {
            genConnectionLock.unlock();
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
