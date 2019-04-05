package com.butent.bee.client.modules.tasks;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.ReminderDialog;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.widget.*;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

public class RequestReminder extends ReminderDialog {
    private static final String REMINDER_ACTIVE = "bee-reminder-dialog-active";

    private IsRow reminderDataRow;
    private Long requestID;

    RequestReminder() {
        super();
        addDefaultCloseBox();
        super.addCloseHandler(event -> loadData(null));
    }

    @Override
    protected void afterButtonPanelRender(Button create, Button update, Button suspend, Button cancel) {
        create.addClickHandler(e -> performCreateReminder());
        update.addClickHandler(e -> performUpdateReminder());
        suspend.addClickHandler(e -> performSuspendReminder());
    }

    public Long getRequestID() {
        return requestID;
    }

    public void loadData(Queries.RowSetCallback callback) {
        Queries.getRowSet(VIEW_REQUEST_REMINDERS, null, Filter.equals(COL_REQUEST, requestID),
                result -> {
                    if (!DataUtils.isEmpty(result)) {
                        reminderDataRow = result.getRow(0);
                        setReminderDT(Data.getDateTime(VIEW_REQUEST_REMINDERS, reminderDataRow, COL_REQUEST_REMINDER_DATE));
                        setReminderNotes(Data.getString(VIEW_REQUEST_REMINDERS, reminderDataRow, ClassifierConstants.COL_EMAIL));
                        createButton.setVisible(false);
                        updateButton.setVisible(true);
                    } else {
                        setReminderDT(null);
                        setReminderNotes(BeeConst.STRING_EMPTY);
                        createButton.setVisible(true);
                        updateButton.setVisible(false);
                    }

                    suspendButton.setVisible(isActive(reminderDataRow));
                    getDialogAction().setStyleName(REMINDER_ACTIVE, isActive(reminderDataRow));

                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
    }

    public void setRequestID(Long requestID) {
        this.requestID = requestID;
    }

    private static boolean isActive(IsRow dataRow) {
        return dataRow != null && BeeUtils.toBoolean(dataRow
                .getString(Data.getColumnIndex(VIEW_REQUEST_REMINDERS, COL_REQUEST_REMINDER_ACTIVE)));
    }

    private static void showDateError() {
        Global.showError(Localized.dictionary().error(), Collections.singletonList(
                Localized.dictionary().userReminderSendRemindDateError()));
    }

    private void performUpdateReminder() {
        Long time = getReminderDT() == null ? null : getReminderDT().getTime();

        if (time != null && System.currentTimeMillis() < time) {

            final List<String> columns = Lists.newArrayList(COL_REQUEST_REMINDER_USER, COL_REQUEST_REMINDER_DATE,
                    COL_REQUEST_REMINDER_ACTIVE);

            List<String> values = Lists.newArrayList(BeeUtils.toString(BeeKeeper.getUser().getUserId()),
                    BeeUtils.toString(time), BeeConst.STRING_TRUE);

            Queries.update(VIEW_REQUEST_REMINDERS, Filter.equals(COL_REQUEST, requestID),
                    columns, values, result -> {
                        close();
                        BeeKeeper.getScreen().notifyInfo(Localized.dictionary().userReminderCreated());
                    });
        } else {
            showDateError();
        }
    }

    private void performCreateReminder() {
        Long time = getReminderDT() == null ? null : getReminderDT().getTime();

        if (time != null && System.currentTimeMillis() < time) {

            final List<BeeColumn> columns = Data.getColumns(VIEW_REQUEST_REMINDERS,
                    Lists.newArrayList(COL_REQUEST_REMINDER_USER, COL_REQUEST, COL_REQUEST_REMINDER_DATE,
                            COL_REQUEST_REMINDER_ACTIVE));

            List<String> values = Lists.newArrayList(BeeUtils.toString(BeeKeeper.getUser().getUserId()),
                    BeeUtils.toString(requestID), BeeUtils.toString(time), BeeConst.STRING_TRUE);

            Queries.insert(VIEW_REQUEST_REMINDERS, columns, values, null, row -> {
                close();
                BeeKeeper.getScreen().notifyInfo(Localized.dictionary().userReminderCreated());
            });
        } else {
            showDateError();
        }
    }

    private void performSuspendReminder() {
        Queries.update(VIEW_REQUEST_REMINDERS, Filter.equals(COL_REQUEST, requestID),
                COL_REQUEST_REMINDER_ACTIVE, BeeConst.STRING_FALSE, result -> {
                    close();
                    BeeKeeper.getScreen().notifyInfo(Localized.dictionary().userReminderDisabled());
                });

    }
}
