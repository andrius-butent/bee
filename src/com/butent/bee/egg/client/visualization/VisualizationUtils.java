package com.butent.bee.egg.client.visualization;

import com.google.gwt.core.client.JsArrayString;

import com.butent.bee.egg.client.ajaxloader.AjaxLoader;
import com.butent.bee.egg.client.ajaxloader.AjaxLoader.AjaxLoaderOptions;
import com.butent.bee.egg.shared.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisualizationUtils {
  private static Set<String> loadedPackages = new HashSet<String>();
  
  public static void clearLoadedPackages() {
    loadedPackages.clear();
  }

  public static Set<String> getLoadedPackages() {
    return loadedPackages;
  }

  public static void loadVisualizationApi(Runnable onLoad, String... packages) {
    loadVisualizationApi("1", onLoad, packages);
  }

  public static void loadVisualizationApi(String version, Runnable onLoad, String... packages) {
    Assert.notEmpty(version);
    Assert.notNull(onLoad);
    
    List<String> lst = new ArrayList<String>();
    for (int i = 0; i < packages.length; i++) {
      if (!loadedPackages.contains(packages[i])) {
        lst.add(packages[i]);
        loadedPackages.add(packages[i]);
      }
    }
    
    if (lst.size() <= 0) {
      onLoad.run();
      return;
    }
    
    JsArrayString arr = JsArrayString.createArray().cast();
    for (int i = 0; i < lst.size(); i++) {
      arr.set(i, lst.get(i));
    }
    
    loadVisualizationApi(version, onLoad, arr);
  }
  
  private static void loadVisualizationApi(String ver, Runnable onLoad, JsArrayString packages) {
    AjaxLoaderOptions options = AjaxLoaderOptions.newInstance();
    options.setPackages(packages);
    AjaxLoader.loadApi("visualization", ver, onLoad, options); 
  }

  private VisualizationUtils() {
  }
}
