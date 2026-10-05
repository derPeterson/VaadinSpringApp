# Java → Python CheatSheet

Ein ausführliches Nachschlagewerk für Java-Entwickler, die Python lesen und schreiben möchten.

> Fokus: Java als Denkbrücke verwenden, ohne Python künstlich zu Java zu machen.

## Inhaltsverzeichnis

1. [Grundsyntax](#1-grundsyntax)
2. [Variablen und Typen](#2-variablen-und-typen)
3. [Strings und f-Strings](#3-strings-und-f-strings)
4. [Funktionen und Methoden](#4-funktionen-und-methoden)
5. [Klassen, Konstruktor und self](#5-klassen-konstruktor-und-self)
6. [Vererbung, ABC und abstractmethod](#6-vererbung-abc-und-abstractmethod)
7. [staticmethod und classmethod](#7-staticmethod-und-classmethod)
8. [Sichtbarkeit und Konventionen](#8-sichtbarkeit-und-konventionen)
9. [Collections](#9-collections)
10. [Schleifen und Comprehensions](#10-schleifen-und-comprehensions)
11. [if, match und Truthiness](#11-if-match-und-truthiness)
12. [None, Optional, == und is](#12-none-optional--und-is)
13. [Exceptions und with](#13-exceptions-und-with)
14. [Pathlib und Dateien](#14-pathlib-und-dateien)
15. [dataclass und Pydantic](#15-dataclass-und-pydantic)
16. [Decorators](#16-decorators)
17. [Imports, Packages und __init__.py](#17-imports-packages-und-__init__py)
18. [__main__ und argparse](#18-__main__-und-argparse)
19. [Factory Pattern](#19-factory-pattern)
20. [Dependency Injection](#20-dependency-injection)
21. [Properties, Enums und Konstanten](#21-properties-enums-und-konstanten)
22. [Tuples, Slicing und len](#22-tuples-slicing-und-len)
23. [JSON, Generics und Any](#23-json-generics-und-any)
24. [Prompt-Improver-Schnellreferenz](#24-prompt-improver-schnellreferenz)
25. [Typische Stolperfallen](#25-typische-stolperfallen)
26. [PyCharm-Tipps](#26-pycharm-tipps)

---

## 1. Grundsyntax

| Java | Python |
|---|---|
| `{ ... }` | Einrückung |
| `;` | entfällt |
| `// Kommentar` | `# Kommentar` |
| `null` | `None` |
| `true / false` | `True / False` |
| `&&` | `and` |
| `||` | `or` |
| `!x` | `not x` |
| `throw` | `raise` |

Java:

```java
if (user != null) {
    System.out.println(user);
}
```

Python:

```python
if user is not None:
    print(user)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 2. Variablen und Typen

Java:

```java
String name = "Chris";
int age = 30;
boolean active = true;
```

Python:

```python
name: str = "Chris"
age: int = 30
active: bool = True
```

| Java | Python |
|---|---|
| `String` | `str` |
| `int` / `long` | `int` |
| `double` | `float` |
| `boolean` | `bool` |
| `Object` | `Any` |
| `List<String>` | `list[str]` |
| `Set<String>` | `set[str]` |
| `Map<String,Integer>` | `dict[str, int]` |
| `Optional<String>` | `str \| None` |

Python ist dynamisch typisiert. Type Hints helfen IDE, Leser und Tools, sind aber nicht automatisch Runtime-Zwang.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 3. Strings und f-Strings

Java:

```java
String message = "Hallo " + name;
```

Python:

```python
message = f"Hallo {name}"
```

Das `f` aktiviert String-Interpolation.

```python
provider = "ollama"
model = "qwen"
print(f"Using provider {provider} with model {model}")
```

Mehrzeilig:

```python
text = """
Mehrzeiliger
Text
"""
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 4. Funktionen und Methoden

Java:

```java
public String greet(String name) {
    return "Hallo " + name;
}
```

Python:

```python
def greet(name: str) -> str:
    return f"Hallo {name}"
```

`-> str` ist der Rückgabetyp. `void` entspricht meist `-> None`.

```python
def save() -> None:
    ...
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 5. Klassen, Konstruktor und self

Java:

```java
public class UserService {
    private final String name;

    public UserService(String name) {
        this.name = name;
    }
}
```

Python:

```python
class UserService:

    def __init__(self, name: str):
        self.name = name
```

Instanziieren:

```python
service = UserService("Test")
```

Kein `new`.

`self` entspricht gedanklich `this`. Es steht explizit in der Methodensignatur:

```python
def chat(self, prompt: str) -> str:
    ...
```

Beim Aufruf wird `self` nicht mitgegeben:

```python
client.chat("Hallo")
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 6. Vererbung, ABC und abstractmethod

Java:

```java
public interface LlmClient {
    String chat(String prompt);
}
```

Python:

```python
from abc import ABC, abstractmethod

class LlmClient(ABC):

    @abstractmethod
    def chat(self, prompt: str) -> str:
        pass
```

`ABC` = Abstract Base Class.

`@abstractmethod` zwingt konkrete Unterklassen zur Implementierung.

```python
class OllamaClient(LlmClient):
    ...
```

Das kannst du gedanklich wie `implements LlmClient` lesen.

`pass` bedeutet: absichtlich nichts tun.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 7. staticmethod und classmethod

Java:

```java
public static User create() {
    ...
}
```

Python:

```python
class UserFactory:

    @staticmethod
    def create():
        ...
```

Aufruf:

```python
UserFactory.create()
```

`@classmethod` erhält statt `self` die Klasse als `cls`:

```python
class User:

    @classmethod
    def create_default(cls):
        return cls()
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 8. Sichtbarkeit und Konventionen

Python hat kein direktes `public/protected/private` wie Java.

Konventionen:

```python
name       # öffentlich
_name      # intern / protected-artig
__name     # Name Mangling
```

Beispiel:

```python
def _build_system_prompt(self) -> str:
    ...
```

Das `_` signalisiert interne Nutzung.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 9. Collections

### Liste

```python
names: list[str] = []
names.append("Peter")
names = ["Peter", "Chris"]
```

### Set

```python
names: set[str] = {"Peter", "Chris"}
```

### Dictionary / Map

```python
ages: dict[str, int] = {
    "Peter": 40,
    "Chris": 30,
}

age = ages.get("Peter", 0)
```

`dict.get(key, default)` entspricht ungefähr `Map.getOrDefault()`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 10. Schleifen und Comprehensions

Java:

```java
for (String name : names) {
    System.out.println(name);
}
```

Python:

```python
for name in names:
    print(name)
```

Mit Index:

```python
for i, name in enumerate(names):
    print(i, name)
```

Range:

```python
for i in range(10):
    ...
```

List Comprehension:

```python
upper = [name.upper() for name in names]
```

Mit Filter:

```python
adults = [user for user in users if user.age >= 18]
```

Gedanklich ähnlich zu Java Streams.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 11. if, match und Truthiness

Python:

```python
if age >= 18:
    ...
elif age >= 16:
    ...
else:
    ...
```

`match` entspricht grob `switch`:

```python
match provider:
    case "ollama":
        return OllamaClient()
    case "openai":
        return OpenAIClient()
    case _:
        raise ValueError("Unsupported provider")
```

Truthiness:

```python
if name:
    ...
```

False-artig sind u. a. `None`, `""`, `[]`, `{}`, `0`, `False`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 12. None, Optional, == und is

Optionaler Wert:

```python
response_format: dict[str, Any] | None = None
```

Das bedeutet: `dict` oder `None`.

Wertvergleich:

```python
a == b
```

Objektidentität:

```python
a is b
```

Für `None`:

```python
if value is None:
    ...
```

Für Strings:

```python
if name == "Peter":
    ...
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 13. Exceptions und with

Java:

```java
throw new IllegalArgumentException("Invalid provider");
```

Python:

```python
raise ValueError("Invalid provider")
```

Try/Catch:

```python
try:
    ...
except OSError as error:
    ...
finally:
    ...
```

Context Manager:

```python
with open(path) as file:
    ...
```

`with` übernimmt Cleanup ähnlich zu `try-with-resources`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 14. Pathlib und Dateien

Java:

```java
Path path = Path.of("config.json");
String content = Files.readString(path);
```

Python:

```python
from pathlib import Path

path = Path("config.json")
content = path.read_text(encoding="utf-8")
```

Schreiben:

```python
path.write_text("Hallo", encoding="utf-8")
```

Ordner:

```python
directory.mkdir(parents=True, exist_ok=True)
```

Pfade verbinden:

```python
config_path = script_directory / "config.json"
```

Der `/`-Operator wird von `Path` überladen.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 15. dataclass und Pydantic

Java Record:

```java
public record User(String name, int age) {}
```

Python:

```python
from dataclasses import dataclass

@dataclass
class User:
    name: str
    age: int
```

Pydantic:

```python
from pydantic import BaseModel, Field

class ImprovedPrompt(BaseModel):
    goal: str
    scope: list[str] = Field(default_factory=list)
```

Pydantic bietet zusätzlich:

- Runtime-Validierung
- JSON-Parsing
- Serialisierung
- JSON-Schema
- strukturierte Fehler

Direkt aus JSON:

```python
ImprovedPrompt.model_validate_json(response)
```

JSON-Schema erzeugen:

```python
ImprovedPrompt.model_json_schema()
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 16. Decorators

Beispiele:

```python
@staticmethod
@classmethod
@abstractmethod
@dataclass
```

Als erste Denkbrücke kannst du sie mit Java-Annotations vergleichen. Technisch sind Python-Decorators mächtiger, weil sie Funktionen und Klassen wirklich verändern oder wrappen können.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 17. Imports, Packages und __init__.py

Java:

```java
import java.nio.file.Path;
```

Python:

```python
from pathlib import Path
```

Projektintern:

```python
from prompt.models import ImprovedPrompt
```

Package-Struktur:

```text
prompt/
├── __init__.py
├── models.py
└── prompt_improver.py
```

`__init__.py` kennzeichnet klassisch ein Python-Package.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 18. __main__ und argparse

```python
if __name__ == "__main__":
    main()
```

Bedeutung: `main()` nur ausführen, wenn die Datei direkt gestartet wurde.

CLI:

```python
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("prompt")
parser.add_argument("--provider", required=False)
parser.add_argument("--model", required=False)

args = parser.parse_args()
```

Dann:

```python
args.prompt
args.provider
args.model
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 19. Factory Pattern

Python:

```python
class LlmClientFactory:

    @staticmethod
    def create(provider: str, model: str) -> LlmClient:
        match provider.lower():
            case "ollama":
                return OllamaClient(model)

            case "openai":
                return OpenAIClient(model)

            case _:
                raise ValueError(
                    f"Unsupported LLM provider: {provider}"
                )
```

Konzeptionell dasselbe Factory Pattern wie in Java.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 20. Dependency Injection

Python:

```python
class PromptImprover:

    def __init__(
        self,
        config: PromptImproverConfig,
        llm_client: LlmClient,
    ):
        self.config = config
        self.llm_client = llm_client
```

Das ist normale Constructor Injection.

Java-Denkweise:

```java
public PromptImprover(
    PromptImproverConfig config,
    LlmClient llmClient
) {
    this.config = config;
    this.llmClient = llmClient;
}
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 21. Properties, Enums und Konstanten

Property:

```python
@property
def name(self) -> str:
    return self._name
```

Aufruf:

```python
user.name
```

Enum:

```python
from enum import Enum

class Provider(Enum):
    OLLAMA = "ollama"
    OPENAI = "openai"
```

Konstante per Konvention:

```python
MAX_RETRIES = 3
```

Großschreibung signalisiert: nicht verändern.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 22. Tuples, Slicing und len

Mehrere Rückgabewerte:

```python
def load_user():
    return "Chris", 30

name, age = load_user()
```

Tuple:

```python
coordinates: tuple[int, int] = (10, 20)
```

Slicing:

```python
values[:3]
values[3:]
values[-3:]
```

Länge:

```python
len(name)
len(values)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 23. JSON, Generics und Any

JSON:

```python
import json

data = json.loads(text)
text = json.dumps(data, indent=2)
```

Datei:

```python
data = json.loads(
    path.read_text(encoding="utf-8")
)
```

Generics:

```python
list[str]
dict[str, int]
tuple[str, int]
```

`Any`:

```python
from typing import Any

response_format: dict[str, Any]
```

Gedanklich ähnlich zu:

```java
Map<String, Object>
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 24. Prompt-Improver-Schnellreferenz

| Python | Java-Denkweise |
|---|---|
| `self` | `this` |
| `str` | `String` |
| `None` | `null` |
| `list[str]` | `List<String>` |
| `dict[str, Any]` | `Map<String,Object>` |
| `A \| None` | nullable / `Optional<A>` |
| `class X(Y)` | `extends` / `implements` |
| `ABC` | abstrakte Basisklasse / Interface |
| `@abstractmethod` | abstrakte / Interface-Methode |
| `@staticmethod` | `static` |
| `raise ValueError()` | `throw new IllegalArgumentException()` |
| `match` | `switch` |
| `f"{value}"` | String-Interpolation |
| `Path(...)` | `java.nio.file.Path` |
| `BaseModel` | validiertes DTO |
| `Field(default_factory=list)` | neue Liste pro Objekt |
| `-> str` | Rückgabetyp `String` |
| `_method()` | intern/private-artig |
| `if __name__ == "__main__"` | Programmeinstieg |
| `model_validate_json()` | JSON parse + validate |
| `model_json_schema()` | JSON-Schema erzeugen |
| `argparse` | CLI-Parser |
| `LlmClientFactory` | Factory Pattern |
| `PromptImprover(..., llm_client)` | Constructor Injection |

Aktuelle Architektur:

```text
CLI
 |
 v
Config + optionale Overrides
 |
 v
LlmClientFactory
 |
 +-- OllamaClient
 |
 +-- OpenAIClient
 |
 +-- ChatGPTClient   # geplant
 |
 v
PromptImprover
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
 v
PromptArtifactWriter
 |
 +-- original-prompt.md
 +-- improved-prompt.md
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 25. Typische Stolperfallen

### Einrückung ist Syntax

```python
if active:
    print("aktiv")
```

### Kein `new`

```python
client = OllamaClient(model)
```

### `None` mit `is`

```python
value is None
```

### Mutable Defaults vermeiden

Problematisch:

```python
def add(value, values=[]):
    ...
```

Besser:

```python
def add(value, values=None):
    if values is None:
        values = []
```

Oder bei Pydantic:

```python
Field(default_factory=list)
```

### Kein klassisches Methoden-Overloading wie in Java

Oft mit Defaults:

```python
def greet(name: str, prefix: str = "Hallo") -> str:
    return f"{prefix} {name}"
```

### Type Hints sind nicht automatisch Runtime-Validierung

Pydantic ist für Runtime-Validierung gedacht.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 26. PyCharm-Tipps

### Structure

```text
View → Tool Windows → Structure
```

Shortcut:

```text
Alt+7
```

### Inlay Hints

```text
Settings → Editor → Inlay Hints
```

Für Python hilfreich:

- Parameter names
- Types
- Values
- Other

### Quick Documentation

```text
Ctrl+Q
```

### Definition öffnen

```text
Ctrl+B
```

oder `Ctrl+Click`.

### Source Root

Für package-basierte Imports kann `.opencode/scripts` als **Sources Root** markiert werden.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

# Merksatz

> Nutze Java als Denkbrücke, aber schreibe Python nicht künstlich wie Java.

Für unser Prompt-Improver-Projekt verwenden wir bewusst eine strukturierte, objektorientierte Python-Variante mit Type Hints, Pydantic, Factory Pattern und Dependency Injection.
