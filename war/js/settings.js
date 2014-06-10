var BeeSettings = {
  appName: "B-NOVO",
  version: "1.10",
  logoImage: "images/logo.png",
  logoOpen: "http://www.butent.com",
  startMillis: new Date().getTime(),
  providerSensitivityMillis: 300,
  providerRepeatMillis: 200,
  providerMinPrefetchSteps: 1,
  providerMaxPrefetchSteps: 100,
  logCapacity: 1000,
  actionSensitivityMillis: 300,
  minimizeNumberOfConcurrentRequests: true,
  webSocketUrl: "ws://crm.osama.lt/b-novo/ws",
  showUserPhoto: true,
  onStartup: "AnnouncementsBoard"
};