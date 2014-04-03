package com.butent.bee.client.modules.service;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.composite.Autocomplete;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowUpdateCallback;
import com.butent.bee.client.event.logical.AutocompleteEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.State;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class ServiceObjectForm extends AbstractFormInterceptor implements ClickHandler {

  private final class AutocompleteFilter implements AutocompleteEvent.Handler {

    private final String source;
    private final String criterion;

    private AutocompleteFilter(String source, String criterion) {
      this.source = source;
      this.criterion = criterion;
    }

    @Override
    public void onDataSelector(AutocompleteEvent event) {
      if (event.getState() == State.OPEN) {
        CompoundFilter flt = Filter.and();

        for (String name : new String[] {COL_OBJECT_CATEGORY, COL_SERVICE_OBJECT}) {
          Long id = getLongValue(name);

          if (DataUtils.isId(id)) {
            if (BeeUtils.same(name, COL_OBJECT_CATEGORY)) {
              flt.add(Filter.isEqual(name, Value.getValue(id)));
            } else {
              flt.add(Filter.isNotEqual(name, Value.getValue(id)));
            }
          }
        }

        if (BeeUtils.isEmpty(source)) {
          flt.add(Filter.isNull(COL_CRITERIA_GROUP_NAME));

          if (!BeeUtils.isEmpty(criterion)) {
            flt.add(Filter.isEqual(COL_CRITERION_NAME, Value.getValue(criterion)));
          }

        } else if (!BeeUtils.same(source, COL_CRITERIA_GROUP_NAME)) {
          if (groupsGrid != null) {
            flt.add(Filter.isEqual(COL_CRITERIA_GROUP_NAME,
                groupsGrid.getPresenter().getActiveRow().getValue(groupsGrid.getPresenter()
                    .getGridView().getDataIndex(COL_CRITERIA_GROUP_NAME))));
          }

          if (BeeUtils.same(source, COL_CRITERION_VALUE) && criteriaGrid != null) {
            flt.add(Filter.isEqual(COL_CRITERION_NAME,
                criteriaGrid.getPresenter().getActiveRow().getValue(criteriaGrid.getPresenter()
                    .getGridView().getDataIndex(COL_CRITERION_NAME))));
          }
        }

        event.getSelector().setAdditionalFilter(flt);
      }
    }
  }

  private final GridInterceptor childInterceptor = new AbstractGridInterceptor() {
    @Override
    public void afterCreateEditor(String source, Editor editor, boolean embedded) {
      if (editor instanceof Autocomplete) {
        ((Autocomplete) editor).addAutocompleteHandler(new AutocompleteFilter(source, null));
      }
    }
  };

  private HasWidgets panel;
  private Long groupId;
  private final Map<String, String> criteriaHistory = Maps.newLinkedHashMap();
  private final Map<String, Editor> criteria = Maps.newLinkedHashMap();
  private final Map<String, Long> ids = Maps.newHashMap();

  private ChildGrid groupsGrid;
  private ChildGrid criteriaGrid;

  ServiceObjectForm() {
    super();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, "MainCriteriaEditor")) {
      widget.asWidget().addDomHandler(this, ClickEvent.getType());

    } else if (widget instanceof HasWidgets && BeeUtils.same(name, "MainCriteriaContainer")) {
      panel = (HasWidgets) widget;

    } else if (widget instanceof ChildGrid) {
      ChildGrid grid = (ChildGrid) widget;

      if (BeeUtils.same(name, VIEW_CRITERIA_GROUPS)) {
        groupsGrid = grid;
        grid.setGridInterceptor(childInterceptor);

      } else if (BeeUtils.same(name, VIEW_CRITERIA)) {
        criteriaGrid = grid;
        grid.setGridInterceptor(childInterceptor);
      }
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    if (!forced) {
      save(result);
    }
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    save(result);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ServiceObjectForm();
  }

  @Override
  public void onClick(ClickEvent event) {
    LocalizableConstants loc = Localized.getConstants();

    Global.inputCollection(loc.mainCriteria(), loc.name(), true, criteria.keySet(),
        new Consumer<Collection<String>>() {
          @Override
          public void accept(Collection<String> collection) {
            Map<String, Editor> oldCriteria = Maps.newHashMap(criteria);
            criteria.clear();

            for (String crit : collection) {
              Editor input = oldCriteria.get(crit);

              if (input == null) {
                input = createAutocomplete(VIEW_DISTINCT_VALUES, COL_CRITERION_VALUE, crit);
              }
              criteria.put(crit, input);
            }
            render();
          }
        },
        new Function<String, Editor>() {
          @Override
          public Editor apply(String value) {
            Editor editor = createAutocomplete(VIEW_DISTINCT_CRITERIA, COL_CRITERION_NAME, null);
            editor.setValue(value);
            return editor;
          }
        });
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
    LocalizableConstants loc = Localized.getConstants();
    List<String> warnings = Lists.newArrayList();

    if (save(null)) {
      warnings.add(loc.mainCriteria());
    }
    if (!BeeUtils.isEmpty(warnings)) {
      messages.add(BeeUtils.joinWords(loc.changedValues(), warnings));
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    if (BeeUtils.isEmpty(event.getColumns())) {
      save(getActiveRow());
    }
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    requery(row);
    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    requery(newRow);
  }

  private Autocomplete createAutocomplete(String viewName, String column, String value) {
    Autocomplete input = Autocomplete.create(Relation.create(viewName,
        Lists.newArrayList(column)), true);

    input.addAutocompleteHandler(new AutocompleteFilter(null, value));
    return input;
  }

  private void render() {
    if (panel == null) {
      getHeaderView().clearCommandPanel();
      return;
    }
    panel.clear();

    if (criteria.size() > 0) {
      int h = 0;
      FlowPanel labelContainer = new FlowPanel();
      labelContainer.getElement().getStyle().setMarginRight(5, Unit.PX);
      panel.add(labelContainer);

      FlowPanel inputContainer = new FlowPanel();
      inputContainer.addStyleName(StyleUtils.NAME_FLEXIBLE);
      panel.add(inputContainer);

      for (Entry<String, Editor> entry : criteria.entrySet()) {
        Label label = new Label(entry.getKey());
        StyleUtils.setTextAlign(label.getElement(), TextAlign.RIGHT);
        SimplePanel labelDiv = new SimplePanel(label);
        labelContainer.add(labelDiv);

        Widget editor = entry.getValue().asWidget();
        editor.setWidth("100%");
        SimplePanel editorDiv = new SimplePanel(editor);
        inputContainer.add(editorDiv);

        if (!BeeUtils.isPositive(h)) {
          h = BeeUtils.max(labelDiv.getElement().getClientHeight(),
              editorDiv.getElement().getClientHeight());

          if (!BeeUtils.isPositive(h)) {
            h = 20;
          }
          h += 5;
        }
        StyleUtils.setHeight(labelDiv, h);
        StyleUtils.setHeight(editorDiv, h);
      }
    }
  }

  private void requery(IsRow row) {
    criteriaHistory.clear();
    criteria.clear();
    ids.clear();
    groupId = null;
    render();

    Long objId = row.getId();
    if (!DataUtils.isId(objId)) {
      return;
    }

    Queries.getRowSet(VIEW_OBJECT_CRITERIA, null,
        Filter.and(Filter.equals(COL_SERVICE_OBJECT, objId),
            Filter.isNull(COL_CRITERIA_GROUP_NAME)),
        new RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (result.getNumberOfRows() > 0) {
              groupId = result.getRow(0).getId();

              for (BeeRow crit : result.getRows()) {
                String name = Data.getString(VIEW_OBJECT_CRITERIA, crit, COL_CRITERION_NAME);

                if (!BeeUtils.isEmpty(name)) {
                  String value = Data.getString(VIEW_OBJECT_CRITERIA, crit, COL_CRITERION_VALUE);

                  Autocomplete box = createAutocomplete(VIEW_DISTINCT_VALUES,
                      COL_CRITERION_VALUE, name);

                  box.setValue(value);

                  criteriaHistory.put(name, value);
                  criteria.put(name, box);
                  ids.put(name, Data.getLong(VIEW_OBJECT_CRITERIA, crit, "ID"));
                }
              }
              render();
            }
          }
        });
  }

  private boolean save(final IsRow row) {
    final Map<String, String> newValues = Maps.newLinkedHashMap();
    Map<Long, String> changedValues = Maps.newHashMap();
    CompoundFilter flt = Filter.or();
    final Holder<Integer> holder = Holder.of(0);

    for (String crit : criteria.keySet()) {
      String value = criteria.get(crit).getValue();
      value = BeeUtils.isEmpty(value) ? null : value;
      Long id = ids.get(crit);

      if (!criteriaHistory.containsKey(crit) || !Objects.equals(value, criteriaHistory.get(crit))) {
        if (DataUtils.isId(id)) {
          changedValues.put(id, value);
        } else {
          newValues.put(crit, value);
        }
        holder.set(holder.get() + 1);
      }
    }
    for (String crit : ids.keySet()) {
      if (!criteria.containsKey(crit)) {
        flt.add(Filter.compareId(ids.get(crit)));
      }
    }
    if (!flt.isEmpty()) {
      holder.set(holder.get() + 1);
    }
    if (row == null) {
      return BeeUtils.isPositive(holder.get());
    }
    final ScheduledCommand scheduler = new ScheduledCommand() {
      @Override
      public void execute() {
        holder.set(holder.get() - 1);

        if (!BeeUtils.isPositive(holder.get())) {
          Queries.getRow(getViewName(), row.getId(), new RowUpdateCallback(getViewName()) {
            @Override
            public void onSuccess(BeeRow result) {
              super.onSuccess(result);
              getGridView().getGrid().refresh();
            }
          });
        }
      }
    };
    if (!BeeUtils.isEmpty(newValues)) {
      final Consumer<Long> consumer = new Consumer<Long>() {
        @Override
        public void accept(Long id) {
          for (Entry<String, String> entry : newValues.entrySet()) {
            Queries.insert(VIEW_CRITERIA, Data.getColumns(VIEW_CRITERIA,
                Lists.newArrayList(COL_CRITERIA_GROUP, COL_CRITERION_NAME, COL_CRITERION_VALUE)),
                Lists.newArrayList(BeeUtils.toString(id), entry.getKey(), entry.getValue()), null,
                new RowCallback() {
                  @Override
                  public void onSuccess(BeeRow result) {
                    scheduler.execute();
                  }
                });
          }
        }
      };

      if (!DataUtils.isId(groupId)) {
        Queries.insert(VIEW_CRITERIA_GROUPS,
            Data.getColumns(VIEW_CRITERIA_GROUPS, Lists.newArrayList(COL_SERVICE_OBJECT)),
            Lists.newArrayList(BeeUtils.toString(row.getId())), null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow result) {
                consumer.accept(result.getId());
              }
            });
      } else {
        consumer.accept(groupId);
      }
    }

    if (!BeeUtils.isEmpty(changedValues)) {
      for (Entry<Long, String> entry : changedValues.entrySet()) {
        Queries.update(VIEW_CRITERIA, Filter.compareId(entry.getKey()),
            COL_CRITERION_VALUE, new TextValue(entry.getValue()), new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                scheduler.execute();
              }
            });
      }
    }

    if (!flt.isEmpty()) {
      Queries.delete(VIEW_CRITERIA, flt, new IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          scheduler.execute();
        }
      });
    }

    return true;
  }
}
