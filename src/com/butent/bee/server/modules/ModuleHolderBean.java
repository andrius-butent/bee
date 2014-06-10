package com.butent.bee.server.modules;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
@Lock(LockType.READ)
public class ModuleHolderBean {

  private enum TABLE_ACTIVATION_MODE {
    DELAYED, FORCED
  }

  private static BeeLogger logger = LogUtils.getLogger(ModuleHolderBean.class);

  private final Map<String, BeeModule> modules = Maps.newLinkedHashMap();

  @EJB
  SystemBean sys;
  @EJB
  ParamHolderBean prm;

  public ResponseObject doModule(RequestInfo reqInfo) {
    Assert.notNull(reqInfo);

    return getModule(reqInfo.getService())
        .doService(reqInfo.getParameter(AdministrationConstants.METHOD), reqInfo);
  }

  public List<SearchResult> doSearch(String query) {
    Assert.notEmpty(query);
    List<SearchResult> results = Lists.newArrayList();

    for (BeeModule module : modules.values()) {
      List<SearchResult> found = module.doSearch(query);
      if (found != null && !found.isEmpty()) {
        results.addAll(found);
      }
    }
    return results;
  }

  public Collection<BeeParameter> getModuleDefaultParameters(String moduleName) {
    return getModule(moduleName).getDefaultParameters();
  }

  public Collection<String> getModules() {
    return ImmutableSet.copyOf(modules.keySet());
  }

  public String getResourcePath(String moduleName, String... resources) {
    Assert.isTrue(!ArrayUtils.isEmpty(resources));
    String resource = ArrayUtils.join("/", resources);

    if (!BeeUtils.isEmpty(moduleName)) {
      resource = BeeUtils.join("/", BeeUtils.normalize(Service.PROPERTY_MODULES),
          getModule(moduleName).getResourcePath(), resource);
    }
    return resource;
  }

  public boolean hasModule(String moduleName) {
    Assert.notEmpty(moduleName);
    return modules.containsKey(moduleName);
  }

  public void initModules() {
    TABLE_ACTIVATION_MODE mode = EnumUtils.getEnumByName(TABLE_ACTIVATION_MODE.class,
        Config.getProperty("TableActivationMode"));

    if (mode != TABLE_ACTIVATION_MODE.DELAYED) {
      for (String tblName : sys.getTableNames()) {
        if (mode == TABLE_ACTIVATION_MODE.FORCED) {
          sys.rebuildTable(tblName);
        } else {
          sys.activateTable(tblName);
        }
      }
    }
    for (String mod : getModules()) {
      prm.refreshModuleParameters(mod);
      getModule(mod).init();
    }
  }

  private BeeModule getModule(String moduleName) {
    Assert.state(hasModule(moduleName), "Unknown module name: " + moduleName);
    return modules.get(moduleName);
  }

  @SuppressWarnings("unchecked")
  @PostConstruct
  private void init() {
    Module.setEnabledModules(Config.getProperty(Service.PROPERTY_MODULES));

    List<String> mods = Lists.newArrayList();

    for (Module modul : Module.values()) {
      if (BeeUtils.isEmpty(modul.getName())) {
        logger.severe("Module", BeeUtils.bracket(modul.name()), "does not have name");
      } else {
        mods.add(modul.getName());
      }
    }
    for (String moduleName : mods) {
      if (hasModule(moduleName)) {
        logger.severe("Dublicate module name:", BeeUtils.bracket(moduleName));
      } else {
        try {
          Class<BeeModule> clazz = (Class<BeeModule>) Class.forName(BeeUtils.join(".",
              this.getClass().getPackage().getName(), moduleName.toLowerCase(),
              moduleName + "ModuleBean"));

          BeeModule module = Invocation.locateRemoteBean(clazz);

          if (module != null) {
            modules.put(moduleName, module);
            logger.info("Registered module:", BeeUtils.bracket(moduleName));
          }
        } catch (ClassNotFoundException | ClassCastException e) {
          logger.error(e);
        }
      }
    }
    if (modules.isEmpty()) {
      logger.warning("No modules registered");
    }
  }
}
