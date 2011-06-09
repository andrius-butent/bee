package com.butent.bee.client.dialog;

/**
 * Requires implementing notification management classes to contain methods to handle info, severe
 * and warning level messages.
 */

public interface NotificationListener {

  void notifyInfo(String... messages);

  void notifySevere(String... messages);

  void notifyWarning(String... messages);
}
