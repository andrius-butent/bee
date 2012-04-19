package com.butent.bee.client.validation;

import com.butent.bee.client.dialog.NotificationListener;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.utils.BeeUtils;

public class ValidationHelper {

  public static boolean validateCell(CellValidation validation, HasCellValidationHandlers source) {
    Assert.notNull(validation);
    if (source == null) {
      return validateCell(validation);
    }
    
    CellValidateEvent event = new CellValidateEvent(validation);
    event.setValidationPhase(ValidationPhase.PRE);
    boolean ok = source.fireCellValidation(event);
    
    if (ok && !event.isCanceled()) {
      event.setValidationPhase(ValidationPhase.DEF);
      ok = validateCell(event.getCellValidation());
    }

    if (ok && !event.isCanceled()) {
      event.setValidationPhase(ValidationPhase.POST);
      ok = source.fireCellValidation(event);
    }

    return ok;
  }

  public static boolean validateRow(IsRow row, Evaluator validation,
      NotificationListener notificationListener) {
    Assert.notNull(row);
    if (validation == null) {
      return true;
    }

    validation.update(row);
    String message = validation.evaluate();

    if (BeeUtils.isEmpty(message)) {
      return true;
    } else {
      if (notificationListener != null) {
        notificationListener.notifySevere(message);
      }
      return false;
    }
  }

  private static boolean validateCell(CellValidation cv) {
    if (!cv.isForced() && BeeUtils.equalsTrimRight(cv.getOldValue(), cv.getNewValue())) {
      return true;
    }
    String errorMessage = null;

    if (cv.getEvaluator() != null) {
      cv.getEvaluator().update(cv.getRow(), BeeConst.UNDEF, cv.getColIndex(), cv.getType(),
          cv.getOldValue(), cv.getNewValue());
      String msg = cv.getEvaluator().evaluate();
      if (!BeeUtils.isEmpty(msg)) {
        errorMessage = msg;
      }
    }

    if (errorMessage == null && !cv.isNullable() && BeeUtils.isEmpty(cv.getNewValue())) {
      errorMessage = "Value required";
    }

    if (errorMessage == null
        && (!BeeUtils.isEmpty(cv.getMinValue()) || !BeeUtils.isEmpty(cv.getMaxValue()))) {
      Value value = Value.parseValue(cv.getType(), cv.getNewValue(), false);

      if (!BeeUtils.isEmpty(cv.getMinValue())
          && value.compareTo(Value.parseValue(cv.getType(), cv.getMinValue(), true)) < 0) {
        errorMessage = BeeUtils.concat(1, errorMessage, "Min value:", cv.getMinValue());
      }
      if (!BeeUtils.isEmpty(cv.getMaxValue())
          && value.compareTo(Value.parseValue(cv.getType(), cv.getMaxValue(), true)) > 0) {
        errorMessage = BeeUtils.concat(1, errorMessage, "Max value:", cv.getMaxValue());
      }
    }

    if (errorMessage == null) {
      return true;
    } else {
      if (cv.getNotificationListener() != null) {
        cv.getNotificationListener().notifySevere(cv.getCaption(), errorMessage);
      }
      return false;
    }
  }

  private ValidationHelper() {
  }
}
