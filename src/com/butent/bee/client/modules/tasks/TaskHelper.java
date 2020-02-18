package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.grid.ChildGrid;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.MultiSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.composite.Relations;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskUtils;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.*;

/**
 * Shared utility for Task creating or editing between create and edit task's forms.
 */
final class TaskHelper {

  static final String NAME_TASK_TREE = "TaskTree";
  static final String NAME_ORDER = "TaskEventsOrder";
  private static final List<String> widgetNames = Arrays.asList("Supplier", "PressSeries", "PressNumber", "PressDate", "Price",
		"SupplierNote", "DecorationSupplier", "PriceSeries", "Number", "Date", "DecorationPrice", "DecorationSupplierNote",
		"InvoiceSupplier", "InvoicePrice", "InvoiceNote", "InvoiceSeries", "InvoiceNumber", "InvoiceDate",
		"TransportationSupplier", "TransportationSeries", "TransportationNumber", "TransportationPrice",
		"TransportationNote");

  static ParameterList createTaskParams(FormView form, TaskConstants.TaskEvent event,
                                        BeeRow newRow, Collection<RowChildren> updatedRelations,
                                        String comment) {
    String viewName = form.getViewName();
    List<String> notes = new ArrayList<>();
    IsRow oldRow = form.getOldRow();

    BeeRowSet updated = DataUtils.getUpdated(viewName, form.getDataColumns(), oldRow, newRow,
        form.getChildrenForUpdate());

    if (!DataUtils.isEmpty(updated)) {
      BeeRow updRow = updated.getRow(0);

      for (int i = 0; i < updated.getNumberOfColumns(); i++) {
        int index = form.getDataIndex(updated.getColumnId(i));

        newRow.setValue(index, oldRow.getString(index));
        newRow.preliminaryUpdate(index, updRow.getString(i));
      }
    }

    if (getBooleanValueFromStorage(NAME_ORDER)) {
      newRow.setProperty(PROP_DESCENDING, BeeConst.INT_TRUE);
    } else {
      newRow.removeProperty(PROP_DESCENDING);
    }

    BeeRowSet rowSet = new BeeRowSet(viewName, form.getDataColumns());
    rowSet.addRow(newRow);

    ParameterList params = TasksKeeper.createTaskRequestParameters(event);
    params.addDataItem(VAR_TASK_DATA, Codec.beeSerialize(rowSet));
    params.addDataItem(VAR_TASK_USERS, getTaskUsers(form, oldRow));

    if (!BeeUtils.isEmpty(comment)) {
      params.addDataItem(VAR_TASK_COMMENT, comment);
    }

    if (event != TaskEvent.CREATE_SCHEDULED) {
      notes = TaskUtils.getUpdateNotes(Data.getDataInfo(viewName), oldRow, newRow,
          Format.getDateRenderer(), Format.getDateTimeRenderer());
    }

    if (form.isEnabled() && event != TaskEvent.CREATE_SCHEDULED) {
      if (!TaskUtils.sameObservers(oldRow, newRow)) {
        String oldObservers = oldRow.getProperty(PROP_OBSERVERS);
        String newObservers = newRow.getProperty(PROP_OBSERVERS);

        MultiSelector selector = getMultiSelector(form, PROP_OBSERVERS);

        Set<Long> removed = DataUtils.getIdSetDifference(oldObservers, newObservers);
        for (long id : removed) {
          String label = selector.getRowLabel(id);
          if (!BeeUtils.isEmpty(label)) {
            notes.add(TaskUtils.getDeleteNote(Localized.dictionary().crmTaskObservers(), label));
          }
        }

        Set<Long> added = DataUtils.getIdSetDifference(newObservers, oldObservers);
        for (long id : added) {
          String label = selector.getRowLabel(id);
          if (!BeeUtils.isEmpty(label)) {
            notes.add(TaskUtils.getInsertNote(Localized.dictionary().crmTaskObservers(), label));
          }
        }
      }
    }

    if (!updatedRelations.isEmpty()
      && form.getWidgetByName(AdministrationConstants.TBL_RELATIONS) instanceof Relations) {
      params.addDataItem(VAR_TASK_RELATIONS, Codec.beeSerialize(updatedRelations));

      if (event != TaskEvent.CREATE_SCHEDULED) {
        Relations relations = (Relations) form.getWidgetByName(
          AdministrationConstants.TBL_RELATIONS);
        Map<String, String> oldRelations = getOldRelations(relations);

        for (RowChildren relation : updatedRelations) {
          String caption = Data.getColumnLabel(relation.getRepository(), relation.getChildColumn());
          String relViewName = Data.getColumnRelation(relation.getRepository(),
            relation.getChildColumn());
          String oldValue = oldRelations.get(relViewName);
          String newValue = relation.getChildrenIds();

          Set<Long> removed = DataUtils.getIdSetDifference(oldValue, newValue);
          for (long id : removed) {
            String label = relations == null ? BeeUtils.toString(id)
              : relations.getSelectorRowLabel(relViewName, id);
            if (!BeeUtils.isEmpty(label)) {
              notes.add(TaskUtils.getDeleteNote(caption, label));
            }
          }

          Set<Long> added = DataUtils.getIdSetDifference(newValue, oldValue);
          for (long id : added) {
            String label = relations == null ? BeeUtils.toString(id)
              : relations.getSelectorRowLabel(relViewName, id);
            if (!BeeUtils.isEmpty(label)) {
              notes.add(TaskUtils.getInsertNote(caption, label));
            }
          }
        }
      }
    }

    if (!notes.isEmpty()) {
      params.addDataItem(VAR_TASK_NOTES, Codec.beeSerialize(notes));
    }
    return params;
  }

  static Map<String, String> getOldRelations(Relations relations) {
    Map<String, String> oldRelations = new HashMap<>();

    if (relations == null) {
      return oldRelations;
    }

    relations.getOldRowChildren().forEach(relation -> {
      String relViewName = Data.getColumnRelation(relation.getRepository(),
        relation.getChildColumn());
      oldRelations.put(relViewName, relation.getChildrenIds());
    });

    return oldRelations;
  }

  static boolean getBooleanValueFromStorage(String name) {
    String key = getStorageKey(name);
    return BeeKeeper.getStorage().hasItem(key);
  }

  static MultiSelector getMultiSelector(FormView form, String source) {
    Widget widget = form.getWidgetBySource(source);
    return (widget instanceof MultiSelector) ? (MultiSelector) widget : null;
  }

  static String getStorageKey(String name) {

    switch (name) {
      case NAME_TASK_TREE:
        return BeeUtils.join(BeeConst.STRING_MINUS, name, BeeKeeper.getUser().getUserId(),
            UiConstants.ATTR_SIZE);

      case NAME_ORDER:
        return BeeUtils.join(BeeConst.STRING_MINUS, name, BeeKeeper.getUser().getUserId());
    }
    return name;
  }

  static List<String> getRelatedViews() {
    List<String> views = new ArrayList<>();

    for (BeeColumn relColumn
        : Data.getDataInfo(AdministrationConstants.TBL_RELATIONS).getColumns()) {
      if (BeeUtils.isEmpty(
        Data.getColumnRelation(AdministrationConstants.TBL_RELATIONS, relColumn.getId()))) {
        continue;
      }

      views.add(Data.getColumnRelation(AdministrationConstants.TBL_RELATIONS, relColumn.getId()));
    }
    return views;
  }

  static String getTaskUsers(FormView form, IsRow row) {
    return DataUtils.buildIdList(TaskUtils.getTaskUsers(row, form.getDataColumns()));
  }

  static void setWidgetEnabled(HasEnabled widget, boolean enabled) {
    if (widget == null) {
      return;
    }

    widget.setEnabled(enabled);

    if (widget instanceof UIObject && !(widget instanceof Relations)) {
      ((UIObject) widget).setStyleName(StyleUtils.NAME_DISABLED, !enabled);
    }
  }

  static void setWidgetsEnabled(FormView form) {
		if (form != null && BeeKeeper.getUser().getUserData().canMergeData("TaskOrderOtherItems")) {
			ChildGrid otherOrderItems = (ChildGrid) form.getWidgetByName("TaskOrderOtherItems");
			if (otherOrderItems != null) {
				setWidgetEnabled(otherOrderItems, true);
			}

			for(String widgetName : widgetNames) {
				Widget widget = form.getWidgetBySource(widgetName);
				if (widget != null) {
					setWidgetEnabled((HasEnabled) widget, true);
				}
			}
		}
	}
  static void setSelectorFilter(DataSelector selector, Filter filter) {
    if (selector == null) {
      return;
    }

    selector.getOracle().setAdditionalFilter(filter, true);
  }

  private TaskHelper() {
  }
}
