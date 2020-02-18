package com.butent.bee.shared.modules.tasks;

import com.google.common.collect.Lists;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskWorkflowAction.WorkflowOperation;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Task module constants of DataSource and UI objects.
 */
public final class TaskConstants {

  /**
   * Workflow or process of tasks handling and validation helper class.
   *
   * <p>
   *     There main data types defined the task workflow process:
   *     <ul>
   *         <li>Task Executor - defines Executor data field related with Users data structure.</li>
   *         <li>Task Owner - defines Owner data field related with Users data structure.</li>
   *         <li>Task Observers - defines set of TaskUsers data structure related with Tasks and
   *         Users structures.</li>
   *         <li>Task status - defines numeric Status data field assigned with {@code TaskStatus}
   *         enumeration.</li>
   *     </ul>
   * </p>
   *
   * <p>
   *     All fields related with Users data structure comparing with current loggined system user.
   *     By these data types combining conditions for user do actions with task. <br/>
   *     Workflow example: current system user is owner of task. Task has status "finished". User
   *     can "approve" or "return of execution" task
   *
   * </p>
   *
   * <p>
   *     {@code
   *     Long currentUserId = BeeKeeper.getUser.getUserId(); // Current logged user id
   *     } <br/>
   *     {@code
   *     Long ownerId = getTaskOwnerId(); // Returns owner user id of task
   *     } <br/>
   *     {@code
   *     Long executorId = getTaskExecutorId(); // Returns executor user id of task;
   *     } <br/>
   *     {@code
   *     TaskStatus status = getTaskStatus(); // Returns task status enum value
   *     } <br/> <br />

   *     {@code
   *     TaskWorkflowAction canApprove = canTaskWorkflowAction.grouped(WorkflowOperation.AND,
   *     } <br/> &nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *        TaskWorkflowAction.canOwner(), TaskWorkflowAction.hasStatus(TaskStatus.COMPLETED));
   *     } <br /> <br />
   *
   *     {@code
   *     TaskWorkflowAction canReturnExecute = TaskWorkflowAction.grouped(WorkflowOperation.AND,
   *     } <br/> &nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *        TaskWorkflowAction.canOwner(),
   *     } <br/> &nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *        TaskWorkflowAction.grouped(WorkflowOperation.OR,
   *     }
   *
   *     <br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *        TaskWorkflowAction.hasStatus(TaskStatus.SUSPENDED),
   *     }
   *     <br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *        TaskWorkflowAction.hasStatus(TaskStatus.CANCELED),
   *     }
   *     <br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *         TaskWorkflowAction.hasStatus(TaskStatus.COMPLETED),
   *     }
   *     <br/> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code
   *       TaskWorkflowAction.hasStatus(TaskStatus.APPROVED))));
   *     } <br /> <br />
   *
   *     {@code
   *       if (canApprove.canExecute(currentUserId == executorId, currentUserId == ownerId,
   *     } <br />  &nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code  false, false,  status)
   *     } { <br /> &nbsp;&nbsp;
   *     {@code
   *      // create UI controls of task approving...
   *     } <br/> }&nbsp;&nbsp;&nbsp;&nbsp; <br/> <br />
   *
   *     {@code
   *       if (canReturnExecute.canExecute(currentUserId == executorId, currentUserId == ownerId,
   *     } <br />  &nbsp;&nbsp;&nbsp;&nbsp;
   *     {@code  false, false,  status)
   *     } { <br /> &nbsp;&nbsp;
   *     {@code
   *      // create UI controls of task return execution...
   *     } <br/> }&nbsp;&nbsp;&nbsp;&nbsp;
   *
   * </p>
   */
  protected static final class TaskWorkflowAction {

      /**
       * List of logical operations for building task workflow conditions.
       */
    public enum WorkflowOperation {
      OR,
      AND;
    }

    private enum WorkflowRole {
      /**
       * This role method {@code isValid}  always returns false value. <br />
       * The action of task never be created in UI by this role.
       */
      HIDDEN {
        @Override
        boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver, boolean isUser,
            boolean hasValidStatus) {
          return false;
        }
      },

      /**
       * This role method {@code isValid} returns true value then the {@code isOwner} parameter of
       * this method will be true. <br />
       */
      OWNER {
        @Override
        boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver, boolean isUser,
            boolean hasValidStatus) {
          return isOwner;
        }
      },

      /**
       * This role method {@code isValid} returns true value then the {@code isExecutor} parameter
       * of this method will be true. <br />
       */
      EXECUTOR {
        @Override
        boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver, boolean isUser,
            boolean hasValidStatus) {
          return isExecutor;
        }
      },

      /**
       * This role method {@code isValid} returns true value then the {@code isObserver} parameter
       * of this method will be true. <br />
       */
      OBSERVER {
        @Override
        boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver, boolean isUser,
            boolean hasValidStatus) {
          return isObserver;
        }
      },

      /**
       * This role method {@code isValid} returns true value then the {@code isUser} parameter
       * of this method will be true. <br />
       */
      USER {
        @Override
        boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver, boolean isUser,
            boolean hasValidStatus) {
          return isUser;
        }
      },

      /**
       * This role method {@code isValid} returns true value then the {@code hasValidStatus}
       * parameter of this method will be true. <br />
       */
      TASK_STATUS {

        @Override
        boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver, boolean isUser,
            boolean hasValidStatus) {
          return hasValidStatus;
        }
      };

      WorkflowRole() {
      }

      abstract boolean isValid(boolean isExecutor, boolean isOwner, boolean isObserver,
          boolean isUser, boolean hasValidStatus);
    }

    private WorkflowRole role;
    private List<TaskWorkflowAction> subWorkflow = new ArrayList<>();
    private WorkflowOperation operation;
    private TaskStatus checkStatus;

    public static TaskWorkflowAction hidden() {
      return new TaskWorkflowAction(WorkflowRole.HIDDEN);
    }

    public static TaskWorkflowAction canOwner() {
      return new TaskWorkflowAction(WorkflowRole.OWNER);
    }

    public static TaskWorkflowAction canExecutor() {
      return new TaskWorkflowAction(WorkflowRole.EXECUTOR);
    }

    public static TaskWorkflowAction canObserver() {
      return new TaskWorkflowAction(WorkflowRole.OBSERVER);
    }

    public static TaskWorkflowAction canUser() {
      return new TaskWorkflowAction(WorkflowRole.USER);
    }

    public static TaskWorkflowAction hasStatus(TaskStatus status) {
      return new TaskWorkflowAction(status);
    }

    public static TaskWorkflowAction grouped(WorkflowOperation op,
        TaskWorkflowAction... groups) {
      Assert.isFalse(ArrayUtils.isEmpty(groups), "TaskWorkflowAction groups required");
      return new TaskWorkflowAction(op, groups);
    }

    public boolean canExecute(boolean isExecutor, boolean isOwner, boolean isObserver,
        boolean isUser, TaskStatus status) {
      if (this.operation != null && !BeeUtils.isEmpty(subWorkflow)) {
        switch (this.operation) {
          case OR:
            for (TaskWorkflowAction workflowRole : subWorkflow) {
              if (workflowRole.canExecute(isExecutor, isOwner, isObserver, isUser, status)) {
                return true;
              }
            }

            return false;
          case AND:
            for (TaskWorkflowAction workflowRole : subWorkflow) {
              if (!workflowRole.canExecute(isExecutor, isOwner, isObserver, isUser,
                  status)) {
                return false;
              }
            }

            return true;
        }

        Assert.unsupported("Task workflow error. Operation required");
        return false;
      } else if (role != null) {
        return role.isValid(isExecutor, isOwner, isObserver, isUser, status == checkStatus);
      } else {
        Assert.unsupported("Task workflow error. Role or group of role required");
        return false;
      }
    }

    private TaskWorkflowAction(WorkflowRole role) {
      this.role = role;
    }

    private TaskWorkflowAction(TaskStatus checkStatus) {
      this(WorkflowRole.TASK_STATUS);
      this.checkStatus = checkStatus;
    }

    private TaskWorkflowAction(WorkflowOperation op,
        TaskWorkflowAction... eventWorkflowRoles) {
      this.operation = op;
      this.subWorkflow = Lists.newArrayList(eventWorkflowRoles);
    }

  }

  public enum TaskEvent implements HasCaption {
    CREATE(Localized.dictionary().crmTaskEventCreated(),
        Localized.dictionary().crmNewTask(),
        FontAwesome.CODE_FORK,
        TaskWorkflowAction.grouped(
            WorkflowOperation.OR,
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.grouped(WorkflowOperation.AND,
                TaskWorkflowAction.canExecutor(),
                TaskWorkflowAction.grouped(WorkflowOperation.OR,
                    TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                    TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                    TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE))))),
    VISIT(Localized.dictionary().crmTaskEventVisited(),
        Localized.dictionary().crmTaskStopExecute(),
        FontAwesome.STOP_CIRCLE_O,
        TaskWorkflowAction.canExecutor(),
        TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE)),
    ACTIVATE(Localized.dictionary().crmTaskEventExecuted(),
        Localized.dictionary().crmTaskDoExecute(),
        FontAwesome.PLAY_CIRCLE_O,
        TaskWorkflowAction.canExecutor(),
        TaskWorkflowAction.hasStatus(TaskStatus.VISITED)),
    COMMENT(Localized.dictionary().crmTaskComment(),
        Localized.dictionary().crmActionComment(),
        FontAwesome.COMMENT_O,
        TaskWorkflowAction.grouped(WorkflowOperation.OR,
            TaskWorkflowAction.canExecutor(),
            TaskWorkflowAction.canObserver(),
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.canUser())),
    EXTEND(Localized.dictionary().crmTaskEventExtended(),
        Localized.dictionary().crmTaskChangeTerm(),
        FontAwesome.CLOCK_O,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE),
                TaskWorkflowAction.hasStatus(TaskStatus.COMPLETED)))),
    SUSPEND(Localized.dictionary().crmTaskStatusSuspended(),
        Localized.dictionary().crmActionSuspend(),
        FontAwesome.MINUS_CIRCLE,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE)))),
    RENEW(Localized.dictionary().crmTaskEventRenewed(),
        Localized.dictionary().crmTaskReturnExecution(),
        FontAwesome.ARROW_CIRCLE_RIGHT,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.SUSPENDED),
                TaskWorkflowAction.hasStatus(TaskStatus.CANCELED),
                TaskWorkflowAction.hasStatus(TaskStatus.COMPLETED),
                TaskWorkflowAction.hasStatus(TaskStatus.APPROVED)))),
    FORWARD(Localized.dictionary().crmTaskEventForwarded(),
        Localized.dictionary().crmActionForward(),
        FontAwesome.ARROW_CIRCLE_O_RIGHT,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.canOwner(),
                TaskWorkflowAction.canExecutor()),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE),
              TaskWorkflowAction.grouped(WorkflowOperation.AND,
                TaskWorkflowAction.canOwner(),
                TaskWorkflowAction.hasStatus(TaskStatus.COMPLETED))))),
    CANCEL(Localized.dictionary().crmTaskStatusCanceled(),
        Localized.dictionary().crmTaskCancel(),
        FontAwesome.BAN,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE)))),
    COMPLETE(Localized.dictionary().crmTaskStatusCompleted(),
        Localized.dictionary().crmActionFinish(),
        FontAwesome.CHECK_CIRCLE_O,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.canExecutor(),
                TaskWorkflowAction.canOwner()),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE)))),
    APPROVE(Localized.dictionary().crmTaskEventApproved(),
        Localized.dictionary().crmTaskConfirm(),
        FontAwesome.CHECK_SQUARE_O,
        TaskWorkflowAction.canOwner(),
        TaskWorkflowAction.hasStatus(TaskStatus.COMPLETED)),
    EDIT(Localized.dictionary().crmTaskEventEdited(),
        null,
        null,
        TaskWorkflowAction.hidden()),
    OUT_OF_OBSERVERS(Localized.dictionary().crmTaskEventOutOfObservers(),
        Localized.dictionary().crmTaskOutOfObservers(),
        FontAwesome.USER_TIMES,
        TaskWorkflowAction.grouped(WorkflowOperation.AND,
            TaskWorkflowAction.canObserver(),
            TaskWorkflowAction.grouped(WorkflowOperation.OR,
                TaskWorkflowAction.hasStatus(TaskStatus.VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.NOT_VISITED),
                TaskWorkflowAction.hasStatus(TaskStatus.ACTIVE)))),
    REFRESH(Localized.dictionary().actionRefresh(),
        Localized.dictionary().actionRefresh(),
        FontAwesome.REFRESH,
        TaskWorkflowAction.grouped(WorkflowOperation.OR,
            TaskWorkflowAction.canExecutor(),
            TaskWorkflowAction.canObserver(),
            TaskWorkflowAction.canOwner(),
            TaskWorkflowAction.canUser())),
    CREATE_NOT_SCHEDULED(Localized.dictionary().crmTaskEventCreated(),
        Localized.dictionary().crmTasksNotScheduledTasks(),
        null,
        TaskWorkflowAction.hidden()),
    CREATE_SCHEDULED(Localized.dictionary().crmTaskForwardedForExecution(),
        Localized.dictionary().crmTaskStatusScheduled(), null, TaskWorkflowAction.hidden());

    private final String caption;
    private final String commandLabel;
    private final FontAwesome commandIcon;
    private final List<TaskWorkflowAction> roles;

    TaskEvent(String caption, String commandLabel, FontAwesome commandIcon,
        TaskWorkflowAction requiredRole, TaskWorkflowAction... otherRoles) {
      Assert.notNull(requiredRole);
      this.caption = caption;
      this.commandLabel = commandLabel;
      this.commandIcon = commandIcon;
      this.roles = Lists.newArrayList(requiredRole);
      if (!ArrayUtils.isEmpty(otherRoles)) {
        this.roles.addAll(Lists.newArrayList(otherRoles));
      }
    }

    @Override
    public String getCaption() {
      return caption;
    }

    public static boolean in(int event, TaskEvent... events) {
      for (TaskEvent te : events) {
        if (te.ordinal() == event) {
          return true;
        }
      }
      return false;
    }

    public String getCommandLabel() {
      return commandLabel;
    }

    public FontAwesome getCommandIcon() {
      return commandIcon;
    }

    public boolean canExecute(Long executor, Long owner, List<Long> observers, TaskStatus status) {
      Long userId = BeeKeeper.getUser().getUserId();
      boolean result = true;
      for (TaskWorkflowAction role : roles) {
        result = result && role.canExecute(BeeUtils.unbox(executor) == userId, BeeUtils.unbox(
            owner) == userId, BeeUtils.contains(observers, userId), userId != BeeUtils.unbox(
            executor)
            && userId != BeeUtils.unbox(owner) && !BeeUtils.contains(observers, userId),
            status);
      }

      return result;
    }
  }

  public enum TaskPriority implements HasCaption {
    LOW(Localized.dictionary().crmTaskPriorityLow()),
    MEDIUM(Localized.dictionary().crmTaskPriorityMedium()),
    HIGH(Localized.dictionary().crmTaskPriorityHigh());

    private final String caption;

    TaskPriority(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public enum TaskStatus implements HasLocalizedCaption {
    /**
     * Task status when Task is created for executor and executor is not Task owner.
     *
     * DataStore ID 0.
     */
    NOT_VISITED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusNotVisited();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        if (differentBorder) {
          return TASK_STATUS_STYLE_NOT_VISITED + TASK_STATUS_STYLE_WITH_BORDER;
        } else {
          return TASK_STATUS_STYLE_NOT_VISITED;
        }
      }
    },

    /**
     * Task status when Task executor initiates ACTIVATE Task Event. The initial task state before
     * the event can be VISITED.
     * 
     * DataStore ID 1.
     */
    ACTIVE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusActive();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        if (differentBorder) {
          return TASK_STATUS_STYLE_ACTIVE + TASK_STATUS_STYLE_WITH_BORDER;
        } else {
          return TASK_STATUS_STYLE_ACTIVE;
        }
      }
    },

    /**
     * Task status when Task is created with later start date than today's date.
     * 
     * DataStore ID 2.
     * 
     * @deprecated Obsolete status. Use NOT_VISITED or VISITED states.
     */
    @Deprecated
    SCHEDULED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusScheduled();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        return null;
      }
    },

    /**
     * Task status when Task owner initiates SUSPEND event. The initial task state before the event
     * can be NOT_VISITED, VISITED or ACTIVE.
     * 
     * DataStore ID 3.
     */
    SUSPENDED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusSuspended();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        if (differentBorder) {
          return TASK_STATUS_STYLE_SUSPENDED + TASK_STATUS_STYLE_WITH_BORDER;
        } else {
          return TASK_STATUS_STYLE_SUSPENDED;
        }
      }
    },

    /**
     * Task status when Task executor (not Task owner) initiates COMPLETE Task event. The initial
     * task state before the event can be, NOT_VISITED, VISITED or ACTIVE.
     * 
     * DataStore ID 4.
     */
    COMPLETED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusCompleted();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        return TASK_STATUS_STYLE_COMPLETED;
      }
    },

    /**
     * Task status when Task owner (or owner can be as executor) initiates APROVE event. The initial
     * task state before the event can be, NOT_VISITED, VISITED or ACTIVE if executor is task owner
     * otherwise COMPLETED only.
     * 
     * DataStore ID 5.
     */
    APPROVED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusApproved();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        return null;
      }
    },

    /**
     * Task status when Task owner (or owner can be as executor) initiates CANCEL event. The initial
     * task state before the event can be, NOT_VISITED, VISITED or ACTIVE.
     * 
     * DataStore ID 6.
     */
    CANCELED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusCanceled();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        return TASK_STATUS_STYLE_CACELED;
      }
    },

    /**
     * Task status when Task executor opens NOT_VISITED Task form and initiates VISIT Event.
     * 
     * DataStore ID 7.
     */
    VISITED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusVisited();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        if (differentBorder) {
          return TASK_STATUS_STYLE_VISITED + TASK_STATUS_STYLE_WITH_BORDER;
        } else {
          return TASK_STATUS_STYLE_VISITED;
        }
      }
    },

    /**
     * Task status when Task is created without executor and start/end dates.
     * 
     * DataStore ID 8.
     */
    NOT_SCHEDULED {

      @Override
      public String getCaption(Dictionary constants) {
        return constants.crmTaskStatusNotScheduled();
      }

      @Override
      public String getStyleName(boolean differentBorder) {
        if (differentBorder) {
          return TASK_STATUS_STYLE_NOT_SCHEDULED + TASK_STATUS_STYLE_WITH_BORDER;
        } else {
          return TASK_STATUS_STYLE_NOT_SCHEDULED;
        }
      }
    };

    public static boolean in(int status, TaskStatus... statuses) {
      for (TaskStatus ts : statuses) {
        if (ts.ordinal() == status) {
          return true;
        }
      }
      return false;
    }

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }

    /**
     * Method used for rendering css elements according to task status.
     * @param differentBorder - indicates then different elements style is needed.
     * @return css class name.
     */
    public abstract String getStyleName(boolean differentBorder);
  }

  public enum ToDoVisibility implements HasCaption {
    PUBLIC(Localized.dictionary().calPublic()),
    PRIVATE(Localized.dictionary().calPrivate());

    private final String caption;

    ToDoVisibility(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static void register() {
    EnumUtils.register(TaskPriority.class);
    EnumUtils.register(TaskEvent.class);
    EnumUtils.register(TaskStatus.class);
    EnumUtils.register(ToDoVisibility.class);
  }

  public static final String CRM_TASK_PREFIX = "task_";

  public static final String SVC_GET_TASK_DATA = "get_task_data";
  public static final String SVC_GET_CHANGED_TASKS = "get_changed_tasks";
  public static final String SVC_ACCESS_TASK = "access_task";
  public static final String SVC_EXTEND_TASK = "extend_task";

  public static final String SVC_CONFIRM_TASKS = "confirm_tasks";

  public static final String SVC_TASKS_REPORTS_PREFIX = "get_tasks_reports_";
  public static final String SVC_TASKS_REPORTS_COMPANY_TIMES = SVC_TASKS_REPORTS_PREFIX
      + "company_times";
  public static final String SVC_TASKS_REPORTS_TYPE_HOURS = SVC_TASKS_REPORTS_PREFIX + "type_hours";
  public static final String SVC_TASKS_REPORTS_USERS_HOURS = SVC_TASKS_REPORTS_PREFIX
      + "users_hours";

  public static final String SVC_GET_REQUEST_FILES = "get_request_files";
  public static final String SVC_FINISH_REQUEST_WITH_TASK = "finish_request_with_task";

  public static final String SVC_RT_GET_SCHEDULING_DATA = "rt_get_scheduling_data";
  public static final String SVC_RT_SPAWN = "rt_spawn";
  public static final String SVC_RT_SCHEDULE = "rt_schedule";
  public static final String SVC_RT_COPY = "rt_copy";

  public static final String SVC_TASK_REPORT = "TaskReport";
  public static final String SVC_COPY_TASK_ORDER = "CopyTaskOrder";

  public static final String VAR_TASK_DATA = Service.RPC_VAR_PREFIX + "task_data";
  public static final String VAR_TASK_ID = Service.RPC_VAR_PREFIX + "task_id";
  public static final String VAR_USER_ID = Service.RPC_VAR_PREFIX + "user_id";
  public static final String VAR_TASK_APPROVED_TIME = Service.RPC_VAR_PREFIX + "task_approved";

  public static final String VAR_TASK_COMMENT = Service.RPC_VAR_PREFIX + "task_comment";
  public static final String VAR_TASK_NOTES = Service.RPC_VAR_PREFIX + "task_notes";
  public static final String VAR_TASK_FINISH_TIME = Service.RPC_VAR_PREFIX + "task_finish_time";
  public static final String VAR_TASK_PUBLISHER = Service.RPC_VAR_PREFIX + "task_publisher";
  public static final String VAR_TASK_COMPANY = Service.RPC_VAR_PREFIX + "task_company";
  public static final String VAR_TASK_ACTIVE = "Active";
  public static final String VAR_TASK_LATE = "Late";
  public static final String VAR_TASK_COMPLETED = "Completed";
  public static final String VAR_TASK_SHEDULED = "Sheduled";

  public static final String VAR_TASK_DURATION_DATE = Service.RPC_VAR_PREFIX + "task_duration_date";
  public static final String VAR_TASK_DURATION_TIME = Service.RPC_VAR_PREFIX + "task_duration_time";
  public static final String VAR_TASK_DURATION_TYPE = Service.RPC_VAR_PREFIX + "task_duration_type";
  public static final String VAR_TASK_DURATION_DATE_FROM = VAR_TASK_DURATION_DATE + "_from";
  public static final String VAR_TASK_DURATION_DATE_TO = VAR_TASK_DURATION_DATE + "_to";
  public static final String VAR_TASK_DURATION_HIDE_ZEROS = Service.RPC_VAR_PREFIX
      + "task_duration_hide_zeros";

  public static final String VAR_TASK_RELATIONS = Service.RPC_VAR_PREFIX + "task_relations";
  public static final String VAR_COPY_RELATIONS = Service.RPC_VAR_PREFIX + "copy_task_relations";
  public static final String VAR_TASK_USERS = Service.RPC_VAR_PREFIX + "task_users";
  public static final String VAR_TASK_PROPERTIES = Service.RPC_VAR_PREFIX + "task_properties";
  public static final String VAR_TASK_PROJECT = Service.RPC_VAR_PREFIX + "task_project";

  public static final String VAR_TASK_VISITED = Service.RPC_VAR_PREFIX + "task_visited";
  public static final String VAR_TASK_VISITED_STATE = Service.RPC_VAR_PREFIX + "task_visited_state";

  public static final String VAR_RT_ID = Service.RPC_VAR_PREFIX + "rt_id";
  public static final String VAR_RT_DAY = Service.RPC_VAR_PREFIX + "rt_day";

  public static final String TBL_REQUESTS = "Requests";
  public static final String TBL_REQUEST_FILES = "RequestFiles";

  public static final String TBL_TASKS = "Tasks";
  public static final String TBL_TASK_USERS = "TaskUsers";
  public static final String TBL_TASK_EVENTS = "TaskEvents";
  public static final String TBL_TASK_FILES = "TaskFiles";

  public static final String TBL_TASK_TYPES = "TaskTypes";

  public static final String TBL_DURATION_TYPES = "DurationTypes";
  public static final String TBL_EVENT_DURATIONS = "EventDurations";

  public static final String TBL_RECURRING_TASKS = "RecurringTasks";

  public static final String TBL_RT_DATES = "RTDates";
  public static final String TBL_RT_FILES = "RTFiles";
  public static final String TBL_RT_EXECUTORS = "RTExecutors";
  public static final String TBL_RT_EXECUTOR_GROUPS = "RTExecutorGroups";
  public static final String TBL_RT_OBSERVERS = "RTObservers";
  public static final String TBL_RT_OBSERVER_GROUPS = "RTObserverGroups";

  public static final String VIEW_TASKS = "Tasks";
  public static final String VIEW_TASK_FILES = "TaskFiles";
  public static final String VIEW_TASK_USERS = "TaskUsers";
  public static final String VIEW_TASK_EVENTS = "TaskEvents";
  public static final String VIEW_TASK_DURATIONS = "TaskDurations";
  public static final String VIEW_TASK_DURATION_TYPES = "TaskDurationTypes";
  public static final String VIEW_TASK_PRODUCTS = "TaskProducts";
  public static final String VIEW_TASK_ORDERS = "TaskOrders";
  public static final String VIEW_TASK_ORDER_COLUMNS = "TaskOrderColumns";
  public static final String VIEW_TASK_ORDER_ITEMS = "TaskOrderItems";
  public static final String VIEW_TASK_ORDER_OTHER_ITEMS = "TaskOrderOtherItems";

  public static final String VIEW_TASK_TEMPLATES = "TaskTemplates";
  public static final String VIEW_TASK_TYPES = "TaskTypes";

  public static final String VIEW_DURATION_TYPES = "DurationTypes";

  public static final String VIEW_RECURRING_TASKS = "RecurringTasks";
  public static final String VIEW_REQUEST_EVENTS = "RequestEvents";
  public static final String VIEW_RT_DATES = "RTDates";
  public static final String VIEW_RT_FILES = "RTFiles";

  public static final String VIEW_REQUEST_DURATION_TYPES = "RequestDurationTypes";
  public static final String VIEW_REQUEST_FILES = "RequestFiles";

  public static final String VIEW_RELATED_TASKS = "RelatedTasks";
  public static final String VIEW_RELATED_RECURRING_TASKS = "RelatedRecurringTasks";

  public static final String VIEW_REQUESTS = "Requests";

  public static final String VIEW_TODO_LIST = "TodoList";

  public static final String COL_START_TIME = "StartTime";
  public static final String COL_FINISH_TIME = "FinishTime";

  public static final String COL_TASK_TYPE = "Type";
  public static final String COL_PRIORITY = "Priority";

  public static final String COL_OWNER = "Owner";
  public static final String COL_EXECUTOR = "Executor";

  public static final String COL_TASK_ID = "TaskID";
  public static final String COL_ID = "Id";

  public static final String COL_TASK = "Task";

  public static final String COL_TASK_ORDER = "TaskOrder";

  public static final String COL_TASK_TEMPLATE_NAME = "Name";
  public static final String COL_TASK_TYPE_NAME = "Name";

  public static final String COL_SUMMARY = "Summary";
  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_CAPTION = "Caption";
  public static final String COL_TASK_COMPANY = "Company";
  public static final String COL_PRIVATE_TASK = "PrivateTask";

  public static final String COL_PARENT = "Parent";
  public static final String COL_ORDER = "Order";

  public static final String COL_FILE_DATE = "FileDate";
  public static final String COL_FILE_VERSION = "FileVersion";

  public static final String COL_EXPIRES = "Expires";

  public static final String COL_REMINDER = "Reminder";
  public static final String COL_REMINDER_TIME = "ReminderTime";
  public static final String COL_REMINDER_SENT = "ReminderSent";
  public static final String COL_STATUS = "Status";
  public static final String COL_EXPECTED_DURATION = "ExpectedDuration";

  public static final String COL_PUBLISH_TIME = "PublishTime";
  public static final String COL_PUBLISHER = "Publisher";

  public static final String COL_COMMENT = "Comment";

  public static final String COL_PRODUCT = "Product";
  public static final String COL_PRODUCT_NAME = "Name";
  public static final String COL_PRODUCT_REQUIRED = "ProductRequired";

  public static final String COL_TASK_EVENT = "TaskEvent";

  public static final String COL_DURATION_DATE = "DurationDate";
  public static final String COL_DURATION_TYPE = "DurationType";
  public static final String COL_DURATION = "Duration";

  public static final String COL_DURATION_TYPE_NAME = "Name";

  public static final String COL_EVENT = "Event";
  public static final String COL_EVENT_PROPERTIES = "Properties";
  public static final String COL_EVENT_NOTE = "EventNote";
  public static final String COL_EVENT_DATA = "EventData";
  public static final String COL_EVENT_DURATION = "EventDuration";

  public static final String COL_LAST_ACCESS = "LastAccess";
  public static final String COL_STAR = "Star";

  public static final String COL_COMPLETED = "Completed";
  public static final String COL_APPROVED = "Approved";
  public static final String COL_ACTUAL_DURATION = "ActualDuration";
  public static final String COL_ACTUAL_EXPENSES = "ActualExpenses";
  public static final String COL_EXPECTED_EXPENSES = "ExpectedExpenses";

  public static final String COL_REQUEST = "Request";
  public static final String COL_REQUEST_CONTACTS = "Contacts";
  public static final String COL_REQUEST_CONTENT = "Content";
  public static final String COL_REQUEST_CUSTOMER = "Customer";
  public static final String COL_REQUEST_CUSTOMER_NAME = "CustomerName";
  public static final String COL_REQUEST_CUSTOMER_PERSON = "CustomerPerson";
  public static final String COL_REQUEST_DATE = "Date";
  public static final String COL_REQUEST_EVENT = "RequestEvent";
  public static final String COL_REQUEST_MANAGER = "Manager";
  public static final String COL_REQUEST_RESULT = "Result";
  public static final String COL_REQUEST_RESULT_PROPERTIES = "ResultProperties";
  public static final String COL_REQUEST_FINISHED = "Finished";
  public static final String COL_REQUEST_TYPE = "RequestType";

  public static final String COL_RECURRING_TASK = "RecurringTask";

  public static final String COL_RT_SCHEDULE_FROM = "ScheduleFrom";
  public static final String COL_RT_SCHEDULE_UNTIL = "ScheduleUntil";
  public static final String COL_RT_SCHEDULE_DAYS = "ScheduleDays";
  public static final String COL_RT_WORKDAY_TRANSITION = "WorkdayTransition";
  public static final String COL_RT_DAY_OF_MONTH = "DayOfMonth";
  public static final String COL_RT_MONTH = "Month";
  public static final String COL_RT_DAY_OF_WEEK = "DayOfWeek";
  public static final String COL_RT_YEAR = "Year";
  public static final String COL_RT_START_AT = "StartAt";
  public static final String COL_RT_DURATION_DAYS = "DurationDays";
  public static final String COL_RT_DURATION_TIME = "DurationTime";
  public static final String COL_RT_REMINDER = "Reminder";
  public static final String COL_RT_REMIND_BEFORE = "RemindBefore";
  public static final String COL_RT_REMIND_AT = "RemindAt";
  public static final String COL_RT_COPY_BY_MAIL = "CopyByMail";

  public static final String COL_RTD_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTD_FROM = "DateFrom";
  public static final String COL_RTD_UNTIL = "DateUntil";
  public static final String COL_RTD_MODE = "Mode";

  public static final String COL_RTF_RECURRING_TASK = "RecurringTask";

  public static final String COL_RTEX_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTEX_USER = "User";
  public static final String COL_RTEXGR_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTEXGR_GROUP = "Group";

  public static final String COL_RTOB_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTOB_USER = "User";
  public static final String COL_RTOBGR_RECURRING_TASK = "RecurringTask";
  public static final String COL_RTOBGR_GROUP = "Group";

  public static final String COL_MAIL_ASSIGNED_TASKS = "MailAssignedTasks";

  public static final String ALS_CONTACT_FIRST_NAME = "ContactFirstName";
  public static final String ALS_CONTACT_LAST_NAME = "ContactLastName";

  public static final String ALS_REQUEST_PRODUCT_FOREGROUND = "ProductForeground";
  public static final String ALS_REQUEST_PRODUCT_BACKGROUND = "ProductBackground";

  public static final String ALS_DURATION_TYPE_NAME = "DurationTypeName";

  public static final String ALS_EXECUTOR_FIRST_NAME = "ExecutorFirstName";
  public static final String ALS_EXECUTOR_LAST_NAME = "ExecutorLastName";

  public static final String ALS_OWNER_FIRST_NAME = "OwnerFirstName";
  public static final String ALS_OWNER_LAST_NAME = "OwnerLastName";

  public static final String ALS_PUBLISHER_FIRST_NAME = "PublisherFirstName";
  public static final String ALS_PUBLISHER_LAST_NAME = "PublisherLastName";

  public static final String ALS_PERSON_FIRST_NAME = "PersonFirstName";
  public static final String ALS_PERSON_LAST_NAME = "PersonLastName";
  public static final String ALS_PERSON_COMPANY_NAME = "PersonCompanyName";

  public static final String ALS_TASK_SUBJECT = "TaskSubject";
  public static final String ALS_TASK_TYPE_NAME = "TypeName";
  public static final String ALS_TASK_TYPE_BACKGROUND = "TypeBackground";
  public static final String ALS_TASK_TYPE_FOREGROUND = "TypeForeground";
  public static final String ALS_PROJECT_OWNER = "ProjectOwner";
  public static final String ALS_PROJECT_STATUS = "ProjectStatus";
  public static final String ALS_REMINDER_NAME = "ReminderName";
  public static final String ALS_LAST_BREAK_EVENT = "LastBreakEvent";

  public static final String ALS_LAST_SPAWN = "LastSpawn";

  public static final String ALS_TASK_PRODUCT_NAME = "ProductName";

  public static final String MENU_TASKS = "Tasks";

  public static final String PROP_EXECUTORS = "Executors";
  public static final String PROP_EXECUTOR_GROUPS = "ExecutorGroups";
  public static final String PROP_OBSERVERS = "Observers";
  public static final String PROP_OBSERVER_GROUPS = "ObserverGroups";

  public static final String PROP_FILES = "Files";
  public static final String PROP_EVENTS = "Events";
  public static final String PROP_DESCENDING = "Descending";

  public static final String PROP_USER = "User";
  public static final String PROP_STAR = "Star";
  public static final String PROP_LAST_ACCESS = "LastAccess";
  public static final String PROP_LAST_PUBLISH = "LastPublish";

  public static final String PROP_LAST_EVENT_ID = "LastEventId";

  public static final String PROP_MAIL = "Mail";

  public static final String GRID_TASKS = "Tasks";
  public static final String GRID_TODO_LIST = "TodoList";

  public static final String GRID_TASKS_TYPE_HOURS_REPORT = "TasksTypeHoursReport";

  public static final String GRID_RECURRING_TASKS = "RecurringTasks";
  public static final String GRID_CHILD_RECURRING_TASKS = "ChildRecurringTasks";
  public static final String GRID_RT_FILES = "RTFiles";

  public static final String GRID_RELATED_TASKS = "RelatedTasks";
  public static final String GRID_CHILD_TASKS = "ChildTasks";
  public static final String GRID_CHILD_TASK_TEMPLATES = "ChildTaskTemplates";
  public static final String GRID_CHILD_REQUESTS = "ChildRequests";
  public static final String GRID_RELATED_RECURRING_TASKS = "RelatedRecurringTasks";

  public static final String GRID_REQUESTS = "Requests";

  public static final String GRID_TASK_TYPES = "TaskTypes";
  public static final String GRID_TASK_TEMPLATES = "TaskTemplates";
  public static final String GRID_TASK_PRODUCTS = "TaskProducts";

  public static final String FORM_NEW_REQUEST_COMMENT = "NewRequestComment";
  public static final String FORM_NEW_TASK = "NewTask";
  public static final String FORM_NEW_TASK_ORDER = "NewTaskOrder";
  public static final String FORM_TASK = "Task";
  public static final String FORM_TASK_ORDER = "TaskOrder";
  public static final String FORM_TASK_PREVIEW = "TaskPreview";

  public static final String FORM_RECURRING_TASK = "RecurringTask";

  public static final String FORM_TASKS_REPORT = "TasksReport";

  public static final String FORM_NEW_REQUEST = "NewRequest";
  public static final String FORM_REQUEST = "Request";

  public static final String CRM_STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "crm-";

  public static final String FILTER_TASKS_NEW = "tasks_new";
  public static final String FILTER_TASKS_UPDATED = "tasks_updated";

  public static final String PRM_END_OF_WORK_DAY = "EndOfWorkDay";
  public static final String PRM_START_OF_WORK_DAY = "StartOfWorkDay";
  public static final String PRM_DEFAULT_DBA_TEMPLATE = "DefaultDBATemplate";
  public static final String PRM_DEFAULT_DBA_DOCUMENT_TYPE = "DefaultDBADocumentType";
  public static final String PRM_CREATE_PRIVATE_TASK_FIRST = "CreatePrivateTaskFirst";

  public static final String TASK_STATUS_STYLE = "bee-header-caption_state";
  public static final String TASK_STATUS_STYLE_NOT_VISITED = TASK_STATUS_STYLE + "_not_visited";
  public static final String TASK_STATUS_STYLE_ACTIVE = TASK_STATUS_STYLE + "_active";
  public static final String TASK_STATUS_STYLE_SUSPENDED = TASK_STATUS_STYLE + "_suspended";
  public static final String TASK_STATUS_STYLE_COMPLETED = TASK_STATUS_STYLE + "_completed";
  public static final String TASK_STATUS_STYLE_CACELED = TASK_STATUS_STYLE + "_canceled";
  public static final String TASK_STATUS_STYLE_VISITED = TASK_STATUS_STYLE + "_visited";
  public static final String TASK_STATUS_STYLE_NOT_SCHEDULED = TASK_STATUS_STYLE + "_not_scheduled";
  public static final String TASK_STATUS_STYLE_WITH_BORDER = "_with_border";
  public static final String DEFAULT_TASK_PROPERTIES  =
        BeeUtils.join(BeeConst.STRING_COMMA, PROP_OBSERVERS, PROP_FILES, PROP_EVENTS);

  private TaskConstants() {
  }
}
