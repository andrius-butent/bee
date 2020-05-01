//@formatter:off
// CHECKSTYLE:OFF
package com.butent.bee.shared.i18n;

import java.util.HashMap;
import java.util.Map;

@FunctionalInterface
public interface Dictionary {

  String g(String key);

  default String accept() {return g("accept");}

  default String account() {return g("account");}

  default String actionAdd() {return g("actionAdd");}

  default String actionAppend() {return g("actionAppend");}

  default String actionAudit() {return g("actionAudit");}

  default String actionBlock() {return g("actionBlock");}

  default String actionBookmark() {return g("actionBookmark");}

  default String actionCanNotBeExecuted() {return g("actionCanNotBeExecuted");}

  default String actionCancel() {return g("actionCancel");}

  default String actionChange() {return g("actionChange");}

  default String actionClose() {return g("actionClose");}

  default String actionConfigure() {return g("actionConfigure");}

  default String actionCopy() {return g("actionCopy");}

  default String actionCreate() {return g("actionCreate");}

  default String actionDelete() {return g("actionDelete");}

  default String actionDeleteFilter() {return g("actionDeleteFilter");}

  default String actionEdit() {return g("actionEdit");}

  default String actionExchange() {return g("actionExchange");}

  default String actionExport() {return g("actionExport");}

  default String actionFilter() {return g("actionFilter");}

  default String actionImport() {return g("actionImport");}

  default String actionMaximize() {return g("actionMaximize");}

  default String actionMerge() {return g("actionMerge");}

  default String actionMinimize() {return g("actionMinimize");}

  default String actionMove() {return g("actionMove");}

  default String actionNew() {return g("actionNew");}

  default String actionNew1() {return g("actionNew1");}

  default String actionNotAllowed() {return g("actionNotAllowed");}

  default String actionOpen() {return g("actionOpen");}

  default String actionPrint() {return g("actionPrint");}

  default String actionRefresh() {return g("actionRefresh");}

  default String actionRefreshComments() {return g("actionRefreshComments");}

  default String actionRemove() {return g("actionRemove");}

  default String actionRemoveFilter() {return g("actionRemoveFilter");}

  default String actionRename() {return g("actionRename");}

  default String actionRenameFilter() {return g("actionRenameFilter");}

  default String actionResetSettings() {return g("actionResetSettings");}

  default String actionSave() {return g("actionSave");}

  default String actionSelect() {return g("actionSelect");}

  default String actionUpdate() {return g("actionUpdate");}

  default String actionWorkspaceBookmarkAll() {return g("actionWorkspaceBookmarkAll");}

  default String actionWorkspaceBookmarkTab() {return g("actionWorkspaceBookmarkTab");}

  default String actionWorkspaceCloseAll() {return g("actionWorkspaceCloseAll");}

  default String actionWorkspaceCloseOther() {return g("actionWorkspaceCloseOther");}

  default String actionWorkspaceCloseRight() {return g("actionWorkspaceCloseRight");}

  default String actionWorkspaceCloseTab() {return g("actionWorkspaceCloseTab");}

  default String actionWorkspaceCloseTile() {return g("actionWorkspaceCloseTile");}

  default String actionWorkspaceEnlargeDown() {return g("actionWorkspaceEnlargeDown");}

  default String actionWorkspaceEnlargeToLeft() {return g("actionWorkspaceEnlargeToLeft");}

  default String actionWorkspaceEnlargeToRight() {return g("actionWorkspaceEnlargeToRight");}

  default String actionWorkspaceEnlargeUp() {return g("actionWorkspaceEnlargeUp");}

  default String actionWorkspaceMaxSize() {return g("actionWorkspaceMaxSize");}

  default String actionWorkspaceNewBottom() {return g("actionWorkspaceNewBottom");}

  default String actionWorkspaceNewLeft() {return g("actionWorkspaceNewLeft");}

  default String actionWorkspaceNewRight() {return g("actionWorkspaceNewRight");}

  default String actionWorkspaceNewTab() {return g("actionWorkspaceNewTab");}

  default String actionWorkspaceNewTop() {return g("actionWorkspaceNewTop");}

  default String actionWorkspaceRestoreSize() {return g("actionWorkspaceRestoreSize");}

  default String activityType() {return g("activityType");}

  default String activityTypes() {return g("activityTypes");}

  default String adTopic() {return g("adTopic");}

  default String additionalContacts() {return g("additionalContacts");}

  default String additionalEquipment() {return g("additionalEquipment");}

  default String additionalInfo() {return g("additionalInfo");}

  default String additionalItemUnit() {return g("additionalItemUnit");}

  default String additionalServices() {return g("additionalServices");}

  default String address() {return g("address");}

  default String adjustmentDutyOrder() {return g("adjustmentDutyOrder");}

  default String administration() {return g("administration");}

  default String adsTopics() {return g("adsTopics");}

  default String advance() {return g("advance");}

  default String after() {return g("after");}

  default String allValuesCannotBeEmpty() {return g("allValuesCannotBeEmpty");}

  default String allValuesEmpty(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("allValuesEmpty"), _m);
  }

  default String allValuesIdentical(Object p0, Object p1, Object p2) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    _m.put("{2}", p2);
    return Localized.format(g("allValuesIdentical"), _m);
  }

  default String allowPhotoSize() {return g("allowPhotoSize");}

  default String amount() {return g("amount");}

  default String amountWithoutDiscount() {return g("amountWithoutDiscount");}

  default String amountWithoutVat() {return g("amountWithoutVat");}

  default String announcement() {return g("announcement");}

  default String announcementNew() {return g("announcementNew");}

  default String announcements() {return g("announcements");}

  default String announcementsBoard() {return g("announcementsBoard");}

  default String area() {return g("area");}

  default String arrived() {return g("arrived");}

  default String article() {return g("article");}

  default String article2() {return g("article2");}

  default String article3() {return g("article3");}

  default String article4() {return g("article4");}

  default String askDeleteAll() {return g("askDeleteAll");}

  default String assessments() {return g("assessments");}

  default String assignCargoToTripCaption() {return g("assignCargoToTripCaption");}

  default String assignCargoToTripQuestion() {return g("assignCargoToTripQuestion");}

  default String assignDriverToTripCaption() {return g("assignDriverToTripCaption");}

  default String assignDriverToTripQuestion() {return g("assignDriverToTripQuestion");}

  default String assignTrailerToTripCaption() {return g("assignTrailerToTripCaption");}

  default String assignTrailerToTripQuestion() {return g("assignTrailerToTripQuestion");}

  default String assignTruckToTripCaption() {return g("assignTruckToTripCaption");}

  default String assignTruckToTripQuestion() {return g("assignTruckToTripQuestion");}

  default String assistant() {return g("assistant");}

  default String attribute() {return g("attribute");}

  default String author() {return g("author");}

  default String autoFit() {return g("autoFit");}

  default String autocompletion() {return g("autocompletion");}

  default String bAssistant() {return g("bAssistant");}

  default String background() {return g("background");}

  default String backgroundColor() {return g("backgroundColor");}

  default String bank() {return g("bank");}

  default String bankAccount() {return g("bankAccount");}

  default String bankAccounts() {return g("bankAccounts");}

  default String bankDetails() {return g("bankDetails");}

  default String banks() {return g("banks");}

  default String banksAccounts() {return g("banksAccounts");}

  default String baseUnit() {return g("baseUnit");}

  default String before() {return g("before");}

  default String birthdaysParties() {return g("birthdaysParties");}

  default String blockAfter() {return g("blockAfter");}

  default String blockBefore() {return g("blockBefore");}

  default String bookmarkName() {return g("bookmarkName");}

  default String branch() {return g("branch");}

  default String branches() {return g("branches");}

  default String brutto() {return g("brutto");}

  default String bundle() {return g("bundle");}

  default String bundles() {return g("bundles");}

  default String calAction() {return g("calAction");}

  default String calActionRegisterResult() {return g("calActionRegisterResult");}

  default String calActionResult() {return g("calActionResult");}

  default String calActiveView() {return g("calActiveView");}

  default String calAddAttendees() {return g("calAddAttendees");}

  default String calAddExecutorGroups() {return g("calAddExecutorGroups");}

  default String calAddExecutors() {return g("calAddExecutors");}

  default String calAddOwners() {return g("calAddOwners");}

  default String calAddParameters() {return g("calAddParameters");}

  default String calAppointment() {return g("calAppointment");}

  default String calAppointmentAttendees() {return g("calAppointmentAttendees");}

  default String calAppointmentCreator() {return g("calAppointmentCreator");}

  default String calAppointmentEditor() {return g("calAppointmentEditor");}

  default String calAppointmentEnd() {return g("calAppointmentEnd");}

  default String calAppointmentParameters() {return g("calAppointmentParameters");}

  default String calAppointmentProperties() {return g("calAppointmentProperties");}

  default String calAppointmentPropertyGroups() {return g("calAppointmentPropertyGroups");}

  default String calAppointmentRenderMultiBody() {return g("calAppointmentRenderMultiBody");}

  default String calAppointmentRenderMultiHeader() {return g("calAppointmentRenderMultiHeader");}

  default String calAppointmentRenderSimpleBody() {return g("calAppointmentRenderSimpleBody");}

  default String calAppointmentRenderSimpleHeader() {return g("calAppointmentRenderSimpleHeader");}

  default String calAppointmentStart() {return g("calAppointmentStart");}

  default String calAppointmentStatus() {return g("calAppointmentStatus");}

  default String calAppointmentStatusCanceled() {return g("calAppointmentStatusCanceled");}

  default String calAppointmentStatusCompleted() {return g("calAppointmentStatusCompleted");}

  default String calAppointmentStatusConfirmed() {return g("calAppointmentStatusConfirmed");}

  default String calAppointmentStatusDelayed() {return g("calAppointmentStatusDelayed");}

  default String calAppointmentStatusRunning() {return g("calAppointmentStatusRunning");}

  default String calAppointmentStatusTentative() {return g("calAppointmentStatusTentative");}

  default String calAppointmentStyle() {return g("calAppointmentStyle");}

  default String calAppointmentStyleBody() {return g("calAppointmentStyleBody");}

  default String calAppointmentStyleFooter() {return g("calAppointmentStyleFooter");}

  default String calAppointmentStyleHeader() {return g("calAppointmentStyleHeader");}

  default String calAppointmentStyleName() {return g("calAppointmentStyleName");}

  default String calAppointmentStyleSimple() {return g("calAppointmentStyleSimple");}

  default String calAppointmentStyles() {return g("calAppointmentStyles");}

  default String calAppointmentTitle() {return g("calAppointmentTitle");}

  default String calAppointmentType() {return g("calAppointmentType");}

  default String calAppointmentTypeDescription() {return g("calAppointmentTypeDescription");}

  default String calAppointmentTypeName() {return g("calAppointmentTypeName");}

  default String calAppointmentTypes() {return g("calAppointmentTypes");}

  default String calAppointments() {return g("calAppointments");}

  default String calAttendee() {return g("calAttendee");}

  default String calAttendeeName() {return g("calAttendeeName");}

  default String calAttendeeParameters() {return g("calAttendeeParameters");}

  default String calAttendeeType() {return g("calAttendeeType");}

  default String calAttendeeTypeDescription() {return g("calAttendeeTypeDescription");}

  default String calAttendeeTypes() {return g("calAttendeeTypes");}

  default String calAttendees() {return g("calAttendees");}

  default String calCar() {return g("calCar");}

  default String calClient() {return g("calClient");}

  default String calComment() {return g("calComment");}

  default String calCompact() {return g("calCompact");}

  default String calCompactForm() {return g("calCompactForm");}

  default String calCreateNewAppointment() {return g("calCreateNewAppointment");}

  default String calDayView() {return g("calDayView");}

  default String calDaysView() {return g("calDaysView");}

  default String calDefaultDisplayedDays() {return g("calDefaultDisplayedDays");}

  default String calDefaultParameter() {return g("calDefaultParameter");}

  default String calDeleteAppointment() {return g("calDeleteAppointment");}

  default String calEditable() {return g("calEditable");}

  default String calEnterAttendees() {return g("calEnterAttendees");}

  default String calEnterClient() {return g("calEnterClient");}

  default String calEnterDurationOrPlannedEndDate() {return g("calEnterDurationOrPlannedEndDate");}

  default String calEnterPlannedStartTime() {return g("calEnterPlannedStartTime");}

  default String calEnterRepairType() {return g("calEnterRepairType");}

  default String calEnterServiceType() {return g("calEnterServiceType");}

  default String calEnterVehicle() {return g("calEnterVehicle");}

  default String calHoursTo() {return g("calHoursTo");}

  default String calIntervalsPerHour() {return g("calIntervalsPerHour");}

  default String calInvalidDateInterval() {return g("calInvalidDateInterval");}

  default String calInvalidHoursInterval() {return g("calInvalidHoursInterval");}

  default String calLastActionDate() {return g("calLastActionDate");}

  default String calLinkOpen() {return g("calLinkOpen");}

  default String calListOfActions() {return g("calListOfActions");}

  default String calMailPlannedActionSubject() {return g("calMailPlannedActionSubject");}

  default String calMailPlannedActionText() {return g("calMailPlannedActionText");}

  default String calMailPlannedActions() {return g("calMailPlannedActions");}

  default String calMessage() {return g("calMessage");}

  default String calMinutesTo() {return g("calMinutesTo");}

  default String calMonthView() {return g("calMonthView");}

  default String calMultiday() {return g("calMultiday");}

  default String calMultidayLayout() {return g("calMultidayLayout");}

  default String calMultidayLayoutHorizontal() {return g("calMultidayLayoutHorizontal");}

  default String calMultidayLayoutLastDay() {return g("calMultidayLayoutLastDay");}

  default String calMultidayLayoutVertical() {return g("calMultidayLayoutVertical");}

  default String calMultidayLayoutWorkingHours() {return g("calMultidayLayoutWorkingHours");}

  default String calMultidayTaskLayout() {return g("calMultidayTaskLayout");}

  default String calName() {return g("calName");}

  default String calNew() {return g("calNew");}

  default String calNewAction() {return g("calNewAction");}

  default String calNewAppointment() {return g("calNewAppointment");}

  default String calNewAppointmentStyle() {return g("calNewAppointmentStyle");}

  default String calNewAppointmentType() {return g("calNewAppointmentType");}

  default String calNewAttendee() {return g("calNewAttendee");}

  default String calNewAttendeeType() {return g("calNewAttendeeType");}

  default String calNewCalendar() {return g("calNewCalendar");}

  default String calNewParameter() {return g("calNewParameter");}

  default String calNewParameterGroup() {return g("calNewParameterGroup");}

  default String calNewReminder() {return g("calNewReminder");}

  default String calOpaque() {return g("calOpaque");}

  default String calOverlappingAppointments() {return g("calOverlappingAppointments");}

  default String calParameter() {return g("calParameter");}

  default String calParameterGroup() {return g("calParameterGroup");}

  default String calParameters() {return g("calParameters");}

  default String calParametersGroups() {return g("calParametersGroups");}

  default String calPixelsPerInterval() {return g("calPixelsPerInterval");}

  default String calPlannedEndDateMustBeGreater() {return g("calPlannedEndDateMustBeGreater");}

  default String calPrivate() {return g("calPrivate");}

  default String calPublic() {return g("calPublic");}

  default String calReminder() {return g("calReminder");}

  default String calReminderClient() {return g("calReminderClient");}

  default String calReminders() {return g("calReminders");}

  default String calRemindersEarliestTime() {return g("calRemindersEarliestTime");}

  default String calRemindersLatestTime() {return g("calRemindersLatestTime");}

  default String calRepairDescription() {return g("calRepairDescription");}

  default String calRepairType() {return g("calRepairType");}

  default String calReportLowerDate() {return g("calReportLowerDate");}

  default String calReportLowerHour() {return g("calReportLowerHour");}

  default String calReportOptions() {return g("calReportOptions");}

  default String calReportType() {return g("calReportType");}

  default String calReportTypeBusyHours() {return g("calReportTypeBusyHours");}

  default String calReportTypeBusyMonths() {return g("calReportTypeBusyMonths");}

  default String calReportTypeCancelHours() {return g("calReportTypeCancelHours");}

  default String calReportTypeCancelMonths() {return g("calReportTypeCancelMonths");}

  default String calReportUpperDate() {return g("calReportUpperDate");}

  default String calReportUpperHour() {return g("calReportUpperHour");}

  default String calResourceView() {return g("calResourceView");}

  default String calScrollToHour() {return g("calScrollToHour");}

  default String calSelectAppointment() {return g("calSelectAppointment");}

  default String calSelectVehicle() {return g("calSelectVehicle");}

  default String calSendTo() {return g("calSendTo");}

  default String calSent() {return g("calSent");}

  default String calSeparateAttendees() {return g("calSeparateAttendees");}

  default String calServiceAdd() {return g("calServiceAdd");}

  default String calServiceType() {return g("calServiceType");}

  default String calTable() {return g("calTable");}

  default String calTasksAssigned() {return g("calTasksAssigned");}

  default String calTasksAssignedBackground() {return g("calTasksAssignedBackground");}

  default String calTasksAssignedForeground() {return g("calTasksAssignedForeground");}

  default String calTasksAssignedStyle() {return g("calTasksAssignedStyle");}

  default String calTasksDelegated() {return g("calTasksDelegated");}

  default String calTasksDelegatedBackground() {return g("calTasksDelegatedBackground");}

  default String calTasksDelegatedForeground() {return g("calTasksDelegatedForeground");}

  default String calTasksDelegatedStyle() {return g("calTasksDelegatedStyle");}

  default String calTasksObserved() {return g("calTasksObserved");}

  default String calTasksObservedBackground() {return g("calTasksObservedBackground");}

  default String calTasksObservedForeground() {return g("calTasksObservedForeground");}

  default String calTasksObservedStyle() {return g("calTasksObservedStyle");}

  default String calTheme() {return g("calTheme");}

  default String calTimeBlockClickNumber() {return g("calTimeBlockClickNumber");}

  default String calToday() {return g("calToday");}

  default String calTotal() {return g("calTotal");}

  default String calTransparency() {return g("calTransparency");}

  default String calTransparent() {return g("calTransparent");}

  default String calUserAttendees() {return g("calUserAttendees");}

  default String calViews() {return g("calViews");}

  default String calVisibility() {return g("calVisibility");}

  default String calWeekView() {return g("calWeekView");}

  default String calWorkWeekView() {return g("calWorkWeekView");}

  default String calWorkingHourEnd() {return g("calWorkingHourEnd");}

  default String calWorkingHourStart() {return g("calWorkingHourStart");}

  default String calendar() {return g("calendar");}

  default String calendarAppointmentTypes() {return g("calendarAppointmentTypes");}

  default String calendarAttendeeTypes() {return g("calendarAttendeeTypes");}

  default String calendarAttendees() {return g("calendarAttendees");}

  default String calendarTaskExecutorGroups() {return g("calendarTaskExecutorGroups");}

  default String calendarTaskExecutors() {return g("calendarTaskExecutors");}

  default String calendars() {return g("calendars");}

  default String cancel() {return g("cancel");}

  default String canceled() {return g("canceled");}

  default String caption() {return g("caption");}

  default String captionId() {return g("captionId");}

  default String captionPid() {return g("captionPid");}

  default String car() {return g("car");}

  default String carService() {return g("carService");}

  default String cargo() {return g("cargo");}

  default String cargoChangeOfPallets() {return g("cargoChangeOfPallets");}

  default String cargoHandlingPlaces() {return g("cargoHandlingPlaces");}

  default String cargoInformation() {return g("cargoInformation");}

  default String cargoLoading() {return g("cargoLoading");}

  default String cargoLoadingPlace() {return g("cargoLoadingPlace");}

  default String cargoLoadingPlaces() {return g("cargoLoadingPlaces");}

  default String cargoNotes() {return g("cargoNotes");}

  default String cargoNumber() {return g("cargoNumber");}

  default String cargoTermsOfService() {return g("cargoTermsOfService");}

  default String cargoUnloading() {return g("cargoUnloading");}

  default String cargoUnloadingPlace() {return g("cargoUnloadingPlace");}

  default String cargoUnloadingPlaces() {return g("cargoUnloadingPlaces");}

  default String cargoValue() {return g("cargoValue");}

  default String cargos() {return g("cargos");}

  default String carrier() {return g("carrier");}

  default String carriers() {return g("carriers");}

  default String cars() {return g("cars");}

  default String cash() {return g("cash");}

  default String categories() {return g("categories");}

  default String category() {return g("category");}

  default String categoryEdit() {return g("categoryEdit");}

  default String cellIsReadOnly() {return g("cellIsReadOnly");}

  default String changePassword() {return g("changePassword");}

  default String changedValues() {return g("changedValues");}

  default String chartOfAccounts() {return g("chartOfAccounts");}

  default String chat() {return g("chat");}

  default String chatDeleteQuestion() {return g("chatDeleteQuestion");}

  default String chatFiles() {return g("chatFiles");}

  default String chatLeaveQuestion() {return g("chatLeaveQuestion");}

  default String chatMessage() {return g("chatMessage");}

  default String chatMessages() {return g("chatMessages");}

  default String chatName() {return g("chatName");}

  default String chatNew() {return g("chatNew");}

  default String chatReminderTitle() {return g("chatReminderTitle");}

  default String chatSettings() {return g("chatSettings");}

  default String chatStartNew() {return g("chatStartNew");}

  default String chatUpdateTime() {return g("chatUpdateTime");}

  default String chatUsers() {return g("chatUsers");}

  default String chats() {return g("chats");}

  default String chatsShowAll() {return g("chatsShowAll");}

  default String checkNo() {return g("checkNo");}

  default String checked() {return g("checked");}

  default String chief() {return g("chief");}

  default String chooseContactSource() {return g("chooseContactSource");}

  default String chooseFiles() {return g("chooseFiles");}

  default String chooseLanguage() {return g("chooseLanguage");}

  default String choosePrintingForm() {return g("choosePrintingForm");}

  default String cities() {return g("cities");}

  default String city() {return g("city");}

  default String classifiers() {return g("classifiers");}

  default String clear() {return g("clear");}

  default String clearFilter() {return g("clearFilter");}

  default String clearNews() {return g("clearNews");}

  default String clearTimeSheetQuestion() {return g("clearTimeSheetQuestion");}

  default String clearWorkScheduleQuestion() {return g("clearWorkScheduleQuestion");}

  default String clickSensitivityDistance() {return g("clickSensitivityDistance");}

  default String clickSensitivityMillis() {return g("clickSensitivityMillis");}

  default String client() {return g("client");}

  default String clientGroup() {return g("clientGroup");}

  default String clientStatus() {return g("clientStatus");}

  default String clientTurnovers() {return g("clientTurnovers");}

  default String clients() {return g("clients");}

  default String clientsCompanyActivities() {return g("clientsCompanyActivities");}

  default String clientsCompanySizes() {return g("clientsCompanySizes");}

  default String clientsFinancialStates() {return g("clientsFinancialStates");}

  default String clientsGroups() {return g("clientsGroups");}

  default String clientsInformationSources() {return g("clientsInformationSources");}

  default String clientsPriorities() {return g("clientsPriorities");}

  default String clientsRelationTypeStates() {return g("clientsRelationTypeStates");}

  default String clientsRelationTypes() {return g("clientsRelationTypes");}

  default String code() {return g("code");}

  default String color() {return g("color");}

  default String colorDescription() {return g("colorDescription");}

  default String colorIsInvalid() {return g("colorIsInvalid");}

  default String colorTheme() {return g("colorTheme");}

  default String colorThemes() {return g("colorThemes");}

  default String colorTitle() {return g("colorTitle");}

  default String colors() {return g("colors");}

  default String column() {return g("column");}

  default String columnResults() {return g("columnResults");}

  default String columns() {return g("columns");}

  default String comment() {return g("comment");}

  default String communication() {return g("communication");}

  default String companies() {return g("companies");}

  default String company() {return g("company");}

  default String companyActivities() {return g("companyActivities");}

  default String companyActivity() {return g("companyActivity");}

  default String companyAndPerson() {return g("companyAndPerson");}

  default String companyCode() {return g("companyCode");}

  default String companyCreditLimit1() {return g("companyCreditLimit1");}

  default String companyCreditLimit2() {return g("companyCreditLimit2");}

  default String companyCreditLimitCurrency() {return g("companyCreditLimitCurrency");}

  default String companyCreditLimitDays() {return g("companyCreditLimitDays");}

  default String companyDepartment() {return g("companyDepartment");}

  default String companyDepartments() {return g("companyDepartments");}

  default String companyInfo() {return g("companyInfo");}

  default String companyName() {return g("companyName");}

  default String companyObjects() {return g("companyObjects");}

  default String companyPerson() {return g("companyPerson");}

  default String companyPersons() {return g("companyPersons");}

  default String companyRating() {return g("companyRating");}

  default String companyRelation() {return g("companyRelation");}

  default String companyRelationState() {return g("companyRelationState");}

  default String companyRelationType() {return g("companyRelationType");}

  default String companyRelationTypes() {return g("companyRelationTypes");}

  default String companyResponsibleUser() {return g("companyResponsibleUser");}

  default String companyResponsibleUsers() {return g("companyResponsibleUsers");}

  default String companySize() {return g("companySize");}

  default String companyStatus() {return g("companyStatus");}

  default String companyStatusName() {return g("companyStatusName");}

  default String companyStructure() {return g("companyStructure");}

  default String companyToleratedDays() {return g("companyToleratedDays");}

  default String companyVATCode() {return g("companyVATCode");}

  default String condition() {return g("condition");}

  default String conditions() {return g("conditions");}

  default String configuration() {return g("configuration");}

  default String constant() {return g("constant");}

  default String contact() {return g("contact");}

  default String contactFamily() {return g("contactFamily");}

  default String contactFamilyRelation() {return g("contactFamilyRelation");}

  default String contactFamilyRelations() {return g("contactFamilyRelations");}

  default String contactInfo() {return g("contactInfo");}

  default String contactReportCompanyByType() {return g("contactReportCompanyByType");}

  default String contactReportCompanyUsage() {return g("contactReportCompanyUsage");}

  default String contactReports() {return g("contactReports");}

  default String contacts() {return g("contacts");}

  default String content() {return g("content");}

  default String continueQuestion() {return g("continueQuestion");}

  default String contract() {return g("contract");}

  default String contractNo() {return g("contractNo");}

  default String correspondent() {return g("correspondent");}

  default String cost() {return g("cost");}

  default String costBasis() {return g("costBasis");}

  default String costCenter() {return g("costCenter");}

  default String costCenters() {return g("costCenters");}

  default String costCurrency() {return g("costCurrency");}

  default String costLabel() {return g("costLabel");}

  default String countries() {return g("countries");}

  default String country() {return g("country");}

  default String countryCode() {return g("countryCode");}

  default String countryEUMember() {return g("countryEUMember");}

  default String countryEUMemberShort() {return g("countryEUMemberShort");}

  default String countryFlag() {return g("countryFlag");}

  default String countryOfOrigin() {return g("countryOfOrigin");}

  default String create() {return g("create");}

  default String createCreditInvoice() {return g("createCreditInvoice");}

  default String createInvoice() {return g("createInvoice");}

  default String createNew() {return g("createNew");}

  default String createNewRow() {return g("createNewRow");}

  default String createPurchaseInvoice() {return g("createPurchaseInvoice");}

  default String createTripForCargoCaption() {return g("createTripForCargoCaption");}

  default String createTripForCargoQuestion() {return g("createTripForCargoQuestion");}

  default String createdFile() {return g("createdFile");}

  default String createdOn() {return g("createdOn");}

  default String createdRows(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("createdRows"), _m);
  }

  default String creationDate() {return g("creationDate");}

  default String creator() {return g("creator");}

  default String credit() {return g("credit");}

  default String creditDays() {return g("creditDays");}

  default String creditDocument() {return g("creditDocument");}

  default String creditDocumentShort() {return g("creditDocumentShort");}

  default String creditLimit() {return g("creditLimit");}

  default String creditReplacement() {return g("creditReplacement");}

  default String creditReplacementShort() {return g("creditReplacementShort");}

  default String creditSeries() {return g("creditSeries");}

  default String creditSeriesShort() {return g("creditSeriesShort");}

  default String creditTolerance() {return g("creditTolerance");}

  default String criteria() {return g("criteria");}

  default String criteriaGroup() {return g("criteriaGroup");}

  default String criteriaGroups() {return g("criteriaGroups");}

  default String criterionName() {return g("criterionName");}

  default String criterionValue() {return g("criterionValue");}

  default String crmActionComment() {return g("crmActionComment");}

  default String crmActionFinish() {return g("crmActionFinish");}

  default String crmActionForward() {return g("crmActionForward");}

  default String crmActionSuspend() {return g("crmActionSuspend");}

  default String crmAdded() {return g("crmAdded");}

  default String crmCreatedNewTasks(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("crmCreatedNewTasks"), _m);
  }

  default String crmDeleted() {return g("crmDeleted");}

  default String crmDurationType() {return g("crmDurationType");}

  default String crmEnterComment() {return g("crmEnterComment");}

  default String crmEnterCommentOrDuration() {return g("crmEnterCommentOrDuration");}

  default String crmEnterCompleteDate() {return g("crmEnterCompleteDate");}

  default String crmEnterConfirmDate() {return g("crmEnterConfirmDate");}

  default String crmEnterDueDate() {return g("crmEnterDueDate");}

  default String crmEnterDuration() {return g("crmEnterDuration");}

  default String crmEnterDurationType() {return g("crmEnterDurationType");}

  default String crmEnterExecutor() {return g("crmEnterExecutor");}

  default String crmEnterOneExecutor() {return g("crmEnterOneExecutor");}

  default String crmEnterFinishDate() {return g("crmEnterFinishDate");}

  default String crmEnterFinishDateOrEstimatedTime() {return g("crmEnterFinishDateOrEstimatedTime");}

  default String crmEnterStartDate() {return g("crmEnterStartDate");}

  default String crmEnterSubject() {return g("crmEnterSubject");}

  default String crmFinishDate() {return g("crmFinishDate");}

  default String crmFinishDateMustBeGreaterThan() {return g("crmFinishDateMustBeGreaterThan");}

  default String crmFinishDateMustBeGreaterThanStart() {return g("crmFinishDateMustBeGreaterThanStart");}

  default String crmFinishTimeMustBeGreaterThanStart() {return g("crmFinishTimeMustBeGreaterThanStart");}

  default String crmMailAssignedTasks() {return g("crmMailAssignedTasks");}

  default String crmMailTaskSubject() {return g("crmMailTaskSubject");}

  default String crmMailTasksSummary() {return g("crmMailTasksSummary");}

  default String crmMailTasksSummarySubject() {return g("crmMailTasksSummarySubject");}

  default String crmMailTasksSummaryText() {return g("crmMailTasksSummaryText");}

  default String crmNewDurationType() {return g("crmNewDurationType");}

  default String crmNewRecurringTask() {return g("crmNewRecurringTask");}

  default String crmNewRequest() {return g("crmNewRequest");}

  default String crmNewRequestForm() {return g("crmNewRequestForm");}

  default String crmNewRequestType() {return g("crmNewRequestType");}

  default String crmNewTask() {return g("crmNewTask");}

  default String crmNewTaskTemplate() {return g("crmNewTaskTemplate");}

  default String crmNewTaskType() {return g("crmNewTaskType");}

  default String crmNewTodoItem() {return g("crmNewTodoItem");}

  default String crmRTActionSchedule() {return g("crmRTActionSchedule");}

  default String crmRTCopyQuestion() {return g("crmRTCopyQuestion");}

  default String crmRTDateExceptionFrom() {return g("crmRTDateExceptionFrom");}

  default String crmRTDateExceptionMode() {return g("crmRTDateExceptionMode");}

  default String crmRTDateExceptionNew() {return g("crmRTDateExceptionNew");}

  default String crmRTDateExceptionUntil() {return g("crmRTDateExceptionUntil");}

  default String crmRTDateExceptions() {return g("crmRTDateExceptions");}

  default String crmRTDayOfMonth() {return g("crmRTDayOfMonth");}

  default String crmRTDayOfWeek() {return g("crmRTDayOfWeek");}

  default String crmRTDurationDays() {return g("crmRTDurationDays");}

  default String crmRTDurationTime() {return g("crmRTDurationTime");}

  default String crmRTFiles() {return g("crmRTFiles");}

  default String crmRTMonth() {return g("crmRTMonth");}

  default String crmRTRemindAt() {return g("crmRTRemindAt");}

  default String crmRTRemindBefore() {return g("crmRTRemindBefore");}

  default String crmRTScheduleDays() {return g("crmRTScheduleDays");}

  default String crmRTScheduleFrom() {return g("crmRTScheduleFrom");}

  default String crmRTScheduleUntil() {return g("crmRTScheduleUntil");}

  default String crmRTSpawnTaskQuestion() {return g("crmRTSpawnTaskQuestion");}

  default String crmRTSpawnTasksQuestion() {return g("crmRTSpawnTasksQuestion");}

  default String crmRTStartAt() {return g("crmRTStartAt");}

  default String crmRTWorkdayTransition() {return g("crmRTWorkdayTransition");}

  default String crmRTYear() {return g("crmRTYear");}

  default String crmRecurrence() {return g("crmRecurrence");}

  default String crmRecurringTask() {return g("crmRecurringTask");}

  default String crmRecurringTasks() {return g("crmRecurringTasks");}

  default String crmRecurringTasksRelated() {return g("crmRecurringTasksRelated");}

  default String crmReminder() {return g("crmReminder");}

  default String crmReminderDate() {return g("crmReminderDate");}

  default String crmReminderMailSubject() {return g("crmReminderMailSubject");}

  default String crmReminderSent() {return g("crmReminderSent");}

  default String crmReminderTimeMustBeGreaterThan() {return g("crmReminderTimeMustBeGreaterThan");}

  default String crmReminderTimeMustBeLessThan() {return g("crmReminderTimeMustBeLessThan");}

  default String crmRequest() {return g("crmRequest");}

  default String crmRequestCompleted() {return g("crmRequestCompleted");}

  default String crmRequestFileCaption() {return g("crmRequestFileCaption");}

  default String crmRequestFiles() {return g("crmRequestFiles");}

  default String crmRequestFilterActive() {return g("crmRequestFilterActive");}

  default String crmRequestFinish() {return g("crmRequestFinish");}

  default String crmRequestForm() {return g("crmRequestForm");}

  default String crmRequestForms() {return g("crmRequestForms");}

  default String crmRequestType() {return g("crmRequestType");}

  default String crmRequestTypeName() {return g("crmRequestTypeName");}

  default String crmRequestTypes() {return g("crmRequestTypes");}

  default String crmRequests() {return g("crmRequests");}

  default String crmSelectExecutor() {return g("crmSelectExecutor");}

  default String crmSelectedSameExecutor() {return g("crmSelectedSameExecutor");}

  default String crmSpentTime() {return g("crmSpentTime");}

  default String crmStartDate() {return g("crmStartDate");}

  default String crmTask() {return g("crmTask");}

  default String crmTaskAddSenderToObservers() {return g("crmTaskAddSenderToObservers");}

  default String crmTaskAddToProject() {return g("crmTaskAddToProject");}

  default String crmTaskAskRemoveFromProject() {return g("crmTaskAskRemoveFromProject");}

  default String crmTaskCancel() {return g("crmTaskCancel");}

  default String crmTaskCancellation() {return g("crmTaskCancellation");}

  default String crmTaskChangeTerm() {return g("crmTaskChangeTerm");}

  default String crmTaskClassifiers() {return g("crmTaskClassifiers");}

  default String crmTaskColumnMode() {return g("crmTaskColumnMode");}

  default String crmTaskColumnStar() {return g("crmTaskColumnStar");}

  default String crmTaskComment() {return g("crmTaskComment");}

  default String crmTaskCommentTimeRegistration() {return g("crmTaskCommentTimeRegistration");}

  default String crmTaskCommentsAsc() {return g("crmTaskCommentsAsc");}

  default String crmTaskCommentsDesc() {return g("crmTaskCommentsDesc");}

  default String crmTaskCompleteDate() {return g("crmTaskCompleteDate");}

  default String crmTaskConfirm() {return g("crmTaskConfirm");}

  default String crmTaskConfirmCanManager() {return g("crmTaskConfirmCanManager");}

  default String crmTaskConfirmDate() {return g("crmTaskConfirmDate");}

  default String crmTaskConfirmation() {return g("crmTaskConfirmation");}

  default String crmTaskCopyQuestion() {return g("crmTaskCopyQuestion");}

  default String crmTaskCreated() {return g("crmTaskCreated");}

  default String crmTaskDelayPlannedShort() {return g("crmTaskDelayPlannedShort");}

  default String crmTaskDeleteCanManager() {return g("crmTaskDeleteCanManager");}

  default String crmTaskDeleteQuestion() {return g("crmTaskDeleteQuestion");}

  default String crmTaskDescription() {return g("crmTaskDescription");}

  default String crmTaskDoExecute() {return g("crmTaskDoExecute");}

  default String crmTaskDuration() {return g("crmTaskDuration");}

  default String crmTaskDurationTypes() {return g("crmTaskDurationTypes");}

  default String crmTaskDurations() {return g("crmTaskDurations");}

  default String crmTaskEvent() {return g("crmTaskEvent");}

  default String crmTaskEventApproved() {return g("crmTaskEventApproved");}

  default String crmTaskEventCreated() {return g("crmTaskEventCreated");}

  default String crmTaskEventEdited() {return g("crmTaskEventEdited");}

  default String crmTaskEventExecuted() {return g("crmTaskEventExecuted");}

  default String crmTaskEventExtended() {return g("crmTaskEventExtended");}

  default String crmTaskEventForwarded() {return g("crmTaskEventForwarded");}

  default String crmTaskEventNote() {return g("crmTaskEventNote");}

  default String crmTaskEventOutOfObservers() {return g("crmTaskEventOutOfObservers");}

  default String crmTaskEventRenewed() {return g("crmTaskEventRenewed");}

  default String crmTaskEventVisited() {return g("crmTaskEventVisited");}

  default String crmTaskEvents() {return g("crmTaskEvents");}

  default String crmTaskExecutor() {return g("crmTaskExecutor");}

  default String crmTaskExecutorGroups() {return g("crmTaskExecutorGroups");}

  default String crmTaskExecutors() {return g("crmTaskExecutors");}

  default String crmTaskExpectedDuration() {return g("crmTaskExpectedDuration");}

  default String crmTaskExpectedExpenses() {return g("crmTaskExpectedExpenses");}

  default String crmTaskFileCaption() {return g("crmTaskFileCaption");}

  default String crmTaskFiles() {return g("crmTaskFiles");}

  default String crmTaskFilterAll() {return g("crmTaskFilterAll");}

  default String crmTaskFilterLate() {return g("crmTaskFilterLate");}

  default String crmTaskFilterNew() {return g("crmTaskFilterNew");}

  default String crmTaskFilterNewOrUpdated() {return g("crmTaskFilterNewOrUpdated");}

  default String crmTaskFilterNotVisitedOrActive() {return g("crmTaskFilterNotVisitedOrActive");}

  default String crmTaskFilterNotVisitedOrActiveOrCompleted() {return g("crmTaskFilterNotVisitedOrActiveOrCompleted");}

  default String crmTaskFilterNotVisitedOrActiveOrVisited() {return g("crmTaskFilterNotVisitedOrActiveOrVisited");}

  default String crmTaskFilterScheduled() {return g("crmTaskFilterScheduled");}

  default String crmTaskFilterStarred() {return g("crmTaskFilterStarred");}

  default String crmTaskFilterUpdated() {return g("crmTaskFilterUpdated");}

  default String crmTaskFinishDate() {return g("crmTaskFinishDate");}

  default String crmTaskFinishing() {return g("crmTaskFinishing");}

  default String crmTaskForwardForExecution() {return g("crmTaskForwardForExecution");}

  default String crmTaskForwardedForExecution() {return g("crmTaskForwardedForExecution");}

  default String crmTaskForwarding() {return g("crmTaskForwarding");}

  default String crmTaskForwardingForExecution() {return g("crmTaskForwardingForExecution");}

  default String crmTaskLabelDelayedHours() {return g("crmTaskLabelDelayedHours");}

  default String crmTaskLabelLate() {return g("crmTaskLabelLate");}

  default String crmTaskLabelScheduled() {return g("crmTaskLabelScheduled");}

  default String crmTaskLabelStarred() {return g("crmTaskLabelStarred");}

  default String crmTaskLastAccess() {return g("crmTaskLastAccess");}

  default String crmTaskManager() {return g("crmTaskManager");}

  default String crmTaskMustBePerformed() {return g("crmTaskMustBePerformed");}

  default String crmTaskNewProduct() {return g("crmTaskNewProduct");}

  default String crmTaskNotFound() {return g("crmTaskNotFound");}

  default String crmTaskObserverGroups() {return g("crmTaskObserverGroups");}

  default String crmTaskObservers() {return g("crmTaskObservers");}

  default String crmTaskOutOfObservers() {return g("crmTaskOutOfObservers");}

  default String crmTaskOwnerCanNotBe(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("crmTaskOwnerCanNotBe"), _m);
  }

  default String crmTaskParameters() {return g("crmTaskParameters");}

  default String crmTaskPriority() {return g("crmTaskPriority");}

  default String crmTaskPriorityHigh() {return g("crmTaskPriorityHigh");}

  default String crmTaskPriorityLow() {return g("crmTaskPriorityLow");}

  default String crmTaskPriorityMedium() {return g("crmTaskPriorityMedium");}

  default String crmTaskPrivate() {return g("crmTaskPrivate");}

  default String crmTaskProduct() {return g("crmTaskProduct");}

  default String crmTaskProductRequired() {return g("crmTaskProductRequired");}

  default String crmTaskProducts() {return g("crmTaskProducts");}

  default String crmTaskPublishTime() {return g("crmTaskPublishTime");}

  default String crmTaskReminder() {return g("crmTaskReminder");}

  default String crmTaskRemoveFromProject() {return g("crmTaskRemoveFromProject");}

  default String crmTaskReports() {return g("crmTaskReports");}

  default String crmTaskReturnExecution() {return g("crmTaskReturnExecution");}

  default String crmTaskReturningForExecution() {return g("crmTaskReturningForExecution");}

  default String crmTaskShowCommentsBelow() {return g("crmTaskShowCommentsBelow");}

  default String crmTaskShowCommentsRight() {return g("crmTaskShowCommentsRight");}

  default String crmTaskStatus() {return g("crmTaskStatus");}

  default String crmTaskStatusActive() {return g("crmTaskStatusActive");}

  default String crmTaskStatusApproved() {return g("crmTaskStatusApproved");}

  default String crmTaskStatusCanceled() {return g("crmTaskStatusCanceled");}

  default String crmTaskStatusCompleted() {return g("crmTaskStatusCompleted");}

  default String crmTaskStatusNotScheduled() {return g("crmTaskStatusNotScheduled");}

  default String crmTaskStatusNotVisited() {return g("crmTaskStatusNotVisited");}

  default String crmTaskStatusScheduled() {return g("crmTaskStatusScheduled");}

  default String crmTaskStatusSuspended() {return g("crmTaskStatusSuspended");}

  default String crmTaskStatusVisited() {return g("crmTaskStatusVisited");}

  default String crmTaskStopExecute() {return g("crmTaskStopExecute");}

  default String crmTaskSubject() {return g("crmTaskSubject");}

  default String crmTaskSuspension() {return g("crmTaskSuspension");}

  default String crmTaskTemplate() {return g("crmTaskTemplate");}

  default String crmTaskTemplateName() {return g("crmTaskTemplateName");}

  default String crmTaskTemplates() {return g("crmTaskTemplates");}

  default String crmTaskTermChange() {return g("crmTaskTermChange");}

  default String crmTaskType() {return g("crmTaskType");}

  default String crmTaskTypeName() {return g("crmTaskTypeName");}

  default String crmTaskTypes() {return g("crmTaskTypes");}

  default String crmTaskUsers() {return g("crmTaskUsers");}

  default String crmTasks() {return g("crmTasks");}

  default String crmTasksAll() {return g("crmTasksAll");}

  default String crmTasksAssigned() {return g("crmTasksAssigned");}

  default String crmTasksAssignedTasks() {return g("crmTasksAssignedTasks");}

  default String crmTasksConfirmQuestion() {return g("crmTasksConfirmQuestion");}

  default String crmTasksDelegated() {return g("crmTasksDelegated");}

  default String crmTasksDelegatedTasks() {return g("crmTasksDelegatedTasks");}

  default String crmTasksNotScheduledTask() {return g("crmTasksNotScheduledTask");}

  default String crmTasksNotScheduledTasks() {return g("crmTasksNotScheduledTasks");}

  default String crmTasksObserved() {return g("crmTasksObserved");}

  default String crmTasksObservedTasks() {return g("crmTasksObservedTasks");}

  default String crmTasksRelated() {return g("crmTasksRelated");}

  default String crmTermNotChanged() {return g("crmTermNotChanged");}

  default String crmTodoCreateAppointment() {return g("crmTodoCreateAppointment");}

  default String crmTodoCreateTask() {return g("crmTodoCreateTask");}

  default String crmTodoItem() {return g("crmTodoItem");}

  default String crmTodoList() {return g("crmTodoList");}

  default String currencies() {return g("currencies");}

  default String currency() {return g("currency");}

  default String currency1() {return g("currency1");}

  default String currency10() {return g("currency10");}

  default String currency2() {return g("currency2");}

  default String currency3() {return g("currency3");}

  default String currency4() {return g("currency4");}

  default String currency5() {return g("currency5");}

  default String currency6() {return g("currency6");}

  default String currency7() {return g("currency7");}

  default String currency8() {return g("currency8");}

  default String currency9() {return g("currency9");}

  default String currencyRate() {return g("currencyRate");}

  default String currencyRateInverse() {return g("currencyRateInverse");}

  default String currencyRates() {return g("currencyRates");}

  default String currencyShort() {return g("currencyShort");}

  default String currencyUpdateTag() {return g("currencyUpdateTag");}

  default String customConfig() {return g("customConfig");}

  default String customer() {return g("customer");}

  default String customerPrice() {return g("customerPrice");}

  default String dangerous() {return g("dangerous");}

  default String dangerousShort() {return g("dangerousShort");}

  default String data() {return g("data");}

  default String dataCreateImportTemplates() {return g("dataCreateImportTemplates");}

  default String dataImport() {return g("dataImport");}

  default String dataNotAvailable(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("dataNotAvailable"), _m);
  }

  default String date() {return g("date");}

  default String dateFormat() {return g("dateFormat");}

  default String dateFrom() {return g("dateFrom");}

  default String dateFromShort() {return g("dateFromShort");}

  default String dateOfBirth() {return g("dateOfBirth");}

  default String dateTime() {return g("dateTime");}

  default String dateTo() {return g("dateTo");}

  default String dateToShort() {return g("dateToShort");}

  default String day() {return g("day");}

  default String dayOfWeek() {return g("dayOfWeek");}

  default String dayShort() {return g("dayShort");}

  default String days() {return g("days");}

  default String daysShort() {return g("daysShort");}

  default String debit() {return g("debit");}

  default String debitDocument() {return g("debitDocument");}

  default String debitDocumentShort() {return g("debitDocumentShort");}

  default String debitReplacement() {return g("debitReplacement");}

  default String debitReplacementShort() {return g("debitReplacementShort");}

  default String debitSeries() {return g("debitSeries");}

  default String debitSeriesShort() {return g("debitSeriesShort");}

  default String debt() {return g("debt");}

  default String decline() {return g("decline");}

  default String defaultBankAccount() {return g("defaultBankAccount");}

  default String defaultBankAccountShort() {return g("defaultBankAccountShort");}

  default String defaultColor() {return g("defaultColor");}

  default String defaultCompanyUser() {return g("defaultCompanyUser");}

  default String defaultCompanyUserShort() {return g("defaultCompanyUserShort");}

  default String defaultQuantity() {return g("defaultQuantity");}

  default String delete() {return g("delete");}

  default String deleteActiveRow() {return g("deleteActiveRow");}

  default String deletePictureQuestion() {return g("deletePictureQuestion");}

  default String deleteQuestion() {return g("deleteQuestion");}

  default String deleteRecordQuestion() {return g("deleteRecordQuestion");}

  default String deleteRowError() {return g("deleteRowError");}

  default String deleteRowQuestion() {return g("deleteRowQuestion");}

  default String deleteRowsError() {return g("deleteRowsError");}

  default String deleteSelectedRow() {return g("deleteSelectedRow");}

  default String deleteSelectedRows(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("deleteSelectedRows"), _m);
  }

  default String deletedRows(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("deletedRows"), _m);
  }

  default String department() {return g("department");}

  default String departmentEmployee() {return g("departmentEmployee");}

  default String departmentEmployees() {return g("departmentEmployees");}

  default String departmentHead() {return g("departmentHead");}

  default String departmentParent() {return g("departmentParent");}

  default String departmentPositions() {return g("departmentPositions");}

  default String departments() {return g("departments");}

  default String description() {return g("description");}

  default String deselectAll() {return g("deselectAll");}

  default String dictionary() {return g("dictionary");}

  default String differences() {return g("differences");}

  default String dimensionNameDefault(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("dimensionNameDefault"), _m);
  }

  default String dimensionNames() {return g("dimensionNames");}

  default String dimensions() {return g("dimensions");}

  default String disabled() {return g("disabled");}

  default String disabledShort() {return g("disabledShort");}

  default String discount() {return g("discount");}

  default String discountAmountShort() {return g("discountAmountShort");}

  default String discountDateFrom() {return g("discountDateFrom");}

  default String discountDateTo() {return g("discountDateTo");}

  default String discountForDocument() {return g("discountForDocument");}

  default String discountFromAmount() {return g("discountFromAmount");}

  default String discountFromPrice() {return g("discountFromPrice");}

  default String discountIsPercent() {return g("discountIsPercent");}

  default String discountMode() {return g("discountMode");}

  default String discountNew() {return g("discountNew");}

  default String discountNote() {return g("discountNote");}

  default String discountParent() {return g("discountParent");}

  default String discountPercent() {return g("discountPercent");}

  default String discountPrice() {return g("discountPrice");}

  default String discounts() {return g("discounts");}

  default String discussAccessibility() {return g("discussAccessibility");}

  default String discussActionActivate() {return g("discussActionActivate");}

  default String discussActionClose() {return g("discussActionClose");}

  default String discussActionComment() {return g("discussActionComment");}

  default String discussActionDeactivate() {return g("discussActionDeactivate");}

  default String discussActionMark() {return g("discussActionMark");}

  default String discussActionModify() {return g("discussActionModify");}

  default String discussActionReply() {return g("discussActionReply");}

  default String discussActivationQuestion() {return g("discussActivationQuestion");}

  default String discussActive() {return g("discussActive");}

  default String discussAdministration() {return g("discussAdministration");}

  default String discussAll() {return g("discussAll");}

  default String discussAllShort() {return g("discussAllShort");}

  default String discussCloseQuestion() {return g("discussCloseQuestion");}

  default String discussClosed() {return g("discussClosed");}

  default String discussClosedCaption() {return g("discussClosedCaption");}

  default String discussColumnMode() {return g("discussColumnMode");}

  default String discussColumnStar() {return g("discussColumnStar");}

  default String discussComment() {return g("discussComment");}

  default String discussCommentCount() {return g("discussCommentCount");}

  default String discussCommentMarkImageName() {return g("discussCommentMarkImageName");}

  default String discussCommentMarkName() {return g("discussCommentMarkName");}

  default String discussCommentPermit() {return g("discussCommentPermit");}

  default String discussCommentPlaceholder() {return g("discussCommentPlaceholder");}

  default String discussCommentPublished() {return g("discussCommentPublished");}

  default String discussCreated() {return g("discussCreated");}

  default String discussCreatedNewAnnouncement() {return g("discussCreatedNewAnnouncement");}

  default String discussCreatedNewDiscussion() {return g("discussCreatedNewDiscussion");}

  default String discussDeleteCanOwnerOrAdmin() {return g("discussDeleteCanOwnerOrAdmin");}

  default String discussDeleteQuestion() {return g("discussDeleteQuestion");}

  default String discussDescription() {return g("discussDescription");}

  default String discussDetailInfo() {return g("discussDetailInfo");}

  default String discussEnterComment() {return g("discussEnterComment");}

  default String discussEventActivated() {return g("discussEventActivated");}

  default String discussEventClosed() {return g("discussEventClosed");}

  default String discussEventCommentDeleted() {return g("discussEventCommentDeleted");}

  default String discussEventCommented() {return g("discussEventCommented");}

  default String discussEventCreated() {return g("discussEventCreated");}

  default String discussEventDeactivated() {return g("discussEventDeactivated");}

  default String discussEventMarked() {return g("discussEventMarked");}

  default String discussEventModified() {return g("discussEventModified");}

  default String discussEventReplied() {return g("discussEventReplied");}

  default String discussEventVisited() {return g("discussEventVisited");}

  default String discussFile() {return g("discussFile");}

  default String discussFileCaption() {return g("discussFileCaption");}

  default String discussFiles() {return g("discussFiles");}

  default String discussFilterStarred() {return g("discussFilterStarred");}

  default String discussHasFiles() {return g("discussHasFiles");}

  default String discussHasRelations() {return g("discussHasRelations");}

  default String discussInvalidFile() {return g("discussInvalidFile");}

  default String discussLastAccess() {return g("discussLastAccess");}

  default String discussLastComment() {return g("discussLastComment");}

  default String discussLastCommentator() {return g("discussLastCommentator");}

  default String discussMailNewAnnouncementSubject() {return g("discussMailNewAnnouncementSubject");}

  default String discussMailNewAnnouncements() {return g("discussMailNewAnnouncements");}

  default String discussMailNewDiscussionSubject() {return g("discussMailNewDiscussionSubject");}

  default String discussMailNewDiscussions() {return g("discussMailNewDiscussions");}

  default String discussMarkStats() {return g("discussMarkStats");}

  default String discussMarkTypes() {return g("discussMarkTypes");}

  default String discussMarked() {return g("discussMarked");}

  default String discussMarks() {return g("discussMarks");}

  default String discussMember() {return g("discussMember");}

  default String discussMembers() {return g("discussMembers");}

  default String discussMembersOnly() {return g("discussMembersOnly");}

  default String discussNew() {return g("discussNew");}

  default String discussNotCreated() {return g("discussNotCreated");}

  default String discussObserved() {return g("discussObserved");}

  default String discussOwner() {return g("discussOwner");}

  default String discussParameters() {return g("discussParameters");}

  default String discussPrivate() {return g("discussPrivate");}

  default String discussPrivateDiscussion() {return g("discussPrivateDiscussion");}

  default String discussPrivateShort() {return g("discussPrivateShort");}

  default String discussPublic() {return g("discussPublic");}

  default String discussPublic1() {return g("discussPublic1");}

  default String discussPublicLong() {return g("discussPublicLong");}

  default String discussRelatedInformation() {return g("discussRelatedInformation");}

  default String discussSelectMembers() {return g("discussSelectMembers");}

  default String discussStarred() {return g("discussStarred");}

  default String discussStatus() {return g("discussStatus");}

  default String discussStatusActive() {return g("discussStatusActive");}

  default String discussStatusClosed() {return g("discussStatusClosed");}

  default String discussStatusCommented() {return g("discussStatusCommented");}

  default String discussStatusInactive() {return g("discussStatusInactive");}

  default String discussStatusNew() {return g("discussStatusNew");}

  default String discussSubject() {return g("discussSubject");}

  default String discussSummary() {return g("discussSummary");}

  default String discussTotalComments() {return g("discussTotalComments");}

  default String discussTotalMarks() {return g("discussTotalMarks");}

  default String discussUserGroup() {return g("discussUserGroup");}

  default String discussion() {return g("discussion");}

  default String discussionFiles() {return g("discussionFiles");}

  default String discussions() {return g("discussions");}

  default String displayInBoard() {return g("displayInBoard");}

  default String doFilter() {return g("doFilter");}

  default String document() {return g("document");}

  default String documentCategories() {return g("documentCategories");}

  default String documentCategory() {return g("documentCategory");}

  default String documentContentIsEmpty() {return g("documentContentIsEmpty");}

  default String documentCount() {return g("documentCount");}

  default String documentData() {return g("documentData");}

  default String documentDate() {return g("documentDate");}

  default String documentDescription() {return g("documentDescription");}

  default String documentExpireReminderMailSubject() {return g("documentExpireReminderMailSubject");}

  default String documentExpires() {return g("documentExpires");}

  default String documentFileCaption() {return g("documentFileCaption");}

  default String documentFileComment() {return g("documentFileComment");}

  default String documentFileDate() {return g("documentFileDate");}

  default String documentFileDescription() {return g("documentFileDescription");}

  default String documentFileExists() {return g("documentFileExists");}

  default String documentFileOpen() {return g("documentFileOpen");}

  default String documentFileOwner() {return g("documentFileOwner");}

  default String documentFileVersion() {return g("documentFileVersion");}

  default String documentFilterNotReturned() {return g("documentFilterNotReturned");}

  default String documentFilterReceived() {return g("documentFilterReceived");}

  default String documentFilterSent() {return g("documentFilterSent");}

  default String documentItem() {return g("documentItem");}

  default String documentItems() {return g("documentItems");}

  default String documentName() {return g("documentName");}

  default String documentNew() {return g("documentNew");}

  default String documentNewPlace() {return g("documentNewPlace");}

  default String documentNewStatus() {return g("documentNewStatus");}

  default String documentNewType() {return g("documentNewType");}

  default String documentNumber() {return g("documentNumber");}

  default String documentNumberPrefix() {return g("documentNumberPrefix");}

  default String documentNumberPrefixes() {return g("documentNumberPrefixes");}

  default String documentParentCategory() {return g("documentParentCategory");}

  default String documentPlace() {return g("documentPlace");}

  default String documentPlaces() {return g("documentPlaces");}

  default String documentReceived() {return g("documentReceived");}

  default String documentReceivedNumber() {return g("documentReceivedNumber");}

  default String documentRegistrationNumber() {return g("documentRegistrationNumber");}

  default String documentRegistrationNumberShort() {return g("documentRegistrationNumberShort");}

  default String documentReturned() {return g("documentReturned");}

  default String documentSent() {return g("documentSent");}

  default String documentSentNumber() {return g("documentSentNumber");}

  default String documentStatus() {return g("documentStatus");}

  default String documentStatuses() {return g("documentStatuses");}

  default String documentTemplate() {return g("documentTemplate");}

  default String documentTemplateName() {return g("documentTemplateName");}

  default String documentTemplates() {return g("documentTemplates");}

  default String documentTree() {return g("documentTree");}

  default String documentType() {return g("documentType");}

  default String documentTypes() {return g("documentTypes");}

  default String documents() {return g("documents");}

  default String documentsRelated() {return g("documentsRelated");}

  default String domainNews() {return g("domainNews");}

  default String domainOnline() {return g("domainOnline");}

  default String driverAbsence() {return g("driverAbsence");}

  default String driverGroups() {return g("driverGroups");}

  default String driverGroupsShort() {return g("driverGroupsShort");}

  default String driverPosition() {return g("driverPosition");}

  default String driverTimeBoard() {return g("driverTimeBoard");}

  default String drivers() {return g("drivers");}

  default String duration() {return g("duration");}

  default String dutyOrder() {return g("dutyOrder");}

  default String ecAdministration() {return g("ecAdministration");}

  default String ecAnalogBinding() {return g("ecAnalogBinding");}

  default String ecBannerAfter() {return g("ecBannerAfter");}

  default String ecBannerBefore() {return g("ecBannerBefore");}

  default String ecBannerHeight() {return g("ecBannerHeight");}

  default String ecBannerLink() {return g("ecBannerLink");}

  default String ecBannerSort() {return g("ecBannerSort");}

  default String ecBannerWidth() {return g("ecBannerWidth");}

  default String ecBanners() {return g("ecBanners");}

  default String ecBikeItems() {return g("ecBikeItems");}

  default String ecBikeItemsShort() {return g("ecBikeItemsShort");}

  default String ecBranchPrimary() {return g("ecBranchPrimary");}

  default String ecBranchSecondary() {return g("ecBranchSecondary");}

  default String ecCarCylinders() {return g("ecCarCylinders");}

  default String ecCarEngine() {return g("ecCarEngine");}

  default String ecCarManufacturer() {return g("ecCarManufacturer");}

  default String ecCarManufacturerVisible() {return g("ecCarManufacturerVisible");}

  default String ecCarMaxWeight() {return g("ecCarMaxWeight");}

  default String ecCarModel() {return g("ecCarModel");}

  default String ecCarModelVisible() {return g("ecCarModelVisible");}

  default String ecCarPower() {return g("ecCarPower");}

  default String ecCarPowerFrom() {return g("ecCarPowerFrom");}

  default String ecCarPowerTo() {return g("ecCarPowerTo");}

  default String ecCarProduced() {return g("ecCarProduced");}

  default String ecCarProducedFrom() {return g("ecCarProducedFrom");}

  default String ecCarProducedTo() {return g("ecCarProducedTo");}

  default String ecCarTypeHistory() {return g("ecCarTypeHistory");}

  default String ecCarTypeHistorySize() {return g("ecCarTypeHistorySize");}

  default String ecCarTypeVisible() {return g("ecCarTypeVisible");}

  default String ecCarVolume() {return g("ecCarVolume");}

  default String ecCarYear() {return g("ecCarYear");}

  default String ecCatalog() {return g("ecCatalog");}

  default String ecCategoryMerge() {return g("ecCategoryMerge");}

  default String ecCategoryMigrate(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("ecCategoryMigrate"), _m);
  }

  default String ecCategoryMove() {return g("ecCategoryMove");}

  default String ecClient() {return g("ecClient");}

  default String ecClientActivity() {return g("ecClientActivity");}

  default String ecClientCompanyCode() {return g("ecClientCompanyCode");}

  default String ecClientCompanyName() {return g("ecClientCompanyName");}

  default String ecClientCreditLimitWarning() {return g("ecClientCreditLimitWarning");}

  default String ecClientFirstName() {return g("ecClientFirstName");}

  default String ecClientLastName() {return g("ecClientLastName");}

  default String ecClientNew() {return g("ecClientNew");}

  default String ecClientPersonCode() {return g("ecClientPersonCode");}

  default String ecClientPrice() {return g("ecClientPrice");}

  default String ecClientRegistrationDate() {return g("ecClientRegistrationDate");}

  default String ecClientType() {return g("ecClientType");}

  default String ecClientTypeCompany() {return g("ecClientTypeCompany");}

  default String ecClientTypePerson() {return g("ecClientTypePerson");}

  default String ecClientUser() {return g("ecClientUser");}

  default String ecClientVatCode() {return g("ecClientVatCode");}

  default String ecClientVatPayer() {return g("ecClientVatPayer");}

  default String ecClients() {return g("ecClients");}

  default String ecContacts() {return g("ecContacts");}

  default String ecCostChanges() {return g("ecCostChanges");}

  default String ecCreditLimit() {return g("ecCreditLimit");}

  default String ecCriterion() {return g("ecCriterion");}

  default String ecCriterionNew() {return g("ecCriterionNew");}

  default String ecCriterionValue() {return g("ecCriterionValue");}

  default String ecDaysForPayment() {return g("ecDaysForPayment");}

  default String ecDebt() {return g("ecDebt");}

  default String ecDeliveryAddress() {return g("ecDeliveryAddress");}

  default String ecDeliveryMethod() {return g("ecDeliveryMethod");}

  default String ecDeliveryMethodNew() {return g("ecDeliveryMethodNew");}

  default String ecDeliveryMethodRequired() {return g("ecDeliveryMethodRequired");}

  default String ecDeliveryMethods() {return g("ecDeliveryMethods");}

  default String ecDisplayedPrice() {return g("ecDisplayedPrice");}

  default String ecDoSearch() {return g("ecDoSearch");}

  default String ecExceededCreditLimit() {return g("ecExceededCreditLimit");}

  default String ecExceededCreditLimitSend() {return g("ecExceededCreditLimitSend");}

  default String ecFeaturedBanner() {return g("ecFeaturedBanner");}

  default String ecFeaturedItems() {return g("ecFeaturedItems");}

  default String ecFeaturedPercent() {return g("ecFeaturedPercent");}

  default String ecFeaturedPrice() {return g("ecFeaturedPrice");}

  default String ecFeaturedUntil() {return g("ecFeaturedUntil");}

  default String ecFinancialInformation() {return g("ecFinancialInformation");}

  default String ecFoundItems() {return g("ecFoundItems");}

  default String ecGlobalSearchPlaceholder() {return g("ecGlobalSearchPlaceholder");}

  default String ecGroup() {return g("ecGroup");}

  default String ecGroupBrandSelection() {return g("ecGroupBrandSelection");}

  default String ecGroupCategories() {return g("ecGroupCategories");}

  default String ecGroupCriteria() {return g("ecGroupCriteria");}

  default String ecGroupMoto() {return g("ecGroupMoto");}

  default String ecGroupNew() {return g("ecGroupNew");}

  default String ecGroups() {return g("ecGroups");}

  default String ecHistoryArticle() {return g("ecHistoryArticle");}

  default String ecHistoryCount() {return g("ecHistoryCount");}

  default String ecHistoryDuration() {return g("ecHistoryDuration");}

  default String ecHistoryQuery() {return g("ecHistoryQuery");}

  default String ecHistoryService() {return g("ecHistoryService");}

  default String ecInMyCart(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("ecInMyCart"), _m);
  }

  default String ecInvoiceAmount() {return g("ecInvoiceAmount");}

  default String ecInvoiceDate() {return g("ecInvoiceDate");}

  default String ecInvoiceDebt() {return g("ecInvoiceDebt");}

  default String ecInvoiceIndulgence() {return g("ecInvoiceIndulgence");}

  default String ecInvoiceNumber() {return g("ecInvoiceNumber");}

  default String ecInvoiceOverdue() {return g("ecInvoiceOverdue");}

  default String ecInvoiceTerm() {return g("ecInvoiceTerm");}

  default String ecInvoices() {return g("ecInvoices");}

  default String ecItem() {return g("ecItem");}

  default String ecItemAnalog() {return g("ecItemAnalog");}

  default String ecItemAnalogNew() {return g("ecItemAnalogNew");}

  default String ecItemAnalogs() {return g("ecItemAnalogs");}

  default String ecItemBrand() {return g("ecItemBrand");}

  default String ecItemCapacity() {return g("ecItemCapacity");}

  default String ecItemCategories() {return g("ecItemCategories");}

  default String ecItemCategory() {return g("ecItemCategory");}

  default String ecItemCategoryFullName() {return g("ecItemCategoryFullName");}

  default String ecItemCategoryNew() {return g("ecItemCategoryNew");}

  default String ecItemCategoryNote() {return g("ecItemCategoryNote");}

  default String ecItemCharge() {return g("ecItemCharge");}

  default String ecItemCode() {return g("ecItemCode");}

  default String ecItemCost() {return g("ecItemCost");}

  default String ecItemCriteria() {return g("ecItemCriteria");}

  default String ecItemDescription() {return g("ecItemDescription");}

  default String ecItemDetailsCarTypes() {return g("ecItemDetailsCarTypes");}

  default String ecItemDetailsOeNumbers() {return g("ecItemDetailsOeNumbers");}

  default String ecItemDetailsRemainders() {return g("ecItemDetailsRemainders");}

  default String ecItemDetailsSuppliers() {return g("ecItemDetailsSuppliers");}

  default String ecItemDiameter() {return g("ecItemDiameter");}

  default String ecItemGraphics() {return g("ecItemGraphics");}

  default String ecItemHeight() {return g("ecItemHeight");}

  default String ecItemLength() {return g("ecItemLength");}

  default String ecItemName() {return g("ecItemName");}

  default String ecItemNew() {return g("ecItemNew");}

  default String ecItemNote() {return g("ecItemNote");}

  default String ecItemOeNumber() {return g("ecItemOeNumber");}

  default String ecItemOriginalNumber() {return g("ecItemOriginalNumber");}

  default String ecItemPrice() {return g("ecItemPrice");}

  default String ecItemQuantity() {return g("ecItemQuantity");}

  default String ecItemQuantityOrdered() {return g("ecItemQuantityOrdered");}

  default String ecItemQuantitySubmit() {return g("ecItemQuantitySubmit");}

  default String ecItemSeason() {return g("ecItemSeason");}

  default String ecItemSpeedIndex() {return g("ecItemSpeedIndex");}

  default String ecItemSupplier() {return g("ecItemSupplier");}

  default String ecItemSupplierCode() {return g("ecItemSupplierCode");}

  default String ecItemType() {return g("ecItemType");}

  default String ecItemUnit() {return g("ecItemUnit");}

  default String ecItemViscosity() {return g("ecItemViscosity");}

  default String ecItemVisible() {return g("ecItemVisible");}

  default String ecItemVoltage() {return g("ecItemVoltage");}

  default String ecItemWeight() {return g("ecItemWeight");}

  default String ecItemWidth() {return g("ecItemWidth");}

  default String ecKeyboardShortcuts() {return g("ecKeyboardShortcuts");}

  default String ecListPrice() {return g("ecListPrice");}

  default String ecListPriceShort() {return g("ecListPriceShort");}

  default String ecLocateAnalogs(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("ecLocateAnalogs"), _m);
  }

  default String ecMailAccount() {return g("ecMailAccount");}

  default String ecMailAccountNotFound() {return g("ecMailAccountNotFound");}

  default String ecMailClientAddressNotFound() {return g("ecMailClientAddressNotFound");}

  default String ecMailFailed() {return g("ecMailFailed");}

  default String ecMailIncoming() {return g("ecMailIncoming");}

  default String ecMailParameters() {return g("ecMailParameters");}

  default String ecMailSent() {return g("ecMailSent");}

  default String ecManager() {return g("ecManager");}

  default String ecManagerNew() {return g("ecManagerNew");}

  default String ecManagerTabNr() {return g("ecManagerTabNr");}

  default String ecManagers() {return g("ecManagers");}

  default String ecMarginDefaultPercent() {return g("ecMarginDefaultPercent");}

  default String ecMaxedOut() {return g("ecMaxedOut");}

  default String ecMenu() {return g("ecMenu");}

  default String ecModule() {return g("ecModule");}

  default String ecMoreItems() {return g("ecMoreItems");}

  default String ecNothingToOrder() {return g("ecNothingToOrder");}

  default String ecNoveltyBanner() {return g("ecNoveltyBanner");}

  default String ecNoveltyItems() {return g("ecNoveltyItems");}

  default String ecNoveltyUntil() {return g("ecNoveltyUntil");}

  default String ecOrder() {return g("ecOrder");}

  default String ecOrderAmount() {return g("ecOrderAmount");}

  default String ecOrderCommandFinish() {return g("ecOrderCommandFinish");}

  default String ecOrderCommandMail() {return g("ecOrderCommandMail");}

  default String ecOrderCommandReject() {return g("ecOrderCommandReject");}

  default String ecOrderCommandSendToERP() {return g("ecOrderCommandSendToERP");}

  default String ecOrderCommandUnsuppliedItems() {return g("ecOrderCommandUnsuppliedItems");}

  default String ecOrderCommentClient() {return g("ecOrderCommentClient");}

  default String ecOrderCommentManager() {return g("ecOrderCommentManager");}

  default String ecOrderCopyByMail() {return g("ecOrderCopyByMail");}

  default String ecOrderDate() {return g("ecOrderDate");}

  default String ecOrderEvents() {return g("ecOrderEvents");}

  default String ecOrderFinishConfirm() {return g("ecOrderFinishConfirm");}

  default String ecOrderId(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("ecOrderId"), _m);
  }

  default String ecOrderItems() {return g("ecOrderItems");}

  default String ecOrderMailConfirm() {return g("ecOrderMailConfirm");}

  default String ecOrderNew() {return g("ecOrderNew");}

  default String ecOrderNumber() {return g("ecOrderNumber");}

  default String ecOrderRejectCaption() {return g("ecOrderRejectCaption");}

  default String ecOrderRejectConfirm() {return g("ecOrderRejectConfirm");}

  default String ecOrderSendToERPConfirm() {return g("ecOrderSendToERPConfirm");}

  default String ecOrderStatus() {return g("ecOrderStatus");}

  default String ecOrderStatusActive() {return g("ecOrderStatusActive");}

  default String ecOrderStatusActiveSubject() {return g("ecOrderStatusActiveSubject");}

  default String ecOrderStatusFinished() {return g("ecOrderStatusFinished");}

  default String ecOrderStatusFinishedSubject() {return g("ecOrderStatusFinishedSubject");}

  default String ecOrderStatusNew() {return g("ecOrderStatusNew");}

  default String ecOrderStatusNewSubject() {return g("ecOrderStatusNewSubject");}

  default String ecOrderStatusRejected() {return g("ecOrderStatusRejected");}

  default String ecOrderStatusRejectedSubject() {return g("ecOrderStatusRejectedSubject");}

  default String ecOrderSubmissionDate() {return g("ecOrderSubmissionDate");}

  default String ecOrderSubmitted() {return g("ecOrderSubmitted");}

  default String ecOrderTotal(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("ecOrderTotal"), _m);
  }

  default String ecOrders() {return g("ecOrders");}

  default String ecOrdersJoin() {return g("ecOrdersJoin");}

  default String ecOrdersSubmitted() {return g("ecOrdersSubmitted");}

  default String ecOrphans() {return g("ecOrphans");}

  default String ecParameters() {return g("ecParameters");}

  default String ecPriceList() {return g("ecPriceList");}

  default String ecPriceListBase() {return g("ecPriceListBase");}

  default String ecPriceListClient() {return g("ecPriceListClient");}

  default String ecPricing() {return g("ecPricing");}

  default String ecRegister() {return g("ecRegister");}

  default String ecRegistration() {return g("ecRegistration");}

  default String ecRegistrationCommandCreate() {return g("ecRegistrationCommandCreate");}

  default String ecRegistrationFormCaption() {return g("ecRegistrationFormCaption");}

  default String ecRegistrationMailContent(Object p0, Object p1, Object p2) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    _m.put("{2}", p2);
    return Localized.format(g("ecRegistrationMailContent"), _m);
  }

  default String ecRegistrationMailSubject(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("ecRegistrationMailSubject"), _m);
  }

  default String ecRegistrationNew() {return g("ecRegistrationNew");}

  default String ecRegistrationReceived() {return g("ecRegistrationReceived");}

  default String ecRegistrations() {return g("ecRegistrations");}

  default String ecRejectionReason() {return g("ecRejectionReason");}

  default String ecRejectionReasonNew() {return g("ecRejectionReasonNew");}

  default String ecRejectionReasonRequired() {return g("ecRejectionReasonRequired");}

  default String ecRejectionReasons() {return g("ecRejectionReasons");}

  default String ecRemainder() {return g("ecRemainder");}

  default String ecRemoveCartItem(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("ecRemoveCartItem"), _m);
  }

  default String ecSearchBy() {return g("ecSearchBy");}

  default String ecSearchByBrand() {return g("ecSearchByBrand");}

  default String ecSearchByCar() {return g("ecSearchByCar");}

  default String ecSearchByItemCode() {return g("ecSearchByItemCode");}

  default String ecSearchByItemGroup() {return g("ecSearchByItemGroup");}

  default String ecSearchByOeNumber() {return g("ecSearchByOeNumber");}

  default String ecSearchDidNotMatch(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("ecSearchDidNotMatch"), _m);
  }

  default String ecSelectBrand() {return g("ecSelectBrand");}

  default String ecSelectCategory() {return g("ecSelectCategory");}

  default String ecSelectGroup() {return g("ecSelectGroup");}

  default String ecShoppingCart() {return g("ecShoppingCart");}

  default String ecShoppingCartAlternative() {return g("ecShoppingCartAlternative");}

  default String ecShoppingCartAlternativeShort() {return g("ecShoppingCartAlternativeShort");}

  default String ecShoppingCartMain() {return g("ecShoppingCartMain");}

  default String ecShoppingCartMainShort() {return g("ecShoppingCartMainShort");}

  default String ecShoppingCartRemove() {return g("ecShoppingCartRemove");}

  default String ecShoppingCartSubmit() {return g("ecShoppingCartSubmit");}

  default String ecShoppingCartTotal() {return g("ecShoppingCartTotal");}

  default String ecShowDetails() {return g("ecShowDetails");}

  default String ecStockAsk() {return g("ecStockAsk");}

  default String ecSupplierWarehouse() {return g("ecSupplierWarehouse");}

  default String ecTermsOfDelivery() {return g("ecTermsOfDelivery");}

  default String ecToggleListPrice() {return g("ecToggleListPrice");}

  default String ecTogglePrice() {return g("ecTogglePrice");}

  default String ecToggleStockLimit() {return g("ecToggleStockLimit");}

  default String ecTotalOrdered() {return g("ecTotalOrdered");}

  default String ecTotalTaken() {return g("ecTotalTaken");}

  default String ecUnsuppliedItemNew() {return g("ecUnsuppliedItemNew");}

  default String ecUnsuppliedItemOrder() {return g("ecUnsuppliedItemOrder");}

  default String ecUnsuppliedItems() {return g("ecUnsuppliedItems");}

  default String ecUnsuppliedItemsAppend() {return g("ecUnsuppliedItemsAppend");}

  default String ecUnsuppliedItemsNotFound() {return g("ecUnsuppliedItemsNotFound");}

  default String ecUnsuppliedItemsRemove() {return g("ecUnsuppliedItemsRemove");}

  default String ecUnsuppliedItemsTotal() {return g("ecUnsuppliedItemsTotal");}

  default String ecUpdateCartItem(Object p0, Object p1, Object p2) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    _m.put("{2}", p2);
    return Localized.format(g("ecUpdateCartItem"), _m);
  }

  default String ecUpdateCosts() {return g("ecUpdateCosts");}

  default String ecUpdateTime() {return g("ecUpdateTime");}

  default String ecUpdatedCost() {return g("ecUpdatedCost");}

  default String ecUserActions() {return g("ecUserActions");}

  default String ecWarehousesPrimary() {return g("ecWarehousesPrimary");}

  default String ecWarehousesSecondary() {return g("ecWarehousesSecondary");}

  default String editForm() {return g("editForm");}

  default String editMode() {return g("editMode");}

  default String editing() {return g("editing");}

  default String editorTemplates() {return g("editorTemplates");}

  default String email() {return g("email");}

  default String emailAddresses() {return g("emailAddresses");}

  default String employee() {return g("employee");}

  default String employeeSubstituteFor() {return g("employeeSubstituteFor");}

  default String employeeSubstitution() {return g("employeeSubstitution");}

  default String employees() {return g("employees");}

  default String employeesAndObjects() {return g("employeesAndObjects");}

  default String employment() {return g("employment");}

  default String empty() {return g("empty");}

  default String enabled() {return g("enabled");}

  default String enabledShort() {return g("enabledShort");}

  default String endRow() {return g("endRow");}

  default String endSession(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("endSession"), _m);
  }

  default String endingDate() {return g("endingDate");}

  default String endingMonth() {return g("endingMonth");}

  default String endingTime() {return g("endingTime");}

  default String endingYear() {return g("endingYear");}

  default String endingYearMonth() {return g("endingYearMonth");}

  default String enterColor() {return g("enterColor");}

  default String enterDate() {return g("enterDate");}

  default String enterTime() {return g("enterTime");}

  default String equipment() {return g("equipment");}

  default String error() {return g("error");}

  default String errorMessage() {return g("errorMessage");}

  default String errors() {return g("errors");}

  default String eulaAgreement() {return g("eulaAgreement");}

  default String event() {return g("event");}

  default String eventHistory() {return g("eventHistory");}

  default String exchangeCode() {return g("exchangeCode");}

  default String exchangeFromTo(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("exchangeFromTo"), _m);
  }

  default String executorFullName() {return g("executorFullName");}

  default String expectedDuration() {return g("expectedDuration");}

  default String expeditionShort() {return g("expeditionShort");}

  default String expenditureReport() {return g("expenditureReport");}

  default String expenditures() {return g("expenditures");}

  default String expenses() {return g("expenses");}

  default String exportToMsExcel() {return g("exportToMsExcel");}

  default String exporting() {return g("exporting");}

  default String expression() {return g("expression");}

  default String extendWorkSchedule(Object ym) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{ym}", ym);
    return Localized.format(g("extendWorkSchedule"), _m);
  }

  default String externalCode() {return g("externalCode");}

  default String externalId() {return g("externalId");}

  default String fax() {return g("fax");}

  default String feed() {return g("feed");}

  default String feedAppointmentsAll() {return g("feedAppointmentsAll");}

  default String feedAppointmentsMy() {return g("feedAppointmentsMy");}

  default String feedCompaniesAll() {return g("feedCompaniesAll");}

  default String feedCompaniesMy() {return g("feedCompaniesMy");}

  default String feedDocuments() {return g("feedDocuments");}

  default String feedEcClientsAll() {return g("feedEcClientsAll");}

  default String feedEcClientsMy() {return g("feedEcClientsMy");}

  default String feedEcOrdersAll() {return g("feedEcOrdersAll");}

  default String feedEcOrdersMy() {return g("feedEcOrdersMy");}

  default String feedEcRegistrations() {return g("feedEcRegistrations");}

  default String feedGoods() {return g("feedGoods");}

  default String feedNew() {return g("feedNew");}

  default String feedPersons() {return g("feedPersons");}

  default String feedRequestsAll() {return g("feedRequestsAll");}

  default String feedRequestsAssigned() {return g("feedRequestsAssigned");}

  default String feedSubscriptionDate() {return g("feedSubscriptionDate");}

  default String feedTasksAll() {return g("feedTasksAll");}

  default String feedTasksAssigned() {return g("feedTasksAssigned");}

  default String feedTasksDelegated() {return g("feedTasksDelegated");}

  default String feedTasksObserved() {return g("feedTasksObserved");}

  default String feedTrAssessmentAllOrders() {return g("feedTrAssessmentAllOrders");}

  default String feedTrAssessmentAllRequests() {return g("feedTrAssessmentAllRequests");}

  default String feedTrAssessmentMyOrders() {return g("feedTrAssessmentMyOrders");}

  default String feedTrAssessmentMyRequests() {return g("feedTrAssessmentMyRequests");}

  default String feedTrAssessmentTransportations() {return g("feedTrAssessmentTransportations");}

  default String feedTrCargo() {return g("feedTrCargo");}

  default String feedTrCargoCreditInvoices() {return g("feedTrCargoCreditInvoices");}

  default String feedTrCargoProformaInvoices() {return g("feedTrCargoProformaInvoices");}

  default String feedTrCargoPurchaseInvoices() {return g("feedTrCargoPurchaseInvoices");}

  default String feedTrDrivers() {return g("feedTrDrivers");}

  default String feedTrOrderCargoCreditSales() {return g("feedTrOrderCargoCreditSales");}

  default String feedTrOrderCargoInvoices() {return g("feedTrOrderCargoInvoices");}

  default String feedTrOrderCargoSales() {return g("feedTrOrderCargoSales");}

  default String feedTrOrdersAll() {return g("feedTrOrdersAll");}

  default String feedTrOrdersMy() {return g("feedTrOrdersMy");}

  default String feedTrRegistrations() {return g("feedTrRegistrations");}

  default String feedTrRequestsAll() {return g("feedTrRequestsAll");}

  default String feedTrRequestsMy() {return g("feedTrRequestsMy");}

  default String feedTrRequestsUnregisteredAll() {return g("feedTrRequestsUnregisteredAll");}

  default String feedTrRequestsUnregisteredMy() {return g("feedTrRequestsUnregisteredMy");}

  default String feedTrTripCosts() {return g("feedTrTripCosts");}

  default String feedTrTripsAll() {return g("feedTrTripsAll");}

  default String feedTrTripsMy() {return g("feedTrTripsMy");}

  default String feedTrVehicles() {return g("feedTrVehicles");}

  default String feeds() {return g("feeds");}

  default String fetchWorkSchedule() {return g("fetchWorkSchedule");}

  default String fieldRequired(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("fieldRequired"), _m);
  }

  default String fields() {return g("fields");}

  default String file() {return g("file");}

  default String fileDataCorrection() {return g("fileDataCorrection");}

  default String fileDescription() {return g("fileDescription");}

  default String fileIcon() {return g("fileIcon");}

  default String fileName() {return g("fileName");}

  default String fileNotFound(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("fileNotFound"), _m);
  }

  default String fileOriginalName() {return g("fileOriginalName");}

  default String fileSize() {return g("fileSize");}

  default String fileSizeExceeded(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("fileSizeExceeded"), _m);
  }

  default String fileType() {return g("fileType");}

  default String fileVersion() {return g("fileVersion");}

  default String files() {return g("files");}

  default String filter() {return g("filter");}

  default String filterAll() {return g("filterAll");}

  default String filterAnswered() {return g("filterAnswered");}

  default String filterLost() {return g("filterLost");}

  default String filterNew() {return g("filterNew");}

  default String filterNotNullLabel() {return g("filterNotNullLabel");}

  default String filterNullLabel() {return g("filterNullLabel");}

  default String filterRemove() {return g("filterRemove");}

  default String filters() {return g("filters");}

  default String finAccounts() {return g("finAccounts");}

  default String finAdvancePaymentsGiven() {return g("finAdvancePaymentsGiven");}

  default String finAdvancePaymentsReceived() {return g("finAdvancePaymentsReceived");}

  default String finAnalysis() {return g("finAnalysis");}

  default String finAnalysisCalculate() {return g("finAnalysisCalculate");}

  default String finAnalysisColumn() {return g("finAnalysisColumn");}

  default String finAnalysisColumnAbbreviation() {return g("finAnalysisColumnAbbreviation");}

  default String finAnalysisColumnAndFormPeriodsDoNotIntersect() {return g("finAnalysisColumnAndFormPeriodsDoNotIntersect");}

  default String finAnalysisColumnFilters() {return g("finAnalysisColumnFilters");}

  default String finAnalysisColumnName() {return g("finAnalysisColumnName");}

  default String finAnalysisColumnNew() {return g("finAnalysisColumnNew");}

  default String finAnalysisColumnSelected() {return g("finAnalysisColumnSelected");}

  default String finAnalysisColumnSplitLevels() {return g("finAnalysisColumnSplitLevels");}

  default String finAnalysisColumns() {return g("finAnalysisColumns");}

  default String finAnalysisFilterExtra() {return g("finAnalysisFilterExtra");}

  default String finAnalysisFilterInclude() {return g("finAnalysisFilterInclude");}

  default String finAnalysisFilterNew() {return g("finAnalysisFilterNew");}

  default String finAnalysisFilters() {return g("finAnalysisFilters");}

  default String finAnalysisForm() {return g("finAnalysisForm");}

  default String finAnalysisFormNew() {return g("finAnalysisFormNew");}

  default String finAnalysisForms() {return g("finAnalysisForms");}

  default String finAnalysisInvalidAbbreviation() {return g("finAnalysisInvalidAbbreviation");}

  default String finAnalysisInvalidExtraFilter() {return g("finAnalysisInvalidExtraFilter");}

  default String finAnalysisInvalidSplit() {return g("finAnalysisInvalidSplit");}

  default String finAnalysisName() {return g("finAnalysisName");}

  default String finAnalysisPrimaryColumnsNotAvailable() {return g("finAnalysisPrimaryColumnsNotAvailable");}

  default String finAnalysisPrimaryRowsNotAvailable() {return g("finAnalysisPrimaryRowsNotAvailable");}

  default String finAnalysisResults() {return g("finAnalysisResults");}

  default String finAnalysisRow() {return g("finAnalysisRow");}

  default String finAnalysisRowAbbreviation() {return g("finAnalysisRowAbbreviation");}

  default String finAnalysisRowAndFormPeriodsDoNotIntersect() {return g("finAnalysisRowAndFormPeriodsDoNotIntersect");}

  default String finAnalysisRowFilters() {return g("finAnalysisRowFilters");}

  default String finAnalysisRowName() {return g("finAnalysisRowName");}

  default String finAnalysisRowNew() {return g("finAnalysisRowNew");}

  default String finAnalysisRowSelected() {return g("finAnalysisRowSelected");}

  default String finAnalysisRowSplitLevels() {return g("finAnalysisRowSplitLevels");}

  default String finAnalysisRows() {return g("finAnalysisRows");}

  default String finAnalysisScale() {return g("finAnalysisScale");}

  default String finAnalysisScript() {return g("finAnalysisScript");}

  default String finAnalysisSelectColumns() {return g("finAnalysisSelectColumns");}

  default String finAnalysisSelectRows() {return g("finAnalysisSelectRows");}

  default String finAnalysisShowColumnDimension(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finAnalysisShowColumnDimension"), _m);
  }

  default String finAnalysisShowColumnEmployee() {return g("finAnalysisShowColumnEmployee");}

  default String finAnalysisShowColumnFilters() {return g("finAnalysisShowColumnFilters");}

  default String finAnalysisShowRowDimension(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finAnalysisShowRowDimension"), _m);
  }

  default String finAnalysisShowRowEmployee() {return g("finAnalysisShowRowEmployee");}

  default String finAnalysisShowRowFilters() {return g("finAnalysisShowRowFilters");}

  default String finAnalysisShowTotal() {return g("finAnalysisShowTotal");}

  default String finAnalysisSpecifyBudgetType() {return g("finAnalysisSpecifyBudgetType");}

  default String finAnalysisSpecifyIndicators() {return g("finAnalysisSpecifyIndicators");}

  default String finAnalysisSplit(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finAnalysisSplit"), _m);
  }

  default String finAnalysisSplitLevels() {return g("finAnalysisSplitLevels");}

  default String finAnalysisValueActual() {return g("finAnalysisValueActual");}

  default String finAnalysisValueActualShort() {return g("finAnalysisValueActualShort");}

  default String finAnalysisValueBudget() {return g("finAnalysisValueBudget");}

  default String finAnalysisValueBudgetShort() {return g("finAnalysisValueBudgetShort");}

  default String finAnalysisValueDifference() {return g("finAnalysisValueDifference");}

  default String finAnalysisValueDifferenceShort() {return g("finAnalysisValueDifferenceShort");}

  default String finAnalysisValuePercentage() {return g("finAnalysisValuePercentage");}

  default String finAnalysisValuePercentageShort() {return g("finAnalysisValuePercentageShort");}

  default String finAnalysisValues() {return g("finAnalysisValues");}

  default String finAnalysisVerified() {return g("finAnalysisVerified");}

  default String finAnalysisVerify() {return g("finAnalysisVerify");}

  default String finBudget() {return g("finBudget");}

  default String finBudgetCopy(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finBudgetCopy"), _m);
  }

  default String finBudgetEntries() {return g("finBudgetEntries");}

  default String finBudgetEntry() {return g("finBudgetEntry");}

  default String finBudgetEntryNew() {return g("finBudgetEntryNew");}

  default String finBudgetHeader() {return g("finBudgetHeader");}

  default String finBudgetHeaders() {return g("finBudgetHeaders");}

  default String finBudgetName() {return g("finBudgetName");}

  default String finBudgetNew() {return g("finBudgetNew");}

  default String finBudgetShowDimension(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finBudgetShowDimension"), _m);
  }

  default String finBudgetShowEmployee() {return g("finBudgetShowEmployee");}

  default String finBudgetType() {return g("finBudgetType");}

  default String finBudgetTypeNew() {return g("finBudgetTypeNew");}

  default String finBudgetTypes() {return g("finBudgetTypes");}

  default String finBudgets() {return g("finBudgets");}

  default String finCashInBank() {return g("finCashInBank");}

  default String finClosingBalance() {return g("finClosingBalance");}

  default String finClosingBalanceShort() {return g("finClosingBalanceShort");}

  default String finClosingEntries() {return g("finClosingEntries");}

  default String finClosingEntriesShort() {return g("finClosingEntriesShort");}

  default String finConfiguration() {return g("finConfiguration");}

  default String finContent() {return g("finContent");}

  default String finContents() {return g("finContents");}

  default String finCostAccount() {return g("finCostAccount");}

  default String finCostOfGoodsSold() {return g("finCostOfGoodsSold");}

  default String finCostOfMerchandise() {return g("finCostOfMerchandise");}

  default String finCreditOnly() {return g("finCreditOnly");}

  default String finCreditShort() {return g("finCreditShort");}

  default String finDebitOnly() {return g("finDebitOnly");}

  default String finDebitShort() {return g("finDebitShort");}

  default String finDefaultAccounts() {return g("finDefaultAccounts");}

  default String finDefaultJournal() {return g("finDefaultJournal");}

  default String finDistribution() {return g("finDistribution");}

  default String finDistributionOfItems() {return g("finDistributionOfItems");}

  default String finDistributionOfTradeDocuments() {return g("finDistributionOfTradeDocuments");}

  default String finDistributionOfTradeOperations() {return g("finDistributionOfTradeOperations");}

  default String finDistributionShort() {return g("finDistributionShort");}

  default String finForeignExchangeGain() {return g("finForeignExchangeGain");}

  default String finForeignExchangeLoss() {return g("finForeignExchangeLoss");}

  default String finIndicator() {return g("finIndicator");}

  default String finIndicatorAbbreviation() {return g("finIndicatorAbbreviation");}

  default String finIndicatorAbbreviationClosingBalance(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorAbbreviationClosingBalance"), _m);
  }

  default String finIndicatorAbbreviationCredit(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorAbbreviationCredit"), _m);
  }

  default String finIndicatorAbbreviationDebit(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorAbbreviationDebit"), _m);
  }

  default String finIndicatorAbbreviationOpeningBalance(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorAbbreviationOpeningBalance"), _m);
  }

  default String finIndicatorAbbreviationTurnover(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorAbbreviationTurnover"), _m);
  }

  default String finIndicatorAccounts() {return g("finIndicatorAccounts");}

  default String finIndicatorCreate() {return g("finIndicatorCreate");}

  default String finIndicatorFilterExtra() {return g("finIndicatorFilterExtra");}

  default String finIndicatorFilterInclude() {return g("finIndicatorFilterInclude");}

  default String finIndicatorFilterNew() {return g("finIndicatorFilterNew");}

  default String finIndicatorFilters() {return g("finIndicatorFilters");}

  default String finIndicatorNameClosingBalance(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorNameClosingBalance"), _m);
  }

  default String finIndicatorNameCredit(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorNameCredit"), _m);
  }

  default String finIndicatorNameDebit(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorNameDebit"), _m);
  }

  default String finIndicatorNameOpeningBalance(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorNameOpeningBalance"), _m);
  }

  default String finIndicatorNameTurnover(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("finIndicatorNameTurnover"), _m);
  }

  default String finIndicatorPrimary() {return g("finIndicatorPrimary");}

  default String finIndicatorPrimaryNew() {return g("finIndicatorPrimaryNew");}

  default String finIndicatorScale() {return g("finIndicatorScale");}

  default String finIndicatorSecondary() {return g("finIndicatorSecondary");}

  default String finIndicatorSecondaryNew() {return g("finIndicatorSecondaryNew");}

  default String finIndicatorSource() {return g("finIndicatorSource");}

  default String finIndicators() {return g("finIndicators");}

  default String finIndicatorsPrimary() {return g("finIndicatorsPrimary");}

  default String finIndicatorsSecondary() {return g("finIndicatorsSecondary");}

  default String finLiabilitiesToEmployees() {return g("finLiabilitiesToEmployees");}

  default String finNormalBalance() {return g("finNormalBalance");}

  default String finNormalBalanceShort() {return g("finNormalBalanceShort");}

  default String finOpeningBalance() {return g("finOpeningBalance");}

  default String finOpeningBalanceShort() {return g("finOpeningBalanceShort");}

  default String finPettyCash() {return g("finPettyCash");}

  default String finPostAction() {return g("finPostAction");}

  default String finPostingPrecedence() {return g("finPostingPrecedence");}

  default String finReceivablesFromEmployees() {return g("finReceivablesFromEmployees");}

  default String finRevenueAndExpenseSummary() {return g("finRevenueAndExpenseSummary");}

  default String finSalesDiscounts() {return g("finSalesDiscounts");}

  default String finSalesRevenue() {return g("finSalesRevenue");}

  default String finTradeAccounts() {return g("finTradeAccounts");}

  default String finTradeAccountsPrecedence() {return g("finTradeAccountsPrecedence");}

  default String finTradeDimensionsPrecedence() {return g("finTradeDimensionsPrecedence");}

  default String finTradePayables() {return g("finTradePayables");}

  default String finTradeReceivables() {return g("finTradeReceivables");}

  default String finTransitoryAccount() {return g("finTransitoryAccount");}

  default String finTurnover() {return g("finTurnover");}

  default String finTurnoverOrBalance() {return g("finTurnoverOrBalance");}

  default String finVatPayable() {return g("finVatPayable");}

  default String finVatReceivable() {return g("finVatReceivable");}

  default String finWriteOffAccount() {return g("finWriteOffAccount");}

  default String finance() {return g("finance");}

  default String financialInfo() {return g("financialInfo");}

  default String financialRecord() {return g("financialRecord");}

  default String financialRecords() {return g("financialRecords");}

  default String financialState() {return g("financialState");}

  default String financialStateName() {return g("financialStateName");}

  default String firstName() {return g("firstName");}

  default String footer() {return g("footer");}

  default String foreground() {return g("foreground");}

  default String foregroundColor() {return g("foregroundColor");}

  default String formula() {return g("formula");}

  default String freight() {return g("freight");}

  default String freightExchange() {return g("freightExchange");}

  default String from() {return g("from");}

  default String full() {return g("full");}

  default String generateReport() {return g("generateReport");}

  default String goods() {return g("goods");}

  default String greatest() {return g("greatest");}

  default String gridColumnSettings() {return g("gridColumnSettings");}

  default String gridSettings() {return g("gridSettings");}

  default String group() {return g("group");}

  default String groupBy() {return g("groupBy");}

  default String groupName() {return g("groupName");}

  default String groupResults() {return g("groupResults");}

  default String groups() {return g("groups");}

  default String height() {return g("height");}

  default String heightOfApplianceHeader() {return g("heightOfApplianceHeader");}

  default String heightOfViewHeader() {return g("heightOfViewHeader");}

  default String help() {return g("help");}

  default String hideOrShowMenu() {return g("hideOrShowMenu");}

  default String hideZeroTimes() {return g("hideZeroTimes");}

  default String highPriority() {return g("highPriority");}

  default String history() {return g("history");}

  default String historyField() {return g("historyField");}

  default String historyObject() {return g("historyObject");}

  default String historyRecord() {return g("historyRecord");}

  default String historyRelation() {return g("historyRelation");}

  default String historyTime() {return g("historyTime");}

  default String historyTransaction() {return g("historyTransaction");}

  default String historyUser() {return g("historyUser");}

  default String historyValue() {return g("historyValue");}

  default String holidays() {return g("holidays");}

  default String hour() {return g("hour");}

  default String hourlyWage() {return g("hourlyWage");}

  default String hours() {return g("hours");}

  default String hoursByCompanies() {return g("hoursByCompanies");}

  default String hoursByExecutors() {return g("hoursByExecutors");}

  default String hoursByTypes() {return g("hoursByTypes");}

  default String hoursByUsers() {return g("hoursByUsers");}

  default String ibanCode() {return g("ibanCode");}

  default String imageUploadFailed() {return g("imageUploadFailed");}

  default String important() {return g("important");}

  default String imported() {return g("imported");}

  default String importing() {return g("importing");}

  default String income() {return g("income");}

  default String incomes() {return g("incomes");}

  default String informationSource() {return g("informationSource");}

  default String informationSourceName() {return g("informationSourceName");}

  default String initialFilter() {return g("initialFilter");}

  default String inputForm() {return g("inputForm");}

  default String inputFull() {return g("inputFull");}

  default String inputSimple() {return g("inputSimple");}

  default String intermediateLoading() {return g("intermediateLoading");}

  default String intermediateUnloading() {return g("intermediateUnloading");}

  default String internalRelation() {return g("internalRelation");}

  default String intrastat() {return g("intrastat");}

  default String invalidDate() {return g("invalidDate");}

  default String invalidDateFormat() {return g("invalidDateFormat");}

  default String invalidFilter() {return g("invalidFilter");}

  default String invalidIdValue(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("invalidIdValue"), _m);
  }

  default String invalidImageFileType(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("invalidImageFileType"), _m);
  }

  default String invalidNumber() {return g("invalidNumber");}

  default String invalidNumberFormat() {return g("invalidNumberFormat");}

  default String invalidPeriod(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("invalidPeriod"), _m);
  }

  default String invalidRange() {return g("invalidRange");}

  default String invalidTime() {return g("invalidTime");}

  default String ipBlockCommand() {return g("ipBlockCommand");}

  default String ipBlocked() {return g("ipBlocked");}

  default String ipFilterNew() {return g("ipFilterNew");}

  default String ipFilters() {return g("ipFilters");}

  default String is() {return g("is");}

  default String isNot() {return g("isNot");}

  default String item() {return g("item");}

  default String itemBarcode() {return g("itemBarcode");}

  default String itemCategories() {return g("itemCategories");}

  default String itemCategory() {return g("itemCategory");}

  default String itemCategoryTree() {return g("itemCategoryTree");}

  default String itemFilter() {return g("itemFilter");}

  default String itemGraphics() {return g("itemGraphics");}

  default String itemGroup() {return g("itemGroup");}

  default String itemGroups() {return g("itemGroups");}

  default String itemName() {return g("itemName");}

  default String itemName2() {return g("itemName2");}

  default String itemName3() {return g("itemName3");}

  default String itemOrService() {return g("itemOrService");}

  default String itemSelection() {return g("itemSelection");}

  default String itemType() {return g("itemType");}

  default String itemTypes() {return g("itemTypes");}

  default String join() {return g("join");}

  default String journal() {return g("journal");}

  default String journals() {return g("journals");}

  default String key() {return g("key");}

  default String keyNotFound(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("keyNotFound"), _m);
  }

  default String keywords() {return g("keywords");}

  default String kilogramShort() {return g("kilogramShort");}

  default String kilometers() {return g("kilometers");}

  default String kilometersShort() {return g("kilometersShort");}

  default String kind() {return g("kind");}

  default String kpnCode() {return g("kpnCode");}

  default String language() {return g("language");}

  default String languageTag() {return g("languageTag");}

  default String lastAccess() {return g("lastAccess");}

  default String lastLogin() {return g("lastLogin");}

  default String lastLoginIP() {return g("lastLoginIP");}

  default String lastLogout() {return g("lastLogout");}

  default String lastName() {return g("lastName");}

  default String layout() {return g("layout");}

  default String least() {return g("least");}

  default String length() {return g("length");}

  default String level() {return g("level");}

  default String limitCurrency() {return g("limitCurrency");}

  default String link() {return g("link");}

  default String list() {return g("list");}

  default String liters() {return g("liters");}

  default String loading() {return g("loading");}

  default String loadingStateDelayMillis() {return g("loadingStateDelayMillis");}

  default String location() {return g("location");}

  default String loggedIn() {return g("loggedIn");}

  default String loggedOut() {return g("loggedOut");}

  default String loginCommandQuery() {return g("loginCommandQuery");}

  default String loginCommandRegister() {return g("loginCommandRegister");}

  default String loginFailed() {return g("loginFailed");}

  default String loginHistory() {return g("loginHistory");}

  default String loginInfoHelp() {return g("loginInfoHelp");}

  default String loginInfoLabel() {return g("loginInfoLabel");}

  default String loginPassword() {return g("loginPassword");}

  default String loginSessionTime() {return g("loginSessionTime");}

  default String loginSubmit() {return g("loginSubmit");}

  default String loginUserName() {return g("loginUserName");}

  default String lowPriority() {return g("lowPriority");}

  default String mail() {return g("mail");}

  default String mailAccount() {return g("mailAccount");}

  default String mailAccountNotFound() {return g("mailAccountNotFound");}

  default String mailAccountRules() {return g("mailAccountRules");}

  default String mailAccountSettings() {return g("mailAccountSettings");}

  default String mailAccounts() {return g("mailAccounts");}

  default String mailActionMoveToTrash() {return g("mailActionMoveToTrash");}

  default String mailAddContacts() {return g("mailAddContacts");}

  default String mailAddress() {return g("mailAddress");}

  default String mailAddressbook() {return g("mailAddressbook");}

  default String mailAttachments() {return g("mailAttachments");}

  default String mailBcc() {return g("mailBcc");}

  default String mailCancelFolderSynchronizationQuestion(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailCancelFolderSynchronizationQuestion"), _m);
  }

  default String mailCc() {return g("mailCc");}

  default String mailCopiedMessagesToFolder(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailCopiedMessagesToFolder"), _m);
  }

  default String mailCreateNewFolder() {return g("mailCreateNewFolder");}

  default String mailDefault() {return g("mailDefault");}

  default String mailDefaultShort() {return g("mailDefaultShort");}

  default String mailDeleteFolderQuestion(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailDeleteFolderQuestion"), _m);
  }

  default String mailDeletedMessages(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailDeletedMessages"), _m);
  }

  default String mailEmptyTrashFolder() {return g("mailEmptyTrashFolder");}

  default String mailFolderContentsWillBeRemovedFromTheMailServer() {return g("mailFolderContentsWillBeRemovedFromTheMailServer");}

  default String mailFolderDrafts() {return g("mailFolderDrafts");}

  default String mailFolderInbox() {return g("mailFolderInbox");}

  default String mailFolderSent() {return g("mailFolderSent");}

  default String mailFolderTrash() {return g("mailFolderTrash");}

  default String mailFolders() {return g("mailFolders");}

  default String mailForward() {return g("mailForward");}

  default String mailForwardedMessage() {return g("mailForwardedMessage");}

  default String mailForwardedPrefix() {return g("mailForwardedPrefix");}

  default String mailFrom() {return g("mailFrom");}

  default String mailGetAllAttachments() {return g("mailGetAllAttachments");}

  default String mailHasAttachments() {return g("mailHasAttachments");}

  default String mailIMAP() {return g("mailIMAP");}

  default String mailInFolder() {return g("mailInFolder");}

  default String mailMailingCopies() {return g("mailMailingCopies");}

  default String mailMarkAsUnread() {return g("mailMarkAsUnread");}

  default String mailMessage() {return g("mailMessage");}

  default String mailMessageBodyIsEmpty() {return g("mailMessageBodyIsEmpty");}

  default String mailMessageIsSavedInDraft() {return g("mailMessageIsSavedInDraft");}

  default String mailMessageSent() {return g("mailMessageSent");}

  default String mailMessageSentCount() {return g("mailMessageSentCount");}

  default String mailMessageWasNotSent() {return g("mailMessageWasNotSent");}

  default String mailMessages(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailMessages"), _m);
  }

  default String mailMovedMessagesToFolder(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailMovedMessagesToFolder"), _m);
  }

  default String mailMovedMessagesToTrash(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailMovedMessagesToTrash"), _m);
  }

  default String mailName() {return g("mailName");}

  default String mailNewAccount() {return g("mailNewAccount");}

  default String mailNewAccountPassword() {return g("mailNewAccountPassword");}

  default String mailNewMessage() {return g("mailNewMessage");}

  default String mailNewNewsletter() {return g("mailNewNewsletter");}

  default String mailNewRecipientsGroup() {return g("mailNewRecipientsGroup");}

  default String mailNewRule() {return g("mailNewRule");}

  default String mailNewSignature() {return g("mailNewSignature");}

  default String mailNewsletter() {return g("mailNewsletter");}

  default String mailNewsletters() {return g("mailNewsletters");}

  default String mailNoAccountsFound() {return g("mailNoAccountsFound");}

  default String mailNotify() {return g("mailNotify");}

  default String mailOnlyInFolder(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailOnlyInFolder"), _m);
  }

  default String mailPOP3() {return g("mailPOP3");}

  default String mailParameters() {return g("mailParameters");}

  default String mailPrivate() {return g("mailPrivate");}

  default String mailPublic() {return g("mailPublic");}

  default String mailQuestionSaveToDraft() {return g("mailQuestionSaveToDraft");}

  default String mailRecipientAddressNotFound() {return g("mailRecipientAddressNotFound");}

  default String mailRecipientType() {return g("mailRecipientType");}

  default String mailRecipients() {return g("mailRecipients");}

  default String mailRecipientsGroup() {return g("mailRecipientsGroup");}

  default String mailRecipientsGroups() {return g("mailRecipientsGroups");}

  default String mailRenameFolder(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("mailRenameFolder"), _m);
  }

  default String mailReplayPrefix() {return g("mailReplayPrefix");}

  default String mailReply() {return g("mailReply");}

  default String mailReplyAll() {return g("mailReplyAll");}

  default String mailRule() {return g("mailRule");}

  default String mailRuleAction() {return g("mailRuleAction");}

  default String mailRuleActionCopy() {return g("mailRuleActionCopy");}

  default String mailRuleActionDelete() {return g("mailRuleActionDelete");}

  default String mailRuleActionFlag() {return g("mailRuleActionFlag");}

  default String mailRuleActionForward() {return g("mailRuleActionForward");}

  default String mailRuleActionMove() {return g("mailRuleActionMove");}

  default String mailRuleActionOptions() {return g("mailRuleActionOptions");}

  default String mailRuleActionRead() {return g("mailRuleActionRead");}

  default String mailRuleActionReply() {return g("mailRuleActionReply");}

  default String mailRuleActive() {return g("mailRuleActive");}

  default String mailRuleCondition() {return g("mailRuleCondition");}

  default String mailRuleConditionAll() {return g("mailRuleConditionAll");}

  default String mailRuleConditionOptions() {return g("mailRuleConditionOptions");}

  default String mailRuleConditionRecipients() {return g("mailRuleConditionRecipients");}

  default String mailRuleConditionSender() {return g("mailRuleConditionSender");}

  default String mailRuleConditionSubject() {return g("mailRuleConditionSubject");}

  default String mailSMTPServerAddress() {return g("mailSMTPServerAddress");}

  default String mailSMTPServerLogin() {return g("mailSMTPServerLogin");}

  default String mailSMTPServerPassword() {return g("mailSMTPServerPassword");}

  default String mailSMTPServerPort() {return g("mailSMTPServerPort");}

  default String mailSMTPServerProperties() {return g("mailSMTPServerProperties");}

  default String mailSMTPServerSSL() {return g("mailSMTPServerSSL");}

  default String mailSMTPServerSettings() {return g("mailSMTPServerSettings");}

  default String mailSendVisibleCopies() {return g("mailSendVisibleCopies");}

  default String mailSender() {return g("mailSender");}

  default String mailServerAddress() {return g("mailServerAddress");}

  default String mailServerLogin() {return g("mailServerLogin");}

  default String mailServerPassword() {return g("mailServerPassword");}

  default String mailServerPort() {return g("mailServerPort");}

  default String mailServerProperties() {return g("mailServerProperties");}

  default String mailServerSSL() {return g("mailServerSSL");}

  default String mailServerSettings() {return g("mailServerSettings");}

  default String mailServerType() {return g("mailServerType");}

  default String mailShowOriginal() {return g("mailShowOriginal");}

  default String mailSignature() {return g("mailSignature");}

  default String mailSignatures() {return g("mailSignatures");}

  default String mailSpecifyRecipient() {return g("mailSpecifyRecipient");}

  default String mailSpecifySubject() {return g("mailSpecifySubject");}

  default String mailStarred() {return g("mailStarred");}

  default String mailSubject() {return g("mailSubject");}

  default String mailSynchronizeAll() {return g("mailSynchronizeAll");}

  default String mailSynchronizeFolders() {return g("mailSynchronizeFolders");}

  default String mailSynchronizeInbox() {return g("mailSynchronizeInbox");}

  default String mailSynchronizeNothing() {return g("mailSynchronizeNothing");}

  default String mailSystemFolders() {return g("mailSystemFolders");}

  default String mailTextWrote() {return g("mailTextWrote");}

  default String mailThereIsNoMessageSelected() {return g("mailThereIsNoMessageSelected");}

  default String mailThereIsStackOfUnfinishedAttachments() {return g("mailThereIsStackOfUnfinishedAttachments");}

  default String mailTo() {return g("mailTo");}

  default String mailUnread() {return g("mailUnread");}

  default String mails() {return g("mails");}

  default String mainCriteria() {return g("mainCriteria");}

  default String mainEmail() {return g("mainEmail");}

  default String mainInformation() {return g("mainInformation");}

  default String manager() {return g("manager");}

  default String managers() {return g("managers");}

  default String manufacturer() {return g("manufacturer");}

  default String manufacturers() {return g("manufacturers");}

  default String margin() {return g("margin");}

  default String marginPercent() {return g("marginPercent");}

  default String maxValue() {return g("maxValue");}

  default String mediumPriority() {return g("mediumPriority");}

  default String menu() {return g("menu");}

  default String mergeInto() {return g("mergeInto");}

  default String message() {return g("message");}

  default String messageSent() {return g("messageSent");}

  default String minValue() {return g("minValue");}

  default String minorCaption() {return g("minorCaption");}

  default String minorName() {return g("minorName");}

  default String minute() {return g("minute");}

  default String mobile() {return g("mobile");}

  default String modifications() {return g("modifications");}

  default String module() {return g("module");}

  default String modules() {return g("modules");}

  default String month() {return g("month");}

  default String more() {return g("more");}

  default String moreThenOneValue() {return g("moreThenOneValue");}

  default String myCompanyMenu() {return g("myCompanyMenu");}

  default String myEnvironment() {return g("myEnvironment");}

  default String name() {return g("name");}

  default String netto() {return g("netto");}

  default String newAccount() {return g("newAccount");}

  default String newActivityType() {return g("newActivityType");}

  default String newAssessmentRequest() {return g("newAssessmentRequest");}

  default String newBank() {return g("newBank");}

  default String newBranch() {return g("newBranch");}

  default String newBundle() {return g("newBundle");}

  default String newCar() {return g("newCar");}

  default String newCargo() {return g("newCargo");}

  default String newCity() {return g("newCity");}

  default String newClient() {return g("newClient");}

  default String newClientsFinancialState() {return g("newClientsFinancialState");}

  default String newClientsGroups() {return g("newClientsGroups");}

  default String newClientsInformationSource() {return g("newClientsInformationSource");}

  default String newClientsPriority() {return g("newClientsPriority");}

  default String newClientsRelationType() {return g("newClientsRelationType");}

  default String newClientsRelationTypeState() {return g("newClientsRelationTypeState");}

  default String newClientsStatus() {return g("newClientsStatus");}

  default String newColor() {return g("newColor");}

  default String newCompanyActivity() {return g("newCompanyActivity");}

  default String newCompanyPerson() {return g("newCompanyPerson");}

  default String newCompanyPersonMessage() {return g("newCompanyPersonMessage");}

  default String newCompanySize() {return g("newCompanySize");}

  default String newContactFamilyMember() {return g("newContactFamilyMember");}

  default String newContactFamilyRelation() {return g("newContactFamilyRelation");}

  default String newCostCenter() {return g("newCostCenter");}

  default String newCountry() {return g("newCountry");}

  default String newCriteriaGroup() {return g("newCriteriaGroup");}

  default String newCriterion() {return g("newCriterion");}

  default String newCurrency() {return g("newCurrency");}

  default String newCurrencyRate() {return g("newCurrencyRate");}

  default String newDepartment() {return g("newDepartment");}

  default String newDepartmentEmployees() {return g("newDepartmentEmployees");}

  default String newDepartmentPositions() {return g("newDepartmentPositions");}

  default String newDocumentItem() {return g("newDocumentItem");}

  default String newDocumentTemplate() {return g("newDocumentTemplate");}

  default String newEditorTemplate() {return g("newEditorTemplate");}

  default String newEmailAccount() {return g("newEmailAccount");}

  default String newEmailAddress() {return g("newEmailAddress");}

  default String newEmployee() {return g("newEmployee");}

  default String newFinContent() {return g("newFinContent");}

  default String newFinDistribution() {return g("newFinDistribution");}

  default String newFinancialRecord() {return g("newFinancialRecord");}

  default String newItem() {return g("newItem");}

  default String newItemGroup() {return g("newItemGroup");}

  default String newItemType() {return g("newItemType");}

  default String newJournal() {return g("newJournal");}

  default String newKey() {return g("newKey");}

  default String newLoadingPlace() {return g("newLoadingPlace");}

  default String newObject() {return g("newObject");}

  default String newOffer() {return g("newOffer");}

  default String newOrder() {return g("newOrder");}

  default String newParameter() {return g("newParameter");}

  default String newPassword() {return g("newPassword");}

  default String newPasswordIsRequired() {return g("newPasswordIsRequired");}

  default String newPasswordsDoesNotMatch() {return g("newPasswordsDoesNotMatch");}

  default String newPayment() {return g("newPayment");}

  default String newPaymentTerm() {return g("newPaymentTerm");}

  default String newPerson() {return g("newPerson");}

  default String newPersonCompany() {return g("newPersonCompany");}

  default String newPersonPosition() {return g("newPersonPosition");}

  default String newProjectCreated(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("newProjectCreated"), _m);
  }

  default String newProperty() {return g("newProperty");}

  default String newReason() {return g("newReason");}

  default String newRelation() {return g("newRelation");}

  default String newReminderType() {return g("newReminderType");}

  default String newResponsibility() {return g("newResponsibility");}

  default String newRight() {return g("newRight");}

  default String newRole() {return g("newRole");}

  default String newRow() {return g("newRow");}

  default String newSalaryFund() {return g("newSalaryFund");}

  default String newService() {return g("newService");}

  default String newServiceJob() {return g("newServiceJob");}

  default String newServiceOrder() {return g("newServiceOrder");}

  default String newSupplier() {return g("newSupplier");}

  default String newTab() {return g("newTab");}

  default String newTag() {return g("newTag");}

  default String newTemplate() {return g("newTemplate");}

  default String newTheme() {return g("newTheme");}

  default String newThemeColors() {return g("newThemeColors");}

  default String newTimeCardChange() {return g("newTimeCardChange");}

  default String newTimeCardCode() {return g("newTimeCardCode");}

  default String newTimeRange() {return g("newTimeRange");}

  default String newTimeZone() {return g("newTimeZone");}

  default String newTransportationOrder() {return g("newTransportationOrder");}

  default String newUiTheme() {return g("newUiTheme");}

  default String newUnit() {return g("newUnit");}

  default String newUnloadingPlace() {return g("newUnloadingPlace");}

  default String newUser() {return g("newUser");}

  default String newUserGroup() {return g("newUserGroup");}

  default String newValues() {return g("newValues");}

  default String newWarehouse() {return g("newWarehouse");}

  default String newsRefreshIntervalSeconds() {return g("newsRefreshIntervalSeconds");}

  default String no() {return g("no");}

  default String noChanges() {return g("noChanges");}

  default String noData() {return g("noData");}

  default String noMatter() {return g("noMatter");}

  default String not(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("not"), _m);
  }

  default String notANumber() {return g("notANumber");}

  default String notManufactured() {return g("notManufactured");}

  default String note() {return g("note");}

  default String notes() {return g("notes");}

  default String nothingFound() {return g("nothingFound");}

  default String number() {return g("number");}

  default String numberOfEmployees() {return g("numberOfEmployees");}

  default String numeroSign() {return g("numeroSign");}

  default String object() {return g("object");}

  default String objectData() {return g("objectData");}

  default String objectField() {return g("objectField");}

  default String objectMenu() {return g("objectMenu");}

  default String objectModule() {return g("objectModule");}

  default String objectRights() {return g("objectRights");}

  default String objectSalaryFund() {return g("objectSalaryFund");}

  default String objectStatusActive() {return g("objectStatusActive");}

  default String objectStatusInactive() {return g("objectStatusInactive");}

  default String objectType() {return g("objectType");}

  default String objectWidget() {return g("objectWidget");}

  default String objects() {return g("objects");}

  default String offer() {return g("offer");}

  default String offers() {return g("offers");}

  default String ok() {return g("ok");}

  default String oldPassword() {return g("oldPassword");}

  default String oldPasswordIsInvalid() {return g("oldPasswordIsInvalid");}

  default String oldPasswordIsRequired() {return g("oldPasswordIsRequired");}

  default String openInNewTab() {return g("openInNewTab");}

  default String operator() {return g("operator");}

  default String option() {return g("option");}

  default String optionGroup() {return g("optionGroup");}

  default String optionGroups() {return g("optionGroups");}

  default String optionType() {return g("optionType");}

  default String optionTypes() {return g("optionTypes");}

  default String options() {return g("options");}

  default String ordApprove() {return g("ordApprove");}

  default String ordApproved() {return g("ordApproved");}

  default String ordAskApprove() {return g("ordAskApprove");}

  default String ordAskCancel() {return g("ordAskCancel");}

  default String ordAskChangeWarehouse() {return g("ordAskChangeWarehouse");}

  default String ordAskFinish() {return g("ordAskFinish");}

  default String ordAskNotChangeWarehouse() {return g("ordAskNotChangeWarehouse");}

  default String ordAskSearchValue() {return g("ordAskSearchValue");}

  default String ordBoLDepartureDate() {return g("ordBoLDepartureDate");}

  default String ordBoLDriverTabNo() {return g("ordBoLDriverTabNo");}

  default String ordBoLIssueDate() {return g("ordBoLIssueDate");}

  default String ordBoLNumber() {return g("ordBoLNumber");}

  default String ordBoLSeries() {return g("ordBoLSeries");}

  default String ordCancel() {return g("ordCancel");}

  default String ordCanceled() {return g("ordCanceled");}

  default String ordCompleted() {return g("ordCompleted");}

  default String ordCreditLimitEmpty() {return g("ordCreditLimitEmpty");}

  default String ordDebtExceedsCreditLimit() {return g("ordDebtExceedsCreditLimit");}

  default String ordEmptyFreeRemainder() {return g("ordEmptyFreeRemainder");}

  default String ordEmptyInvoice() {return g("ordEmptyInvoice");}

  default String ordFreeRemainder() {return g("ordFreeRemainder");}

  default String ordInvoiceQty() {return g("ordInvoiceQty");}

  default String ordMaxDiscount() {return g("ordMaxDiscount");}

  default String ordOverdueInvoices() {return g("ordOverdueInvoices");}

  default String ordPrepare() {return g("ordPrepare");}

  default String ordPrepared() {return g("ordPrepared");}

  default String ordQtyIsTooBig() {return g("ordQtyIsTooBig");}

  default String ordResNotIncrease() {return g("ordResNotIncrease");}

  default String ordResQty() {return g("ordResQty");}

  default String ordResQtyIsTooBig() {return g("ordResQtyIsTooBig");}

  default String ordResRemainder() {return g("ordResRemainder");}

  default String ordSend() {return g("ordSend");}

  default String ordSent() {return g("ordSent");}

  default String ordSupplierTerm() {return g("ordSupplierTerm");}

  default String ordTransportationActInfo() {return g("ordTransportationActInfo");}

  default String ordUncompleted() {return g("ordUncompleted");}

  default String order() {return g("order");}

  default String orderCargoAddingToTrips(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("orderCargoAddingToTrips"), _m);
  }

  default String orderDate() {return g("orderDate");}

  default String orderNumber() {return g("orderNumber");}

  default String orders() {return g("orders");}

  default String ordinal() {return g("ordinal");}

  default String otherCosts() {return g("otherCosts");}

  default String otherInfo() {return g("otherInfo");}

  default String outsized() {return g("outsized");}

  default String outsizedShort() {return g("outsizedShort");}

  default String overpayment() {return g("overpayment");}

  default String owner() {return g("owner");}

  default String packageUnits() {return g("packageUnits");}

  default String packet() {return g("packet");}

  default String packets() {return g("packets");}

  default String pallets() {return g("pallets");}

  default String parameter() {return g("parameter");}

  default String parameterEdit() {return g("parameterEdit");}

  default String parameterName() {return g("parameterName");}

  default String parameterType() {return g("parameterType");}

  default String parameters() {return g("parameters");}

  default String partTime() {return g("partTime");}

  default String partial() {return g("partial");}

  default String partialCargo() {return g("partialCargo");}

  default String partialShort() {return g("partialShort");}

  default String passportNo() {return g("passportNo");}

  default String password() {return g("password");}

  default String pay() {return g("pay");}

  default String payer() {return g("payer");}

  default String paymentDueDate() {return g("paymentDueDate");}

  default String paymentType() {return g("paymentType");}

  default String paymentTypes() {return g("paymentTypes");}

  default String payroll() {return g("payroll");}

  default String payrollAbsence() {return g("payrollAbsence");}

  default String payrollEarnings() {return g("payrollEarnings");}

  default String payrollEarningsForHolidays() {return g("payrollEarningsForHolidays");}

  default String payrollEarningsTotal() {return g("payrollEarningsTotal");}

  default String payrollEarningsWithoutHolidays() {return g("payrollEarningsWithoutHolidays");}

  default String payrollFullTime() {return g("payrollFullTime");}

  default String payrollFundReport() {return g("payrollFundReport");}

  default String payrollPartTime() {return g("payrollPartTime");}

  default String percent() {return g("percent");}

  default String period() {return g("period");}

  default String person() {return g("person");}

  default String personAccountingCode() {return g("personAccountingCode");}

  default String personCode() {return g("personCode");}

  default String personCompanies() {return g("personCompanies");}

  default String personCompany() {return g("personCompany");}

  default String personContact() {return g("personContact");}

  default String personContacts() {return g("personContacts");}

  default String personContactsTML() {return g("personContactsTML");}

  default String personDateOfDismissal() {return g("personDateOfDismissal");}

  default String personDateOfEmployment() {return g("personDateOfEmployment");}

  default String personFullName() {return g("personFullName");}

  default String personPosition() {return g("personPosition");}

  default String personPositionInDepartment() {return g("personPositionInDepartment");}

  default String personPositionMain() {return g("personPositionMain");}

  default String personPositions() {return g("personPositions");}

  default String personTabNo() {return g("personTabNo");}

  default String personTabNoShort() {return g("personTabNoShort");}

  default String personUnemployment() {return g("personUnemployment");}

  default String persons() {return g("persons");}

  default String phone() {return g("phone");}

  default String phones() {return g("phones");}

  default String photo() {return g("photo");}

  default String picture() {return g("picture");}

  default String pictures() {return g("pictures");}

  default String places() {return g("places");}

  default String plan() {return g("plan");}

  default String pluralName() {return g("pluralName");}

  default String plus() {return g("plus");}

  default String postIndex() {return g("postIndex");}

  default String precision() {return g("precision");}

  default String presenceAway() {return g("presenceAway");}

  default String presenceChangeTooltip() {return g("presenceChangeTooltip");}

  default String presenceIdle() {return g("presenceIdle");}

  default String presenceOffline() {return g("presenceOffline");}

  default String presenceOnline() {return g("presenceOnline");}

  default String preview() {return g("preview");}

  default String previewMode() {return g("previewMode");}

  default String price() {return g("price");}

  default String price1() {return g("price1");}

  default String price10() {return g("price10");}

  default String price10Label() {return g("price10Label");}

  default String price1Label() {return g("price1Label");}

  default String price2() {return g("price2");}

  default String price2Label() {return g("price2Label");}

  default String price3() {return g("price3");}

  default String price3Label() {return g("price3Label");}

  default String price4() {return g("price4");}

  default String price4Label() {return g("price4Label");}

  default String price5() {return g("price5");}

  default String price5Label() {return g("price5Label");}

  default String price6() {return g("price6");}

  default String price6Label() {return g("price6Label");}

  default String price7() {return g("price7");}

  default String price7Label() {return g("price7Label");}

  default String price8() {return g("price8");}

  default String price8Label() {return g("price8Label");}

  default String price9() {return g("price9");}

  default String price9Label() {return g("price9Label");}

  default String priceName() {return g("priceName");}

  default String priceWithVat() {return g("priceWithVat");}

  default String priceWithoutVat() {return g("priceWithoutVat");}

  default String prices() {return g("prices");}

  default String primaryWarehouse() {return g("primaryWarehouse");}

  default String primeCost() {return g("primeCost");}

  default String print() {return g("print");}

  default String printApproved() {return g("printApproved");}

  default String printBankAccount() {return g("printBankAccount");}

  default String printBankCode() {return g("printBankCode");}

  default String printBankSwift() {return g("printBankSwift");}

  default String printBoL() {return g("printBoL");}

  default String printBoLAuthorised() {return g("printBoLAuthorised");}

  default String printBoLCargoInfo() {return g("printBoLCargoInfo");}

  default String printBoLIssued() {return g("printBoLIssued");}

  default String printBoLItemName() {return g("printBoLItemName");}

  default String printBoLLoading() {return g("printBoLLoading");}

  default String printBoLReceived() {return g("printBoLReceived");}

  default String printBoLRecipient() {return g("printBoLRecipient");}

  default String printBoLSender() {return g("printBoLSender");}

  default String printBoLTransport() {return g("printBoLTransport");}

  default String printBoLUnloading() {return g("printBoLUnloading");}

  default String printCommissionChairman() {return g("printCommissionChairman");}

  default String printCommissionMembers() {return g("printCommissionMembers");}

  default String printContinuation() {return g("printContinuation");}

  default String printCostInWords() {return g("printCostInWords");}

  default String printDocumentAmount() {return g("printDocumentAmount");}

  default String printDocumentDiscount() {return g("printDocumentDiscount");}

  default String printDocumentDispenser() {return g("printDocumentDispenser");}

  default String printDocumentDispenserShort() {return g("printDocumentDispenserShort");}

  default String printDocumentLicence() {return g("printDocumentLicence");}

  default String printDocumentNumber() {return g("printDocumentNumber");}

  default String printDocumentReceived() {return g("printDocumentReceived");}

  default String printDocumentReceiver() {return g("printDocumentReceiver");}

  default String printDocumentReceiverShort() {return g("printDocumentReceiverShort");}

  default String printDocumentSeries() {return g("printDocumentSeries");}

  default String printDocumentSeriesAndNumber(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("printDocumentSeriesAndNumber"), _m);
  }

  default String printDocumentSubtotal() {return g("printDocumentSubtotal");}

  default String printDocumentTaxableAmount() {return g("printDocumentTaxableAmount");}

  default String printDocumentTaxableTotal() {return g("printDocumentTaxableTotal");}

  default String printDocumentTotal() {return g("printDocumentTotal");}

  default String printDocumentTotalAmount() {return g("printDocumentTotalAmount");}

  default String printDocumentVat() {return g("printDocumentVat");}

  default String printDocumentVatTotal() {return g("printDocumentVatTotal");}

  default String printDueDate() {return g("printDueDate");}

  default String printDueDays() {return g("printDueDays");}

  default String printDueWithin() {return g("printDueWithin");}

  default String printInternalTransportationAct() {return g("printInternalTransportationAct");}

  default String printInternalTransportationItemName() {return g("printInternalTransportationItemName");}

  default String printInvoice() {return g("printInvoice");}

  default String printInvoiceAuthorised() {return g("printInvoiceAuthorised");}

  default String printInvoiceBuyer() {return g("printInvoiceBuyer");}

  default String printInvoiceCredit() {return g("printInvoiceCredit");}

  default String printInvoiceDebit() {return g("printInvoiceDebit");}

  default String printInvoiceItemName() {return g("printInvoiceItemName");}

  default String printInvoiceProForma() {return g("printInvoiceProForma");}

  default String printInvoiceSeller() {return g("printInvoiceSeller");}

  default String printInvoiceVat() {return g("printInvoiceVat");}

  default String printItemBestBefore() {return g("printItemBestBefore");}

  default String printItemCertificate() {return g("printItemCertificate");}

  default String printItemDiscountAmount() {return g("printItemDiscountAmount");}

  default String printItemDiscountPercent() {return g("printItemDiscountPercent");}

  default String printItemOrdinal() {return g("printItemOrdinal");}

  default String printItemPacking() {return g("printItemPacking");}

  default String printItemProductName() {return g("printItemProductName");}

  default String printItemProductionDate() {return g("printItemProductionDate");}

  default String printItemQuantity() {return g("printItemQuantity");}

  default String printItemSalePrice() {return g("printItemSalePrice");}

  default String printItemStandard() {return g("printItemStandard");}

  default String printItemStoring() {return g("printItemStoring");}

  default String printItemTotalWithVat() {return g("printItemTotalWithVat");}

  default String printItemTotalWithoutVat() {return g("printItemTotalWithoutVat");}

  default String printItemUom() {return g("printItemUom");}

  default String printItemValidity() {return g("printItemValidity");}

  default String printItemVatAmount() {return g("printItemVatAmount");}

  default String printItemVatRate() {return g("printItemVatRate");}

  default String printItemWeight() {return g("printItemWeight");}

  default String printNextPage() {return g("printNextPage");}

  default String printOriginalDocuments() {return g("printOriginalDocuments");}

  default String printPage() {return g("printPage");}

  default String printParameters() {return g("printParameters");}

  default String printProducer() {return g("printProducer");}

  default String printQualityCertificate() {return g("printQualityCertificate");}

  default String printReasonOfReturn() {return g("printReasonOfReturn");}

  default String printReceivingCompany() {return g("printReceivingCompany");}

  default String printRecipient() {return g("printRecipient");}

  default String printResponsible() {return g("printResponsible");}

  default String printResponsibleForQuality() {return g("printResponsibleForQuality");}

  default String printReturnAuthorised() {return g("printReturnAuthorised");}

  default String printReturningCompany() {return g("printReturningCompany");}

  default String printRoundOff() {return g("printRoundOff");}

  default String printSender() {return g("printSender");}

  default String printSignature() {return g("printSignature");}

  default String printStatementOfReceipt() {return g("printStatementOfReceipt");}

  default String printSupplier() {return g("printSupplier");}

  default String printTotalInWords() {return g("printTotalInWords");}

  default String printTransferAct() {return g("printTransferAct");}

  default String printWriteOffAct() {return g("printWriteOffAct");}

  default String printWriteOffAuthorised() {return g("printWriteOffAuthorised");}

  default String printWriteOffItemName() {return g("printWriteOffItemName");}

  default String printWriteOffReason() {return g("printWriteOffReason");}

  default String priority() {return g("priority");}

  default String priorityName() {return g("priorityName");}

  default String prjActualExpenses() {return g("prjActualExpenses");}

  default String prjActualTaskDuration() {return g("prjActualTaskDuration");}

  default String prjColumnMode() {return g("prjColumnMode");}

  default String prjComments() {return g("prjComments");}

  default String prjCreateFromTasks() {return g("prjCreateFromTasks");}

  default String prjDates() {return g("prjDates");}

  default String prjDefaultStage() {return g("prjDefaultStage");}

  default String prjDefaultStageShort() {return g("prjDefaultStageShort");}

  default String prjDeleteCanManager() {return g("prjDeleteCanManager");}

  default String prjDescriptionContent() {return g("prjDescriptionContent");}

  default String prjEvent() {return g("prjEvent");}

  default String prjEventEdited() {return g("prjEventEdited");}

  default String prjExpectedTaskDuration() {return g("prjExpectedTaskDuration");}

  default String prjExpectedTaskExpenses() {return g("prjExpectedTaskExpenses");}

  default String prjExpenses() {return g("prjExpenses");}

  default String prjFiles() {return g("prjFiles");}

  default String prjFilterActive() {return g("prjFilterActive");}

  default String prjFilterAll() {return g("prjFilterAll");}

  default String prjFilterNotLate() {return g("prjFilterNotLate");}

  default String prjIncomePlan() {return g("prjIncomePlan");}

  default String prjInitialStage() {return g("prjInitialStage");}

  default String prjLabelNotLate() {return g("prjLabelNotLate");}

  default String prjManager() {return g("prjManager");}

  default String prjManagerPosition() {return g("prjManagerPosition");}

  default String prjMenu() {return g("prjMenu");}

  default String prjMustBeOneStage() {return g("prjMustBeOneStage");}

  default String prjNewProject() {return g("prjNewProject");}

  default String prjNewStage() {return g("prjNewStage");}

  default String prjNewTemplate() {return g("prjNewTemplate");}

  default String prjObserver() {return g("prjObserver");}

  default String prjObservers() {return g("prjObservers");}

  default String prjOtherExpenses() {return g("prjOtherExpenses");}

  default String prjOverdue() {return g("prjOverdue");}

  default String prjOverduePercent() {return g("prjOverduePercent");}

  default String prjParticipant() {return g("prjParticipant");}

  default String prjPersonRate() {return g("prjPersonRate");}

  default String prjPrice() {return g("prjPrice");}

  default String prjPublishTime() {return g("prjPublishTime");}

  default String prjReasonActive() {return g("prjReasonActive");}

  default String prjSchedule() {return g("prjSchedule");}

  default String prjScope() {return g("prjScope");}

  default String prjStage() {return g("prjStage");}

  default String prjStageHasTasks() {return g("prjStageHasTasks");}

  default String prjStageTaskCount() {return g("prjStageTaskCount");}

  default String prjStages() {return g("prjStages");}

  default String prjStagesTML() {return g("prjStagesTML");}

  default String prjStatusActive() {return g("prjStatusActive");}

  default String prjStatusApproved() {return g("prjStatusApproved");}

  default String prjStatusScheduled() {return g("prjStatusScheduled");}

  default String prjStatusSuspended() {return g("prjStatusSuspended");}

  default String prjTeam() {return g("prjTeam");}

  default String prjTeamTML() {return g("prjTeamTML");}

  default String prjTemplate() {return g("prjTemplate");}

  default String prjTemplateDateMark() {return g("prjTemplateDateMark");}

  default String prjTemplates() {return g("prjTemplates");}

  default String prjTerm() {return g("prjTerm");}

  default String prjTypes() {return g("prjTypes");}

  default String prjUserCanDeleteManager() {return g("prjUserCanDeleteManager");}

  default String prjUserHasSameTasks() {return g("prjUserHasSameTasks");}

  default String prmActNumberLength() {return g("prmActNumberLength");}

  default String prmAllowDeleteOwnComments() {return g("prmAllowDeleteOwnComments");}

  default String prmAutoReservation() {return g("prmAutoReservation");}

  default String prmCargoService() {return g("prmCargoService");}

  default String prmCargoType() {return g("prmCargoType");}

  default String prmCheckDebt() {return g("prmCheckDebt");}

  default String prmClearReservationsTime() {return g("prmClearReservationsTime");}

  default String prmClientChangingSetting() {return g("prmClientChangingSetting");}

  default String prmCompanyName() {return g("prmCompanyName");}

  default String prmCountry() {return g("prmCountry");}

  default String prmCreatePrivateTaskFirst() {return g("prmCreatePrivateTaskFirst");}

  default String prmCurrencyRefreshHours() {return g("prmCurrencyRefreshHours");}

  default String prmDefaultInvoicePrefix() {return g("prmDefaultInvoicePrefix");}

  default String prmDefaultMaintenanceType() {return g("prmDefaultMaintenanceType");}

  default String prmDefaultSaleOperation() {return g("prmDefaultSaleOperation");}

  default String prmDefaultTripPrefix() {return g("prmDefaultTripPrefix");}

  default String prmDefaultWarrantyType() {return g("prmDefaultWarrantyType");}

  default String prmDimensions() {return g("prmDimensions");}

  default String prmDiscussBirthdays() {return g("prmDiscussBirthdays");}

  default String prmDiscussInactiveTimeInDays() {return g("prmDiscussInactiveTimeInDays");}

  default String prmDiscussionsAdmin() {return g("prmDiscussionsAdmin");}

  default String prmERPAddress() {return g("prmERPAddress");}

  default String prmERPLogin() {return g("prmERPLogin");}

  default String prmERPPassword() {return g("prmERPPassword");}

  default String prmERPRefreshIntervalInMinutes() {return g("prmERPRefreshIntervalInMinutes");}

  default String prmEndOfWorkDay() {return g("prmEndOfWorkDay");}

  default String prmExcludeVAT() {return g("prmExcludeVAT");}

  default String prmExportERPReservationsTime() {return g("prmExportERPReservationsTime");}

  default String prmExternalMaintenanceUrl() {return g("prmExternalMaintenanceUrl");}

  default String prmFilterAllDevices() {return g("prmFilterAllDevices");}

  default String prmForbiddenFilesExtentions() {return g("prmForbiddenFilesExtentions");}

  default String prmImportActItemRegEx() {return g("prmImportActItemRegEx");}

  default String prmImportERPItemsTime() {return g("prmImportERPItemsTime");}

  default String prmImportERPStocksTime() {return g("prmImportERPStocksTime");}

  default String prmMainCurrency() {return g("prmMainCurrency");}

  default String prmMaintenanceServiceGroup() {return g("prmMaintenanceServiceGroup");}

  default String prmManagerDiscount() {return g("prmManagerDiscount");}

  default String prmManagerWarehouse() {return g("prmManagerWarehouse");}

  default String prmMessageTemplate() {return g("prmMessageTemplate");}

  default String prmNotifyAboutDebts() {return g("prmNotifyAboutDebts");}

  default String prmOverdueInvoices() {return g("prmOverdueInvoices");}

  default String prmProjectCommonRate() {return g("prmProjectCommonRate");}

  default String prmProjectHourUnit() {return g("prmProjectHourUnit");}

  default String prmReturnedActStatus() {return g("prmReturnedActStatus");}

  default String prmRole() {return g("prmRole");}

  default String prmSQLMessagesGENERIC() {return g("prmSQLMessagesGENERIC");}

  default String prmSQLMessagesMSSQL() {return g("prmSQLMessagesMSSQL");}

  default String prmSQLMessagesORACLE() {return g("prmSQLMessagesORACLE");}

  default String prmSQLMessagesPOSTGRESQL() {return g("prmSQLMessagesPOSTGRESQL");}

  default String prmSelfServiceRole() {return g("prmSelfServiceRole");}

  default String prmSendNewslettersCount() {return g("prmSendNewslettersCount");}

  default String prmSendNewslettersInterval() {return g("prmSendNewslettersInterval");}

  default String prmServerProperties() {return g("prmServerProperties");}

  default String prmServiceManagerWarehouse() {return g("prmServiceManagerWarehouse");}

  default String prmServiceTradeOperation() {return g("prmServiceTradeOperation");}

  default String prmServiceWarehouse() {return g("prmServiceWarehouse");}

  default String prmSmsDisplayText() {return g("prmSmsDisplayText");}

  default String prmSmsPassword() {return g("prmSmsPassword");}

  default String prmSmsRequestContactInfoFrom() {return g("prmSmsRequestContactInfoFrom");}

  default String prmSmsRequestHeaders() {return g("prmSmsRequestHeaders");}

  default String prmSmsRequestServiceAddress() {return g("prmSmsRequestServiceAddress");}

  default String prmSmsRequestServiceFrom() {return g("prmSmsRequestServiceFrom");}

  default String prmSmsRequestServicePassword() {return g("prmSmsRequestServicePassword");}

  default String prmSmsRequestServiceUserName() {return g("prmSmsRequestServiceUserName");}

  default String prmSmsServiceAddress() {return g("prmSmsServiceAddress");}

  default String prmSmsServiceId() {return g("prmSmsServiceId");}

  default String prmSmsUserName() {return g("prmSmsUserName");}

  default String prmStartOfWorkDay() {return g("prmStartOfWorkDay");}

  default String prmUrgentRate() {return g("prmUrgentRate");}

  default String prmUrl() {return g("prmUrl");}

  default String prmVATPercent() {return g("prmVATPercent");}

  default String productService() {return g("productService");}

  default String productsServices() {return g("productsServices");}

  default String profit() {return g("profit");}

  default String progress() {return g("progress");}

  default String prohibitComment() {return g("prohibitComment");}

  default String project() {return g("project");}

  default String projectCanCreateTaskOwner(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("projectCanCreateTaskOwner"), _m);
  }

  default String projects() {return g("projects");}

  default String projectsAll() {return g("projectsAll");}

  default String projectsMy() {return g("projectsMy");}

  default String properties() {return g("properties");}

  default String property() {return g("property");}

  default String qrCode() {return g("qrCode");}

  default String quantity() {return g("quantity");}

  default String quantityFrom() {return g("quantityFrom");}

  default String quantityTo() {return g("quantityTo");}

  default String quarter() {return g("quarter");}

  default String questionLogout() {return g("questionLogout");}

  default String rating() {return g("rating");}

  default String readOnly() {return g("readOnly");}

  default String reason() {return g("reason");}

  default String reasons() {return g("reasons");}

  default String rebuildTradeStockCaption() {return g("rebuildTradeStockCaption");}

  default String rebuildTradeStockNotification() {return g("rebuildTradeStockNotification");}

  default String rebuildTradeStockQuestion() {return g("rebuildTradeStockQuestion");}

  default String recalculateTradeItemCostsCaption() {return g("recalculateTradeItemCostsCaption");}

  default String recalculateTradeItemCostsNotification(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("recalculateTradeItemCostsNotification"), _m);
  }

  default String recalculateTradeItemCostsQuestion() {return g("recalculateTradeItemCostsQuestion");}

  default String recalculateTradeItemPriceCaption() {return g("recalculateTradeItemPriceCaption");}

  default String recalculateTradeItemPriceForAllItems() {return g("recalculateTradeItemPriceForAllItems");}

  default String recalculateTradeItemPriceNotification(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("recalculateTradeItemPriceNotification"), _m);
  }

  default String recall() {return g("recall");}

  default String recalls() {return g("recalls");}

  default String received() {return g("received");}

  default String receivedDateFrom() {return g("receivedDateFrom");}

  default String receivedDateTo() {return g("receivedDateTo");}

  default String receivedMonth() {return g("receivedMonth");}

  default String receivedYear() {return g("receivedYear");}

  default String recipient() {return g("recipient");}

  default String recordDependency() {return g("recordDependency");}

  default String recordDependencyNew(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("recordDependencyNew"), _m);
  }

  default String recordDependent() {return g("recordDependent");}

  default String recordIsInUse(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("recordIsInUse"), _m);
  }

  default String recordsDependency() {return g("recordsDependency");}

  default String ref() {return g("ref");}

  default String refNumber() {return g("refNumber");}

  default String references() {return g("references");}

  default String register() {return g("register");}

  default String registered() {return g("registered");}

  default String registration() {return g("registration");}

  default String relatedInformation() {return g("relatedInformation");}

  default String relatedMessages() {return g("relatedMessages");}

  default String relatedTo() {return g("relatedTo");}

  default String relation() {return g("relation");}

  default String relationStateName() {return g("relationStateName");}

  default String relations() {return g("relations");}

  default String reminderCaption() {return g("reminderCaption");}

  default String reminderDateField() {return g("reminderDateField");}

  default String reminderDateIndicator() {return g("reminderDateIndicator");}

  default String reminderMethod() {return g("reminderMethod");}

  default String reminderModule() {return g("reminderModule");}

  default String reminderName() {return g("reminderName");}

  default String reminderTemplate() {return g("reminderTemplate");}

  default String reminderType() {return g("reminderType");}

  default String reminderTypes() {return g("reminderTypes");}

  default String remoteHost() {return g("remoteHost");}

  default String removeCargoFromTripCaption() {return g("removeCargoFromTripCaption");}

  default String removeCargoFromTripQuestion() {return g("removeCargoFromTripQuestion");}

  default String removeFilter() {return g("removeFilter");}

  default String removeQuestion(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("removeQuestion"), _m);
  }

  default String remuneration() {return g("remuneration");}

  default String repeatNewPassword() {return g("repeatNewPassword");}

  default String report() {return g("report");}

  default String reports() {return g("reports");}

  default String requestFinish() {return g("requestFinish");}

  default String requestFinishToTask() {return g("requestFinishToTask");}

  default String requestFinishToTaskQuestion() {return g("requestFinishToTaskQuestion");}

  default String requestFinishing() {return g("requestFinishing");}

  default String requestShowFinished() {return g("requestShowFinished");}

  default String requestShowRegistered() {return g("requestShowRegistered");}

  default String requestUpdatingQuestion() {return g("requestUpdatingQuestion");}

  default String required() {return g("required");}

  default String reservation() {return g("reservation");}

  default String reserve() {return g("reserve");}

  default String resource() {return g("resource");}

  default String responsibilities() {return g("responsibilities");}

  default String responsibility() {return g("responsibility");}

  default String responsibleEmployee() {return g("responsibleEmployee");}

  default String responsiblePerson() {return g("responsiblePerson");}

  default String responsiblePersons() {return g("responsiblePersons");}

  default String restrictions() {return g("restrictions");}

  default String result() {return g("result");}

  default String results() {return g("results");}

  default String rightStateCreate() {return g("rightStateCreate");}

  default String rightStateDelete() {return g("rightStateDelete");}

  default String rightStateEdit() {return g("rightStateEdit");}

  default String rightStateMerge() {return g("rightStateMerge");}

  default String rightStateRequired() {return g("rightStateRequired");}

  default String rightStateView() {return g("rightStateView");}

  default String rights() {return g("rights");}

  default String rightsAll() {return g("rightsAll");}

  default String rightsDefault() {return g("rightsDefault");}

  default String rightsInheritedFrom() {return g("rightsInheritedFrom");}

  default String role() {return g("role");}

  default String roleAddUsers() {return g("roleAddUsers");}

  default String roleRights() {return g("roleRights");}

  default String roleRightsSaved(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("roleRightsSaved"), _m);
  }

  default String roleState() {return g("roleState");}

  default String roleUsers() {return g("roleUsers");}

  default String roles() {return g("roles");}

  default String route() {return g("route");}

  default String row() {return g("row");}

  default String rowIsNotRemovable() {return g("rowIsNotRemovable");}

  default String rowIsReadOnly() {return g("rowIsReadOnly");}

  default String rowResults() {return g("rowResults");}

  default String rows() {return g("rows");}

  default String rowsRetrieved(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("rowsRetrieved"), _m);
  }

  default String rowsUpdated(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("rowsUpdated"), _m);
  }

  default String salary() {return g("salary");}

  default String salaryFund() {return g("salaryFund");}

  default String salePrice() {return g("salePrice");}

  default String salePriceLabel() {return g("salePriceLabel");}

  default String salePriceShort() {return g("salePriceShort");}

  default String saveAndPrintAction() {return g("saveAndPrintAction");}

  default String saveAndPrintQuestion() {return g("saveAndPrintQuestion");}

  default String saveAsEditorTemplate() {return g("saveAsEditorTemplate");}

  default String saveChanges() {return g("saveChanges");}

  default String saveFilter() {return g("saveFilter");}

  default String saveSelectedItems() {return g("saveSelectedItems");}

  default String scheduleDateExclude() {return g("scheduleDateExclude");}

  default String scheduleDateInclude() {return g("scheduleDateInclude");}

  default String scheduleDateNonWork() {return g("scheduleDateNonWork");}

  default String scheduleDateWork() {return g("scheduleDateWork");}

  default String scheduledEndingDate() {return g("scheduledEndingDate");}

  default String scheduledEndingTime() {return g("scheduledEndingTime");}

  default String scheduledStartingDate() {return g("scheduledStartingDate");}

  default String scheduledStartingTime() {return g("scheduledStartingTime");}

  default String scvAddressRequired() {return g("scvAddressRequired");}

  default String search() {return g("search");}

  default String searchQueryRestriction(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("searchQueryRestriction"), _m);
  }

  default String searchTips() {return g("searchTips");}

  default String selectAll() {return g("selectAll");}

  default String selectAtLeastOneRow() {return g("selectAtLeastOneRow");}

  default String selectDocumentTemplate() {return g("selectDocumentTemplate");}

  default String selectImport() {return g("selectImport");}

  default String selectRole() {return g("selectRole");}

  default String selectionColumnLabel() {return g("selectionColumnLabel");}

  default String send() {return g("send");}

  default String sendReminder() {return g("sendReminder");}

  default String sendReminderMail() {return g("sendReminderMail");}

  default String separator() {return g("separator");}

  default String seriesPlural() {return g("seriesPlural");}

  default String serverParameters() {return g("serverParameters");}

  default String service() {return g("service");}

  default String serviceJob() {return g("serviceJob");}

  default String serviceJobGroupNew() {return g("serviceJobGroupNew");}

  default String serviceJobGroups() {return g("serviceJobGroups");}

  default String serviceJobs() {return g("serviceJobs");}

  default String serviceOrder() {return g("serviceOrder");}

  default String serviceOrders() {return g("serviceOrders");}

  default String services() {return g("services");}

  default String setAsPrimary() {return g("setAsPrimary");}

  default String settings() {return g("settings");}

  default String sheetName() {return g("sheetName");}

  default String shipper() {return g("shipper");}

  default String shippingSchedule() {return g("shippingSchedule");}

  default String showAvailableEmployees() {return g("showAvailableEmployees");}

  default String showNewMessagesNotifier() {return g("showNewMessagesNotifier");}

  default String signDate() {return g("signDate");}

  default String signOut() {return g("signOut");}

  default String singularName() {return g("singularName");}

  default String size() {return g("size");}

  default String socialContacts() {return g("socialContacts");}

  default String softwareBuild() {return g("softwareBuild");}

  default String softwareLicenceNo() {return g("softwareLicenceNo");}

  default String softwareReleaseDate() {return g("softwareReleaseDate");}

  default String softwareVersion() {return g("softwareVersion");}

  default String sorry() {return g("sorry");}

  default String sort() {return g("sort");}

  default String specification() {return g("specification");}

  default String specifyCondition() {return g("specifyCondition");}

  default String specifyResult() {return g("specifyResult");}

  default String stageAction() {return g("stageAction");}

  default String stageCondition() {return g("stageCondition");}

  default String stageConfirmation() {return g("stageConfirmation");}

  default String stageTrigger() {return g("stageTrigger");}

  default String startRow() {return g("startRow");}

  default String startingDate() {return g("startingDate");}

  default String startingMonth() {return g("startingMonth");}

  default String startingTime() {return g("startingTime");}

  default String startingYear() {return g("startingYear");}

  default String startingYearMonth() {return g("startingYearMonth");}

  default String stateVisible() {return g("stateVisible");}

  default String status() {return g("status");}

  default String statusUpdated() {return g("statusUpdated");}

  default String statuses() {return g("statuses");}

  default String style() {return g("style");}

  default String stocks() {return g("stocks");}

  default String newStocks() {return g("newStocks");}

  default String writeOffStocks() {return g("writeOffStocks");}

  default String additionStocks() {return g("additionStocks");}

  default String stocksCategories() {return g("stocksCategories");}

  default String newBuying() {return g("newBuying");}

  default String summary() {return g("summary");}

  default String summer() {return g("summer");}

  default String supplier() {return g("supplier");}

  default String supplierCreditDays() {return g("supplierCreditDays");}

  default String suppliers() {return g("suppliers");}

  default String svcBasicAmount() {return g("svcBasicAmount");}

  default String svcCalendar() {return g("svcCalendar");}

  default String svcCalendarPixelsPerCompany() {return g("svcCalendarPixelsPerCompany");}

  default String svcCalendarPixelsPerInfo() {return g("svcCalendarPixelsPerInfo");}

  default String svcCalendarSeparateObjects() {return g("svcCalendarSeparateObjects");}

  default String svcChangedClient() {return g("svcChangedClient");}

  default String svcChangingClient() {return g("svcChangingClient");}

  default String svcComment() {return g("svcComment");}

  default String svcComments() {return g("svcComments");}

  default String svcConfirmationDate() {return g("svcConfirmationDate");}

  default String svcConfirmedPayroll() {return g("svcConfirmedPayroll");}

  default String svcConfirmedUser() {return g("svcConfirmedUser");}

  default String svcContractor() {return g("svcContractor");}

  default String svcDates() {return g("svcDates");}

  default String svcDaysActive() {return g("svcDaysActive");}

  default String svcDefect() {return g("svcDefect");}

  default String svcDefectId() {return g("svcDefectId");}

  default String svcDefectItems() {return g("svcDefectItems");}

  default String svcDefectPrintItemLabel() {return g("svcDefectPrintItemLabel");}

  default String svcDefectPrintNumberLabel() {return g("svcDefectPrintNumberLabel");}

  default String svcDefectSupplier() {return g("svcDefectSupplier");}

  default String svcDefects() {return g("svcDefects");}

  default String svcDetailedMaintenanceTicket() {return g("svcDetailedMaintenanceTicket");}

  default String svcDevice() {return g("svcDevice");}

  default String svcDeviceAddress() {return g("svcDeviceAddress");}

  default String svcDeviceCategory() {return g("svcDeviceCategory");}

  default String svcDevices() {return g("svcDevices");}

  default String svcEmptySmsFromError() {return g("svcEmptySmsFromError");}

  default String svcEquipment() {return g("svcEquipment");}

  default String svcFaultInfo() {return g("svcFaultInfo");}

  default String svcFinal() {return g("svcFinal");}

  default String svcInform() {return g("svcInform");}

  default String svcInitial() {return g("svcInitial");}

  default String svcInvoices() {return g("svcInvoices");}

  default String svcIsValid() {return g("svcIsValid");}

  default String svcMaintenance() {return g("svcMaintenance");}

  default String svcMaintenanceEmailContent(Object p0, Object p1, Object p2) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    _m.put("{2}", p2);
    return Localized.format(g("svcMaintenanceEmailContent"), _m);
  }

  default String svcMaintenanceEquipment() {return g("svcMaintenanceEquipment");}

  default String svcMaintenanceItemsServices() {return g("svcMaintenanceItemsServices");}

  default String svcMaintenanceState() {return g("svcMaintenanceState");}

  default String svcMaintenanceTicket() {return g("svcMaintenanceTicket");}

  default String svcMaster() {return g("svcMaster");}

  default String svcMenu() {return g("svcMenu");}

  default String svcModel() {return g("svcModel");}

  default String svcModule() {return g("svcModule");}

  default String svcMyMaintenance() {return g("svcMyMaintenance");}

  default String svcNewDate() {return g("svcNewDate");}

  default String svcNewDefect() {return g("svcNewDefect");}

  default String svcNewDevice() {return g("svcNewDevice");}

  default String svcNewMaintenance() {return g("svcNewMaintenance");}

  default String svcNewMaintenanceItemService() {return g("svcNewMaintenanceItemService");}

  default String svcNewObject() {return g("svcNewObject");}

  default String svcNewSalary() {return g("svcNewSalary");}

  default String svcNewServiceState() {return g("svcNewServiceState");}

  default String svcNewServiceType() {return g("svcNewServiceType");}

  default String svcNewTariff() {return g("svcNewTariff");}

  default String svcNewWarrantyType() {return g("svcNewWarrantyType");}

  default String svcNotifyCustomer() {return g("svcNotifyCustomer");}

  default String svcObject() {return g("svcObject");}

  default String svcObjectFiles() {return g("svcObjectFiles");}

  default String svcObjects() {return g("svcObjects");}

  default String svcPayrollReport() {return g("svcPayrollReport");}

  default String svcProhibitEdit() {return g("svcProhibitEdit");}

  default String svcPublishTime() {return g("svcPublishTime");}

  default String svcRepair() {return g("svcRepair");}

  default String svcSearchingAllDevices() {return g("svcSearchingAllDevices");}

  default String svcSendEmail() {return g("svcSendEmail");}

  default String svcSendSms() {return g("svcSendSms");}

  default String svcSerialNo() {return g("svcSerialNo");}

  default String svcServiceState() {return g("svcServiceState");}

  default String svcServiceStates() {return g("svcServiceStates");}

  default String svcServiceType() {return g("svcServiceType");}

  default String svcServiceTypes() {return g("svcServiceTypes");}

  default String svcShowCustomer() {return g("svcShowCustomer");}

  default String svcSpecification() {return g("svcSpecification");}

  default String svcStateComment() {return g("svcStateComment");}

  default String svcStateProcess() {return g("svcStateProcess");}

  default String svcStickers() {return g("svcStickers");}

  default String svcTariff() {return g("svcTariff");}

  default String svcTariffs() {return g("svcTariffs");}

  default String svcTerm() {return g("svcTerm");}

  default String svcTypeOfWork() {return g("svcTypeOfWork");}

  default String svcUrgent() {return g("svcUrgent");}

  default String svcWarranty() {return g("svcWarranty");}

  default String svcWarrantyBasis() {return g("svcWarrantyBasis");}

  default String svcWarrantyDuration() {return g("svcWarrantyDuration");}

  default String svcWarrantyMaintenance() {return g("svcWarrantyMaintenance");}

  default String svcWarrantyType() {return g("svcWarrantyType");}

  default String svcWarrantyTypes() {return g("svcWarrantyTypes");}

  default String svcWarrantyValidTo() {return g("svcWarrantyValidTo");}

  default String swift() {return g("swift");}

  default String symptoms() {return g("symptoms");}

  default String systemAllUsers() {return g("systemAllUsers");}

  default String systemParameters() {return g("systemParameters");}

  default String systemUsers() {return g("systemUsers");}

  default String taAlterKind() {return g("taAlterKind");}

  default String taDate() {return g("taDate");}

  default String taDaysPerWeek() {return g("taDaysPerWeek");}

  default String taDaysPerWeekShort() {return g("taDaysPerWeekShort");}

  default String taFactor() {return g("taFactor");}

  default String taFactorShort() {return g("taFactorShort");}

  default String taInvoiceBuilder() {return g("taInvoiceBuilder");}

  default String taInvoiceCompose() {return g("taInvoiceCompose");}

  default String taInvoiceSave() {return g("taInvoiceSave");}

  default String taInvoiceSources() {return g("taInvoiceSources");}

  default String taItemPrices() {return g("taItemPrices");}

  default String taKind() {return g("taKind");}

  default String taKindPurchase() {return g("taKindPurchase");}

  default String taKindReserve() {return g("taKindReserve");}

  default String taKindReturn() {return g("taKindReturn");}

  default String taKindSale() {return g("taKindSale");}

  default String taKindSupplement() {return g("taKindSupplement");}

  default String taKindTender() {return g("taKindTender");}

  default String taKindWriteOff() {return g("taKindWriteOff");}

  default String taMinTerm() {return g("taMinTerm");}

  default String taMinTermShort() {return g("taMinTermShort");}

  default String taParent() {return g("taParent");}

  default String taQuantityRemained() {return g("taQuantityRemained");}

  default String taQuantityReturn() {return g("taQuantityReturn");}

  default String taQuantityReturned() {return g("taQuantityReturned");}

  default String taRecalculatePrices() {return g("taRecalculatePrices");}

  default String taRecalculatedPrices(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("taRecalculatedPrices"), _m);
  }

  default String taRemainders() {return g("taRemainders");}

  default String taReportItemsByCompany() {return g("taReportItemsByCompany");}

  default String taReportServices() {return g("taReportServices");}

  default String taReportStock() {return g("taReportStock");}

  default String taReportTransfer() {return g("taReportTransfer");}

  default String taReports() {return g("taReports");}

  default String taTariff() {return g("taTariff");}

  default String taTimeUnit() {return g("taTimeUnit");}

  default String taTimeUnitDay() {return g("taTimeUnitDay");}

  default String taTimeUnitMonth() {return g("taTimeUnitMonth");}

  default String taUntil() {return g("taUntil");}

  default String tabControl() {return g("tabControl");}

  default String tag() {return g("tag");}

  default String tags() {return g("tags");}

  default String taskAssignedToProject(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("taskAssignedToProject"), _m);
  }

  default String tcdArticleCategories() {return g("tcdArticleCategories");}

  default String tcdArticleCodes() {return g("tcdArticleCodes");}

  default String tcdArticleCriteria() {return g("tcdArticleCriteria");}

  default String tcdArticleGraphics() {return g("tcdArticleGraphics");}

  default String tcdArticlePrices() {return g("tcdArticlePrices");}

  default String tcdArticleSuppliers() {return g("tcdArticleSuppliers");}

  default String tcdBrand() {return g("tcdBrand");}

  default String tcdBrandNew() {return g("tcdBrandNew");}

  default String tcdBrands() {return g("tcdBrands");}

  default String tcdBrandsMapping() {return g("tcdBrandsMapping");}

  default String tcdCategories() {return g("tcdCategories");}

  default String tcdCriteria() {return g("tcdCriteria");}

  default String tcdManufacturer() {return g("tcdManufacturer");}

  default String tcdManufacturerNew() {return g("tcdManufacturerNew");}

  default String tcdManufacturers() {return g("tcdManufacturers");}

  default String tcdModelNew() {return g("tcdModelNew");}

  default String tcdModels() {return g("tcdModels");}

  default String tcdPriceLists() {return g("tcdPriceLists");}

  default String tcdRemainders() {return g("tcdRemainders");}

  default String tcdSupplierBrand() {return g("tcdSupplierBrand");}

  default String tcdTypeArticles() {return g("tcdTypeArticles");}

  default String tcdTypeNew() {return g("tcdTypeNew");}

  default String tcdTypes() {return g("tcdTypes");}

  default String template() {return g("template");}

  default String templates() {return g("templates");}

  default String text() {return g("text");}

  default String textConstants() {return g("textConstants");}

  default String themeColors() {return g("themeColors");}

  default String time() {return g("time");}

  default String timeCardChanges() {return g("timeCardChanges");}

  default String timeCardCode() {return g("timeCardCode");}

  default String timeCardCodes() {return g("timeCardCodes");}

  default String timeDifference() {return g("timeDifference");}

  default String timeRange() {return g("timeRange");}

  default String timeRangeCode() {return g("timeRangeCode");}

  default String timeRangeUsage() {return g("timeRangeUsage");}

  default String timeRanges() {return g("timeRanges");}

  default String timeSheet() {return g("timeSheet");}

  default String timeSheetLock() {return g("timeSheetLock");}

  default String timeZone() {return g("timeZone");}

  default String timeZoneName() {return g("timeZoneName");}

  default String timeZoneOffset() {return g("timeZoneOffset");}

  default String timeZoneUTCOffset() {return g("timeZoneUTCOffset");}

  default String timeZones() {return g("timeZones");}

  default String timeboardFooterHeight() {return g("timeboardFooterHeight");}

  default String timeboardFooterMap() {return g("timeboardFooterMap");}

  default String timeboardHeaderHeight() {return g("timeboardHeaderHeight");}

  default String timeboardItemOpacity() {return g("timeboardItemOpacity");}

  default String timeboardPixelsPerDay() {return g("timeboardPixelsPerDay");}

  default String timeboardPixelsPerRow() {return g("timeboardPixelsPerRow");}

  default String timeboardStripOpacity() {return g("timeboardStripOpacity");}

  default String to() {return g("to");}

  default String tooLittleData() {return g("tooLittleData");}

  default String total() {return g("total");}

  default String totalOf() {return g("totalOf");}

  default String trAbsenceDateFrom() {return g("trAbsenceDateFrom");}

  default String trAbsenceDateTo() {return g("trAbsenceDateTo");}

  default String trAbsenceReason() {return g("trAbsenceReason");}

  default String trAbsenceTypes() {return g("trAbsenceTypes");}

  default String trAccountingItem() {return g("trAccountingItem");}

  default String trAdditionalHandlingPlaces() {return g("trAdditionalHandlingPlaces");}

  default String trAdvances() {return g("trAdvances");}

  default String trAgreeWithConditions() {return g("trAgreeWithConditions");}

  default String trAgreeWithTermsAndConditions() {return g("trAgreeWithTermsAndConditions");}

  default String trArrivalCity() {return g("trArrivalCity");}

  default String trArrivalCountry() {return g("trArrivalCountry");}

  default String trArrivalDate() {return g("trArrivalDate");}

  default String trAssessment() {return g("trAssessment");}

  default String trAssessmentAskAnswered() {return g("trAssessmentAskAnswered");}

  default String trAssessmentAskCanceled() {return g("trAssessmentAskCanceled");}

  default String trAssessmentAskCompleted() {return g("trAssessmentAskCompleted");}

  default String trAssessmentAskLost() {return g("trAssessmentAskLost");}

  default String trAssessmentAskOrder() {return g("trAssessmentAskOrder");}

  default String trAssessmentAskRequest() {return g("trAssessmentAskRequest");}

  default String trAssessmentInvalidStatusError(Object p0, Object p1) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    _m.put("{1}", p1);
    return Localized.format(g("trAssessmentInvalidStatusError"), _m);
  }

  default String trAssessmentOrders() {return g("trAssessmentOrders");}

  default String trAssessmentPlannedKm() {return g("trAssessmentPlannedKm");}

  default String trAssessmentQuantityReport() {return g("trAssessmentQuantityReport");}

  default String trAssessmentReason() {return g("trAssessmentReason");}

  default String trAssessmentRejection() {return g("trAssessmentRejection");}

  default String trAssessmentReportAllOrders() {return g("trAssessmentReportAllOrders");}

  default String trAssessmentReportAnswered() {return g("trAssessmentReportAnswered");}

  default String trAssessmentReportApproved() {return g("trAssessmentReportApproved");}

  default String trAssessmentReportApprovedToAnswered() {return g("trAssessmentReportApprovedToAnswered");}

  default String trAssessmentReportApprovedToReceived() {return g("trAssessmentReportApprovedToReceived");}

  default String trAssessmentReportGrowth() {return g("trAssessmentReportGrowth");}

  default String trAssessmentReportLost() {return g("trAssessmentReportLost");}

  default String trAssessmentReportPercent() {return g("trAssessmentReportPercent");}

  default String trAssessmentReportQuantity() {return g("trAssessmentReportQuantity");}

  default String trAssessmentReportReceived() {return g("trAssessmentReportReceived");}

  default String trAssessmentReportSecondary() {return g("trAssessmentReportSecondary");}

  default String trAssessmentRequest() {return g("trAssessmentRequest");}

  default String trAssessmentRequests() {return g("trAssessmentRequests");}

  default String trAssessmentStatusAnswered() {return g("trAssessmentStatusAnswered");}

  default String trAssessmentStatusApproved() {return g("trAssessmentStatusApproved");}

  default String trAssessmentStatusLost() {return g("trAssessmentStatusLost");}

  default String trAssessmentStatusNew() {return g("trAssessmentStatusNew");}

  default String trAssessmentToRequests() {return g("trAssessmentToRequests");}

  default String trAssessmentTransportations() {return g("trAssessmentTransportations");}

  default String trAssessmentTurnoverReport() {return g("trAssessmentTurnoverReport");}

  default String trAssignCargo() {return g("trAssignCargo");}

  default String trAssignDriver() {return g("trAssignDriver");}

  default String trAssignTrip() {return g("trAssignTrip");}

  default String trAssignVehicle() {return g("trAssignVehicle");}

  default String trAverageFuelCost() {return g("trAverageFuelCost");}

  default String trAverageKilometerCost() {return g("trAverageKilometerCost");}

  default String trBriefInvoice() {return g("trBriefInvoice");}

  default String trCargoActualPlaces() {return g("trCargoActualPlaces");}

  default String trCargoCosts() {return g("trCargoCosts");}

  default String trCargoCreditInvoiceReason() {return g("trCargoCreditInvoiceReason");}

  default String trCargoDocuments() {return g("trCargoDocuments");}

  default String trCargoGroups() {return g("trCargoGroups");}

  default String trCargoIncomes() {return g("trCargoIncomes");}

  default String trCargoLoadingDateShort() {return g("trCargoLoadingDateShort");}

  default String trCargoSelectCargo() {return g("trCargoSelectCargo");}

  default String trCargoSelectTrip() {return g("trCargoSelectTrip");}

  default String trCargoTripThereCargosAssignedInTripsAlarm() {return g("trCargoTripThereCargosAssignedInTripsAlarm");}

  default String trCargoType() {return g("trCargoType");}

  default String trCargoTypes() {return g("trCargoTypes");}

  default String trCargoValueCurrency() {return g("trCargoValueCurrency");}

  default String trCarrierDriver() {return g("trCarrierDriver");}

  default String trCarrierVehicle() {return g("trCarrierVehicle");}

  default String trChildOrder() {return g("trChildOrder");}

  default String trChooseForwarder() {return g("trChooseForwarder");}

  default String trCommandCreateNewOrder() {return g("trCommandCreateNewOrder");}

  default String trCommandCreateNewUser() {return g("trCommandCreateNewUser");}

  default String trCommandSaveRequestAsTemplate() {return g("trCommandSaveRequestAsTemplate");}

  default String trComment() {return g("trComment");}

  default String trCompletedOrderEmailContent() {return g("trCompletedOrderEmailContent");}

  default String trConfirmCreateNewOrder() {return g("trConfirmCreateNewOrder");}

  default String trConfirmProforma() {return g("trConfirmProforma");}

  default String trConstantCosts() {return g("trConstantCosts");}

  default String trContract() {return g("trContract");}

  default String trContractMailContent() {return g("trContractMailContent");}

  default String trContractMailContentText() {return g("trContractMailContentText");}

  default String trContractPrinting() {return g("trContractPrinting");}

  default String trCountryNorm() {return g("trCountryNorm");}

  default String trCountryNorms() {return g("trCountryNorms");}

  default String trCreateTransportation() {return g("trCreateTransportation");}

  default String trCreditIdShort() {return g("trCreditIdShort");}

  default String trCreditInvoice() {return g("trCreditInvoice");}

  default String trCreditInvoices() {return g("trCreditInvoices");}

  default String trCreditSales() {return g("trCreditSales");}

  default String trCustomer() {return g("trCustomer");}

  default String trDailyAmount() {return g("trDailyAmount");}

  default String trDailyCosts() {return g("trDailyCosts");}

  default String trDailyCostsItem() {return g("trDailyCostsItem");}

  default String trDateFrom() {return g("trDateFrom");}

  default String trDateTo() {return g("trDateTo");}

  default String trDeparted() {return g("trDeparted");}

  default String trDepartureCity() {return g("trDepartureCity");}

  default String trDepartureCountry() {return g("trDepartureCountry");}

  default String trDepartureDate() {return g("trDepartureDate");}

  default String trDept() {return g("trDept");}

  default String trDirections() {return g("trDirections");}

  default String trDocumentNumberShort() {return g("trDocumentNumberShort");}

  default String trDriverEndingDate() {return g("trDriverEndingDate");}

  default String trDriverExperience() {return g("trDriverExperience");}

  default String trDriverName() {return g("trDriverName");}

  default String trDriverStartingDate() {return g("trDriverStartingDate");}

  default String trEconomyBonus() {return g("trEconomyBonus");}

  default String trEmptyKilometers() {return g("trEmptyKilometers");}

  default String trEmptyKilometersTotal() {return g("trEmptyKilometersTotal");}

  default String trExpedition() {return g("trExpedition");}

  default String trExpeditionTrips() {return g("trExpeditionTrips");}

  default String trExpeditionType() {return g("trExpeditionType");}

  default String trExpeditionTypeName() {return g("trExpeditionTypeName");}

  default String trExpeditionTypeSelfService() {return g("trExpeditionTypeSelfService");}

  default String trExpeditionTypes() {return g("trExpeditionTypes");}

  default String trExpenses() {return g("trExpenses");}

  default String trExpensesRegistered() {return g("trExpensesRegistered");}

  default String trFuelBalanceAfter() {return g("trFuelBalanceAfter");}

  default String trFuelBalanceBefore() {return g("trFuelBalanceBefore");}

  default String trFuelConsumptions() {return g("trFuelConsumptions");}

  default String trFuelConsumptionsAverage() {return g("trFuelConsumptionsAverage");}

  default String trFuelConsumptionsSummer() {return g("trFuelConsumptionsSummer");}

  default String trFuelConsumptionsWinter() {return g("trFuelConsumptionsWinter");}

  default String trFuelCosts() {return g("trFuelCosts");}

  default String trFuelTemperatures() {return g("trFuelTemperatures");}

  default String trFuelTypes() {return g("trFuelTypes");}

  default String trFuelTypesName() {return g("trFuelTypesName");}

  default String trGenerateDailyCosts() {return g("trGenerateDailyCosts");}

  default String trGenerateRoute() {return g("trGenerateRoute");}

  default String trGroup() {return g("trGroup");}

  default String trImportConditions() {return g("trImportConditions");}

  default String trImportCosts() {return g("trImportCosts");}

  default String trImportMapping() {return g("trImportMapping");}

  default String trImportMappings() {return g("trImportMappings");}

  default String trImportNewCondition() {return g("trImportNewCondition");}

  default String trImportNewMapping() {return g("trImportNewMapping");}

  default String trImportNewOption() {return g("trImportNewOption");}

  default String trImportNewProperty() {return g("trImportNewProperty");}

  default String trImportOption() {return g("trImportOption");}

  default String trImportOptions() {return g("trImportOptions");}

  default String trImportProperties() {return g("trImportProperties");}

  default String trImportProperty() {return g("trImportProperty");}

  default String trImportTracking() {return g("trImportTracking");}

  default String trImportType() {return g("trImportType");}

  default String trImportValue() {return g("trImportValue");}

  default String trIncomeInvoices() {return g("trIncomeInvoices");}

  default String trInvoiceHasNotAttribute() {return g("trInvoiceHasNotAttribute");}

  default String trInvoiceIdShort() {return g("trInvoiceIdShort");}

  default String trInvoices() {return g("trInvoices");}

  default String trLoaded() {return g("trLoaded");}

  default String trLoadedKilometers() {return g("trLoadedKilometers");}

  default String trLoadedKilometersTotal() {return g("trLoadedKilometersTotal");}

  default String trLoadingAddress() {return g("trLoadingAddress");}

  default String trLoadingCity() {return g("trLoadingCity");}

  default String trLoadingCompany() {return g("trLoadingCompany");}

  default String trLoadingContact() {return g("trLoadingContact");}

  default String trLoadingCountry() {return g("trLoadingCountry");}

  default String trLoadingDate() {return g("trLoadingDate");}

  default String trLoadingEmail() {return g("trLoadingEmail");}

  default String trLoadingFax() {return g("trLoadingFax");}

  default String trLoadingInfo() {return g("trLoadingInfo");}

  default String trLoadingNote() {return g("trLoadingNote");}

  default String trLoadingNumber() {return g("trLoadingNumber");}

  default String trLoadingPhone() {return g("trLoadingPhone");}

  default String trLoadingPostIndex() {return g("trLoadingPostIndex");}

  default String trLogistics() {return g("trLogistics");}

  default String trLogisticsSelfService() {return g("trLogisticsSelfService");}

  default String trLossReasons() {return g("trLossReasons");}

  default String trMarking() {return g("trMarking");}

  default String trMenuRegistrations() {return g("trMenuRegistrations");}

  default String trMenuRequestTemplates() {return g("trMenuRequestTemplates");}

  default String trMenuRequests() {return g("trMenuRequests");}

  default String trMenuSelfService() {return g("trMenuSelfService");}

  default String trMenuUnregisteredRequests() {return g("trMenuUnregisteredRequests");}

  default String trMessageTemplates() {return g("trMessageTemplates");}

  default String trModifyType() {return g("trModifyType");}

  default String trMotoHour() {return g("trMotoHour");}

  default String trMotoHourShort() {return g("trMotoHourShort");}

  default String trNewAbsence() {return g("trNewAbsence");}

  default String trNewAbsenceType() {return g("trNewAbsenceType");}

  default String trNewAssessment() {return g("trNewAssessment");}

  default String trNewCargoCreditInvoice() {return g("trNewCargoCreditInvoice");}

  default String trNewCargoExpense() {return g("trNewCargoExpense");}

  default String trNewCargoGroup() {return g("trNewCargoGroup");}

  default String trNewCargoInvoice() {return g("trNewCargoInvoice");}

  default String trNewCargoPlace() {return g("trNewCargoPlace");}

  default String trNewCargoPurchaseInvoice() {return g("trNewCargoPurchaseInvoice");}

  default String trNewCargoType() {return g("trNewCargoType");}

  default String trNewCarrier() {return g("trNewCarrier");}

  default String trNewCountryNorm() {return g("trNewCountryNorm");}

  default String trNewDriver() {return g("trNewDriver");}

  default String trNewExpedition() {return g("trNewExpedition");}

  default String trNewExpeditionTrip() {return g("trNewExpeditionTrip");}

  default String trNewExpeditionTypes() {return g("trNewExpeditionTypes");}

  default String trNewFuelType() {return g("trNewFuelType");}

  default String trNewService() {return g("trNewService");}

  default String trNewServiceTypes() {return g("trNewServiceTypes");}

  default String trNewShippingTerm() {return g("trNewShippingTerm");}

  default String trNewSparePart() {return g("trNewSparePart");}

  default String trNewTransportGroup() {return g("trNewTransportGroup");}

  default String trNewTrip() {return g("trNewTrip");}

  default String trNewValues() {return g("trNewValues");}

  default String trNewVehicle() {return g("trNewVehicle");}

  default String trNewVehicleModel() {return g("trNewVehicleModel");}

  default String trNewVehicleType() {return g("trNewVehicleType");}

  default String trObtained() {return g("trObtained");}

  default String trOrder() {return g("trOrder");}

  default String trOrderCargoIncomes() {return g("trOrderCargoIncomes");}

  default String trOrderCargoServices() {return g("trOrderCargoServices");}

  default String trOrderDocuments() {return g("trOrderDocuments");}

  default String trOrderStatus() {return g("trOrderStatus");}

  default String trOrderStatusActive() {return g("trOrderStatusActive");}

  default String trOrderStatusCanceled() {return g("trOrderStatusCanceled");}

  default String trOrderStatusCompleted() {return g("trOrderStatusCompleted");}

  default String trOrderStatusNew() {return g("trOrderStatusNew");}

  default String trOrderStatusRequest() {return g("trOrderStatusRequest");}

  default String trOrders() {return g("trOrders");}

  default String trOtherCosts() {return g("trOtherCosts");}

  default String trOwned() {return g("trOwned");}

  default String trPalettesQuantity() {return g("trPalettesQuantity");}

  default String trParameters() {return g("trParameters");}

  default String trPlannedEndDate() {return g("trPlannedEndDate");}

  default String trPreInvoice() {return g("trPreInvoice");}

  default String trProformaInvoice() {return g("trProformaInvoice");}

  default String trProformaInvoices() {return g("trProformaInvoices");}

  default String trPurchaseInvoice() {return g("trPurchaseInvoice");}

  default String trPurchaseInvoices() {return g("trPurchaseInvoices");}

  default String trRatePercentShort() {return g("trRatePercentShort");}

  default String trRegistration() {return g("trRegistration");}

  default String trRegistrationActionCancel() {return g("trRegistrationActionCancel");}

  default String trRegistrationActionSubmit() {return g("trRegistrationActionSubmit");}

  default String trRegistrationAddress() {return g("trRegistrationAddress");}

  default String trRegistrationBank() {return g("trRegistrationBank");}

  default String trRegistrationBankAccount() {return g("trRegistrationBankAccount");}

  default String trRegistrationBankAddress() {return g("trRegistrationBankAddress");}

  default String trRegistrationCity() {return g("trRegistrationCity");}

  default String trRegistrationCompanyCode() {return g("trRegistrationCompanyCode");}

  default String trRegistrationCompanyName() {return g("trRegistrationCompanyName");}

  default String trRegistrationContact() {return g("trRegistrationContact");}

  default String trRegistrationContactPosition() {return g("trRegistrationContactPosition");}

  default String trRegistrationCountry() {return g("trRegistrationCountry");}

  default String trRegistrationDate() {return g("trRegistrationDate");}

  default String trRegistrationEmail() {return g("trRegistrationEmail");}

  default String trRegistrationExchangeCode() {return g("trRegistrationExchangeCode");}

  default String trRegistrationFax() {return g("trRegistrationFax");}

  default String trRegistrationFormCaption() {return g("trRegistrationFormCaption");}

  default String trRegistrationMailContent() {return g("trRegistrationMailContent");}

  default String trRegistrationMailContentText() {return g("trRegistrationMailContentText");}

  default String trRegistrationMobile() {return g("trRegistrationMobile");}

  default String trRegistrationNew() {return g("trRegistrationNew");}

  default String trRegistrationNotes() {return g("trRegistrationNotes");}

  default String trRegistrationPhone() {return g("trRegistrationPhone");}

  default String trRegistrationReceived() {return g("trRegistrationReceived");}

  default String trRegistrationStatus() {return g("trRegistrationStatus");}

  default String trRegistrationStatusConfirmed() {return g("trRegistrationStatusConfirmed");}

  default String trRegistrationStatusNew() {return g("trRegistrationStatusNew");}

  default String trRegistrationStatusRejected() {return g("trRegistrationStatusRejected");}

  default String trRegistrationSwift() {return g("trRegistrationSwift");}

  default String trRegistrationVatCode() {return g("trRegistrationVatCode");}

  default String trRegistrationVatPayer() {return g("trRegistrationVatPayer");}

  default String trRegistrations() {return g("trRegistrations");}

  default String trRepairsHistory() {return g("trRepairsHistory");}

  default String trReportCustomerProfit() {return g("trReportCustomerProfit");}

  default String trReportFuelUsage() {return g("trReportFuelUsage");}

  default String trReportOrderProfit() {return g("trReportOrderProfit");}

  default String trReportProfitability() {return g("trReportProfitability");}

  default String trReportTripProfit() {return g("trReportTripProfit");}

  default String trReports() {return g("trReports");}

  default String trRequest() {return g("trRequest");}

  default String trRequestActionSubmit() {return g("trRequestActionSubmit");}

  default String trRequestAdditionalInfo() {return g("trRequestAdditionalInfo");}

  default String trRequestCargoCurrency() {return g("trRequestCargoCurrency");}

  default String trRequestCargoDescription() {return g("trRequestCargoDescription");}

  default String trRequestCargoHeight() {return g("trRequestCargoHeight");}

  default String trRequestCargoInfo() {return g("trRequestCargoInfo");}

  default String trRequestCargoLdm() {return g("trRequestCargoLdm");}

  default String trRequestCargoLength() {return g("trRequestCargoLength");}

  default String trRequestCargoPalettes() {return g("trRequestCargoPalettes");}

  default String trRequestCargoQuantity() {return g("trRequestCargoQuantity");}

  default String trRequestCargoValue() {return g("trRequestCargoValue");}

  default String trRequestCargoVolume() {return g("trRequestCargoVolume");}

  default String trRequestCargoWeight() {return g("trRequestCargoWeight");}

  default String trRequestCargoWidth() {return g("trRequestCargoWidth");}

  default String trRequestCommonTerms() {return g("trRequestCommonTerms");}

  default String trRequestConfirmedMailContent() {return g("trRequestConfirmedMailContent");}

  default String trRequestConfirmedMailContentText() {return g("trRequestConfirmedMailContentText");}

  default String trRequestCustomer() {return g("trRequestCustomer");}

  default String trRequestCustomerAddress() {return g("trRequestCustomerAddress");}

  default String trRequestCustomerCode() {return g("trRequestCustomerCode");}

  default String trRequestCustomerContact() {return g("trRequestCustomerContact");}

  default String trRequestCustomerContactPosition() {return g("trRequestCustomerContactPosition");}

  default String trRequestCustomerEmail() {return g("trRequestCustomerEmail");}

  default String trRequestCustomerExchangeCode() {return g("trRequestCustomerExchangeCode");}

  default String trRequestCustomerInfo() {return g("trRequestCustomerInfo");}

  default String trRequestCustomerName() {return g("trRequestCustomerName");}

  default String trRequestCustomerPhone() {return g("trRequestCustomerPhone");}

  default String trRequestCustomerVatCode() {return g("trRequestCustomerVatCode");}

  default String trRequestCustomerVatPayer() {return g("trRequestCustomerVatPayer");}

  default String trRequestCustomsBrokerage() {return g("trRequestCustomsBrokerage");}

  default String trRequestCustomsBrokeragePlaceholder() {return g("trRequestCustomsBrokeragePlaceholder");}

  default String trRequestDate() {return g("trRequestDate");}

  default String trRequestDateTitle() {return g("trRequestDateTitle");}

  default String trRequestDeliveryDate() {return g("trRequestDeliveryDate");}

  default String trRequestDeliveryDateAndTime() {return g("trRequestDeliveryDateAndTime");}

  default String trRequestDeliveryTime() {return g("trRequestDeliveryTime");}

  default String trRequestExpeditionType() {return g("trRequestExpeditionType");}

  default String trRequestFreightInsurance() {return g("trRequestFreightInsurance");}

  default String trRequestFreightInsurancePlaceholder() {return g("trRequestFreightInsurancePlaceholder");}

  default String trRequestLostMailContent() {return g("trRequestLostMailContent");}

  default String trRequestLostMailContentText() {return g("trRequestLostMailContentText");}

  default String trRequestNew() {return g("trRequestNew");}

  default String trRequestNotes() {return g("trRequestNotes");}

  default String trRequestPlaceCompanyName() {return g("trRequestPlaceCompanyName");}

  default String trRequestPlaceContact() {return g("trRequestPlaceContact");}

  default String trRequestPlaceFax() {return g("trRequestPlaceFax");}

  default String trRequestReceived() {return g("trRequestReceived");}

  default String trRequestResponsibleManager() {return g("trRequestResponsibleManager");}

  default String trRequestShipmentInfo() {return g("trRequestShipmentInfo");}

  default String trRequestShippingTerms() {return g("trRequestShippingTerms");}

  default String trRequestStatus() {return g("trRequestStatus");}

  default String trRequestStatusAnswered() {return g("trRequestStatusAnswered");}

  default String trRequestStatusApproved() {return g("trRequestStatusApproved");}

  default String trRequestStatusConfirmed() {return g("trRequestStatusConfirmed");}

  default String trRequestStatusContractSent() {return g("trRequestStatusContractSent");}

  default String trRequestStatusLost() {return g("trRequestStatusLost");}

  default String trRequestStatusNew() {return g("trRequestStatusNew");}

  default String trRequestStatusRejected() {return g("trRequestStatusRejected");}

  default String trRequestSubmittedContent() {return g("trRequestSubmittedContent");}

  default String trRequestSubmittedContentText() {return g("trRequestSubmittedContentText");}

  default String trRequestTemplate() {return g("trRequestTemplate");}

  default String trRequestTemplateDescription() {return g("trRequestTemplateDescription");}

  default String trRequestTemplateName() {return g("trRequestTemplateName");}

  default String trRequestTemplateNew() {return g("trRequestTemplateNew");}

  default String trRequestTemplateUser() {return g("trRequestTemplateUser");}

  default String trRequestTemplates() {return g("trRequestTemplates");}

  default String trRequestTermsOfDelivery() {return g("trRequestTermsOfDelivery");}

  default String trRequestUnregistered() {return g("trRequestUnregistered");}

  default String trRequestUser() {return g("trRequestUser");}

  default String trRequests() {return g("trRequests");}

  default String trRequestsShort() {return g("trRequestsShort");}

  default String trRequestsUnregistered() {return g("trRequestsUnregistered");}

  default String trReturned() {return g("trReturned");}

  default String trRoadCosts() {return g("trRoadCosts");}

  default String trRoadCostsItem() {return g("trRoadCostsItem");}

  default String trSeason() {return g("trSeason");}

  default String trSelfService() {return g("trSelfService");}

  default String trSelfServiceCommandHistory() {return g("trSelfServiceCommandHistory");}

  default String trSelfServiceCommandNewRequest() {return g("trSelfServiceCommandNewRequest");}

  default String trSelfServiceCommandRequests() {return g("trSelfServiceCommandRequests");}

  default String trSelfServiceCommandTemplates() {return g("trSelfServiceCommandTemplates");}

  default String trSendToERP() {return g("trSendToERP");}

  default String trSendToERPConfirm() {return g("trSendToERPConfirm");}

  default String trServiceHistory() {return g("trServiceHistory");}

  default String trServiceName() {return g("trServiceName");}

  default String trServiceTypeName() {return g("trServiceTypeName");}

  default String trServiceTypes() {return g("trServiceTypes");}

  default String trServices() {return g("trServices");}

  default String trShippingTermName() {return g("trShippingTermName");}

  default String trShippingTermSelfService() {return g("trShippingTermSelfService");}

  default String trShippingTerms() {return g("trShippingTerms");}

  default String trShowFinished() {return g("trShowFinished");}

  default String trSparePartMounted() {return g("trSparePartMounted");}

  default String trSparePartRemoved() {return g("trSparePartRemoved");}

  default String trSparePartUsage() {return g("trSparePartUsage");}

  default String trSpeedometerAfter() {return g("trSpeedometerAfter");}

  default String trSpeedometerFrom() {return g("trSpeedometerFrom");}

  default String trSpeedometerFromShort() {return g("trSpeedometerFromShort");}

  default String trSpeedometerToShort() {return g("trSpeedometerToShort");}

  default String trSupplier() {return g("trSupplier");}

  default String trTbSettingsAdditionalInfo() {return g("trTbSettingsAdditionalInfo");}

  default String trTbSettingsCompletedTrips() {return g("trTbSettingsCompletedTrips");}

  default String trTbSettingsCountryFlags() {return g("trTbSettingsCountryFlags");}

  default String trTbSettingsOrderCustomer() {return g("trTbSettingsOrderCustomer");}

  default String trTbSettingsOrderNo() {return g("trTbSettingsOrderNo");}

  default String trTbSettingsPixelsPerCustomer() {return g("trTbSettingsPixelsPerCustomer");}

  default String trTbSettingsPixelsPerDriver() {return g("trTbSettingsPixelsPerDriver");}

  default String trTbSettingsPixelsPerInfo() {return g("trTbSettingsPixelsPerInfo");}

  default String trTbSettingsPixelsPerNumber() {return g("trTbSettingsPixelsPerNumber");}

  default String trTbSettingsPixelsPerOrder() {return g("trTbSettingsPixelsPerOrder");}

  default String trTbSettingsPixelsPerTrip() {return g("trTbSettingsPixelsPerTrip");}

  default String trTbSettingsPixelsPerTruck() {return g("trTbSettingsPixelsPerTruck");}

  default String trTbSettingsPlaceCities() {return g("trTbSettingsPlaceCities");}

  default String trTbSettingsPlaceCodes() {return g("trTbSettingsPlaceCodes");}

  default String trTbSettingsPlaceInfo() {return g("trTbSettingsPlaceInfo");}

  default String trTbSettingsRefreshLocalChanges() {return g("trTbSettingsRefreshLocalChanges");}

  default String trTbSettingsRefreshRemoteChanges() {return g("trTbSettingsRefreshRemoteChanges");}

  default String trTbSettingsSeparateCargo() {return g("trTbSettingsSeparateCargo");}

  default String trTbSettingsSeparateTrips() {return g("trTbSettingsSeparateTrips");}

  default String trTemparatureToShort() {return g("trTemparatureToShort");}

  default String trTemperatureFromShort() {return g("trTemperatureFromShort");}

  default String trTemperatureShort() {return g("trTemperatureShort");}

  default String trTonneKilometer() {return g("trTonneKilometer");}

  default String trTransportGroups() {return g("trTransportGroups");}

  default String trTransportation() {return g("trTransportation");}

  default String trTransportationOrders() {return g("trTransportationOrders");}

  default String trTransportationPrice() {return g("trTransportationPrice");}

  default String trTripConstant() {return g("trTripConstant");}

  default String trTripConstants() {return g("trTripConstants");}

  default String trTripCosts() {return g("trTripCosts");}

  default String trTripDate() {return g("trTripDate");}

  default String trTripDateFrom() {return g("trTripDateFrom");}

  default String trTripDateTo() {return g("trTripDateTo");}

  default String trTripDocuments() {return g("trTripDocuments");}

  default String trTripFuelConsumptions() {return g("trTripFuelConsumptions");}

  default String trTripNo() {return g("trTripNo");}

  default String trTripPurchaseInvoices() {return g("trTripPurchaseInvoices");}

  default String trTripRoutes() {return g("trTripRoutes");}

  default String trTripStatus() {return g("trTripStatus");}

  default String trTripStatusActive() {return g("trTripStatusActive");}

  default String trTripStatusArranged() {return g("trTripStatusArranged");}

  default String trTripStatusCanceled() {return g("trTripStatusCanceled");}

  default String trTripStatusCompleted() {return g("trTripStatusCompleted");}

  default String trTripStatusNew() {return g("trTripStatusNew");}

  default String trTrips() {return g("trTrips");}

  default String trTruck() {return g("trTruck");}

  default String trTrucks() {return g("trTrucks");}

  default String trUnassignedFuelCosts() {return g("trUnassignedFuelCosts");}

  default String trUnassignedTripCosts() {return g("trUnassignedTripCosts");}

  default String trUnloaded() {return g("trUnloaded");}

  default String trUnloadingAddress() {return g("trUnloadingAddress");}

  default String trUnloadingCity() {return g("trUnloadingCity");}

  default String trUnloadingCompany() {return g("trUnloadingCompany");}

  default String trUnloadingContact() {return g("trUnloadingContact");}

  default String trUnloadingCountry() {return g("trUnloadingCountry");}

  default String trUnloadingDate() {return g("trUnloadingDate");}

  default String trUnloadingDateShort() {return g("trUnloadingDateShort");}

  default String trUnloadingEmail() {return g("trUnloadingEmail");}

  default String trUnloadingFax() {return g("trUnloadingFax");}

  default String trUnloadingInfo() {return g("trUnloadingInfo");}

  default String trUnloadingNote() {return g("trUnloadingNote");}

  default String trUnloadingNumber() {return g("trUnloadingNumber");}

  default String trUnloadingPhone() {return g("trUnloadingPhone");}

  default String trUnloadingPostIndex() {return g("trUnloadingPostIndex");}

  default String trVehicle() {return g("trVehicle");}

  default String trVehicleEndDate() {return g("trVehicleEndDate");}

  default String trVehicleMake() {return g("trVehicleMake");}

  default String trVehiclePartUsage() {return g("trVehiclePartUsage");}

  default String trVehicleRun() {return g("trVehicleRun");}

  default String trVehicleRunStart() {return g("trVehicleRunStart");}

  default String trVehicleServiceValid() {return g("trVehicleServiceValid");}

  default String trVehicleServiceValidKilometers() {return g("trVehicleServiceValidKilometers");}

  default String trVehicleServiceValidMotohours() {return g("trVehicleServiceValidMotohours");}

  default String trVehicleStartDate() {return g("trVehicleStartDate");}

  default String trVehicleTypeNumberShort() {return g("trVehicleTypeNumberShort");}

  default String trVehicleTypes() {return g("trVehicleTypes");}

  default String trVehicleTypesShort() {return g("trVehicleTypesShort");}

  default String trWeightInTons() {return g("trWeightInTons");}

  default String trWriteEmail() {return g("trWriteEmail");}

  default String trWrittenOff() {return g("trWrittenOff");}

  default String trade() {return g("trade");}

  default String tradeAct() {return g("tradeAct");}

  default String tradeActCopyQuestion() {return g("tradeActCopyQuestion");}

  default String tradeActInvoices() {return g("tradeActInvoices");}

  default String tradeActItems() {return g("tradeActItems");}

  default String tradeActName() {return g("tradeActName");}

  default String tradeActNames() {return g("tradeActNames");}

  default String tradeActNew() {return g("tradeActNew");}

  default String tradeActNewName() {return g("tradeActNewName");}

  default String tradeActNewTemplate() {return g("tradeActNewTemplate");}

  default String tradeActSaveAsTemplate() {return g("tradeActSaveAsTemplate");}

  default String tradeActServices() {return g("tradeActServices");}

  default String tradeActTemplate() {return g("tradeActTemplate");}

  default String tradeActTemplateItems() {return g("tradeActTemplateItems");}

  default String tradeActTemplateServices() {return g("tradeActTemplateServices");}

  default String tradeActTemplates() {return g("tradeActTemplates");}

  default String tradeActs() {return g("tradeActs");}

  default String tradeActsAll() {return g("tradeActsAll");}

  default String tradeActsAndItems() {return g("tradeActsAndItems");}

  default String trailer() {return g("trailer");}

  default String trailerTimeBoard() {return g("trailerTimeBoard");}

  default String trailers() {return g("trailers");}

  default String transport() {return g("transport");}

  default String transportArrival() {return g("transportArrival");}

  default String transportAssignTrip() {return g("transportAssignTrip");}

  default String transportConditions() {return g("transportConditions");}

  default String transportCost() {return g("transportCost");}

  default String transportDeparture() {return g("transportDeparture");}

  default String transportExpeditionTrip() {return g("transportExpeditionTrip");}

  default String transportExpenses() {return g("transportExpenses");}

  default String transportFuelBalanceInTank() {return g("transportFuelBalanceInTank");}

  default String transportGroup() {return g("transportGroup");}

  default String transportIncomes() {return g("transportIncomes");}

  default String transportMainItemCaption() {return g("transportMainItemCaption");}

  default String transportOrder() {return g("transportOrder");}

  default String transportOtherDetails() {return g("transportOtherDetails");}

  default String transportSalesInvoiceSerialNo() {return g("transportSalesInvoiceSerialNo");}

  default String transportSalesTerm() {return g("transportSalesTerm");}

  default String transportSpeedometer() {return g("transportSpeedometer");}

  default String transportTripColor() {return g("transportTripColor");}

  default String transportTripCosts() {return g("transportTripCosts");}

  default String transportTripMainData() {return g("transportTripMainData");}

  default String transportationContacts() {return g("transportationContacts");}

  default String transportationCustomers() {return g("transportationCustomers");}

  default String transportationNumber() {return g("transportationNumber");}

  default String travelSheet() {return g("travelSheet");}

  default String trdAmount() {return g("trdAmount");}

  default String trdAmountByRate() {return g("trdAmountByRate");}

  default String trdAmountWoVat() {return g("trdAmountWoVat");}

  default String trdCashRegisterNo() {return g("trdCashRegisterNo");}

  default String trdCustomer() {return g("trdCustomer");}

  default String trdDate() {return g("trdDate");}

  default String trdDaysForPayment() {return g("trdDaysForPayment");}

  default String trdDebt() {return g("trdDebt");}

  default String trdDebtReports() {return g("trdDebtReports");}

  default String trdDebts() {return g("trdDebts");}

  default String trdDocument() {return g("trdDocument");}

  default String trdDocumentExpenditures() {return g("trdDocumentExpenditures");}

  default String trdDocumentFiles() {return g("trdDocumentFiles");}

  default String trdDocumentItem() {return g("trdDocumentItem");}

  default String trdDocumentItems() {return g("trdDocumentItems");}

  default String trdDocumentLine() {return g("trdDocumentLine");}

  default String trdDocumentLong() {return g("trdDocumentLong");}

  default String trdDocumentPhase() {return g("trdDocumentPhase");}

  default String trdDocumentPhaseActive() {return g("trdDocumentPhaseActive");}

  default String trdDocumentPhaseApproved() {return g("trdDocumentPhaseApproved");}

  default String trdDocumentPhaseCompleted() {return g("trdDocumentPhaseCompleted");}

  default String trdDocumentPhaseOrder() {return g("trdDocumentPhaseOrder");}

  default String trdDocumentPhasePending() {return g("trdDocumentPhasePending");}

  default String trdDocumentPhaseTransitionQuestion(Object from, Object to) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{from}", from);
    _m.put("{to}", to);
    return Localized.format(g("trdDocumentPhaseTransitionQuestion"), _m);
  }

  default String trdDocumentPhases() {return g("trdDocumentPhases");}

  default String trdDocumentSelection() {return g("trdDocumentSelection");}

  default String trdDocumentShort() {return g("trdDocumentShort");}

  default String trdDocumentTags() {return g("trdDocumentTags");}

  default String trdDocumentType() {return g("trdDocumentType");}

  default String trdDocumentTypes() {return g("trdDocumentTypes");}

  default String trdDocuments() {return g("trdDocuments");}

  default String trdDriver() {return g("trdDriver");}

  default String trdExpenditureType() {return g("trdExpenditureType");}

  default String trdExpenditureTypes() {return g("trdExpenditureTypes");}

  default String trdExported() {return g("trdExported");}

  default String trdGenerateDocument() {return g("trdGenerateDocument");}

  default String trdGeneratedDocument() {return g("trdGeneratedDocument");}

  default String trdInvoice() {return g("trdInvoice");}

  default String trdInvoiceId() {return g("trdInvoiceId");}

  default String trdInvoiceNo() {return g("trdInvoiceNo");}

  default String trdInvoiceOverdue() {return g("trdInvoiceOverdue");}

  default String trdInvoicePrefix() {return g("trdInvoicePrefix");}

  default String trdInvoices() {return g("trdInvoices");}

  default String trdItemStock() {return g("trdItemStock");}

  default String trdItemVehicle() {return g("trdItemVehicle");}

  default String trdItemVehicleShort() {return g("trdItemVehicleShort");}

  default String trdItemWarehouseFrom() {return g("trdItemWarehouseFrom");}

  default String trdItemWarehouseTo() {return g("trdItemWarehouseTo");}

  default String trdManager() {return g("trdManager");}

  default String trdMovement() {return g("trdMovement");}

  default String trdMovementOfGoods() {return g("trdMovementOfGoods");}

  default String trdNewDocument() {return g("trdNewDocument");}

  default String trdNewDocumentType() {return g("trdNewDocumentType");}

  default String trdNewExpenditure() {return g("trdNewExpenditure");}

  default String trdNewExpenditureType() {return g("trdNewExpenditureType");}

  default String trdNewInvoice() {return g("trdNewInvoice");}

  default String trdNewNoteTemplate() {return g("trdNewNoteTemplate");}

  default String trdNewOperation() {return g("trdNewOperation");}

  default String trdNewPurchase() {return g("trdNewPurchase");}

  default String trdNewSeries() {return g("trdNewSeries");}

  default String trdNewStatus() {return g("trdNewStatus");}

  default String trdNewTurnover() {return g("trdNewTurnover");}

  default String trdNote() {return g("trdNote");}

  default String trdNoteTemplate() {return g("trdNoteTemplate");}

  default String trdNoteTemplates() {return g("trdNoteTemplates");}

  default String trdNotes() {return g("trdNotes");}

  default String trdNumber() {return g("trdNumber");}

  default String trdNumber1() {return g("trdNumber1");}

  default String trdNumber2() {return g("trdNumber2");}

  default String trdOperation() {return g("trdOperation");}

  default String trdOperationDefault() {return g("trdOperationDefault");}

  default String trdOperationType() {return g("trdOperationType");}

  default String trdOperations() {return g("trdOperations");}

  default String trdOperationsShort() {return g("trdOperationsShort");}

  default String trdOverdue() {return g("trdOverdue");}

  default String trdOverdueInDays() {return g("trdOverdueInDays");}

  default String trdPaid() {return g("trdPaid");}

  default String trdParameters() {return g("trdParameters");}

  default String trdPayer() {return g("trdPayer");}

  default String trdPayment() {return g("trdPayment");}

  default String trdPaymentTerms() {return g("trdPaymentTerms");}

  default String trdPaymentTermsShort() {return g("trdPaymentTermsShort");}

  default String trdPaymentTime() {return g("trdPaymentTime");}

  default String trdPayments() {return g("trdPayments");}

  default String trdPrice() {return g("trdPrice");}

  default String trdPrimaryDate() {return g("trdPrimaryDate");}

  default String trdPrimaryDocument() {return g("trdPrimaryDocument");}

  default String trdPrimaryDocumentItem() {return g("trdPrimaryDocumentItem");}

  default String trdProformaInvoice() {return g("trdProformaInvoice");}

  default String trdPurchase() {return g("trdPurchase");}

  default String trdPurchaseId() {return g("trdPurchaseId");}

  default String trdPurchaseItems() {return g("trdPurchaseItems");}

  default String trdPurchases() {return g("trdPurchases");}

  default String trdQuantity() {return g("trdQuantity");}

  default String trdQuantityAvailable() {return g("trdQuantityAvailable");}

  default String trdQuantityReserved() {return g("trdQuantityReserved");}

  default String trdQuantityStock() {return g("trdQuantityStock");}

  default String trdReceivedDate() {return g("trdReceivedDate");}

  default String trdRelatedDocuments() {return g("trdRelatedDocuments");}

  default String trdRemindTemplateFirstParagraph() {return g("trdRemindTemplateFirstParagraph");}

  default String trdRemindTemplateLastParagraph() {return g("trdRemindTemplateLastParagraph");}

  default String trdReminderTemplates() {return g("trdReminderTemplates");}

  default String trdReportColumnsMovement() {return g("trdReportColumnsMovement");}

  default String trdReportColumnsStock() {return g("trdReportColumnsStock");}

  default String trdReportMovementIn() {return g("trdReportMovementIn");}

  default String trdReportMovementOut() {return g("trdReportMovementOut");}

  default String trdReportStock() {return g("trdReportStock");}

  default String trdSaleItems() {return g("trdSaleItems");}

  default String trdSaleSeries() {return g("trdSaleSeries");}

  default String trdSales() {return g("trdSales");}

  default String trdSeries() {return g("trdSeries");}

  default String trdSeriesDefault() {return g("trdSeriesDefault");}

  default String trdSeriesManagers() {return g("trdSeriesManagers");}

  default String trdSeriesNumberLength() {return g("trdSeriesNumberLength");}

  default String trdSeriesNumberPrefix() {return g("trdSeriesNumberPrefix");}

  default String trdStatusActive() {return g("trdStatusActive");}

  default String trdStatuses() {return g("trdStatuses");}

  default String trdStock() {return g("trdStock");}

  default String trdSupplier() {return g("trdSupplier");}

  default String trdTerm() {return g("trdTerm");}

  default String trdTotal() {return g("trdTotal");}

  default String trdTurnover() {return g("trdTurnover");}

  default String trdTurnovers() {return g("trdTurnovers");}

  default String trdTypeCustomerReturn() {return g("trdTypeCustomerReturn");}

  default String trdTypePointOfSale() {return g("trdTypePointOfSale");}

  default String trdTypePurchase() {return g("trdTypePurchase");}

  default String trdTypeReturnToSupplier() {return g("trdTypeReturnToSupplier");}

  default String trdTypeSale() {return g("trdTypeSale");}

  default String trdTypeTransfer() {return g("trdTypeTransfer");}

  default String trdTypeWriteOff() {return g("trdTypeWriteOff");}

  default String trdVat() {return g("trdVat");}

  default String trdVatPercent() {return g("trdVatPercent");}

  default String trdVatPlus() {return g("trdVatPlus");}

  default String trdVehicle() {return g("trdVehicle");}

  default String trdVehicleShort() {return g("trdVehicleShort");}

  default String trdWarehouseFrom() {return g("trdWarehouseFrom");}

  default String trdWarehouseFromShort() {return g("trdWarehouseFromShort");}

  default String trdWarehouseTo() {return g("trdWarehouseTo");}

  default String trdWarehouseToShort() {return g("trdWarehouseToShort");}

  default String trigger() {return g("trigger");}

  default String triggers() {return g("triggers");}

  default String trip() {return g("trip");}

  default String tripDuration() {return g("tripDuration");}

  default String tripManager() {return g("tripManager");}

  default String tripRouteDocs() {return g("tripRouteDocs");}

  default String trips() {return g("trips");}

  default String truckTimeBoard() {return g("truckTimeBoard");}

  default String trucks() {return g("trucks");}

  default String type() {return g("type");}

  default String types() {return g("types");}

  default String uiTheme() {return g("uiTheme");}

  default String uiThemes() {return g("uiThemes");}

  default String unit() {return g("unit");}

  default String unitDayShort() {return g("unitDayShort");}

  default String unitDays() {return g("unitDays");}

  default String unitDaysShort() {return g("unitDaysShort");}

  default String unitFactor() {return g("unitFactor");}

  default String unitHourShort() {return g("unitHourShort");}

  default String unitHours() {return g("unitHours");}

  default String unitKilometers() {return g("unitKilometers");}

  default String unitMinutes() {return g("unitMinutes");}

  default String unitPrice() {return g("unitPrice");}

  default String unitShort() {return g("unitShort");}

  default String unitWeekShort() {return g("unitWeekShort");}

  default String units() {return g("units");}

  default String unloading() {return g("unloading");}

  default String unpacking() {return g("unpacking");}

  default String updateExchangeRatesDateHigh() {return g("updateExchangeRatesDateHigh");}

  default String updateExchangeRatesDateLow() {return g("updateExchangeRatesDateLow");}

  default String updateExchangeRatesDialogCaption() {return g("updateExchangeRatesDialogCaption");}

  default String updateExchangeRatesMenu() {return g("updateExchangeRatesMenu");}

  default String updateExchangeRatesNoCurrencies() {return g("updateExchangeRatesNoCurrencies");}

  default String updated() {return g("updated");}

  default String user() {return g("user");}

  default String userAgent() {return g("userAgent");}

  default String userFullName() {return g("userFullName");}

  default String userGroup() {return g("userGroup");}

  default String userGroupAddMembers() {return g("userGroupAddMembers");}

  default String userGroupList() {return g("userGroupList");}

  default String userGroupMembers() {return g("userGroupMembers");}

  default String userGroupPrivate() {return g("userGroupPrivate");}

  default String userGroupPublic() {return g("userGroupPublic");}

  default String userGroupVisibility() {return g("userGroupVisibility");}

  default String userGroups() {return g("userGroups");}

  default String userHasNotRoles() {return g("userHasNotRoles");}

  default String userInterface() {return g("userInterface");}

  default String userLocale() {return g("userLocale");}

  default String userLogin() {return g("userLogin");}

  default String userMode() {return g("userMode");}

  default String userParameters() {return g("userParameters");}

  default String userProfile() {return g("userProfile");}

  default String userProperties() {return g("userProperties");}

  default String userRemind() {return g("userRemind");}

  default String userReminder() {return g("userReminder");}

  default String userReminderCancel() {return g("userReminderCancel");}

  default String userReminderCreated() {return g("userReminderCreated");}

  default String userReminderDataLabel() {return g("userReminderDataLabel");}

  default String userReminderDisabled() {return g("userReminderDisabled");}

  default String userReminderOtherTime() {return g("userReminderOtherTime");}

  default String userReminderSendRemind() {return g("userReminderSendRemind");}

  default String userReminderSendRemindDateError() {return g("userReminderSendRemindDateError");}

  default String userReminderSuspend() {return g("userReminderSuspend");}

  default String userReminderUpdate() {return g("userReminderUpdate");}

  default String userRoles() {return g("userRoles");}

  default String userSettings() {return g("userSettings");}

  default String users() {return g("users");}

  default String validUntil() {return g("validUntil");}

  default String valuation() {return g("valuation");}

  default String value() {return g("value");}

  default String valueEmpty(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("valueEmpty"), _m);
  }

  default String valueExists(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("valueExists"), _m);
  }

  default String valueNotUnique(Object p0) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{0}", p0);
    return Localized.format(g("valueNotUnique"), _m);
  }

  default String valueParameter() {return g("valueParameter");}

  default String valueRequired() {return g("valueRequired");}

  default String values() {return g("values");}

  default String vat() {return g("vat");}

  default String vatAmount() {return g("vatAmount");}

  default String vatIsPercent() {return g("vatIsPercent");}

  default String vatMode() {return g("vatMode");}

  default String vatModeInclusive() {return g("vatModeInclusive");}

  default String vatModePlus() {return g("vatModePlus");}

  default String vatPercent() {return g("vatPercent");}

  default String vatPlus() {return g("vatPlus");}

  default String vehicleAirConditioner() {return g("vehicleAirConditioner");}

  default String vehicleAutomaticTransmission() {return g("vehicleAutomaticTransmission");}

  default String vehicleBodyNumber() {return g("vehicleBodyNumber");}

  default String vehicleBodyType() {return g("vehicleBodyType");}

  default String vehicleBrand() {return g("vehicleBrand");}

  default String vehicleBrands() {return g("vehicleBrands");}

  default String vehicleBrutto() {return g("vehicleBrutto");}

  default String vehicleCaravan() {return g("vehicleCaravan");}

  default String vehicleChassis() {return g("vehicleChassis");}

  default String vehicleCoupe() {return g("vehicleCoupe");}

  default String vehicleCylinderNumber() {return g("vehicleCylinderNumber");}

  default String vehicleDoorsNumber() {return g("vehicleDoorsNumber");}

  default String vehicleDriver() {return g("vehicleDriver");}

  default String vehicleEmissionStandard() {return g("vehicleEmissionStandard");}

  default String vehicleEmissionStandards() {return g("vehicleEmissionStandards");}

  default String vehicleEngineCode() {return g("vehicleEngineCode");}

  default String vehicleEngineNumber() {return g("vehicleEngineNumber");}

  default String vehicleEngineVolume() {return g("vehicleEngineVolume");}

  default String vehicleFuel() {return g("vehicleFuel");}

  default String vehicleGearbox() {return g("vehicleGearbox");}

  default String vehicleGearboxes() {return g("vehicleGearboxes");}

  default String vehicleGearsNumber() {return g("vehicleGearsNumber");}

  default String vehicleGroups() {return g("vehicleGroups");}

  default String vehicleGroupsShort() {return g("vehicleGroupsShort");}

  default String vehicleHatchback() {return g("vehicleHatchback");}

  default String vehicleHorsepower() {return g("vehicleHorsepower");}

  default String vehicleManualTransmission() {return g("vehicleManualTransmission");}

  default String vehicleManufacturerNr() {return g("vehicleManufacturerNr");}

  default String vehicleMinivan() {return g("vehicleMinivan");}

  default String vehicleModel() {return g("vehicleModel");}

  default String vehicleModelNr() {return g("vehicleModelNr");}

  default String vehicleModels() {return g("vehicleModels");}

  default String vehicleModelsEdit() {return g("vehicleModelsEdit");}

  default String vehicleModelsShort() {return g("vehicleModelsShort");}

  default String vehicleNetto() {return g("vehicleNetto");}

  default String vehicleNewEmissionStandard() {return g("vehicleNewEmissionStandard");}

  default String vehicleNewGearbox() {return g("vehicleNewGearbox");}

  default String vehicleNewPlaceCode() {return g("vehicleNewPlaceCode");}

  default String vehicleNewWarranty() {return g("vehicleNewWarranty");}

  default String vehicleNotes() {return g("vehicleNotes");}

  default String vehicleNumber() {return g("vehicleNumber");}

  default String vehicleOwnerName() {return g("vehicleOwnerName");}

  default String vehiclePlaceCode() {return g("vehiclePlaceCode");}

  default String vehiclePlaceCodes() {return g("vehiclePlaceCodes");}

  default String vehiclePower() {return g("vehiclePower");}

  default String vehicleProductionDate() {return g("vehicleProductionDate");}

  default String vehicleProductionDateShort() {return g("vehicleProductionDateShort");}

  default String vehicleRepairs() {return g("vehicleRepairs");}

  default String vehicleResponsiblePerson() {return g("vehicleResponsiblePerson");}

  default String vehicleSUV() {return g("vehicleSUV");}

  default String vehicleSedan() {return g("vehicleSedan");}

  default String vehicleService() {return g("vehicleService");}

  default String vehicleServiceSettings() {return g("vehicleServiceSettings");}

  default String vehicleSpeedometer() {return g("vehicleSpeedometer");}

  default String vehicleTankCapacity() {return g("vehicleTankCapacity");}

  default String vehicleType() {return g("vehicleType");}

  default String vehicleWarranties() {return g("vehicleWarranties");}

  default String vehicleWarranty() {return g("vehicleWarranty");}

  default String vehicles() {return g("vehicles");}

  default String vehiclesShort() {return g("vehiclesShort");}

  default String volume() {return g("volume");}

  default String volumeUnit() {return g("volumeUnit");}

  default String wage() {return g("wage");}

  default String warehouse() {return g("warehouse");}

  default String warehouses() {return g("warehouses");}

  default String website() {return g("website");}

  default String weight() {return g("weight");}

  default String weightUnit() {return g("weightUnit");}

  default String welcome() {return g("welcome");}

  default String welcomeMessage() {return g("welcomeMessage");}

  default String widgets() {return g("widgets");}

  default String width() {return g("width");}

  default String winter() {return g("winter");}

  default String withoutRemainder() {return g("withoutRemainder");}

  default String withoutVat() {return g("withoutVat");}

  default String workSchedule() {return g("workSchedule");}

  default String workScheduleActual() {return g("workScheduleActual");}

  default String workScheduleActualShort() {return g("workScheduleActualShort");}

  default String workScheduleExtension(Object from, Object to) {
    Map<String, Object> _m = new HashMap<>();
    _m.put("{from}", from);
    _m.put("{to}", to);
    return Localized.format(g("workScheduleExtension"), _m);
  }

  default String workScheduleHolidaysInclusiveShort() {return g("workScheduleHolidaysInclusiveShort");}

  default String workScheduleHolihoursInclusiveShort() {return g("workScheduleHolihoursInclusiveShort");}

  default String workSchedulePlanned() {return g("workSchedulePlanned");}

  default String workSchedulePlannedShort() {return g("workSchedulePlannedShort");}

  default String workdayTransitionBackward() {return g("workdayTransitionBackward");}

  default String workdayTransitionForward() {return g("workdayTransitionForward");}

  default String workdayTransitionNearest() {return g("workdayTransitionNearest");}

  default String workdayTransitionNone() {return g("workdayTransitionNone");}

  default String workspaceContinue() {return g("workspaceContinue");}

  default String workspaceStartup() {return g("workspaceStartup");}

  default String workspaces() {return g("workspaces");}

  default String year() {return g("year");}

  default String yearMonth() {return g("yearMonth");}

  default String yes() {return g("yes");}

  default String yesterday() {return g("yesterday");}
}
//@formatter:on
