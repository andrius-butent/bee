package com.butent.bee.client.modules.administration;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AdministrationUtils {

  private static final String STYLE_UPDATE_RATES_PREFIX = BeeConst.CSS_CLASS_PREFIX
      + "co-updateRates-";

  public static void blockHost(String caption, final String host,
      final NotificationListener notificationListener, final Callback<String> callback) {

    if (BeeUtils.isEmpty(host)) {
      if (callback != null) {
        callback.onFailure("host not specified");
      }
      return;
    }

    Global.confirm(caption, Icon.WARNING, Lists.newArrayList(host),
        Localized.dictionary().actionBlock(), Localized.dictionary().actionCancel(), () -> {
          ParameterList args = AdministrationKeeper.createArgs(SVC_BLOCK_HOST);
          args.addDataItem(COL_IP_FILTER_HOST, host);

          BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              if (response.hasResponse()) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_IP_FILTERS);
              }

              if (notificationListener != null) {
                response.notify(notificationListener);
              }

              if (response.is(host)) {
                if (notificationListener != null) {
                  notificationListener.notifyInfo(Localized.dictionary().ipBlocked(), host);
                }
                if (callback != null) {
                  callback.onSuccess(host);
                }
              }
            }
          });
        });
  }

  public static void createUser(String caption, final String login, final String password,
      final UserInterface userInterface, final Map<String, String> parameters,
      final NotificationListener notificationListener, final IdCallback callback) {

    if (BeeUtils.isEmpty(login)) {
      if (callback != null) {
        callback.onFailure("login not specified");
      }
      return;
    }

    final String pswd = BeeUtils.notEmpty(password, login.trim().substring(0, 1));

    String separator = BeeConst.STRING_COLON + BeeConst.STRING_SPACE;
    final String msgLogin = Localized.dictionary().userLogin() + separator + login.trim();
    final String msgPswd = Localized.dictionary().password() + separator + pswd.trim();

    Global.confirm(caption, Icon.QUESTION, Lists.newArrayList(msgLogin, msgPswd),
        Localized.dictionary().actionCreate(), Localized.dictionary().actionCancel(), () -> {
          ParameterList args = AdministrationKeeper.createArgs(SVC_CREATE_USER);
          args.addDataItem(COL_LOGIN, login);
          args.addDataItem(COL_PASSWORD, Codec.encodePassword(pswd));

          if (userInterface != null) {
            args.addDataItem(COL_USER_INTERFACE, userInterface.ordinal());
          }
          if (!BeeUtils.isEmpty(parameters)) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
              if (!BeeUtils.anyEmpty(entry.getKey(), entry.getValue())) {
                args.addDataItem(entry.getKey(), entry.getValue());
              }
            }
          }

          BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
            @Override
            public void onResponse(ResponseObject response) {
              if (notificationListener != null) {
                response.notify(notificationListener);
              }

              if (response.hasResponse(Long.class)) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_USERS);

                if (notificationListener != null) {
                  notificationListener.notifyInfo(Localized.dictionary().newUser(), msgLogin,
                      msgPswd);
                }
                if (callback != null) {
                  callback.onSuccess(response.getResponseAsLong());
                }
              }
            }
          });
        });
  }

  public static void updateExchangeRates() {
    Flow panel = new Flow(STYLE_UPDATE_RATES_PREFIX + "panel");

    Label lowLabel = new Label(Localized.dictionary().updateExchangeRatesDateLow());
    lowLabel.addStyleName(STYLE_UPDATE_RATES_PREFIX + "lowLabel");
    panel.add(lowLabel);

    final InputDate lowInput = new InputDate();
    lowInput.addStyleName(STYLE_UPDATE_RATES_PREFIX + "lowInput");
    lowInput.setDate(TimeUtils.today());
    lowInput.setNullable(false);
    panel.add(lowInput);

    CustomDiv rangeSeparator = new CustomDiv(STYLE_UPDATE_RATES_PREFIX + "rangeSeparator");
    panel.add(rangeSeparator);

    Label highLabel = new Label(Localized.dictionary().updateExchangeRatesDateHigh());
    highLabel.addStyleName(STYLE_UPDATE_RATES_PREFIX + "highLabel");
    panel.add(highLabel);

    final InputDate highInput = new InputDate();
    highInput.addStyleName(STYLE_UPDATE_RATES_PREFIX + "highInput");
    highInput.setDate(TimeUtils.today());
    highInput.setNullable(false);
    panel.add(highInput);

    final Flow output = new Flow(STYLE_UPDATE_RATES_PREFIX + "output");
    panel.add(output);

    CustomDiv actionSeparator = new CustomDiv(STYLE_UPDATE_RATES_PREFIX + "actionSeparator");
    panel.add(actionSeparator);

    final Button submit = new Button(Localized.dictionary().actionUpdate());
    submit.addStyleName(STYLE_UPDATE_RATES_PREFIX + "submit");
    panel.add(submit);

    Button cancel = new Button(Localized.dictionary().actionCancel());
    cancel.addStyleName(STYLE_UPDATE_RATES_PREFIX + "cancel");
    panel.add(cancel);

    String caption = Localized.dictionary().updateExchangeRatesDialogCaption();
    final DialogBox dialog = DialogBox.create(caption, STYLE_UPDATE_RATES_PREFIX + "dialog");
    dialog.setWidget(panel);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.center();

    submit.addClickHandler(event -> {
      JustDate lowDate = lowInput.getDate();
      if (lowDate == null) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().valueRequired());
        lowInput.setFocus(true);
        return;
      }

      JustDate highDate = highInput.getDate();
      if (highDate == null) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().valueRequired());
        highInput.setFocus(true);
        return;
      }

      if (TimeUtils.isMore(lowDate, highDate)) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().invalidRange(),
            BeeUtils.joinWords(lowDate, highDate));
        return;
      }

      submit.setEnabled(false);

      output.clear();
      output.add(new Image(Global.getImages().loading()));

      ParameterList params = AdministrationKeeper.createArgs(SVC_UPDATE_EXCHANGE_RATES);
      params.addQueryItem(VAR_DATE_LOW, lowDate.getDays());
      params.addQueryItem(VAR_DATE_HIGH, highDate.getDays());

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          output.clear();

          if (response.hasMessages()) {
            for (ResponseMessage rm : response.getMessages()) {
              Label label = new Label(rm.getMessage());
              String styleSuffix = (rm.getLevel() == null)
                  ? "message" : rm.getLevel().name().toLowerCase();
              label.addStyleName(STYLE_UPDATE_RATES_PREFIX + styleSuffix);

              output.add(label);
            }
          }

          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_CURRENCY_RATES);

          submit.setEnabled(true);
        }
      });
    });

    cancel.addClickHandler(event -> dialog.close());
  }

  static void updateRelatedDimensions(DataView targetView, IsRow targetRow,
      DataInfo sourceInfo, IsRow sourceRow, int excludeOrdinal) {

    if (targetView != null && targetRow != null && sourceInfo != null && sourceRow != null) {
      List<BeeColumn> targetColumns = targetView.getDataColumns();
      List<BeeColumn> sourceColumns = sourceInfo.getColumns();

      for (int ordinal = 1; ordinal <= Dimensions.getObserved(); ordinal++) {
        if (ordinal != excludeOrdinal) {
          String relationColumn = Dimensions.getRelationColumn(ordinal);

          int tri = DataUtils.getColumnIndex(relationColumn, targetColumns);
          int sri = DataUtils.getColumnIndex(relationColumn, sourceColumns);

          if (tri >= 0 && sri >= 0
              && DataUtils.isId(sourceRow.getLong(sri))
              && !Objects.equals(targetRow.getLong(tri), sourceRow.getLong(sri))) {

            if (targetView.isFlushable()) {
              targetRow.setValue(tri, sourceRow.getLong(sri));

              for (String colName : new String[] {
                  Dimensions.getNameColumn(ordinal),
                  Dimensions.getBackgroundColumn(ordinal),
                  Dimensions.getForegroundColumn(ordinal)}) {

                int ti = DataUtils.getColumnIndex(colName, targetColumns);
                int si = DataUtils.getColumnIndex(colName, sourceColumns);

                if (ti >= 0 && si >= 0) {
                  targetRow.setValue(ti, sourceRow.getString(si));
                }
              }

              targetView.refreshBySource(relationColumn);

            } else {
              targetRow.preliminaryUpdate(tri, sourceRow.getString(sri));
            }
          }
        }
      }
    }
  }

  private AdministrationUtils() {
  }
}
