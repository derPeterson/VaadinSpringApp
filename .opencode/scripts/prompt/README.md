# Prompt Improver

Der Prompt Improver bereitet kurze oder unstrukturierte Softwareentwicklungs-Aufträge für einen Coding-Agenten auf.

Ziel ist eine präzise, strukturierte Aufgabenbeschreibung, ohne die Benutzerintention zu verändern oder unbelegte Produkt-, Architektur- oder Implementierungsanforderungen zu erfinden.

## Aktueller Stand

Der Prompt Improver ist als eigenständiges Python-Modul funktionsfähig und unterstützt aktuell drei Provider:

- `ollama` – lokale Modelle über Ollama
- `openai` – klassische OpenAI API mit API-Key und separater API-Abrechnung
- `chatgpt` – Sign in with ChatGPT mit gespeicherten OAuth-Credentials und ChatGPT-Plan-Nutzung

Der vollständige CLI-Pfad mit `chatgpt` wurde vor dem Abschluss-Paket erfolgreich
live Ende-zu-Ende getestet. Die jetzt ergänzte Fehlerbehandlung und Messung sind
offline einschließlich echter SDK-Antworttypen und paralleler Prozesse geprüft:

```text
CLI
  -> config.json
  -> LlmClientFactory
  -> ChatGPTClient
  -> gespeicherte Credentials / automatischer Token-Refresh
  -> Responses API
  -> Structured Output
  -> Pydantic-Validierung
  -> ImprovedPrompt
  -> Markdown-Ausgabe
```

Der fachliche Improver-Kern ist als optionales `prompt`-Modul im Python-Workflow
integriert. Aufrufe und Installation sind in [../../README.md](../../README.md)
beschrieben. Config, fachliche Regeln und Prompt-Schema bleiben erhalten. Die Provider erfassen
eigene Usage und behandeln Fehler ausdrücklich; ChatGPT-Refresh und Speichern
sind pro Benutzer über einen eigenen OS-Lock synchronisiert.

## Verantwortung

Der Prompt Improver soll:

- den ursprünglichen Auftrag erhalten,
- die Benutzerintention beibehalten,
- den Auftrag strukturieren und präzisieren,
- offene Punkte als `uncertainties` kennzeichnen,
- konfigurierbare Standardregeln ergänzen,
- strukturierte LLM-Ausgaben validieren,
- verschiedene LLM-Provider austauschbar machen,
- Original- und verbesserten Prompt optional als Artefakte speichern.

Er soll nicht:

- Projektdateien verändern,
- Git-Befehle ausführen,
- Builds oder Tests des Zielprojekts starten,
- eigenständig neue fachliche Anforderungen erfinden,
- unnötig den Scope erweitern,
- nicht genannte Technologien, Klassen, Dateien oder Architekturentscheidungen voraussetzen.

## Architektur

### Kontext und Grenzen im Workflow

Der Improver bekommt den ursprünglichen Auftragstext und die eigene Config.
Er erhält keine Projektdateien, keine `AGENTS.md` und keine Inhalte erwähnter
Findings-Dateien. Ein kurzer Auftrag mit konkreten IDs und Dateipfad ist dennoch
nutzbar: Der ausführende Agent liest diese Quelle anschließend im `/start`-Ablauf
und berücksichtigt Originalauftrag und verbesserten Prompt gemeinsam. Inhalte
fehlender Quellen darf der Improver nicht erfinden.

Die Defaults für `non_goals` und `verification` sind allgemeine Leitlinien.
Sie erweitern weder den fachlichen Umfang noch erlauben sie Codeänderungen bei
einem Analyseauftrag. Projektregeln kommen aus `AGENTS.md`, Ablauf und Abschluss
aus `/start`; Details der Aufgabe kommen vom Benutzer und seinen Quellen.

### Aufrufpfad

```text
CLI
 |
 v
config.json + optionale CLI-Overrides
 |
 v
LlmClientFactory
 |
 +-- OllamaClient
 |
 +-- OpenAIClient
 |
 +-- ChatGPTClient
 |
 v
PromptImprover
 |
 v
ImprovedPrompt JSON Schema
 |
 v
Structured JSON
 |
 v
Pydantic Validation
 |
 v
ImprovedPrompt
 |
 +--> Markdown-Ausgabe
 |
 +--> PromptArtifactWriter
       |
       +-- original-prompt.md
       +-- improved-prompt.md
prompt-metadata.json
prompt-usage.json
```

Der `PromptImprover` kennt den konkreten Provider nicht. Alle Provider implementieren dieselbe `LlmClient`-Schnittstelle.

## Verzeichnisstruktur

```text
prompt/
├─ __init__.py
├─ llm_client.py
├─ ollama_client.py
├─ openai_client.py
├─ chatgpt_client.py
├─ llm_client_factory.py
├─ execution.py
├─ telemetry.py
├─ models.py
├─ prompt_improver.py
├─ prompt_artifact_writer.py
├─ config.json
├─ README.md
├─ chatgpt/
│  ├─ __init__.py
│  ├─ auth.py
│  ├─ callback_server.py
│  ├─ credential_store.py
│  ├─ credentials.py
│  ├─ identity.py
│  ├─ token_client.py
│  └─ session_lock.py
├─ cli/
│  ├─ __init__.py
│  └─ prompt_improver_cli.py
└─ tests/
   ├─ __init__.py
   ├─ test_ollama.py
   ├─ test_openai.py
   ├─ test_prompt_improver.py
   ├─ test_chatgpt_auth.py
   ├─ test_chatgpt_callback.py
   ├─ test_chatgpt_login.py
   └─ test_chatgpt_client.py
```

`__pycache__`-Ordner und `.pyc`-Dateien sind Laufzeit-Artefakte und gehören nicht in die Quellstruktur.

## Datenmodelle

### `PromptImproverConfig`

Enthält:

- `provider`
- `model`
- `default_non_goals`
- `default_verification`

Die beiden Listen besitzen lokale Default-Werte über `Field(default_factory=list)`.

### `ImprovedPrompt`

Enthält exakt:

- `goal`
- `scope`
- `requirements`
- `non_goals`
- `verification`
- `uncertainties`

Alle Felder sind für Structured Output verpflichtend. Das Modell verwendet:

```python
ConfigDict(extra="forbid")
```

Dadurch erzeugt Pydantic ein Strict-JSON-Schema mit `additionalProperties: false`, das mit OpenAI Structured Outputs kompatibel ist.

Leere Listen werden in Markdown als `Not specified` dargestellt.

## Prompt-Regeln

`prompt_improver.py` definiert die fachlichen Regeln für die Verbesserung.

Wesentliche Vorgaben:

- ursprüngliche Intention erhalten,
- keine Produktanforderungen erfinden,
- keine Architekturentscheidungen erfinden,
- keine nicht genannten Klassen, Dateien, Frameworks, Datenbanken oder APIs voraussetzen,
- Scope nicht unnötig erweitern,
- nur direkt gestützte Anforderungen unter `requirements` aufnehmen,
- Annahmen stattdessen unter `uncertainties` aufführen,
- semantische Duplikate vermeiden,
- wenige präzise Einträge gegenüber vielen vagen Einträgen bevorzugen.

Nach der Modellantwort ergänzt `_apply_defaults()` die in `config.json` definierten Standardwerte für `non_goals` und `verification`.

## Konfiguration

Aktueller Stand von `config.json`:

```json
{
  "provider": "chatgpt",
  "model": "gpt-6.1-sol",
  "default_non_goals": [
    "Do not make unrelated code changes.",
    "Do not introduce architecture changes that are not required by the task."
  ],
  "default_verification": [
    "Run the relevant automated tests.",
    "Do not weaken existing tests.",
    "Verify that the requested behavior is covered."
  ]
}
```

Provider und Modell können über die CLI überschrieben werden.

Priorität:

```text
1. expliziter CLI-Parameter
2. config.json
```

## Provider

### Ollama

Datei: `ollama_client.py`

- verwendet die offizielle `ollama`-Python-Bibliothek,
- sendet System- und User-Prompt,
- unterstützt JSON-Schema über `format=...`,
- verwendet aktuell `temperature=0`.

Beispielmodell:

```text
qwen3-coder-q3-tools:latest
```

### OpenAI API

Datei: `openai_client.py`

- verwendet die offizielle `openai`-Python-Bibliothek,
- verwendet die Responses API,
- unterstützt normales Text-Output und Strict Structured Output,
- erwartet einen klassischen OpenAI API-Key, typischerweise über `OPENAI_API_KEY`,
- API-Nutzung und ChatGPT-Abonnement sind getrennte Abrechnungswege.

### ChatGPT

Datei: `chatgpt_client.py`

Der Client verwendet den offiziellen Sign-in-with-ChatGPT-Flow und anschließend die öffentliche Responses API.

Für Inference werden aktuell die für diesen Flow erforderlichen Einstellungen verwendet:

```text
store = false
stream = true
```

Der Client:

1. lädt lokal gespeicherte Credentials,
2. prüft die Gültigkeit des Access Tokens,
3. erneuert einen abgelaufenen Access Token über den Refresh Token,
4. speichert die rotierenden Credentials erneut,
5. erzeugt einen OpenAI-SDK-Client mit dem OAuth Access Token,
6. konsumiert den Stream bis zu einer terminalen Response,
7. übernimmt Text und Usage aus der endgültigen Response. Nur eine vollständige,
   nicht verweigerte und nicht leere Ausgabe wird akzeptiert; Deltas werden nicht
   zu einer zweiten Text- oder Usage-Summe addiert.

Auch Strict Structured Output wird über `text.format` unterstützt.

## ChatGPT-Authentifizierung

Die Authentifizierungslogik liegt getrennt unter `prompt/chatgpt/`.

### `auth.py`

Erzeugt:

- stabile Host-ID,
- `state`,
- `nonce`,
- PKCE `code_verifier`,
- PKCE `code_challenge`,
- Authorization-URL.

### `callback_server.py`

Stellt lokal den Callback bereit:

```text
http://127.0.0.1:1455/auth/callback
```

Der Listener muss vor dem Öffnen des Browser-Logins mit `start()` gestartet werden.

### `token_client.py`

Unterstützt:

- Authorization-Code-Exchange,
- Refresh-Token-Exchange.

Beim Refresh wird die bereits ausgegebene `client_id` verwendet und der rotierende neue Refresh Token gespeichert.

### `identity.py`

Validiert den ID Token gegen die OpenAI-JWKS und prüft unter anderem:

- Signatur,
- Issuer,
- Audience,
- Ablaufzeit,
- Nonce,
- Subject.

### `credential_store.py`

Speichert unkritische Metadaten unter:

```text
~/.vaadin-prompt-improver/chatgpt/
├─ host.json
└─ profile.json
```

Unter Windows entspricht das typischerweise:

```text
C:\Users\<Benutzer>\.vaadin-prompt-improver\chatgpt\
```

Die geheimen Werte

- `access_token`,
- `refresh_token`,
- `id_token`

werden nicht in den JSON-Dateien gespeichert, sondern über `keyring` im Windows Credential Manager.

Da Windows Credential Manager nur kleine Credential-Blobs akzeptiert, teilt der Store lange Tokens in Chunks auf.

### Access-Token-Gültigkeit

`ChatGPTCredentials.is_access_token_valid()` berechnet die Ablaufzeit aus:

```text
saved_at + expires_in
```

und verwendet standardmäßig einen Sicherheitspuffer von 60 Sekunden.

## Einmaliger ChatGPT-Login

Der aktuelle Bootstrap für die erstmalige Anmeldung erfolgt über das Smoke-Skript:

```text
tests/test_chatgpt_login.py
```

Es führt durch:

```text
stabile Host-ID
 -> Browser-Login
 -> Callback
 -> state-Prüfung
 -> PKCE Token Exchange
 -> ID-Token-Validierung
 -> Scope-Prüfung
 -> Credential Store
```

Bei späteren Starts werden gespeicherte Credentials geladen. Abgelaufene Access Tokens werden über den Refresh Token erneuert, ohne erneut den Browser zu öffnen.

Der Login ist aktuell noch nicht als eigener produktiver CLI-Befehl gekapselt. Das ist eine der verbleibenden Aufräumarbeiten.

## CLI

Datei:

```text
cli/prompt_improver_cli.py
```

Optionen:

```text
prompt
--provider
--model
--output-dir
```

Unterstützte Provider:

```text
ollama
openai
chatgpt
```

Empfohlener direkter Aufruf außerhalb einer IDE:

```powershell
cd .opencode\scripts
python -m prompt.cli.prompt_improver_cli "Add validation for empty usernames."
```

Alternativ kann `.opencode/scripts` in PyCharm als Sources Root markiert werden.

Beispiel mit Override:

```powershell
python -m prompt.cli.prompt_improver_cli `
  "Improve email normalization." `
  --provider ollama `
  --model qwen3-coder-q3-tools:latest
```

Artefakte schreiben:

```powershell
python -m prompt.cli.prompt_improver_cli `
  "Add validation for empty usernames." `
  --output-dir C:\Temp\prompt-run
```

Erzeugt:

```text
original-prompt.md
improved-prompt.md
prompt-metadata.json
prompt-usage.json
```

## Python-Abhängigkeiten

Aktuell werden mindestens folgende direkte Python-Pakete verwendet:

```text
pydantic
ollama
openai
httpx
keyring
PyJWT[crypto]
```

Die direkten Abhängigkeiten stehen mit den geprüften Versionen in
`.opencode/requirements.txt`.

## Tests und Smoke-Skripte

Die Dateien unter `tests/` sind aktuell überwiegend manuell ausführbare Smoke-/Integrationstests und keine vollständige automatisierte Pytest-Suite.

### `test_ollama.py`

Direkter Ollama-Client-Smoke-Test.

### `test_openai.py`

Direkter OpenAI-API-Smoke-Test. Benötigt API-Key und API-Guthaben.

### `test_prompt_improver.py`

Manueller Prompt-Improver-Smoke-Test. Die geladene `config.json` bestimmt
Provider und Modell über `LlmClientFactory`.

### `test_chatgpt_auth.py`

Prüft Erzeugung von Host-ID, PKCE-Daten und Authorization-URL.

### `test_chatgpt_callback.py`

Manueller Callback-Test. Der Listener wird vor `wait_for_callback()` gestartet:

```python
server.start()
```

Ohne `start()` beendet sich der Listener weiterhin mit
`Callback server must be started first.`; der Smoke-Test wurde korrigiert.

### `test_chatgpt_login.py`

End-to-End-Smoke-Test für:

- gespeicherte Credentials,
- Token-Gültigkeit,
- Token-Refresh,
- Browser-Login,
- ID-Token-Validierung,
- Scope-Prüfung,
- Credential-Speicherung.

### `test_chatgpt_client.py`

Testet den ChatGPT-Client mit Strict Structured Output und anschließendem Pydantic-Parsing.

## Fehlerbehandlung und eigene Usage

Workflow und CLI verwenden `execution.py`; `PromptImprover`, Config, Factory
und das Prompt-Schema bleiben fachlich unverändert. Incomplete, Refusal,
leere Ausgaben, abgerissene Streams und ungültiges JSON beenden den Aufruf.
Keine automatischen Inference-Retries oder stillen Provider-Wechsel.
HTTP-Anfragen haben 120 Sekunden Timeout (OAuth-Refresh: 30 Sekunden).

Mit `--output-dir` schreibt die CLI wie das Workflow-Modul zusätzlich
`prompt-metadata.json` und `prompt-usage.json`, auch bei Fehlern. Vorhandene
Provider-Messwerte bleiben bei fehlgeschlagener Pydantic-Validierung erhalten.
Gemessen werden Input, Output, Total, Reasoning, Cache, Inference-Versuche,
abgeschlossene Responses, Retries und Providerdauer, soweit tatsächlich vorhanden.
Kosten werden von den drei Clients nicht ausgewiesen und bleiben unbekannt.
OpenAI-Input enthält Cache-Reads; Output enthält Reasoning. Teilmengen nicht
nochmals addieren. Ollama-Reasoning-/Cache-Werte werden nicht aus Text geschätzt.

Der gemeinsame Session-Lock gilt für alle Projekte und Prozesse dieses Benutzers.
Nach Erwerb wird der aktuelle Keyring-Stand neu geladen. Neue Tokens und deren
Ablaufzeit werden zusammen als versionierter Snapshot gespeichert; alte bekannte
Chunks werden anschließend gelöscht. Bereits vorhandene Profile/Credentials
bleiben lesbar. `profile.json` enthält weiterhin keine Tokens.

Ein dauerhaft ungültiger Refresh-Token verlangt einen neuen Login; die gespeicherte
Invalidierung verhindert Wiederholungen. Temporäre Netzwerkfehler erhalten die
Sitzung. Der Benchmark öffnet keinen Browser. Die bewusste Wiederanmeldung erfolgt
vom `.opencode/scripts`-Ordner über:

```powershell
python -B -m prompt.tests.test_chatgpt_login
```

Das bestehende Bootstrap-Skript nutzt ebenfalls den synchronisierten Refresh und
startet bei dauerhaft ungültiger oder fehlender Sitzung den vorhandenen
PKCE-/Callback-/Identity-/Scope-geprüften Login. Dynamischer Modellkatalog,
Deduplizierung der Default-Listen und zusätzliche Workflow-Module sind nicht
Teil dieses Schritts. Bei einem Prozessabbruch zwischen serverseitiger Token-
Rotation und lokalem Speichern kann ein neuer Login nötig sein.

Die automatische Suite in `scripts/tests/test_prompt_providers.py` prüft alle
Provider offline, Fake-Keyring, Fehlerspeicherung und zwei parallele echte
Python-Prozesse. Die `prompt/tests`-Skripte sind bewusst aufrufbare Live-Smoke-
Tests und gehören nicht zum automatischen Offline-Lauf.

## Security-Grundsätze

- Access-, Refresh- und ID-Tokens niemals ausgeben oder loggen.
- Tokens niemals committen.
- Access- und Refresh-Tokens niemals in URLs übertragen.
- `state` vor dem Token Exchange prüfen.
- ID Token vor Verwendung der Identität validieren.
- den Scope `chatgpt.tokens.use.direct` prüfen.
- die stabile Host-ID beibehalten.
- rotierende Refresh Tokens nach jedem erfolgreichen Refresh ersetzen.
- lokale Secret-Speicherung über den Credential Store beibehalten.

## Artefakte

`PromptArtifactWriter` schreibt Original- und verbesserten Prompt. Die gemeinsame
Ausführung in `execution.py` ergänzt Metadaten und Usage, auch bei Fehlern.
Zusammen entstehen:

```text
original-prompt.md
improved-prompt.md
prompt-metadata.json
prompt-usage.json
```

Das `prompt`-Modul schreibt diese Dateien direkt in den externen Run-Ordner.

## Bewusster Umfang

Der bestehende Stand bleibt bei `branch`, `prompt`, `benchmark`, `usage` und
`complete` als Preset. Diese technischen Hilfsdateien sind keine zusätzlichen
Workflow-Module. Details der Usage-Spalten, Messgrenzen und Fehlerkategorien
stehen in [../../README.md](../../README.md#prompt-improver-eigene-usage-und-fehlerbehandlung).
