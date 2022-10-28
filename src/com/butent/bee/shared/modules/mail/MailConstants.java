package com.butent.bee.shared.modules.mail;

import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public final class MailConstants {

  public enum SystemFolder {
    Inbox("INBOX"), Sent("Sent"), Drafts("Drafts"), Trash("Trash");

    private final String name;

    SystemFolder(String name) {
      this.name = name;
    }

    public String getFolderName() {
      return name;
    }
  }

  public enum AddressType {
    TO, CC, BCC
  }

  public enum Protocol {
    POP3, IMAP, SMTP
  }

  public enum MessageFlag {
    ANSWERED(1), DELETED(2), FLAGGED(4), SEEN(8), FORWARDED(16);

    final int mask;

    MessageFlag(int mask) {
      this.mask = mask;
    }

    public int clear(Integer bits) {
      return BeeUtils.unbox(bits) & ~getMask();
    }

    public int getMask() {
      return mask;
    }

    public boolean isSet(Integer bits) {
      return (BeeUtils.unbox(bits) & getMask()) != 0;
    }

    public int set(Integer bits) {
      return BeeUtils.unbox(bits) | getMask();
    }
  }

  public enum RecipientsGroupsVisibility implements HasLocalizedCaption {
    PUBLIC {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailPublic();
      }
    },
    PRIVATE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailPrivate();
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.dictionary());
    }
  }

  public enum RuleCondition implements HasLocalizedCaption {
    SENDER {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleConditionSender();
      }
    },
    RECIPIENTS {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleConditionRecipients();
      }
    },
    SUBJECT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleConditionSubject();
      }
    },
    ALL {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleConditionAll();
      }
    }
  }

  public enum RuleAction implements HasLocalizedCaption {
    MOVE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionMove();
      }
    },
    COPY {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionCopy();
      }
    },
    DELETE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionDelete();
      }
    },
    READ {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionRead();
      }
    },
    FLAG {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionFlag();
      }
    },
    REPLY {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionReply();
      }
    },
    FORWARD {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailRuleActionForward();
      }
    }
  }

  public enum SyncMode implements HasLocalizedCaption {
    SYNC_INBOX {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailSynchronizeInbox();
      }
    },
    SYNC_ALL {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailSynchronizeAll();
      }
    },
    SYNC_NOTHING {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.mailSynchronizeNothing();
      }
    }
  }

  public static final String SIGNATURE_SEPARATOR = "<br><br><br>";

  public static final String SVC_GET_ACCOUNTS = "get_accounts";
  public static final String SVC_GET_FOLDERS = "get_folders";
  public static final String SVC_CREATE_FOLDER = "create_folder";
  public static final String SVC_RENAME_FOLDER = "rename_folder";
  public static final String SVC_DISCONNECT_FOLDER = "disconnect_folder";
  public static final String SVC_DROP_FOLDER = "drop_folder";
  public static final String SVC_GET_MESSAGE = "get_message";
  public static final String SVC_FLAG_MESSAGE = "flag_message";
  public static final String SVC_COPY_MESSAGES = "copy_messages";
  public static final String SVC_REMOVE_MESSAGES = "remove_messages";
  public static final String SVC_CHECK_MAIL = "check_mail";
  public static final String SVC_SEND_MAIL = "send_mail";
  public static final String SVC_STRIP_HTML = "strip_html";
  public static final String SVC_GET_RAW_CONTENT = "get_raw_content";
  public static final String SVC_GET_UNREAD_COUNT = "get_unread_count";
  public static final String SVC_GET_NEWSLETTER_CONTACTS = "get_newsletter_contacts";

  public static final String TBL_SIGNATURES = "Signatures";
  public static final String TBL_ACCOUNTS = "Accounts";
  public static final String TBL_ADDRESSBOOK = "Addressbook";
  public static final String TBL_FOLDERS = "Folders";
  public static final String TBL_RULES = "Rules";
  public static final String TBL_ACCOUNT_USERS = "AccountUsers";

  public static final String TBL_MESSAGES = "Messages";
  public static final String TBL_PARTS = "Parts";
  public static final String TBL_ATTACHMENTS = "Attachments";

  public static final String TBL_RECIPIENTS = "Recipients";
  public static final String TBL_PLACES = "Places";

  public static final String VIEW_NEWSLETTER_CONTACTS = "NewsletterContacts";
  public static final String VIEW_NEWSLETTER_FILES = "NewsletterFiles";
  public static final String VIEW_NEWSLETTERS = "Newsletters";
  public static final String VIEW_USER_EMAILS = "UserEmails";

  public static final String VIEW_RECIPIENTS_GROUPS = "RecipientsGroups";
  public static final String VIEW_RCPS_GROUPS_CONTACTS = "RcpsGroupsContacts";

  public static final String VIEW_NEWS_COMPANIES = "NewsCompanies";
  public static final String VIEW_NEWS_PERSONS = "NewsPersons";
  public static final String VIEW_NEWS_COMPANY_PERSONS = "NewsCompanyPersons";
  public static final String VIEW_NEWS_COMPANY_CONTACTS = "NewsCompanyContacts";

  public static final String VIEW_SELECT_COMPANIES = "SelectCompanies";
  public static final String VIEW_SELECT_COMPANY_PERSONS = "SelectCompanyPersons";
  public static final String VIEW_SELECT_COMPANY_CONTACTS = "SelectCompanyContacts";
  public static final String VIEW_SELECT_PERSONS = "SelectPersons";

  public static final String COL_MESSAGE = "Message";
  public static final String COL_RAW_CONTENT = "RawContent";
  public static final String COL_ATTACHMENT_COUNT = "AttachmentCount";
  public static final String COL_ATTACHMENT_NAME = "FileName";
  public static final String COL_EMAIL_LABEL = "Label";
  public static final String COL_ADDRESS = "Address";
  public static final String COL_ADDRESS_TYPE = "Type";
  public static final String COL_USER = "User";
  public static final String COL_UNIQUE_ID = "UniqueId";
  public static final String COL_DATE = "Date";
  public static final String COL_SENDER = "Sender";
  public static final String COL_SUBJECT = "Subject";
  public static final String COL_CONTENT = "Content";
  public static final String COL_HTML_CONTENT = "HtmlContent";

  public static final String COL_SIGNATURE = "Signature";
  public static final String COL_SIGNATURE_NAME = "Description";
  public static final String COL_SIGNATURE_CONTENT = "Content";

  public static final String COL_ACCOUNT_DESCRIPTION = "Description";
  public static final String COL_ACCOUNT_DEFAULT = "Main";
  public static final String COL_ACCOUNT_PRIVATE = "Private";
  public static final String COL_ACCOUNT_SYNC_MODE = "SyncMode";
  public static final String COL_ACCOUNT_LAST_CONNECT = "LastConnect";
  public static final String COL_STORE_TYPE = "StoreType";
  public static final String COL_STORE_SERVER = "StoreServer";
  public static final String COL_STORE_SPORT = "StorePort";
  public static final String COL_STORE_LOGIN = "StoreLogin";
  public static final String COL_STORE_PASSWORD = "StorePassword";
  public static final String COL_STORE_SSL = "StoreSSL";
  public static final String COL_STORE_PROPERTIES = "StoreProperties";

  public static final String COL_TRANSPORT_SERVER = "TransportServer";
  public static final String COL_TRANSPORT_PORT = "TransportPort";
  public static final String COL_TRANSPORT_LOGIN = "TransportLogin";
  public static final String COL_TRANSPORT_PASSWORD = "TransportPassword";
  public static final String COL_TRANSPORT_SSL = "TransportSSL";
  public static final String COL_TRANSPORT_PROPERTIES = "TransportProperties";

  public static final String COL_ACCOUNT = "Account";
  public static final String COL_FOLDER_PARENT = "Parent";
  public static final String COL_FOLDER_NAME = "Name";
  public static final String COL_FOLDER_UID = "UIDValidity";
  public static final String COL_FOLDER_MODSEQ = "ModSeq";

  public static final String COL_PLACE = "Place";
  public static final String COL_FOLDER = "Folder";
  public static final String COL_FLAGS = "Flags";
  public static final String COL_MESSAGE_UID = "MessageUID";
  public static final String COL_IN_REPLY_TO = "InReplyTo";

  public static final String COL_RULE = "Rule";
  public static final String COL_RULE_ACTIVE = "Active";
  public static final String COL_RULE_CONDITION = "Condition";
  public static final String COL_RULE_CONDITION_OPTIONS = "ConditionOptions";
  public static final String COL_RULE_ACTION = "Action";
  public static final String COL_RULE_ACTION_OPTIONS = "ActionOptions";

  public static final String COL_ADDRESSBOOK_LABEL = "Label";
  public static final String COL_ADDRESSBOOK_AUTOREPLY = "LastAutoReply";

  public static final String COL_NEWSLETTER = "Newsletter";
  public static final String COL_GROUP_NAME = "GroupName";
  public static final String COL_RECIPIENTS_GROUP = "RecipientsGroup";

  public static final String COL_NEWSLETTER_VISIBLE_COPIES = "VisibleCopies";

  public static final String FORM_ACCOUNT = "Account";
  public static final String FORM_NEW_ACCOUNT = "NewAccount";
  public static final String FORM_RULE = "Rule";

  public static final String FORM_MAIL = "Mail";
  public static final String FORM_MAIL_MESSAGE = "MailMessage";
  public static final String FORM_NEW_MAIL_MESSAGE = "NewMailMessage";

  public static final String FORM_RECIPIENTS_GROUP = "RecipientsGroup";

  public static final String DATA_TYPE_MESSAGE = "Message";

  public static final String PRM_SIGNATURE_POSITION = "MailSignatureAboveQuotedText";
  public static final String PRM_DEFAULT_ACCOUNT = "DefaultAccount";
  public static final String PRM_MAIL_CHECK_INTERVAL = "MailCheckIntervalInMinutes";
  public static final String PRM_SEND_NEWSLETTERS_COUNT = "SendNewslettersCount";
  public static final String PRM_SEND_NEWSLETTERS_INTERVAL = "SendNewslettersInterval";
  public static final String PRM_REMINDERS_MAIL_TEMPLATE = "RemindersMailTemplate";

  public static final String PRM_OUTLOOK_365_CLIENT_ID = "Outlook365ClientId";
  public static final String PRM_OUTLOOK_365_TENANT_ID = "Outlook365TenantId";
  public static final String PRM_OUTLOOK_365_SECRET_KEY = "Outlook365SecretKey";
  public static final String PRM_OUTLOOK_365_SCOPE = "Outlook365Scope";

  public static final String REMINDERS_MAIL_TEMPLATE_HEADER_TEXT = "{template_header_text}";
  public static final String REMINDERS_MAIL_TEMPLATE_HEADER_COLOR = "{template_header_color}";
  public static final String REMINDERS_MAIL_DEFAULT_COLOR = "#6bae45";
  public static final String DEFAULT_REMINDERS_MAIL_TEMPLATE_VALUE =
      "<h3 style=\"background-color: " + REMINDERS_MAIL_TEMPLATE_HEADER_COLOR
          + "; color: white; text-transform: uppercase;"
          + " padding: 10px;\">" + REMINDERS_MAIL_TEMPLATE_HEADER_TEXT + "</h3>";

  public static void register() {
    EnumUtils.register(RuleCondition.class);
    EnumUtils.register(RuleAction.class);
    EnumUtils.register(RecipientsGroupsVisibility.class);
    EnumUtils.register(SyncMode.class);
  }

  private MailConstants() {
  }
}
