package com.jetbrains.youtrack.db.internal.common.thread;

import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The same as thread {@link ThreadPoolExecutor} but also logs all exceptions happened inside of the
 * tasks which caused tasks to stop.
 */
public class ThreadPoolExecutorWithLogging extends ThreadPoolExecutor {

  public ThreadPoolExecutorWithLogging(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
  }

  @SuppressWarnings("unused")
  public ThreadPoolExecutorWithLogging(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory,
      RejectedExecutionHandler handler) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);

    if ((t == null) && (r instanceof Future<?> future)) {
      try {
        future.get();
      } catch (CancellationException ce) {
        // ignore it we cancel tasks on shutdown that is normal
      } catch (ExecutionException ee) {
        t = ee.getCause();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt(); // ignore/reset
      }
    }

    if (t != null) {
      final Thread thread = Thread.currentThread();
      LogManager.instance().error(this, "Exception in thread '%s'", t, thread.getName());
    }
  }
}
