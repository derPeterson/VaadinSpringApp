# Offene Sonar-Bereinigung der Designübernahme

## Freigegebener Umfang

Komplettbereinigung nach dem Design-Merge. Ausgangsstand `54073e4`,
Analyse `9f6b6785-c56c-4a1c-9c68-7d3a86182a1d`: 54 offene Issues,
45 bestehende Ausnahmen, keine Sicherheits-Hotspots. Keine Regel-, Scope- oder
Quality-Gate-Änderung. Keine neue Gestaltung oder Fachfunktion.

## Paket: UI-Zustände und Wartbarkeit

Branch: `codex/sonar-design-uebernahme`. Zuerst drei Nullwertbefunde an
Dauereingaben prüfen und Werte konsistent lokal lesen. Danach serialisierbare
UI-Callbacks, kleine Gliederungen, Konstanten und Testlesbarkeit bereinigen.
Die drei Hinweise zur Vaadin-Vererbungstiefe fachlich am vorhandenen gemeinsamen
Seitenrahmen bewerten. Begründungen gegebenenfalls direkt am jeweiligen Finding.

Prüfungen: gezielte Dauer-, Benachrichtigungs-, Header-, Konfigurations- und
Dialogtests. Abschließend vollständiger Production-/Coverage-Scan auf main mit
App-/Shared- und Browser-Helfertests. Keine zusätzliche Browserrunde ohne Freigabe.
Darstellung und Fachregeln bleiben unverändert.

## Ausstehende Bestätigung

Die 51 Codekorrekturen sind umgesetzt und gezielt geprüft. Bestätigung erst nach
lokalem Paketcommit, Integration nach main und vollständigem Nachscan. Die drei
Vererbungshinweise sind mit Einzelbegründung direkt in Sonar akzeptiert.
Gemeinsame CSS-Klassen stehen auf ausdrücklichen Benutzerwunsch in `CosmosClasses`;
Literalhinweise werden durch Code behoben, nicht akzeptiert.

## Im Nachscan zu bestätigende Befunde

- `7bc04dbd-9e37-461a-94cc-fc7cba459d75` – `java:S1192`: app/src/main/java/de/derpeterson/app/ui/components/UserPopoverMenu.java:188
- `3cc4a9ac-65d0-45f9-ad99-09a4b9dfe497` – `java:S1488`: app/src/main/java/de/derpeterson/app/views/AdminView.java:203
- `6cfee957-a38c-4b7a-a44f-39c73849ebe4` – `java:S3776`: app/src/main/java/de/derpeterson/app/helper/ui/NotificationHelper.java:83
- `ba6914fd-ee82-435c-9a9c-126ac794cd84` – `java:S1117`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:349
- `64c314b3-e86f-47fa-a4bc-328dbbdda157` – `java:S6203`: app/src/main/java/de/derpeterson/app/ui/components/PortalPageComponent.java:59
- `8a7574e9-cdcf-4111-9f38-622e8468110c` – `java:S3415`: app/src/test/java/de/derpeterson/app/helper/image/ImageHelperTest.java:36
- `6694a414-9148-4abc-ab9b-4d7d4d532449` – `java:S5785`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/CosmosComponentsBrowserTest.java:225
- `e58ae99d-ff00-43fc-9c3e-8d68fbf6ee2f` – `java:S1192`: app/src/main/java/de/derpeterson/app/ui/base/TrackedUserAppLayout.java:169
- `6969dfca-1250-48af-9477-e7c48ee851f9` – `java:S1948`: app/src/main/java/de/derpeterson/app/ui/components/AccountHeaderActions.java:40
- `dc503ecc-da83-480f-a558-11ceeeeada66` – `java:S2259`: app/src/main/java/de/derpeterson/app/ui/components/AdminDurationField.java:147
- `5985bc9a-df0e-45f3-bc28-b9299349a69c` – `java:S7467`: app/src/main/java/de/derpeterson/app/ui/components/AdminDurationField.java:153
- `4b26824a-3fb2-4fcf-9cba-4af5b924d7f1` – `java:S7467`: app/src/main/java/de/derpeterson/app/ui/components/AdminDurationField.java:200
- `c5e99e1b-72be-4878-ad28-f3b37ad7057e` – `java:S2259`: app/src/main/java/de/derpeterson/app/ui/components/AdminDurationField.java:235
- `1d51f961-0639-4ad8-bd2e-03e5266fc376` – `java:S2259`: app/src/main/java/de/derpeterson/app/ui/components/AdminDurationField.java:259
- `c16ff7aa-bb33-41c6-b741-812e7a167e46` – `java:S6203`: app/src/main/java/de/derpeterson/app/ui/components/PageNavigationComponent.java:33
- `7c8013b1-cc40-4abf-a5a9-9cdec97767f5` – `java:S1612`: app/src/main/java/de/derpeterson/app/ui/components/PortalConfirmationDialog.java:46
- `0844246c-5302-4192-ac8e-85da6d6d18b5` – `java:S1192`: app/src/main/java/de/derpeterson/app/ui/components/PortalPageComponent.java:48
- `f8f55ba8-1455-4a47-9588-3410287003f4` – `java:S1192`: app/src/main/java/de/derpeterson/app/ui/components/PortalPageComponent.java:51
- `d099368f-3bac-426d-8f3f-741ebb77141c` – `java:S1948`: app/src/main/java/de/derpeterson/app/ui/components/PublicPageComponent.java:53
- `bc7db455-1293-4177-bf2f-be6f479610f7` – `java:S1192`: app/src/main/java/de/derpeterson/app/ui/components/PublicPageComponent.java:78
- `73c33a01-1ba9-4e6d-a3d7-79d683491c52` – `java:S1948`: app/src/main/java/de/derpeterson/app/ui/components/UserNotificationBell.java:25
- `a8cf8c5e-ee77-4817-a779-56de3bb32caf` – `java:S1192`: app/src/main/java/de/derpeterson/app/ui/components/UserNotificationsPanel.java:175
- `b50ac4eb-2216-4762-975b-bf9b3f10114e` – `java:S1450`: app/src/main/java/de/derpeterson/app/ui/components/UserPopoverMenu.java:50
- `f36e7a9a-9c72-4292-94f4-615d255c5705` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/VerificationView.java:204
- `48151768-bd64-4ccb-a0b4-7684399eaeac` – `java:S3776`: app/src/main/java/de/derpeterson/app/views/admin/AdminConfigurationSection.java:434
- `d3638562-14e9-4c6d-9790-9b129a3f116e` – `java:S3358`: app/src/main/java/de/derpeterson/app/views/admin/AdminConfigurationSection.java:454
- `52798dbe-75ff-40f8-b645-ba4ea8454546` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminConfigurationSection.java:557
- `ffb9eb66-782b-40b9-ab88-3be5876ce4bb` – `java:S3776`: app/src/main/java/de/derpeterson/app/views/admin/AdminConfigurationSection.java:887
- `bdf538a6-6047-4269-b987-748b0098eb0c` – `java:S1117`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserManagementSection.java:338
- `c88c017a-cf19-4c0f-a0b1-6ebddac25027` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserManagementSection.java:607
- `ee1479cd-c610-4db5-b28e-54b83087cded` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserManagementSection.java:664
- `8fcb73f2-9369-4843-b2db-b67c159de53d` – `java:S1117`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserManagementSection.java:1119
- `88480599-af10-4df1-a223-5ebc6e1adcc4` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:116
- `b076ef47-22dc-4938-85c2-612849b97698` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:118
- `036f5339-06d3-43ec-80e0-1fcf9c4c90a1` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:125
- `b573cd21-776a-449d-85d7-82d5efac1dac` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:204
- `1a1059fd-856b-471e-9179-10475221e939` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:212
- `a0f8761d-ef36-448a-b445-d92b13574fae` – `java:S1192`: app/src/main/java/de/derpeterson/app/views/admin/AdminUserNotificationsSection.java:241
- `753fe77e-cf49-40a6-b0f8-cdead25adba8` – `java:S5778`: app/src/test/java/de/derpeterson/app/ui/components/PortalConfirmationDialogTest.java:94
- `2824ee83-339d-4318-a772-8b3301c59903` – `java:S1117`: app/src/test/java/de/derpeterson/app/ui/components/UserNotificationsUiTest.java:193
- `dcf1c865-a647-4676-8ab5-3f9fb05a4214` – `java:S8924`: app/src/test/java/de/derpeterson/app/ui/components/UserStatusNotificationTest.java:176
- `8d365501-b5f1-444d-9497-3a2c7ed75aed` – `java:S8924`: app/src/test/java/de/derpeterson/app/views/ApplicationHeaderTest.java:305
- `49f36804-bf76-4a60-bb5e-f27388086123` – `java:S5785`: app/src/test/java/de/derpeterson/app/views/admin/AdminConfigurationGroupTest.java:238
- `c8355bb8-a6c4-45bb-a54e-817eca403776` – `java:S5785`: app/src/test/java/de/derpeterson/app/views/admin/AdminConfigurationGroupTest.java:244
- `382482bc-19e1-439d-846d-1cc0a36603be` – `java:S1854`: app/src/test/java/de/derpeterson/app/views/ApplicationHeaderTest.java:224
- `b6cef4dc-7603-4ad5-8e5f-eb7d7dedd918` – `java:S1481`: app/src/test/java/de/derpeterson/app/views/ApplicationHeaderTest.java:224
- `90b7eb8b-2ef9-4355-8dfb-12decb965d26` – `java:S1068`: app/src/main/java/de/derpeterson/app/ui/components/UserNotificationsPanel.java:43
- `28a0c879-2503-460c-8280-95b61ed29863` – `java:S1068`: app/src/main/java/de/derpeterson/app/views/ResetPasswordView.java:55
- `67cf44a6-d88c-44e3-9210-2bc90ec4123c` – `java:S1068`: app/src/main/java/de/derpeterson/app/views/ResetPasswordView.java:57
- `c0f1f0de-d04a-4899-af5b-19569c98bd35` – `java:S1068`: app/src/main/java/de/derpeterson/app/views/VerificationView.java:56
- `717d8de7-f7b6-42b1-9908-0efb43d4043e` – `java:S1068`: app/src/main/java/de/derpeterson/app/views/VerificationView.java:58
