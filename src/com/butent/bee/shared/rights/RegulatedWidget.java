package com.butent.bee.shared.rights;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;

public enum RegulatedWidget implements HasLocalizedCaption {

  NEWS {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.domainNews();
    }
  },
  ONLINE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.domainOnline();
    }
  },
  ADMIN(ModuleAndSub.of(Module.ADMINISTRATION)) {
    @Override
    public String getCaption(Dictionary constants) {
      return "Admin";
    }
  },
  AUDIT {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.actionAudit();
    }
  },
  DROP_EXPORTED {
    @Override
    public String getCaption(Dictionary constants) {
      return "Eksportuotų įrašų šalinimas";
    }
  },
  DOCUMENT_TREE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.documentTree();
    }
  },
  TO_ERP {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.trSendToERP();
    }
  },
  COMPANY_STRUCTURE {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.companyStructure();
    }
  };

  private final ModuleAndSub moduleAndSub;

  RegulatedWidget() {
    this(null);
  }

  RegulatedWidget(ModuleAndSub moduleAndSub) {
    this.moduleAndSub = moduleAndSub;
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getName() {
    return BeeUtils.proper(name());
  }
}
