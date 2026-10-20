/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.samples.ffi;

import com.baomidou.dynamic.datasource.tx.LocalTxUtil;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 跨越多次 {@code dd_tx_*} 调用的会话式事务。
 *
 * <p>dynamic-datasource 把事务的 xid 与连接都放在 ThreadLocal 里，而宿主语言的每次调用可能来自不同的线程 （tokio 的 blocking
 * 线程池、Starlette 的线程池、Node 的 worker），所以每个会话独占一个平台线程， 它的所有语句、提交与回滚都在这个线程上执行。空闲超过 {@code
 * txIdleTimeoutSeconds} 的会话会被自动回滚， 以免宿主进程忘记收尾时一直占着连接。
 */
final class TransactionSessions implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TransactionSessions.class);

    private static final ThreadFactory SESSION_THREADS =
            Thread.ofPlatform().name("dd-tx-", 1).daemon().factory();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final Duration idleTimeout;

    private final ScheduledExecutorService reaper =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform().name("dd-tx-reaper").daemon().factory());

    TransactionSessions(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
        reaper.scheduleWithFixedDelay(this::rollbackIdleSessions, 1, 1, TimeUnit.SECONDS);
    }

    /** 开启事务；返回的 txId 就是 dynamic-datasource 的 xid，便于与它的日志对照。 */
    String begin() throws Exception {
        var session = new Session();
        try {
            session.xid = session.call(LocalTxUtil::startTransaction);
        } catch (Exception e) {
            session.thread.shutdown();
            throw e;
        }
        sessions.put(session.xid, session);
        return session.xid;
    }

    <T> T run(String txId, Callable<T> work) throws Exception {
        var session = sessions.get(txId);
        if (session == null) {
            throw notFound(txId);
        }
        return session.call(work);
    }

    void commit(String txId) throws Exception {
        complete(remove(txId), LocalTxUtil::commit);
    }

    void rollback(String txId) throws Exception {
        complete(remove(txId), LocalTxUtil::rollback);
    }

    /** {@code dd_close} 时回滚所有尚未结束的会话。 */
    @Override
    public void close() {
        reaper.shutdownNow();
        sessions.keySet()
                .forEach(
                        txId -> {
                            var session = sessions.remove(txId);
                            if (session != null) {
                                rollbackQuietly(txId, session);
                            }
                        });
    }

    private void rollbackIdleSessions() {
        var now = System.nanoTime();
        sessions.forEach(
                (txId, session) -> {
                    if (session.idleLongerThan(idleTimeout, now)
                            && sessions.remove(txId, session)) {
                        log.warn(
                                "dynamic-datasource ffi rolled back transaction [{}] idle for more than {}",
                                txId,
                                idleTimeout);
                        rollbackQuietly(txId, session);
                    }
                });
    }

    private Session remove(String txId) {
        var session = sessions.remove(txId);
        if (session == null) {
            throw notFound(txId);
        }
        return session;
    }

    /** 在会话线程上提交或回滚，之后会话线程随即退出。 */
    private static void complete(Session session, Completion completion) throws Exception {
        try {
            session.call(
                    () -> {
                        try {
                            completion.apply(session.xid);
                        } finally {
                            session.completed = true;
                        }
                        return null;
                    });
        } finally {
            session.thread.shutdown();
        }
    }

    private static void rollbackQuietly(String txId, Session session) {
        try {
            complete(session, LocalTxUtil::rollback);
        } catch (Exception e) {
            log.warn("dynamic-datasource ffi failed to roll back transaction [{}]", txId, e);
        }
    }

    private static FacadeException notFound(String txId) {
        return new FacadeException(
                ErrorCode.TRANSACTION_NOT_FOUND,
                "transaction [%s] does not exist, has completed or timed out".formatted(txId));
    }

    /** {@link LocalTxUtil#commit} 或 {@link LocalTxUtil#rollback}。 */
    @FunctionalInterface
    private interface Completion {
        void apply(String xid) throws Exception;
    }

    private static final class Session {

        private final ExecutorService thread = Executors.newSingleThreadExecutor(SESSION_THREADS);

        private final AtomicInteger inFlight = new AtomicInteger();

        private volatile long lastUsedNanos = System.nanoTime();

        private volatile String xid;

        /** 只在会话线程上读写：提交或回滚之后才排到的语句不能再以自动提交的方式执行。 */
        private boolean completed;

        <T> T call(Callable<T> work) throws Exception {
            inFlight.incrementAndGet();
            try {
                return thread.submit(
                                () -> {
                                    if (completed) {
                                        throw notFound(xid);
                                    }
                                    return work.call();
                                })
                        .get();
            } catch (RejectedExecutionException _) {
                throw notFound(xid);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception cause) {
                    throw cause;
                }
                throw (Error) e.getCause();
            } finally {
                lastUsedNanos = System.nanoTime();
                inFlight.decrementAndGet();
            }
        }

        boolean idleLongerThan(Duration timeout, long now) {
            return inFlight.get() == 0 && now - lastUsedNanos > timeout.toNanos();
        }
    }
}
