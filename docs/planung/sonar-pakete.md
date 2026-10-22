# Sonar-Paket: neu einbezogene Shared- und Browserquellen

Status: vom Benutzer freigegeben; Code auf `codex/sonar-modulumfang` umgesetzt.
60 gezielte Regressionen bestanden: 13 App-Textfälle und 47 Browser-Helferfälle;
Formatierung, Spotless-Check und Checkstyle bestanden. Nachscan steht aus.
Ausgang: integriertes `main` `f9556c09837e3b6adcb44ba92ddc5f226bf7c481`,
Analyse `ee5346a6-78e3-40c9-bb93-43a30ae8e687`, Gate ERROR.
Alle 387 Java-Dateien sind korrekt zugeordnet; die bisherigen 37 Ausnahmen samt
Kommentaren sind unverändert auf die verschobenen App-Dateien übernommen.

## Ein freigegebenes Paket

31 Code-Smells: 28 Browser, drei Shared; keine Bugs, Vulnerabilities oder Hotspots.
23 Hinweise durch kleine Codekorrekturen bearbeitet; Serverbestätigung ausstehend.
Fünf begründete Accepted-Entscheidungen für begrenzte Retryabstände und zusammenhängende
Zustandsreisen sowie drei False positives für Launcherparameter und fremdes stdout
direkt am Finding dokumentiert. Bisherige Ausnahmen bleiben erhalten.
Kleine Codekorrekturen und einzeln fachlich begründete Statusentscheidungen,
keine entfernten Assertions, Sperren oder Aufräumgrenzen. Kein Gate-/Profil-/Scopeumbau.

- java:S1481: 7 Finding(s)
  - `fb553433-a711-4c29-a714-4ab107650fe9`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsSetup.java (18).
  - `64daa30f-5a24-4959-b8da-ae1f8e7e823c`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsSetup.java (54).
  - `4bca6317-a1d3-427d-b071-c3a97d62f641`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestCaTest.java (150).
  - `14ffa33e-5771-4e95-9cab-1799be47b3da`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestRunner.java (219).
  - `ed2eddf4-8962-4d0e-88ed-145aafdd1bb2`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTlsVerificationTest.java (34).
  - `cce7bb24-38bc-436d-9333-104384b38883`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/AccountLifecycleBrowserTest.java (197).
  - `c4af88e4-0555-4693-b295-44070ea767fb`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/PresentationBrowserTest.java (99).
- java:S2925: 5 Finding(s)
  - `f150a842-fb2d-436b-a01d-6f01419ac0ed`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestCa.java (290).
  - `a10888f9-033f-4959-890a-b639dd784a67`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestCaTest.java (199).
  - `9e23c4be-0112-45d2-a22e-d4595ad76ede`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsTest.java (209).
  - `4c55003a-d32c-48fa-a6b2-260379471536`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttps.java (106).
  - `193fb6fb-cc98-47e3-b678-18a8ff645c93`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsTest.java (216).
- java:S5778: 4 Finding(s)
  - `2a83bdfb-bc53-4ef1-b073-c8685a917d88`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTlsVerificationTest.java (45).
  - `dcca6b5e-4154-4b68-a229-124ad6820f87`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsTest.java (222).
  - `a5c627f1-6726-4b66-b276-ed82e68df036`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserExecutionPlanTest.java (158).
  - `fadeb0b7-3fe2-4c81-a69a-ca977cc9cbe3`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserExecutionPlanTest.java (161).
- java:S1172: 2 Finding(s)
  - `a193088d-a0dc-4ce9-b15a-dd22c34e6e2e`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsTest.java (202).
  - `9d457ca6-3712-487f-9e2b-0b2ddbeaf363`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserHttpsTest.java (214).
- java:S5961: 3 Finding(s)
  - `d8293d98-e336-46ef-b31e-46de4f7f275d`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/NotificationInboxBrowserTest.java (67).
  - `01fe5383-5427-4b25-b5ec-4f08ea3df054`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/NotificationInboxBrowserTest.java (165).
  - `d89fa828-d3f0-4738-b644-553b7ab0569e`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/AccountSettingsBrowserTest.java (238).
- java:S1612: 2 Finding(s)
  - `f91cb02b-8d97-4741-8130-c492c57a25ff`: browser-tests/src/test/java/de/derpeterson/browsertests/suites/AccountSettingsBrowserTest.java (457).
  - `93063c3f-9b4e-44d8-8f8b-a5222c726bb6`: browser-tests/src/test/java/de/derpeterson/browsertests/pages/UserStatusMenu.java (38).
- java:S7467: 4 Finding(s)
  - `b5e784c7-7231-413d-a7bd-c0bea923c776`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestRunner.java (146).
  - `3ea8df35-35b4-45a1-a6af-9d32ff944ed5`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestRunner.java (682).
  - `264ac337-08ba-497d-a07a-974f28e29a3f`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestRunner.java (696).
  - `35ad1b7e-b9b1-4d1c-9692-3870c91056ea`: browser-tests/src/test/java/de/derpeterson/browsertests/pages/UiActions.java (45).
- java:S9358: 2 Finding(s)
  - `b17aa820-ffbf-4481-a92c-22fc006cc1ba`: shared/src/main/java/de/derpeterson/app/i18n/MessageTexts.java (1040).
  - `94b10abb-d744-482c-a2f7-93e94209ce8c`: shared/src/main/java/de/derpeterson/app/i18n/MessageTexts.java (657).
- java:S4276: 1 Finding(s)
  - `2a53eb6e-1a8b-4716-b860-251b3ecf4d9a`: shared/src/main/java/de/derpeterson/app/i18n/MessageTexts.java (14).
- java:S2093: 1 Finding(s)
  - `d90f026e-c776-47cc-851f-30e2668d6a33`: browser-tests/src/test/java/de/derpeterson/browsertests/BrowserTestRunner.java (230).

## Nachweis und Abschluss

Java-Formatierung, Spotless-Check und Checkstyle; gezielte Shared-Verbraucher- und
Browser-Helfertests. Alle Browserfälle werden kompiliert; keine Browserjourneys
für diesen Sonar-Auftrag. Veränderte fachliche Browserabläufe gegebenenfalls als
offenen DE-/EN-Nachtest in `docs/testbedarf.md` erhalten.
Danach eigener lokaler Paketcommit und Integration nach main sowie vollständiger
Reactor-Nachscan; konkrete Revision, Task, Gate und alle ursprünglichen IDs prüfen.
Keine manuelle Fixed-Markierung. Einzelausnahmen direkt am Finding begründen und
nachlesen. Kein Push. Erst bei nachgewiesenem Abschluss diesen Paketplan entfernen;
der Multi-Modul-Plan bleibt bis zur gemeinsamen Gesamtabnahme erhalten.
