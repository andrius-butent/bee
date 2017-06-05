package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.PRP_TA_SERVICE_FROM;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.PRP_TA_SERVICE_TO;
import static com.butent.bee.shared.modules.transport.TransportConstants.COL_CUSTOMER;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.modules.classifiers.ClassifierUtils;
import com.butent.bee.client.modules.mail.NewMailMessage;
import com.butent.bee.client.modules.trade.acts.TradeActKeeper;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActTimeUnit;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class SalesInvoiceForm extends PrintFormInterceptor {

  SalesInvoiceForm() {
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    if (BeeUtils.inListSame(name, COL_SALE_PAYER, COL_TRADE_SUPPLIER)) {
      description.setAttribute("editEnabled",
          BeeUtils.toString(!TradeActKeeper.isClientArea()).toLowerCase());
    } else if (BeeUtils.same(name, COL_TRADE_CUSTOMER)) {
      description.setAttribute("editForm", TradeActKeeper.isClientArea()
          ? FORM_NEW_COMPANY : FORM_COMPANY);
    }

    return super.beforeCreateWidget(name, description);
  }

  @Override
  public FormInterceptor getInstance() {
    return new SalesInvoiceForm();
  }

  @Override
  protected ReportUtils.ReportCallback getReportCallback() {
    return new ReportUtils.ReportCallback() {
      @Override
      public Widget getActionWidget() {
        FaLabel action = new FaLabel(FontAwesome.ENVELOPE_O);
        action.setTitle(Localized.dictionary().trWriteEmail());
        return action;
      }

      @Override
      public void accept(FileInfo fileInfo) {
        FormView form = getFormView();
        Long id = getActiveRowId();

        String invoice = BeeUtils.same(form.getViewName(), VIEW_SALES)
            ? BeeUtils.join("", form.getStringValue(COL_TRADE_INVOICE_PREFIX),
            form.getStringValue(COL_TRADE_INVOICE_NO))
            : BeeUtils.join("_", form.getCaption(), form.getActiveRowId());

        if (!BeeUtils.isEmpty(invoice)) {
          fileInfo.setCaption(invoice + ".pdf");
        }

        Long companyId = form.getLongValue(COL_COMPANY);

        if (BeeUtils.same(form.getViewName(), VIEW_SALES)) {
          companyId = form.getLongValue(COL_CUSTOMER);
        }

        String content = BeeUtils.same(form.getViewName(), VIEW_SALES)
            ? Localized.dictionary().trdInvoice()
            : Localized.dictionary().tradeAct();

        Queries.getValue(VIEW_COMPANIES, BeeUtils.unbox(companyId),
            COL_EMAIL, new RpcCallback<String>() {
              @Override
              public void onSuccess(String email) {
                NewMailMessage.create(email, invoice, content,
                    Collections.singleton(fileInfo), (messageId, saveMode) -> {
                      if (!BeeUtils.same(form.getViewName(), VIEW_SALES)) {
                        return;
                      }

                      DataInfo info = Data.getDataInfo(VIEW_SALE_FILES);

                      Queries.insert(info.getViewName(),
                          Arrays.asList(
                              info.getColumn(COL_SALE),
                              info.getColumn(AdministrationConstants.COL_FILE),
                              info.getColumn(COL_NOTES),
                              info.getColumn(MailConstants.COL_MESSAGE)),
                          Arrays.asList(
                              BeeUtils.toString(id),
                              BeeUtils.toString(fileInfo.getId()),
                              TimeUtils.nowMinutes().toCompactString(),
                              BeeUtils.toString(messageId)),
                          null,
                          new RowInsertCallback(info.getViewName()) {
                            @Override
                            public void onSuccess(BeeRow result) {
                              Data.onTableChange(info.getTableName(),
                                  DataChangeEvent.RESET_REFRESH);
                              form.updateCell("IsSentToEmail", BeeConst.STRING_TRUE);
                              form.refreshBySource("IsSentToEmail");
                              form.getViewPresenter().handleAction(Action.SAVE);
                              super.onSuccess(result);
                            }
                          });
                    });
              }
            });
      }
    };
  }

  @Override
  protected void getReportData(Consumer<BeeRowSet[]> dataConsumer) {
    long sale = getActiveRowId();

    Queries.getRowSet(VIEW_SALE_ITEMS, null, Filter.equals(COL_SALE, sale),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet saleItems) {
            Filter filter = Filter.equals(COL_SALE, sale);

            Map<String, Filter> filters = new HashMap<>();
            filters.put(TradeActConstants.VIEW_TRADE_ACT_ITEMS, filter);
            filters.put(TradeActConstants.VIEW_TRADE_ACT_INVOICES, filter);

            Queries.getData(Arrays.asList(TradeActConstants.VIEW_TRADE_ACT_ITEMS,
                TradeActConstants.VIEW_TRADE_ACT_INVOICES), filters, CachingPolicy.NONE,
                new Queries.DataCallback() {
                  @Override
                  public void onSuccess(Collection<BeeRowSet> result) {
                    for (BeeRowSet rSet : result) {
                      if (!DataUtils.isEmpty(rSet)) {

                        if (Objects.equals(TradeActConstants.VIEW_TRADE_ACT_ITEMS,
                            rSet.getViewName())) {

                          fillSaleProperties(false, rSet, saleItems);

                        } else {
                          fillSaleProperties(true, rSet, saleItems);
                        }
                      }
                    }

                    dataConsumer.accept(new BeeRowSet[] {saleItems});
                  }
                });
          }
        });
  }

  @Override
  protected void getReportParameters(Consumer<Map<String, String>> parametersConsumer) {
    Map<String, Long> companies = new HashMap<>();

    for (String col : Arrays.asList(COL_TRADE_SUPPLIER, COL_TRADE_CUSTOMER, COL_SALE_PAYER)) {
      Long id = getLongValue(col);

      if (DataUtils.isId(id)) {
        companies.put(col, id);
      }
    }
    if (!companies.containsKey(COL_TRADE_SUPPLIER)) {
      companies.put(COL_TRADE_SUPPLIER, BeeKeeper.getUser().getCompany());
    }

    super.getReportParameters(defaultParameters ->
        ClassifierUtils.getCompaniesInfo(companies, companiesInfo -> {
          defaultParameters.putAll(companiesInfo);

          Filter filter = Filter.and(Filter.equals(COL_COMPANY, companies.get(COL_TRADE_CUSTOMER)),
              Filter.equals(ALS_CATEGORY_NAME, "Nuomos Sutartys"));

          Queries.getRowSet(DocumentConstants.VIEW_DOCUMENTS, Arrays.asList(COL_DOCUMENT_NUMBER,
              ALS_STATUS_NAME, COL_DOCUMENT_DATE), filter, new Order(COL_DOCUMENT_DATE, false),
              new Queries.RowSetCallback() {
                @Override
                public void onSuccess(BeeRowSet result) {
                  String nr = BeeConst.STRING_EMPTY;

                  if (result.getNumberOfRows() > 0) {
                    if (result.getNumberOfRows() == 1) {
                      nr = result.getRow(0).getString(result.getColumnIndex(
                          COL_DOCUMENT_NUMBER));
                    } else {
                      List<BeeRow> rows = DataUtils.filterRows(result, ALS_STATUS_NAME,
                          "Pagrindinė");

                      if (rows.size() > 0) {
                        nr = rows.get(0).getString(result.getColumnIndex(COL_DOCUMENT_NUMBER));
                      } else {
                        nr = result.getRow(0).getString(result.getColumnIndex(COL_DOCUMENT_NUMBER));
                      }
                    }
                  }

                  defaultParameters.put(TradeActConstants.WIDGET_TA_CONTRACT, nr);
                  parametersConsumer.accept(defaultParameters);
                }
              });
        }));
  }

  private static void fillSaleProperties(boolean forAllSaleItems, BeeRowSet rowSet,
      BeeRowSet saleItems) {
    int objNameIdx = rowSet.getColumnIndex(ALS_OBJECT_NAME);
    int tradeSeriesIdx = rowSet.getColumnIndex("Trade" + COL_SERIES_NAME);
    int tradeNumberIdx = rowSet.getColumnIndex("Trade" + COL_TRADE_NUMBER);

    String obj;
    String tradeAct;

    if (forAllSaleItems) {
      for (BeeRow row : rowSet) {
        obj = row.getString(objNameIdx);
        tradeAct = BeeUtils.joinWords(row.getString(tradeSeriesIdx), row.getString(tradeNumberIdx));
        if (!BeeUtils.isEmpty(obj)) {
          saleItems.setTableProperty(COL_OBJECT, COL_OBJECT);
        }

        BeeRow saleItem = saleItems.getRowById(row.getLong(rowSet.getColumnIndex("SaleItem")));

        saleItem.setProperty(ALS_OBJECT_NAME, obj);
        saleItem.setProperty(TradeActConstants.COL_TRADE_ACT, tradeAct);
        saleItem.setProperty(COL_ITEM_IS_SERVICE, COL_ITEM_IS_SERVICE);

        saleItem.setProperty(PRP_TA_SERVICE_FROM, BeeUtils.toString(row.getInteger(
            rowSet.getColumnIndex("DateFrom"))));
        saleItem.setProperty(PRP_TA_SERVICE_TO, BeeUtils.toString(row.getInteger(
            rowSet.getColumnIndex("DateTo"))));

        if (row.getInteger(rowSet.getColumnIndex("DateFrom")) != null
            || row.getInteger(rowSet.getColumnIndex("DateTo")) != null) {

          saleItems.setTableProperty("HasDate", "HasDate");
        }

        if (row.getInteger(rowSet.getColumnIndex(COL_TIME_UNIT)) != null) {
          saleItems.setTableProperty("HasTimeUnit", "HasTimeUnit");
          saleItem.setProperty(COL_TIME_UNIT, EnumUtils.getCaption(TradeActTimeUnit.class,
              row.getInteger(rowSet.getColumnIndex(COL_TIME_UNIT))));
        }
      }
    } else {
      BeeRow row = rowSet.getRow(0);
      obj = row.getString(objNameIdx);
      tradeAct = BeeUtils.joinWords(row.getString(tradeSeriesIdx), row.getString(tradeNumberIdx));

      if (!BeeUtils.isEmpty(obj)) {
        saleItems.setTableProperty(COL_OBJECT, COL_OBJECT);
      }

      for (BeeRow saleItem : saleItems) {
        saleItem.setProperty(ALS_OBJECT_NAME, obj);
        saleItem.setProperty(TradeActConstants.COL_TRADE_ACT, tradeAct);
      }
    }
  }
}
