# Java → Python CheatSheet

Ein ausführliches Nachschlagewerk für Java-Entwickler, die Python lesen und schreiben möchten.

> Ziel: Java als Denkbrücke verwenden, ohne Python künstlich wie Java zu schreiben.
>
> Enthält außerdem eine Schnellreferenz für unser aktuelles Prompt-Improver-Projekt.

---

## Inhaltsverzeichnis

1. [Grundsyntax](#1-grundsyntax)
2. [Variablen und Typen](#2-variablen-und-typen)
3. [Strings](#3-strings)
4. [Methoden und Funktionen](#4-methoden-und-funktionen)
5. [Klassen](#5-klassen)
6. [Was ist `self`?](#6-was-ist-self)
7. [Vererbung](#7-vererbung)
8. [Interface / Abstract Class](#8-interface--abstract-class)
9. [`pass`](#9-pass)
10. [Static Methods](#10-static-methods)
11. [Class Methods](#11-class-methods)
12. [Sichtbarkeit](#12-sichtbarkeit)
13. [Listen](#13-listen)
14. [Maps / Dictionaries](#14-maps--dictionaries)
15. [Schleifen](#15-schleifen)
16. [`range()`](#16-range)
17. [`if`](#17-if)
18. [`switch` → `match`](#18-switch--match)
19. [Exceptions](#19-exceptions)
20. [`try-with-resources` → `with`](#20-try-with-resources--with)
21. [Dateien und `Path`](#21-dateien-und-path)
22. [Pfade verbinden](#22-pfade-verbinden)
23. [Optional / `None`](#23-optional--none)
24. [`or` als Default](#24-or-als-default)
25. [Truthiness](#25-truthiness)
26. [`==` vs. `is`](#26--vs-is)
27. [List Comprehension](#27-list-comprehension)
28. [Lambda](#28-lambda)
29. [`dataclass`](#29-dataclass)
30. [Pydantic](#30-pydantic)
31. [`Field(default_factory=list)`](#31-fielddefault_factorylist)
32. [Decorators](#32-decorators)
33. [Imports](#33-imports)
34. [Packages](#34-packages)
35. [`if __name__ == "__main__"`](#35-if-__name__--__main__)
36. [CLI mit `argparse`](#36-cli-mit-argparse)
37. [Factory Pattern](#37-factory-pattern)
38. [Dependency Injection](#38-dependency-injection)
39. [Properties / Getter und Setter](#39-properties--getter-und-setter)
40. [Enum](#40-enum)
41. [Konstanten](#41-konstanten)
42. [Mehrere Rückgabewerte](#42-mehrere-rückgabewerte)
43. [Tuple](#43-tuple)
44. [Slicing](#44-slicing)
45. [String-/Collection-Länge](#45-string-collection-länge)
46. [String-Methoden](#46-string-methoden)
47. [JSON](#47-json)
48. [Generics](#48-generics)
49. [`Any`](#49-any)
50. [Python ist nicht Java ohne Semikolons](#50-python-ist-nicht-java-ohne-semikolons)
51. [Prompt-Improver-Schnellreferenz](#51-prompt-improver-schnellreferenz)
52. [Typische Stolperfallen für Java-Entwickler](#52-typische-stolperfallen-für-java-entwickler)
53. [PyCharm-Tipps](#53-pycharm-tipps)

---

## 1. Grundsyntax

| Java | Python |
|---|---|
| `{ ... }` | Einrückung |
| `;` | entfällt |
| `// Kommentar` | `# Kommentar` |
| `/* ... */` | meist `"""..."""` für längere Doku/Texte |
| `null` | `None` |
| `true / false` | `True / False` |
| `&&` | `and` |
| `||` | `or` |
| `!x` | `not x` |

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

Für `None` bevorzugt:

```python
is None
is not None
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
name = "Chris"
age = 30
active = True
```

Mit Type Hints:

```python
name: str = "Chris"
age: int = 30
active: bool = True
```

| Java | Python |
|---|---|
| `String` | `str` |
| `int` | `int` |
| `long` | `int` |
| `double` | `float` |
| `boolean` | `bool` |
| `Object` | `Any` |
| `List<String>` | `list[str]` |
| `Set<String>` | `set[str]` |
| `Map<String, Integer>` | `dict[str, int]` |
| `String[]` | `list[str]` |
| `Optional<String>` | `str \| None` |

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 3. Strings

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
Das ist
ein mehrzeiliger
String.
"""
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 4. Methoden und Funktionen

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

Lesart:

```text
def          → Funktion / Methode definieren
name: str    → Parameter soll str sein
-> str       → Rückgabewert soll str sein
```

`void` entspricht:

```python
def save() -> None:
    ...
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 5. Klassen

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

Instanz:

```python
service = UserService("Test")
```

Kein `new`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 6. Was ist `self`?

Java:

```java
this.model = model;
```

Python:

```python
self.model = model
```

`self` ist die aktuelle Instanz.

```python
def chat(self, prompt: str) -> str:
    ...
```

Aufruf:

```python
client.chat("Hallo")
```

Gedanklich macht Python ungefähr:

```python
OllamaClient.chat(client, "Hallo")
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 7. Vererbung

Java:

```java
class Dog extends Animal {
}
```

Python:

```python
class Dog(Animal):
    pass
```

In unserem Projekt:

```python
class OllamaClient(LlmClient):
```

Gedanklich kann man das wie `implements LlmClient` lesen.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 8. Interface / Abstract Class

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

`@abstractmethod` = Unterklassen müssen die Methode implementieren.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 9. `pass`

Java erlaubt:

```java
void method() {
}
```

Python braucht für einen absichtlich leeren Block:

```python
def method():
    pass
```

`pass` bedeutet: absichtlich nichts tun.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 10. Static Methods

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

Bei einer `@staticmethod` gibt es kein `self`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 11. Class Methods

Python hat zusätzlich:

```python
class User:

    @classmethod
    def create_default(cls):
        return cls()
```

`cls` referenziert die aktuelle Klasse.

Das gibt es in Java nicht direkt in derselben Form.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 12. Sichtbarkeit

Java:

```java
public
protected
private
```

Python verwendet hauptsächlich Konventionen:

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

## 13. Listen

Java:

```java
List<String> names = new ArrayList<>();
names.add("Peter");
```

Python:

```python
names: list[str] = []
names.append("Peter")
```

Direkt:

```python
names = ["Peter", "Chris", "Anna"]
```

Erstes Element:

```python
names[0]
```

Letztes Element:

```python
names[-1]
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 14. Maps / Dictionaries

Java:

```java
Map<String, Integer> ages = new HashMap<>();
ages.put("Peter", 40);
```

Python:

```python
ages: dict[str, int] = {}
ages["Peter"] = 40
```

Direkt:

```python
ages = {
    "Peter": 40,
    "Chris": 30,
}
```

Mit Default:

```python
age = ages.get("Peter", 0)
```

entspricht ungefähr:

```java
ages.getOrDefault("Peter", 0);
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 15. Schleifen

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

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 16. `range()`

Java:

```java
for (int i = 0; i < 10; i++) {
}
```

Python:

```python
for i in range(10):
    ...
```

Von 5 bis 9:

```python
range(5, 10)
```

Mit Zweierschritten:

```python
range(0, 10, 2)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 17. `if`

Java:

```java
if (age >= 18) {
    ...
} else if (age >= 16) {
    ...
} else {
    ...
}
```

Python:

```python
if age >= 18:
    ...
elif age >= 16:
    ...
else:
    ...
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 18. `switch` → `match`

Java:

```java
switch (provider) {
    case "ollama":
        return new OllamaClient();
    case "openai":
        return new OpenAIClient();
    default:
        throw new IllegalArgumentException();
}
```

Python:

```python
match provider:
    case "ollama":
        return OllamaClient()

    case "openai":
        return OpenAIClient()

    case _:
        raise ValueError("Unsupported provider")
```

`case _` entspricht dem `default`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 19. Exceptions

Java:

```java
throw new IllegalArgumentException("Invalid provider");
```

Python:

```python
raise ValueError("Invalid provider")
```

Java:

```java
try {
    ...
} catch (IOException e) {
    ...
} finally {
    ...
}
```

Python:

```python
try:
    ...
except OSError as error:
    ...
finally:
    ...
```

Python kennt keine Checked Exceptions wie Java.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 20. `try-with-resources` → `with`

Java:

```java
try (var stream = Files.newInputStream(path)) {
    ...
}
```

Python:

```python
with open(path) as file:
    ...
```

`with` sorgt automatisch für Cleanup.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 21. Dateien und `Path`

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
path.write_text(
    "Hallo",
    encoding="utf-8",
)
```

Ordner:

```python
directory.mkdir(
    parents=True,
    exist_ok=True,
)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 22. Pfade verbinden

Java:

```java
path.resolve("config.json");
```

Python:

```python
path / "config.json"
```

Beispiel:

```python
config_path = script_directory / "config.json"
```

`Path` überlädt den `/`-Operator.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 23. Optional / `None`

Python:

```python
response_format: dict[str, Any] | None = None
```

Gedanklich:

```java
Map<String, Object> responseFormat = null;
```

oder:

```java
Optional<Map<String, Object>>
```

Der `|` bedeutet: `dict` oder `None`.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 24. `or` als Default

Unser Code:

```python
provider = args.provider or config.provider
```

Gedanklich ungefähr:

```java
String provider =
    args.getProvider() != null
        ? args.getProvider()
        : config.getProvider();
```

Python verwendet dabei Truthiness.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 25. Truthiness

Python kann viele Werte direkt als Boolean betrachten.

False-artig sind:

```python
None
""
[]
{}
0
False
```

Beispiel:

```python
if name:
    print(name)
```

Java wäre eher:

```java
if (name != null && !name.isEmpty()) {
    ...
}
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 26. `==` vs. `is`

```python
a == b
```

prüft den Wert.

```python
a is b
```

prüft Objektidentität.

Für `None`:

```python
value is None
```

Für Strings:

```python
name == "Peter"
```

Nicht:

```python
name is "Peter"
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 27. List Comprehension

Java Streams:

```java
List<String> upper =
    names.stream()
        .map(String::toUpperCase)
        .toList();
```

Python:

```python
upper = [
    name.upper()
    for name in names
]
```

Mit Filter:

```python
adults = [
    user
    for user in users
    if user.age >= 18
]
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 28. Lambda

Java:

```java
x -> x.toUpperCase()
```

Python:

```python
lambda x: x.upper()
```

Beispiel:

```python
sorted(
    users,
    key=lambda user: user.name,
)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 29. `dataclass`

Java Record:

```java
public record User(
    String name,
    int age
) {}
```

Python:

```python
from dataclasses import dataclass


@dataclass
class User:
    name: str
    age: int
```

Konzeptionell sehr nah an Java Records.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 30. Pydantic

Unser Projekt nutzt:

```python
from pydantic import BaseModel


class ImprovedPrompt(BaseModel):
    goal: str
    scope: list[str]
```

Gedanklich ein DTO plus:

- Runtime-Validierung
- JSON-Parsing
- JSON-Schema-Erzeugung
- Serialisierung
- strukturierte Fehler

Direkt validieren:

```python
ImprovedPrompt.model_validate_json(response)
```

Schema erzeugen:

```python
ImprovedPrompt.model_json_schema()
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 31. `Field(default_factory=list)`

```python
scope: list[str] = Field(default_factory=list)
```

Bedeutet:

> Wenn nichts angegeben wurde, erzeuge eine neue leere Liste.

Das ist sauberer als ein geteilter mutable Default.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 32. Decorators

Beispiele:

```python
@staticmethod
@classmethod
@abstractmethod
@dataclass
```

Als erste Denkbrücke kannst du sie mit Java-Annotations vergleichen.

Technisch sind Decorators mächtiger, weil sie Funktionen/Klassen tatsächlich verändern oder wrappen können.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 33. Imports

Java:

```java
import java.nio.file.Path;
```

Python:

```python
from pathlib import Path
```

Oder:

```python
import pathlib
```

Dann:

```python
pathlib.Path(...)
```

Projektintern:

```python
from prompt.models import ImprovedPrompt
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 34. Packages

Python-Package:

```text
prompt/
├── __init__.py
├── models.py
└── prompt_improver.py
```

`__init__.py` kennzeichnet klassisch ein Python-Package.

Package-basierter Import:

```python
from prompt.models import ImprovedPrompt
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 35. `if __name__ == "__main__"`

```python
if __name__ == "__main__":
    main()
```

Bedeutung:

> Führe `main()` nur aus, wenn die Datei direkt gestartet wird.

Beim Import wird `main()` nicht automatisch ausgeführt.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 36. CLI mit `argparse`

```python
import argparse

parser = argparse.ArgumentParser()

parser.add_argument(
    "prompt",
    help="The original user prompt.",
)

parser.add_argument(
    "--provider",
    required=False,
)

args = parser.parse_args()
```

Aufruf:

```text
"Improve email normalization." --provider ollama
```

Dann:

```python
args.prompt
args.provider
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 37. Factory Pattern

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

## 38. Dependency Injection

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

Java:

```java
public PromptImprover(
    PromptImproverConfig config,
    LlmClient llmClient
) {
    this.config = config;
    this.llmClient = llmClient;
}
```

Das ist Constructor Injection.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 39. Properties / Getter und Setter

Java:

```java
public String getName() {
    return name;
}
```

Python braucht häufig keinen Getter:

```python
user.name
```

Falls Logik nötig ist:

```python
@property
def name(self) -> str:
    return self._name
```

Der Aufrufer nutzt trotzdem:

```python
user.name
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 40. Enum

Java:

```java
enum Provider {
    OLLAMA,
    OPENAI
}
```

Python:

```python
from enum import Enum


class Provider(Enum):
    OLLAMA = "ollama"
    OPENAI = "openai"
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 41. Konstanten

Java:

```java
private static final int MAX_RETRIES = 3;
```

Python:

```python
MAX_RETRIES = 3
```

Großschreibung signalisiert per Konvention: nicht verändern.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 42. Mehrere Rückgabewerte

Python:

```python
def load_user():
    return "Chris", 30
```

Aufruf:

```python
name, age = load_user()
```

Technisch wird ein Tuple zurückgegeben.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 43. Tuple

```python
coordinates = (10, 20)
```

Typ:

```python
tuple[int, int]
```

Ein Tuple ist eine geordnete, unveränderliche Sequenz.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 44. Slicing

```python
values[0:3]
```

liefert Index 0 bis 2.

Weitere Beispiele:

```python
values[:3]
values[3:]
values[-3:]
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 45. String-/Collection-Länge

Java:

```java
name.length();
list.size();
```

Python:

```python
len(name)
len(values)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 46. String-Methoden

Java:

```java
name.trim();
name.toLowerCase();
name.toUpperCase();
```

Python:

```python
name.strip()
name.lower()
name.upper()
```

Unser Prompt-Improver:

```python
raw_prompt = raw_prompt.strip()
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 47. JSON

Python Standardbibliothek:

```python
import json

data = json.loads(text)
```

Datei:

```python
data = json.loads(
    path.read_text(encoding="utf-8")
)
```

Schreiben:

```python
text = json.dumps(
    data,
    indent=2,
)
```

Mit Pydantic:

```python
ImprovedPrompt.model_validate_json(text)
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 48. Generics

Java:

```java
List<String>
Map<String, Object>
```

Python:

```python
list[str]
dict[str, Any]
```

Methodenbeispiel:

```python
def process(
    values: list[str],
) -> dict[str, int]:
    ...
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 49. `Any`

```python
from typing import Any
```

Beispiel:

```python
dict[str, Any]
```

Gedanklich ungefähr:

```java
Map<String, Object>
```

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 50. Python ist nicht Java ohne Semikolons

Python setzt stärker auf:

- Duck Typing
- weniger Boilerplate
- Funktionen als echte Objekte
- Konvention statt Zugriffsschutz
- dynamische Typisierung
- kleine Module
- expressive Syntax

Du kannst Python trotzdem sehr strukturiert schreiben:

- Klassen
- ABCs / Interfaces
- Dependency Injection
- Factory Pattern
- Type Hints
- Pydantic
- Packages
- Tests

Für unser Projekt ist genau das sinnvoll.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 51. Prompt-Improver-Schnellreferenz

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

## 52. Typische Stolperfallen für Java-Entwickler

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

Oder mit Pydantic:

```python
Field(default_factory=list)
```

### Kein klassisches Methoden-Overloading wie in Java

Oft mit Default-Parametern:

```python
def greet(
    name: str,
    prefix: str = "Hallo",
) -> str:
    return f"{prefix} {name}"
```

### Type Hints sind nicht automatisch Runtime-Validierung

Pydantic übernimmt in unserem Projekt die Runtime-Validierung.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

## 53. PyCharm-Tipps

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

### Refactor → Move

PyCharm kann beim Verschieben von Python-Dateien viele Imports automatisch anpassen.

### Source Root

Für package-basierte Imports kann z. B.

```text
.opencode/scripts
```

als **Sources Root** markiert werden.

[Zurück zum Inhaltsverzeichnis](#inhaltsverzeichnis)

---

# Merksatz

> Nutze Java als Denkbrücke, aber versuche nicht, Python exakt wie Java zu schreiben.

Für unser Prompt-Improver-Projekt verwenden wir bewusst eine strukturierte, objektorientierte Python-Variante mit Type Hints, Pydantic, Factory Pattern und Dependency Injection.
