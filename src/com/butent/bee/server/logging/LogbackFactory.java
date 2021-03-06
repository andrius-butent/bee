package com.butent.bee.server.logging;

import com.butent.bee.server.Config;
import com.butent.bee.server.io.FileUtils;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.BeeLoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogbackFactory implements BeeLoggerFactory {

  private static final String LOGBACK_PROPERTIES = "logback.xml";
  private final LoggerContext context;

  public LogbackFactory() {
    context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.reset();
    context.putProperty("LOG_DIR", Config.LOG_DIR.getAbsolutePath().replace("\\", "/"));
    JoranConfigurator configurator = new JoranConfigurator();

    try {
      configurator.setContext(context);
      configurator.doConfigure(FileUtils.isInputFile(Config.LOCAL_DIR, LOGBACK_PROPERTIES)
          ? new File(Config.LOCAL_DIR, LOGBACK_PROPERTIES)
          : new File(Config.CONFIG_DIR, LOGBACK_PROPERTIES));
    } catch (JoranException je) {
      configurator = null;
      // StatusPrinter will handle this
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
  }

  @Override
  public BeeLogger getLogger(String name) {
    return new LogbackLogger(name);
  }

  @Override
  public BeeLogger getRootLogger() {
    return new LogbackLogger(Logger.ROOT_LOGGER_NAME);
  }

  @Override
  public void stop() {
    context.stop();
  }
}
