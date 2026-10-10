package com.butent.bee.server.concurrency;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.Subscribe;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.ParameterEvent;
import com.butent.bee.server.modules.ParameterEventHandler;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.websocket.messages.LogMessage;

import java.util.HashMap;
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
      Pair<String, Long> pair = runningTasks.get(getId());

      if (Objects.nonNull(pair) && Objects.equals(pair.getA(), hash())) {
        runningTasks.remove(getId());
      }
      if (Objects.isNull(throwable)) {
        logger.info("Ended:", this, TimeUtils.elapsedSeconds(started));
      } else {
        logger.error(throwable, "Failed:", this);
      }
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
      started = System.currentTimeMillis();
      Pair<String, Long> pair = runningTasks.get(getId());

      if (Objects.nonNull(pair) && Objects.equals(pair.getA(), hash())) {
        pair.setB(started);
      }
      logger.info("Started:", this, "Delay:", TimeUtils.elapsedSeconds(submitted));
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
      submitted = System.currentTimeMillis();
      runningTasks.put(getId(), Pair.of(hash(), BeeConst.LONG_UNDEF));
      logger.info("Submitted:", this);
    }

    public String getId() {
      return hash();
    }

    @Override
    public String toString() {
      return BeeUtils.joinWords(getId(), hash());
    }

    private String hash() {
      return Integer.toHexString(System.identityHashCode(this));
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(ConcurrencyBean.class);

  private static final Map<String, Pair<String, Long>> runningTasks = new ConcurrentHashMap<>();

  private final Map<String, Class<? extends HasTimerService>> calendarReg = new HashMap<>();
  private final Map<String, Class<? extends HasTimerService>> intervalReg = new HashMap<>();
  private final Map<String, Long> intervalUnits = new HashMap<>();

  private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

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
    Pair<String, Long> pair = runningTasks.get(runnable.getId());
    long started = BeeUtils.unbox(Objects.isNull(pair) ? null : pair.getB());

    if (!BeeConst.isUndef(started)) {
      if ((System.currentTimeMillis() - started) > TimeUtils.MILLIS_PER_HOUR) {
        if (BeeUtils.isPositive(started)) {
          logger.info("Replaced:", runnable);
        }
        executor.submit(runnable);
      }
    }
  }

  public <T extends HasTimerService> void createCalendarTimer(Class<T> handler, String parameter) {
    calendarReg.put(parameter, handler);
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
    createIntervalTimer(handler, parameter, TimeUtils.MILLIS_PER_MINUTE);
  }

  public <T extends HasTimerService> void createDailyIntervalTimer(Class<T> handler,
      String parameter) {
    createIntervalTimer(handler, parameter, TimeUtils.MILLIS_PER_DAY);
  }

  private <T extends HasTimerService> void createIntervalTimer(Class<T> handler, String parameter,
      long millisPerUnit) {
    intervalReg.put(parameter, handler);
    intervalUnits.put(parameter, millisPerUnit);
    TimerService timerService = removeTimer(handler, parameter);
    Integer interval = prm.getInteger(parameter);

    if (BeeUtils.isPositive(interval)) {
      try {
        long intervalMillis = interval * millisPerUnit;
        Timer timer = timerService.createIntervalTimer(intervalMillis, intervalMillis,
            new TimerConfig(parameter, false));

        logger.info("Created", NameUtils.getClassName(handler), parameter, "timer every", interval,
            millisPerUnit == TimeUtils.MILLIS_PER_DAY ? "days" : "minutes",
            "starting at", timer.getNextTimeout());
      } catch (IllegalArgumentException ex) {
        if (DataUtils.isId(usr.getCurrentUserId())) {
          Endpoint.sendToUser(usr.getCurrentUserId(), LogMessage.error(ex));
        }
        logger.error(ex);
      }
    }
  }

  public void init() {
    prm.registerParameterEventHandler(new ParameterEventHandler() {
      @Subscribe
      public void createTimers(ParameterEvent event) {
        String param = event.getParameter();

        if (calendarReg.containsKey(param)) {
          Invocation.locateRemoteBean(ConcurrencyBean.class)
              .createCalendarTimer(calendarReg.get(param), param);
        }
        if (intervalReg.containsKey(param)) {
          Invocation.locateRemoteBean(ConcurrencyBean.class)
              .createIntervalTimer(intervalReg.get(param), param, intervalUnits.get(param));
        }
      }
    });
  }

  public static boolean isParameterTimer(Timer timer, Object parameter) {
    if (!Config.isInitialized()) {
      return false;
    }
    Assert.noNulls(timer, parameter);
    return Objects.equals(timer.getInfo(), parameter);
  }

  @Lock(LockType.READ)
  public void synchronizedCall(String lockKey, Runnable runnable) {
    Assert.notNull(runnable);
    locks.putIfAbsent(lockKey, new ReentrantLock());
    ReentrantLock lock = locks.get(lockKey);
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
