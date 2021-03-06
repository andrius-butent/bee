package com.butent.bee.client.ui;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.XMLParser;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.presenter.FormPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormImpl;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.ProviderType;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Preloader;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and handles user interface forms.
 */

public final class FormFactory {

  @FunctionalInterface
  public interface FormViewCallback {
    default void onFailure(String... reason) {
      BeeKeeper.getScreen().notifyWarning(reason);
    }

    void onSuccess(FormDescription formDescription, FormView result);
  }

  public interface WidgetDescriptionCallback {
    WidgetDescription getLastWidgetDescription();

    void onFailure(Object... messages);

    void onSuccess(WidgetDescription result, IdentifiableWidget widget);
  }

  private static final BeeLogger logger = LogUtils.getLogger(FormFactory.class);

  public static final String TAG_FORM = "Form";

  private static final Map<String, FormDescription> descriptionCache = new HashMap<>();
  private static final Map<String, Pair<FormInterceptor, Integer>> formInterceptors =
      new HashMap<>();

  private static final Map<String, Preloader> formPreloaders = new HashMap<>();

  private static final Multimap<String, String> hiddenWidgets = HashMultimap.create();

  public static void clearDescriptionCache() {
    descriptionCache.clear();
  }

  public static IdentifiableWidget createForm(FormDescription formDescription, String viewName,
      List<BeeColumn> columns, WidgetDescriptionCallback widgetDescriptionCallback,
      FormInterceptor formInterceptor) {
    Assert.notNull(formDescription);
    Assert.notNull(widgetDescriptionCallback);

    return createWidget(formDescription.getName(), formDescription.getFormElement(), viewName,
        columns, widgetDescriptionCallback, formInterceptor, "createForm:");
  }

  public static FormDescription createFormDescription(String formName,
      Map<String, String> formAttributes, FormWidget rootWidget,
      Map<String, String> rootAttributes) {
    Assert.notNull(formName);

    Document doc = XMLParser.createDocument();
    Element formElement = doc.createElement(TAG_FORM);

    formElement.setAttribute(UiConstants.ATTR_NAME, formName.trim());
    XmlUtils.setAttributes(formElement, formAttributes);

    if (rootWidget != null) {
      Element rootElement = doc.createElement(rootWidget.getTagName());
      XmlUtils.setAttributes(rootElement, rootAttributes);
      formElement.appendChild(rootElement);
    }
    return new FormDescription(formElement);
  }

  public static void createFormView(final String formName, final String viewName,
      final List<BeeColumn> columns, final boolean addStyle, final FormInterceptor formInterceptor,
      final FormViewCallback viewCallback) {

    Assert.notEmpty(formName);
    Assert.notNull(viewCallback);

    getFormDescription(formName, result -> {
      FormView view = new FormImpl(formName);
      view.create(result, viewName, columns, addStyle, formInterceptor);
      viewCallback.onSuccess(result, view);
    });
  }

  public static void createFormView(String formName, String viewName, List<BeeColumn> columns,
      boolean addStyle, FormViewCallback viewCallback) {
    createFormView(formName, viewName, columns, addStyle, getFormInterceptor(formName),
        viewCallback);
  }

  public static IdentifiableWidget createWidget(String formName, Element parent, String viewName,
      List<BeeColumn> columns, WidgetDescriptionCallback widgetDescriptionCallback,
      WidgetInterceptor widgetCallback, String messagePrefix) {

    Assert.notNull(parent);
    Assert.notNull(widgetDescriptionCallback, "createWidget: widgetDescriptionCallback is null");

    List<Element> children = XmlUtils.getChildrenElements(parent);
    if (BeeUtils.isEmpty(children)) {
      logger.severe(messagePrefix, "element has no children");
      return null;
    }

    Element root = null;
    FormWidget formWidget = null;
    int count = 0;

    for (Element child : children) {
      FormWidget fw = FormWidget.getByTagName(XmlUtils.getLocalName(child));
      if (fw != null) {
        root = child;
        formWidget = fw;
        count++;
      }
    }

    if (count <= 0) {
      logger.severe(messagePrefix, "root widget not found");
      return null;
    }
    if (count > 1) {
      logger.severe(messagePrefix, "element has", count, "root widgets");
      return null;
    }

    IdentifiableWidget widget = formWidget.create(formName, root, viewName, columns,
        widgetDescriptionCallback, widgetCallback);
    if (widget == null) {
      logger.severe(messagePrefix, "cannot create root widget", formWidget);
    }
    return widget;
  }

  public static FormDescription getFormDescription(String formName) {
    return descriptionCache.get(getFormKey(Assert.notEmpty(formName)));
  }

  public static void getFormDescription(final String name,
      final Callback<FormDescription> callback) {

    Assert.notEmpty(name);
    Assert.notNull(callback);

    Preloader preloader = formPreloaders.get(name);

    if (preloader == null) {
      loadDescription(name, callback);

    } else {
      preloader.accept(() -> loadDescription(name, callback));

      if (preloader.disposable()) {
        formPreloaders.remove(name);
      }
    }
  }

  public static FormInterceptor getFormInterceptor(String formName) {
    Assert.notEmpty(formName);
    Pair<FormInterceptor, Integer> pair = formInterceptors.get(getFormKey(formName));
    if (pair == null || pair.getA() == null) {
      return null;
    }

    pair.setB(pair.getB() + 1);
    if (pair.getB() > 1) {
      return pair.getA().getInstance();
    } else {
      return pair.getA();
    }
  }

  public static List<Property> getInfo() {
    List<Property> info = new ArrayList<>();

    info.add(new Property("Registered Callbacks", BeeUtils.bracket(formInterceptors.size())));
    for (Map.Entry<String, Pair<FormInterceptor, Integer>> entry : formInterceptors.entrySet()) {
      info.add(new Property(entry.getKey(), BeeUtils.toString(entry.getValue().getB())));
    }

    info.add(new Property("Description Cache", BeeUtils.bracket(descriptionCache.size())));
    for (Map.Entry<String, FormDescription> entry : descriptionCache.entrySet()) {
      info.add(new Property(entry.getKey(),
          BeeUtils.toString(entry.getValue().getFormElement().toString().length())));
    }
    return info;
  }

  public static String getSupplierKey(String formName) {
    Assert.notEmpty(formName);
    return ViewFactory.SupplierKind.FORM.getKey(formName);
  }

  public static FormWidget getWidgetType(BeeColumn column) {
    Assert.notNull(column);

    if (column.isForeign() && !column.isEditable()) {
      return FormWidget.DATA_SELECTOR;
    }

    FormWidget widgetType;

    switch (column.getType()) {
      case BOOLEAN:
        widgetType = FormWidget.CHECK_BOX;
        break;
      case DATE:
        widgetType = FormWidget.INPUT_DATE;
        break;
      case DATE_TIME:
        widgetType = FormWidget.INPUT_DATE_TIME;
        break;
      case DECIMAL:
        widgetType = FormWidget.INPUT_DECIMAL;
        break;
      case INTEGER:
        widgetType = FormWidget.INPUT_INTEGER;
        break;
      case LONG:
        widgetType = FormWidget.INPUT_LONG;
        break;
      case NUMBER:
        widgetType = FormWidget.INPUT_DOUBLE;
        break;
      case TEXT:
        widgetType = column.isText() ? FormWidget.INPUT_AREA : FormWidget.INPUT_TEXT;
        break;
      case TIME_OF_DAY:
        widgetType = FormWidget.INPUT_TIME_OF_DAY;
        break;
      default:
        Assert.untouchable();
        widgetType = null;
    }
    return widgetType;
  }

  public static void hideWidget(String formName, String widgetName) {
    Assert.notEmpty(formName);
    Assert.notEmpty(widgetName);

    hiddenWidgets.put(formName, widgetName);
  }

  public static boolean isHidden(String formName, String widgetName) {
    return hiddenWidgets.containsEntry(formName, widgetName);
  }

  public static void openForm(FormDescription formDescription, FormInterceptor formInterceptor,
      PresenterCallback presenterCallback) {

    String viewName = formDescription.getViewName();
    if (BeeUtils.isEmpty(viewName)) {
      showForm(formDescription, formInterceptor, presenterCallback);
      return;
    }

    if (formInterceptor != null) {
      BeeRowSet rowSet = formInterceptor.getRowSet();
      if (rowSet != null) {
        showForm(formDescription, viewName, rowSet.getNumberOfRows(), rowSet,
            ProviderType.LOCAL, CachingPolicy.NONE, formInterceptor, presenterCallback);
        return;
      }
    }

    getInitialRowSet(viewName, formDescription, formInterceptor, presenterCallback);
  }

  public static void openForm(String formName) {
    openForm(formName, getFormInterceptor(formName));
  }

  /**
   * This method should be used in sync with {@code ViewFactory.registerSupplier}.
   */
  public static void openForm(String formName, FormInterceptor formInterceptor) {
    openForm(formName, formInterceptor, ViewHelper.getPresenterCallback());
  }

  public static void openForm(String formName, final FormInterceptor formInterceptor,
      final PresenterCallback presenterCallback) {

    getFormDescription(formName, result -> openForm(result, formInterceptor, presenterCallback));
  }

  public static FormDescription parseFormDescription(String xml) {
    Assert.notEmpty(xml);

    Document xmlDoc = XmlUtils.parse(xml);
    if (xmlDoc == null) {
      return null;
    }
    Element formElement = xmlDoc.getDocumentElement();
    if (formElement == null) {
      logger.severe("xml form element not found");
      return null;
    }

    return new FormDescription(formElement);
  }

  public static void putFormDescription(String formName, FormDescription formDescription) {
    descriptionCache.put(getFormKey(Assert.notEmpty(formName)), Assert.notNull(formDescription));
  }

  public static void registerFormInterceptor(String formName, FormInterceptor interceptor) {
    Assert.notEmpty(formName);
    formInterceptors.put(getFormKey(formName), Pair.of(interceptor, 0));
  }

  public static void registerPreloader(String formName, Preloader preloader) {
    Assert.notEmpty(formName);
    Assert.notNull(preloader);

    formPreloaders.put(formName, preloader);
  }

  private static String getFormKey(String formName) {
    return formName.trim().toLowerCase();
  }

  private static void getInitialRowSet(final String viewName,
      final FormDescription formDescription, final FormInterceptor interceptor,
      final PresenterCallback presenterCallback) {

    final CachingPolicy cachingPolicy = CachingPolicy.NONE;

    Collection<Property> options = PropertyUtils.createProperties(Service.VAR_VIEW_SIZE, true);

    Queries.getRowSet(viewName, null, null, null, 0, DataUtils.getMaxInitialRowSetSize(),
        cachingPolicy, options, (Queries.RowSetCallback) rowSet -> {
          int rc = Math.max(rowSet.getNumberOfRows(),
              BeeUtils.toInt(rowSet.getTableProperty(Service.VAR_VIEW_SIZE)));

          showForm(formDescription, viewName, rc, rowSet, ProviderType.DEFAULT, cachingPolicy,
              interceptor, presenterCallback);
        });
  }

  private static void loadDescription(final String name, final Callback<FormDescription> callback) {
    final String key = getFormKey(name);
    if (descriptionCache.containsKey(key)) {
      callback.onSuccess(descriptionCache.get(key));
      return;
    }

    ParameterList params = new ParameterList(Service.GET_FORM);
    params.setSummary(name);

    BeeKeeper.getRpc().sendText(params, BeeUtils.trim(name), response -> {
      if (response.hasResponse(String.class)) {
        FormDescription fd = parseFormDescription((String) response.getResponse());
        if (fd == null) {
          callback.onFailure("form", name, "description not created");
        } else {
          if (fd.cacheDescription()) {
            descriptionCache.put(key, fd);
          }
          callback.onSuccess(fd);
        }
      } else {
        callback.onFailure("get form description", name, "response not a string");
      }
    });
  }

  private static void showForm(FormDescription formDescription, FormInterceptor interceptor,
      PresenterCallback presenterCallback) {

    showForm(formDescription, null, BeeConst.UNDEF, null, ProviderType.CACHED,
        CachingPolicy.NONE, interceptor, presenterCallback);
  }

  private static void showForm(FormDescription formDescription, String viewName, int rowCount,
      BeeRowSet rowSet, ProviderType providerType, CachingPolicy cachingPolicy,
      FormInterceptor interceptor, PresenterCallback presenterCallback) {

    FormPresenter presenter = new FormPresenter(formDescription, viewName, rowCount, rowSet,
        providerType, cachingPolicy, interceptor);

    if (interceptor != null) {
      interceptor.afterCreatePresenter(presenter);
    }
    if (presenterCallback != null) {
      presenterCallback.onCreate(presenter);
    }
  }

  private FormFactory() {
  }
}
