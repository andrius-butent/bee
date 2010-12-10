package com.butent.bee.egg.client.dialog;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.egg.client.composite.ButtonGroup;
import com.butent.bee.egg.client.composite.RadioGroup;
import com.butent.bee.egg.client.grid.FlexTable;
import com.butent.bee.egg.client.widget.BeeCheckBox;
import com.butent.bee.egg.client.widget.BeeFileUpload;
import com.butent.bee.egg.client.widget.BeeListBox;
import com.butent.bee.egg.client.widget.BeeSimpleCheckBox;
import com.butent.bee.egg.client.widget.BeeTextBox;
import com.butent.bee.egg.client.widget.InputInteger;
import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.Variable;
import com.butent.bee.egg.shared.BeeService;
import com.butent.bee.egg.shared.BeeStage;
import com.butent.bee.egg.shared.BeeType;
import com.butent.bee.egg.shared.BeeWidget;
import com.butent.bee.egg.shared.utils.BeeUtils;

public class InputBox {
  public void inputVars(BeeStage bst, String cap, Variable... vars) {
    Assert.parameterCount(vars.length + 1, 2);

    FlexTable ft = new FlexTable();

    Widget inp = null;
    String z, w;
    int tp;
    BeeWidget bw;

    int r = 0;
    FocusWidget fw = null;
    boolean ok;

    for (Variable var : vars) {
      tp = var.getType();
      bw = var.getWidget();

      z = var.getCaption();
      if (!BeeUtils.isEmpty(z) && tp != BeeType.TYPE_BOOLEAN) {
        ft.setText(r, 0, z);
      }

      w = var.getWidth();

      ok = false;

      if (bw != null) {
        switch (bw) {
          case LIST:
            inp = new BeeListBox(var);
            ok = true;
            break;
          case RADIO:
            inp = new RadioGroup(var);
            ok = true;
            break;
          default:
            ok = false;
        }
      } else {
        switch (tp) {
          case BeeType.TYPE_FILE:
            inp = new BeeFileUpload(var);
            ok = true;
            break;
          case BeeType.TYPE_BOOLEAN:
            if (BeeUtils.isEmpty(z)) {
              inp = new BeeSimpleCheckBox(var);
            } else {
              inp = new BeeCheckBox(var);
            }
            ok = true;
            break;
          case BeeType.TYPE_INT:
            inp = new InputInteger(var);
            ok = true;
            break;
          default:
            ok = false;
        }
      }

      if (!ok) {
        inp = new BeeTextBox(var);
      }

      if (!BeeUtils.isEmpty(w)) {
        inp.setWidth(w);
      }

      ft.setWidget(r, 1, inp);
      if (fw == null && inp instanceof FocusWidget) {
        fw = (FocusWidget) inp;
      }

      r++;
    }

    ButtonGroup bg = new ButtonGroup();
    if (bst == null) {
      bg.addButton("OK", BeeService.SERVICE_CONFIRM_DIALOG);
    } else {
      bg.addButton("OK", bst);
    }
    bg.addButton("Cancel", BeeService.SERVICE_CANCEL_DIALOG);

    ft.setWidget(r, 0, bg);
    ft.getFlexCellFormatter().setColSpan(r, 0, 2);
    ft.getCellFormatter().setHorizontalAlignment(r, 0, HasHorizontalAlignment.ALIGN_CENTER);

    BeeDialogBox dialog = new BeeDialogBox();

    if (!BeeUtils.isEmpty(cap)) {
      dialog.setText(cap);
    }

    dialog.setAnimationEnabled(true);

    dialog.setWidget(ft);
    dialog.center();

    if (fw != null) {
      fw.setFocus(true);
    }
  }
}
