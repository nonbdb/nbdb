/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core;

import com.jetbrains.youtrack.db.internal.common.directmemory.OByteBufferPool;
import com.jetbrains.youtrack.db.internal.common.directmemory.ODirectMemoryAllocator;
import com.jetbrains.youtrack.db.internal.common.io.FileUtils;
import com.jetbrains.youtrack.db.internal.common.listener.OListenerManger;
import com.jetbrains.youtrack.db.internal.common.log.LogManager;
import com.jetbrains.youtrack.db.internal.common.parser.OSystemVariableResolver;
import com.jetbrains.youtrack.db.internal.common.profiler.AbstractProfiler;
import com.jetbrains.youtrack.db.internal.common.profiler.OProfiler;
import com.jetbrains.youtrack.db.internal.common.profiler.ProfilerStub;
import com.jetbrains.youtrack.db.internal.common.util.OClassLoaderHelper;
import com.jetbrains.youtrack.db.internal.core.cache.OLocalRecordCacheFactory;
import com.jetbrains.youtrack.db.internal.core.cache.OLocalRecordCacheFactoryImpl;
import com.jetbrains.youtrack.db.internal.core.config.GlobalConfiguration;
import com.jetbrains.youtrack.db.internal.core.conflict.ORecordConflictStrategyFactory;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseLifecycleListener;
import com.jetbrains.youtrack.db.internal.core.db.ODatabaseThreadLocalFactory;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBEmbedded;
import com.jetbrains.youtrack.db.internal.core.db.YouTrackDBInternal;
import com.jetbrains.youtrack.db.internal.core.engine.OEngine;
import com.jetbrains.youtrack.db.internal.core.record.ORecordFactoryManager;
import com.jetbrains.youtrack.db.internal.core.shutdown.OShutdownHandler;
import com.jetbrains.youtrack.db.internal.core.storage.Storage;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class YouTrackDBManager extends OListenerManger<YouTrackDBListener> {

  public static final String YOU_TRACK_DB_HOME = "YOU_TRACK_DB_HOME";
  public static final String URL_SYNTAX =
      "<engine>:<db-type>:<db-name>[?<db-param>=<db-value>[&]]*";

  private static volatile YouTrackDBManager instance;
  private static final Lock initLock = new ReentrantLock();

  private static volatile boolean registerDatabaseByPath = false;

  private final ConcurrentMap<String, OEngine> engines = new ConcurrentHashMap<String, OEngine>();

  private final Map<ODatabaseLifecycleListener, ODatabaseLifecycleListener.PRIORITY>
      dbLifecycleListeners =
      new LinkedHashMap<ODatabaseLifecycleListener, ODatabaseLifecycleListener.PRIORITY>();
  private final ThreadGroup threadGroup;
  private final ReadWriteLock engineLock = new ReentrantReadWriteLock();
  private final ORecordConflictStrategyFactory recordConflictStrategy =
      new ORecordConflictStrategyFactory();
  private final ReferenceQueue<YouTrackDBStartupListener> removedStartupListenersQueue =
      new ReferenceQueue<YouTrackDBStartupListener>();
  private final ReferenceQueue<YouTrackDBShutdownListener> removedShutdownListenersQueue =
      new ReferenceQueue<YouTrackDBShutdownListener>();
  private final Set<YouTrackDBStartupListener> startupListeners =
      Collections.newSetFromMap(new ConcurrentHashMap<YouTrackDBStartupListener, Boolean>());
  private final Set<WeakHashSetValueHolder<YouTrackDBStartupListener>> weakStartupListeners =
      Collections.newSetFromMap(
          new ConcurrentHashMap<WeakHashSetValueHolder<YouTrackDBStartupListener>, Boolean>());
  private final Set<WeakHashSetValueHolder<YouTrackDBShutdownListener>> weakShutdownListeners =
      Collections.newSetFromMap(
          new ConcurrentHashMap<WeakHashSetValueHolder<YouTrackDBShutdownListener>, Boolean>());

  private final PriorityQueue<OShutdownHandler> shutdownHandlers =
      new PriorityQueue<OShutdownHandler>(
          11,
          new Comparator<OShutdownHandler>() {
            @Override
            public int compare(OShutdownHandler handlerOne, OShutdownHandler handlerTwo) {
              if (handlerOne.getPriority() > handlerTwo.getPriority()) {
                return 1;
              }

              if (handlerOne.getPriority() < handlerTwo.getPriority()) {
                return -1;
              }

              return 0;
            }
          });

  private final OLocalRecordCacheFactory localRecordCache = new OLocalRecordCacheFactoryImpl();

  private final Set<YouTrackDBEmbedded> factories =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  private final Set<YouTrackDBInternal> runningInstances = new HashSet<>();

  private final String os;

  private volatile Timer timer;
  private volatile ORecordFactoryManager recordFactoryManager = new ORecordFactoryManager();
  private YouTrackDBShutdownHook shutdownHook;
  private volatile AbstractProfiler profiler;
  private ODatabaseThreadLocalFactory databaseThreadFactory;
  private volatile boolean active = false;
  private OSignalHandler signalHandler;

  /**
   * Indicates that engine is initialized inside of web application container.
   */
  private final boolean insideWebContainer;

  /**
   * Prevents duplications because of recursive initialization.
   */
  private static boolean initInProgress = false;

  private static class WeakHashSetValueHolder<T> extends WeakReference<T> {

    private final int hashCode;

    private WeakHashSetValueHolder(T referent, ReferenceQueue<? super T> q) {
      super(referent, q);
      this.hashCode = referent.hashCode();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      WeakHashSetValueHolder that = (WeakHashSetValueHolder) o;

      if (hashCode != that.hashCode) {
        return false;
      }

      final T thisObject = get();
      final Object thatObject = that.get();

      if (thisObject == null && thatObject == null) {
        return super.equals(that);
      } else if (thisObject != null && thatObject != null) {
        return thisObject.equals(thatObject);
      }

      return false;
    }
  }

  YouTrackDBManager(boolean insideWebContainer) {
    super(true);
    this.insideWebContainer = insideWebContainer;
    this.os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    threadGroup = new ThreadGroup("YouTrackDB");
    threadGroup.setDaemon(false);
  }

  public boolean isInsideWebContainer() {
    return insideWebContainer;
  }

  public static YouTrackDBManager instance() {
    if (instance != null) {
      return instance;
    }

    return startUp(false);
  }

  public static YouTrackDBManager startUp(boolean insideWebContainer) {
    initLock.lock();
    try {
      if (initInProgress) {
        return null;
      }

      initInProgress = true;
      if (instance != null) {
        return instance;
      }

      final YouTrackDBManager youTrack = new YouTrackDBManager(insideWebContainer);
      youTrack.startup();

      instance = youTrack;
    } finally {
      initInProgress = false;
      initLock.unlock();
    }

    return instance;
  }

  public static String getHomePath() {
    String v = System.getProperty("orient.home");

    if (v == null) {
      v = OSystemVariableResolver.resolveVariable(YOU_TRACK_DB_HOME);
    }

    return FileUtils.getPath(v);
  }

  public static String getTempPath() {
    return FileUtils.getPath(System.getProperty("java.io.tmpdir") + "/orientdb/");
  }

  /**
   * Tells if to register database by path. Default is false. Setting to true allows to have
   * multiple databases in different path with the same name.
   *
   * @see #setRegisterDatabaseByPath(boolean)
   */
  public static boolean isRegisterDatabaseByPath() {
    return registerDatabaseByPath;
  }

  /**
   * Register database by path. Default is false. Setting to true allows to have multiple databases
   * in different path with the same name.
   */
  public static void setRegisterDatabaseByPath(final boolean iValue) {
    registerDatabaseByPath = iValue;
  }

  public ORecordConflictStrategyFactory getRecordConflictStrategy() {
    return recordConflictStrategy;
  }

  public YouTrackDBManager startup() {
    engineLock.writeLock().lock();
    try {
      if (active)
      // ALREADY ACTIVE
      {
        return this;
      }

      if (timer == null) {
        timer = new Timer(true);
      }

      profiler = new ProfilerStub(false);

      shutdownHook = new YouTrackDBShutdownHook();
      if (signalHandler == null) {
        signalHandler = new OSignalHandler();
        signalHandler.installDefaultSignals();
      }

      registerEngines();

      if (GlobalConfiguration.ENVIRONMENT_DUMP_CFG_AT_STARTUP.getValueAsBoolean()) {
        GlobalConfiguration.dumpConfiguration(System.out);
      }

      active = true;

      for (YouTrackDBStartupListener l : startupListeners) {
        try {
          if (l != null) {
            l.onStartup();
          }
        } catch (Exception e) {
          LogManager.instance().error(this, "Error on startup", e);
        }
      }

      purgeWeakStartupListeners();
      for (final WeakHashSetValueHolder<YouTrackDBStartupListener> wl : weakStartupListeners) {
        try {
          if (wl != null) {
            final YouTrackDBStartupListener l = wl.get();
            if (l != null) {
              l.onStartup();
            }
          }

        } catch (Exception e) {
          LogManager.instance().error(this, "Error on startup", e);
        }
      }

      initShutdownQueue();
      registerWeakYouTrackDBStartupListener(profiler);
    } finally {
      engineLock.writeLock().unlock();
    }

    return this;
  }

  /**
   * Add handler which will be executed during {@link #shutdown()} call.
   *
   * @param shutdownHandler Shutdown handler instance.
   */
  public void addShutdownHandler(OShutdownHandler shutdownHandler) {
    engineLock.writeLock().lock();
    try {
      shutdownHandlers.add(shutdownHandler);
    } finally {
      engineLock.writeLock().unlock();
    }
  }

  /**
   * Adds shutdown handlers in order which will be used during execution of shutdown.
   */
  private void initShutdownQueue() {
    addShutdownHandler(new ShutdownYouTrackDBInstancesHandler());
    addShutdownHandler(new ShutdownPendingThreadsHandler());
    addShutdownHandler(new ShutdownProfilerHandler());
    addShutdownHandler(new ShutdownCallListenersHandler());
  }

  /**
   * Shutdown whole YouTrackDB ecosystem. Usually is called during JVM shutdown by JVM shutdown
   * handler. During shutdown all handlers which were registered by the call of
   * {@link #addShutdownHandler(OShutdownHandler)} are called together with pre-registered system
   * shoutdown handlers according to their priority.
   *
   * @see OShutdownWorkersHandler
   * @see
   */
  private void registerEngines() {
    ClassLoader classLoader = YouTrackDBManager.class.getClassLoader();

    Iterator<OEngine> engines =
        OClassLoaderHelper.lookupProviderWithYouTrackDBClassLoader(OEngine.class, classLoader);

    OEngine engine = null;
    while (engines.hasNext()) {
      try {
        engine = engines.next();
        registerEngine(engine);
      } catch (IllegalArgumentException e) {
        if (engine != null) {
          LogManager.instance().debug(this, "Failed to replace engine " + engine.getName(), e);
        }
      }
    }
  }

  public YouTrackDBManager shutdown() {
    engineLock.writeLock().lock();
    try {
      if (!active) {
        return this;
      }

      active = false;

      LogManager.instance().info(this, "Orient Engine is shutting down...");
      for (OShutdownHandler handler : shutdownHandlers) {
        try {
          LogManager.instance().debug(this, "Shutdown handler %s is going to be called", handler);
          handler.shutdown();
          LogManager.instance().debug(this, "Shutdown handler %s completed", handler);
        } catch (Exception e) {
          LogManager.instance()
              .error(this, "Exception during calling of shutdown handler %s", e, handler);
        }
      }

      shutdownHandlers.clear();

      LogManager.instance().info(this, "Clearing byte buffer pool");
      OByteBufferPool.instance(null).clear();

      OByteBufferPool.instance(null).checkMemoryLeaks();
      ODirectMemoryAllocator.instance().checkMemoryLeaks();

      LogManager.instance().info(this, "YouTrackDB Engine shutdown complete");
      LogManager.instance().flush();
    } finally {
      try {
        removeShutdownHook();
      } finally {
        try {
          removeSignalHandler();
        } finally {
          engineLock.writeLock().unlock();
        }
      }
    }

    return this;
  }

  public TimerTask scheduleTask(final Runnable task, final long delay, final long period) {
    engineLock.readLock().lock();
    try {
      final TimerTask timerTask =
          new TimerTask() {
            @Override
            public void run() {
              try {
                task.run();
              } catch (Exception e) {
                LogManager.instance()
                    .error(
                        this,
                        "Error during execution of task " + task.getClass().getSimpleName(),
                        e);
              } catch (Error e) {
                LogManager.instance()
                    .error(
                        this,
                        "Error during execution of task " + task.getClass().getSimpleName(),
                        e);
                throw e;
              }
            }
          };

      if (active) {
        if (period > 0) {
          timer.schedule(timerTask, delay, period);
        } else {
          timer.schedule(timerTask, delay);
        }
      } else {
        LogManager.instance().warn(this, "YouTrackDB engine is down. Task will not be scheduled.");
      }

      return timerTask;
    } finally {
      engineLock.readLock().unlock();
    }
  }

  public TimerTask scheduleTask(final Runnable task, final Date firstTime, final long period) {
    engineLock.readLock().lock();
    try {
      final TimerTask timerTask =
          new TimerTask() {
            @Override
            public void run() {
              try {
                task.run();
              } catch (Exception e) {
                LogManager.instance()
                    .error(
                        this,
                        "Error during execution of task " + task.getClass().getSimpleName(),
                        e);
              } catch (Error e) {
                LogManager.instance()
                    .error(
                        this,
                        "Error during execution of task " + task.getClass().getSimpleName(),
                        e);
                throw e;
              }
            }
          };

      if (active) {
        if (period > 0) {
          timer.schedule(timerTask, firstTime, period);
        } else {
          timer.schedule(timerTask, firstTime);
        }
      } else {
        LogManager.instance().warn(this, "YouTrackDB engine is down. Task will not be scheduled.");
      }

      return timerTask;
    } finally {
      engineLock.readLock().unlock();
    }
  }

  public boolean isActive() {
    return active;
  }

  public boolean isWindowsOS() {
    return os.contains("win");
  }

  public void registerEngine(final OEngine iEngine) throws IllegalArgumentException {
    OEngine oEngine = engines.get(iEngine.getName());

    if (oEngine != null) {
      if (!oEngine.getClass().isAssignableFrom(iEngine.getClass())) {
        throw new IllegalArgumentException("Cannot replace storage " + iEngine.getName());
      }
    }
    engines.put(iEngine.getName(), iEngine);
  }

  /**
   * Returns the engine by its name.
   *
   * @param engineName Engine name to retrieve
   * @return OEngine instance of found, otherwise null
   */
  public OEngine getEngine(final String engineName) {
    engineLock.readLock().lock();
    try {
      return engines.get(engineName);
    } finally {
      engineLock.readLock().unlock();
    }
  }

  /**
   * Obtains an {@link OEngine engine} instance with the given {@code engineName}, if it is
   * {@link OEngine#isRunning() running}.
   *
   * @param engineName the name of the engine to obtain.
   * @return the obtained engine instance or {@code null} if no such engine known or the engine is
   * not running.
   */
  public OEngine getEngineIfRunning(final String engineName) {
    engineLock.readLock().lock();
    try {
      final OEngine engine = engines.get(engineName);
      return engine == null || !engine.isRunning() ? null : engine;
    } finally {
      engineLock.readLock().unlock();
    }
  }

  /**
   * Obtains a {@link OEngine#isRunning() running} {@link OEngine engine} instance with the given
   * {@code engineName}. If engine is not running, starts it.
   *
   * @param engineName the name of the engine to obtain.
   * @return the obtained running engine instance, never {@code null}.
   * @throws IllegalStateException if an engine with the given is not found or failed to start.
   */
  public OEngine getRunningEngine(final String engineName) {
    engineLock.readLock().lock();
    try {
      OEngine engine = engines.get(engineName);
      if (engine == null) {
        throw new IllegalStateException("Engine '" + engineName + "' is not found.");
      }

      if (!engine.isRunning() && !startEngine(engine)) {
        throw new IllegalStateException("Engine '" + engineName + "' is failed to start.");
      }

      return engine;
    } finally {
      engineLock.readLock().unlock();
    }
  }

  public Set<String> getEngines() {
    engineLock.readLock().lock();
    try {
      return Collections.unmodifiableSet(engines.keySet());
    } finally {
      engineLock.readLock().unlock();
    }
  }

  public Collection<Storage> getStorages() {
    List<Storage> storages = new ArrayList<>();
    for (YouTrackDBEmbedded factory : factories) {
      storages.addAll(factory.getStorages());
    }
    return storages;
  }

  public void removeShutdownHook() {
    if (shutdownHook != null) {
      shutdownHook.cancel();
      shutdownHook = null;
    }
  }

  public OSignalHandler getSignalHandler() {
    return signalHandler;
  }

  public void removeSignalHandler() {
    if (signalHandler != null) {
      signalHandler.cancel();
      signalHandler = null;
    }
  }

  public boolean isSelfManagedShutdown() {
    return shutdownHook != null;
  }

  public Iterator<ODatabaseLifecycleListener> getDbLifecycleListeners() {
    return new LinkedHashSet<>(dbLifecycleListeners.keySet()).iterator();
  }

  public void addDbLifecycleListener(final ODatabaseLifecycleListener iListener) {
    final Map<ODatabaseLifecycleListener, ODatabaseLifecycleListener.PRIORITY> tmp =
        new LinkedHashMap<ODatabaseLifecycleListener, ODatabaseLifecycleListener.PRIORITY>(
            dbLifecycleListeners);
    if (iListener.getPriority() == null) {
      throw new IllegalArgumentException(
          "Priority of DatabaseLifecycleListener '" + iListener + "' cannot be null");
    }

    tmp.put(iListener, iListener.getPriority());
    dbLifecycleListeners.clear();
    for (ODatabaseLifecycleListener.PRIORITY p : ODatabaseLifecycleListener.PRIORITY.values()) {
      for (Map.Entry<ODatabaseLifecycleListener, ODatabaseLifecycleListener.PRIORITY> e :
          tmp.entrySet()) {
        if (e.getValue() == p) {
          dbLifecycleListeners.put(e.getKey(), e.getValue());
        }
      }
    }
  }

  public void removeDbLifecycleListener(final ODatabaseLifecycleListener iListener) {
    dbLifecycleListeners.remove(iListener);
  }

  public ThreadGroup getThreadGroup() {
    return threadGroup;
  }

  public ODatabaseThreadLocalFactory getDatabaseThreadFactory() {
    return databaseThreadFactory;
  }

  public ORecordFactoryManager getRecordFactoryManager() {
    return recordFactoryManager;
  }

  public void setRecordFactoryManager(final ORecordFactoryManager iRecordFactoryManager) {
    recordFactoryManager = iRecordFactoryManager;
  }

  public OProfiler getProfiler() {
    return profiler;
  }

  public void setProfiler(final AbstractProfiler iProfiler) {
    profiler = iProfiler;
  }

  public void registerThreadDatabaseFactory(final ODatabaseThreadLocalFactory iDatabaseFactory) {
    databaseThreadFactory = iDatabaseFactory;
  }

  @Override
  public void registerListener(YouTrackDBListener listener) {
    if (listener instanceof YouTrackDBStartupListener) {
      registerYouTrackDBStartupListener((YouTrackDBStartupListener) listener);
    }

    super.registerListener(listener);
  }

  @Override
  public void unregisterListener(YouTrackDBListener listener) {
    if (listener instanceof YouTrackDBStartupListener) {
      unregisterYouTrackDBStartupListener((YouTrackDBStartupListener) listener);
    }

    super.unregisterListener(listener);
  }

  public void registerYouTrackDBStartupListener(YouTrackDBStartupListener listener) {
    startupListeners.add(listener);
  }

  public void registerWeakYouTrackDBStartupListener(YouTrackDBStartupListener listener) {
    purgeWeakStartupListeners();
    weakStartupListeners.add(
        new WeakHashSetValueHolder<YouTrackDBStartupListener>(listener,
            removedStartupListenersQueue));
  }

  public void unregisterYouTrackDBStartupListener(YouTrackDBStartupListener listener) {
    startupListeners.remove(listener);
  }

  public void unregisterWeakYouTrackDBStartupListener(YouTrackDBStartupListener listener) {
    purgeWeakStartupListeners();
    weakStartupListeners.remove(
        new WeakHashSetValueHolder<YouTrackDBStartupListener>(listener, null));
  }

  public void registerWeakYouTrackDBShutdownListener(YouTrackDBShutdownListener listener) {
    purgeWeakShutdownListeners();
    weakShutdownListeners.add(
        new WeakHashSetValueHolder<YouTrackDBShutdownListener>(
            listener, removedShutdownListenersQueue));
  }

  public void unregisterWeakYouTrackDBShutdownListener(YouTrackDBShutdownListener listener) {
    purgeWeakShutdownListeners();
    weakShutdownListeners.remove(
        new WeakHashSetValueHolder<YouTrackDBShutdownListener>(listener, null));
  }

  @Override
  public void resetListeners() {
    super.resetListeners();

    weakShutdownListeners.clear();

    startupListeners.clear();
    weakStartupListeners.clear();
  }

  public OLocalRecordCacheFactory getLocalRecordCache() {
    return localRecordCache;
  }

  private void purgeWeakStartupListeners() {
    synchronized (removedStartupListenersQueue) {
      WeakHashSetValueHolder<YouTrackDBStartupListener> ref =
          (WeakHashSetValueHolder<YouTrackDBStartupListener>) removedStartupListenersQueue.poll();
      while (ref != null) {
        weakStartupListeners.remove(ref);
        ref = (WeakHashSetValueHolder<YouTrackDBStartupListener>) removedStartupListenersQueue.poll();
      }
    }
  }

  private void purgeWeakShutdownListeners() {
    synchronized (removedShutdownListenersQueue) {
      WeakHashSetValueHolder<YouTrackDBShutdownListener> ref =
          (WeakHashSetValueHolder<YouTrackDBShutdownListener>) removedShutdownListenersQueue.poll();
      while (ref != null) {
        weakShutdownListeners.remove(ref);
        ref =
            (WeakHashSetValueHolder<YouTrackDBShutdownListener>) removedShutdownListenersQueue.poll();
      }
    }
  }

  private boolean startEngine(OEngine engine) {
    final String name = engine.getName();

    try {
      engine.startup();
      return true;
    } catch (Exception e) {
      LogManager.instance()
          .error(
              this, "Error during initialization of engine '%s', engine will be removed", e, name);

      try {
        engine.shutdown();
      } catch (Exception se) {
        LogManager.instance().error(this, "Error during engine shutdown", se);
      }

      engines.remove(name);
    }

    return false;
  }

  /**
   * Closes all storages and shutdown all engines.
   */
  public class ShutdownYouTrackDBInstancesHandler implements OShutdownHandler {

    @Override
    public int getPriority() {
      return SHUTDOWN_ENGINES_PRIORITY;
    }

    @Override
    public void shutdown() throws Exception {
      for (YouTrackDBInternal internal : runningInstances) {
        internal.internalClose();
      }
      runningInstances.clear();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  /**
   * Interrupts all threads in YouTrackDB thread group and stops timer is used in methods
   * {@link #scheduleTask(Runnable, Date, long)} and {@link #scheduleTask(Runnable, long, long)}.
   */
  private class ShutdownPendingThreadsHandler implements OShutdownHandler {

    @Override
    public int getPriority() {
      return SHUTDOWN_PENDING_THREADS_PRIORITY;
    }

    @Override
    public void shutdown() throws Exception {
      if (threadGroup != null)
      // STOP ALL THE PENDING THREADS
      {
        threadGroup.interrupt();
      }

      if (timer != null) {
        timer.cancel();
        timer = null;
      }
    }

    @Override
    public String toString() {
      // it is strange but windows defender block compilation if we get class name programmatically
      // using Class instance
      return "ShutdownPendingThreadsHandler";
    }
  }

  /**
   * Shutdown YouTrackDB profiler.
   */
  private class ShutdownProfilerHandler implements OShutdownHandler {

    @Override
    public int getPriority() {
      return SHUTDOWN_PROFILER_PRIORITY;
    }

    @Override
    public void shutdown() throws Exception {
      // NOTE: DON'T REMOVE PROFILER TO AVOID NPE AROUND THE CODE IF ANY THREADS IS STILL WORKING
      profiler.shutdown();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  /**
   * Calls all shutdown listeners.
   */
  private class ShutdownCallListenersHandler implements OShutdownHandler {

    @Override
    public int getPriority() {
      return SHUTDOWN_CALL_LISTENERS;
    }

    @Override
    public void shutdown() throws Exception {
      purgeWeakShutdownListeners();
      for (final WeakHashSetValueHolder<YouTrackDBShutdownListener> wl : weakShutdownListeners) {
        try {
          if (wl != null) {
            final YouTrackDBShutdownListener l = wl.get();
            if (l != null) {
              l.onShutdown();
            }
          }

        } catch (Exception e) {
          LogManager.instance().error(this, "Error during orient shutdown", e);
        }
      }

      // CALL THE SHUTDOWN ON ALL THE LISTENERS
      for (YouTrackDBListener l : browseListeners()) {
        if (l != null) {
          try {
            l.onShutdown();
          } catch (Exception e) {
            LogManager.instance().error(this, "Error during orient shutdown", e);
          }
        }
      }

      System.gc();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  public void onEmbeddedFactoryInit(YouTrackDBEmbedded embeddedFactory) {
    OEngine memory = engines.get("memory");
    if (memory != null && !memory.isRunning()) {
      memory.startup();
    }
    OEngine disc = engines.get("plocal");
    if (disc != null && !disc.isRunning()) {
      disc.startup();
    }
    factories.add(embeddedFactory);
  }

  public void onEmbeddedFactoryClose(YouTrackDBEmbedded embeddedFactory) {
    factories.remove(embeddedFactory);
    if (factories.isEmpty()) {
      OEngine memory = engines.get("memory");
      if (memory != null && memory.isRunning()) {
        memory.shutdown();
      }
      OEngine disc = engines.get("plocal");
      if (disc != null && disc.isRunning()) {
        disc.shutdown();
      }
    }
  }

  public void addYouTrackDB(YouTrackDBInternal internal) {
    engineLock.writeLock().lock();
    try {
      runningInstances.add(internal);
    } finally {
      engineLock.writeLock().unlock();
    }
  }

  public void removeYouTrackDB(YouTrackDBInternal internal) {
    engineLock.writeLock().lock();
    try {
      runningInstances.remove(internal);
    } finally {
      engineLock.writeLock().unlock();
    }
  }
}
