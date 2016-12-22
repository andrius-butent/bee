package com.butent.bee.server.concurrency;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.ParameterEvent;
import com.butent.bee.server.modules.ParameterEventHandler;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.LogMessage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.TimedObject;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class ConcurrencyBean {

  public interface HasTimerService extends TimedObject {
    TimerService getTimerService();
  }

  public abstract static class AsynchronousRunnable
      implements Runnable, ManagedTask, ManagedTaskListener {

    private long submitted;
    private long started;

    @Override
    public Map<String, String> getExecutionProperties() {
      return ImmutableMap.of(LONGRUNNING_HINT, Boolean.toString(true), IDENTITY_NAME, getId());
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
      return this;
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task,
        Throwable throwable) {
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Object task,
        Throwable throwable) {

      if (Objects.isNull(throwable)) {
        logger.info("Ended:", this, TimeUtils.elapsedSeconds(started));
      } else {
        logger.error(throwable, "Failed:", this);
      }
      runningTasks.remove(getId());
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
      started = System.currentTimeMillis();
      logger.info("Started:", this, TimeUtils.elapsedSeconds(submitted));
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
      submitted = System.currentTimeMillis();
      runningTasks.put(getId(), submitted);
      logger.info("Submitted:", this);
    }

    public String getId() {
      return Integer.toHexString(System.identityHashCode(this));
    }

    @Override
    public String toString() {
      return BeeUtils.joinWords(getId(), Integer.toHexString(System.identityHashCode(this)));
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(ConcurrencyBean.class);

  private static final Map<String, Long> runningTasks = new ConcurrentHashMap<>();

  private Multimap<String, Class<? extends HasTimerService>> calendarRegistry;
  private Multimap<String, Class<? extends HasTimerService>> intervalRegistry;

  private final ReentrantLock lock = new ReentrantLock();

  @EJB
  ParamHolderBean prm;
  @EJB
  UserServiceBean usr;
  @Resource
  ManagedExecutorService executor;
  @Resource
  UserTransaction utx;

  @Lock(LockType.READ)
  public void asynchronousCall(AsynchronousRunnable runnable) {
    if ((System.currentTimeMillis() - BeeUtils.unbox(runningTasks.get(runnable.getId())))
        > TimeUtils.MILLIS_PER_HOUR) {
      executor.submit(runnable);
    }
  }

  public <T extends HasTimerService> void createCalendarTimer(Class<T> handler, String parameter) {
    if (calendarRegistry == null) {
      calendarRegistry = HashMultimap.create();

      prm.registerParameterEventHandler(new ParameterEventHandler() {
        @Subscribe
        public void createTimers(ParameterEvent event) {
          String param = event.getParameter();

          if (calendarRegistry.containsKey(param)) {
            ConcurrencyBean concurrency = Invocation.locateRemoteBean(ConcurrencyBean.class);

            if (concurrency != null) {
              for (Class<? extends HasTimerService> clazz : calendarRegistry.get(param)) {
                concurrency.createCalendarTimer(clazz, param);
              }
            }
          }
        }
      });
    }
    if (!calendarRegistry.containsEntry(parameter, handler)) {
      calendarRegistry.put(parameter, handler);
    }
    TimerService timerService = removeTimer(handler, parameter);
    String hours = prm.getText(parameter);

    if (!BeeUtils.isEmpty(hours)) {
      try {
        Timer timer = timerService.createCalendarTimer(new ScheduleExpression().hour(hours),
            new TimerConfig(parameter, false));

        logger.info("Created", NameUtils.getClassName(handler), parameter, "timer on hours [",
            hours, "] starting at", timer.getNextTimeout());
      } catch (IllegalArgumentException ex) {
        if (DataUtils.isId(usr.getCurrentUserId())) {
          Endpoint.sendToUser(usr.getCurrentUserId(), LogMessage.error(ex));
        }
        logger.error(ex);
      }
    }
  }

  public <T extends HasTimerService> void createIntervalTimer(Class<T> handler, String parameter) {
    if (intervalRegistry == null) {
      intervalRegistry = HashMultimap.create();

      prm.registerParameterEventHandler(new ParameterEventHandler() {
        @Subscribe
        public void createTimers(ParameterEvent event) {
          String param = event.getParameter();

          if (intervalRegistry.containsKey(param)) {
            ConcurrencyBean concurrency = Invocation.locateRemoteBean(ConcurrencyBean.class);

            if (concurrency != null) {
              for (Class<? extends HasTimerService> clazz : intervalRegistry.get(param)) {
                concurrency.createIntervalTimer(clazz, param);
              }
            }
          }
        }
      });
    }
    if (!intervalRegistry.containsEntry(parameter, handler)) {
      intervalRegistry.put(parameter, handler);
    }
    TimerService timerService = removeTimer(handler, parameter);
    Integer minutes = prm.getInteger(parameter);

    if (BeeUtils.isPositive(minutes)) {
      try {
        Timer timer = timerService.createIntervalTimer(minutes * TimeUtils.MILLIS_PER_MINUTE,
            minutes * TimeUtils.MILLIS_PER_MINUTE, new TimerConfig(parameter, false));

        logger.info("Created", NameUtils.getClassName(handler), parameter, "timer every", minutes,
            "minutes starting at", timer.getNextTimeout());
      } catch (IllegalArgumentException ex) {
        if (DataUtils.isId(usr.getCurrentUserId())) {
          Endpoint.sendToUser(usr.getCurrentUserId(), LogMessage.error(ex));
        }
        logger.error(ex);
      }
    }
  }

  public static boolean isParameterTimer(Timer timer, Object parameter) {
    if (!Config.isInitialized()) {
      return false;
    }
    Assert.noNulls(timer, parameter);
    return Objects.equals(timer.getInfo(), parameter);
  }

  @Lock(LockType.READ)
  public void synchronizedCall(Runnable runnable) {
    Assert.notNull(runnable);
    lock.lock();

    try {
      utx.begin();
      runnable.run();
      utx.commit();
    } catch (Throwable ex) {
      logger.error(ex);

      try {
        utx.rollback();
      } catch (SystemException ex2) {
        logger.error(ex2);
      }
    } finally {
      lock.unlock();
    }
  }

  private static <T extends HasTimerService> TimerService removeTimer(Class<T> handler,
      String parameter) {
    T bean = Assert.notNull(Invocation.locateRemoteBean(handler));
    TimerService timerService = Assert.notNull(bean.getTimerService());

    for (Timer timer : timerService.getTimers()) {
      if (Objects.equals(timer.getInfo(), parameter)) {
        timer.cancel();
        logger.info("Removed", NameUtils.getClassName(handler), parameter, "timer");
        break;
      }
    }
    return timerService;
  }
}
