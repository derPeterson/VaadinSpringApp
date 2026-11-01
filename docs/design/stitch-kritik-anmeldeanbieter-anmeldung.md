# Stitch-Kritik zu Anmeldeanbietern und Anmeldung

Stand: 1. November 2026. Die Entscheidung ist vertagt. Die Empfehlungen sind noch
nicht zur Umsetzung freigegeben; die bestehende Oberfläche bleibt unverändert.

Die folgende vollständige Antwort stammt von Stitch und bewertet die hochgeladenen
Ist-Screenshots. Ihre Prioritäten und Gestaltungsvorschläge sind die Einschätzung
von Stitch, keine bereits gemeinsam beschlossenen Änderungen.

- [Anmeldeanbieter: bewerteter Ist-Screenshot](https://stitch.withgoogle.com/projects/43914967159269113?node-id=12157834669425121719)
- [Anmeldung mit ODER-Trenner: bewerteter Ist-Screenshot](https://stitch.withgoogle.com/projects/43914967159269113?node-id=13517430824104027733)
- Zugehöriger [offener Umsetzungsplan](../planung/google-microsoft-anmeldung.md).

## Noch zu entscheiden

Die Anmeldeanbieter-Seite und der ODER-Trenner werden später gemeinsam besprochen.
Es wurde keine der vorgeschlagenen Änderungen ausgewählt. Die Rückmeldung wurde
als reine Textkritik geliefert; neue Design-Screens sind dabei nicht entstanden.

## Vollständige Antwort von Stitch

Hier ist die fundierte fachliche Designkritik zu den beiden konkreten IST-Screenshots (Screen 12157834669425121719 und Screen 13517430824104027733). Wie ausdrücklich gewünscht, erfolgt die Rückmeldung als reine Textanalyse ohne Generierung oder Bearbeitung von Bild- oder Screen-Artefakten.

---

# TEIL A: Adminseite „Anmeldeanbieter“ (Screen 12157834669425121719)

### 1. Was ist bereits gut gelungen? (Bewusst so beibehalten)
- **Tonalität und Cosmos-Farbklima:** Die dunkle Ästhetik (#050811 / #080e1b / #091120) mit den dezenten Rändern fügt sich nahtlos in die Administration ein.
- **Konsistente Tab-Struktur:** Die Aufteilung nach Providern (*Google* / *Microsoft*) verhindert ein unübersichtliches Endlosformular.
- **Logische Dreiteilung:** Die Trennung in *(1) Anbieter einrichten*, *(2) Zugangsdaten hinterlegen* und *(3) Anmeldung aktivieren* bildet den realen Einrichtungsablauf fachlich sauber und nachvollziehbar ab.
- **Aktionsleiste im Footer:** Der Kontrast zwischen sekundärem *„Werte neu laden“* und primärem *„Einstellungen speichern“* ist eindeutig und etabliert.

### 2. Was wirkt unklar oder visuell unruhig?
- **Statusbadges vs. Tab-Status:** Oben rechts im Hauptpanel stehen zwei Badges nebeneinander (`Google AKTIV`, `Microsoft AKTIV`). Da man sich aber bereits im Tab *Google* befindet, irritiert die parallele Statusanzeige beider Anbieter an dieser Stelle. Sie wirkt wie eine globale Schaltfläche oder doppelte Information.
- **Hierarchischer Bruch bei Schritt 1:** Die Ziffer `(1)` leitet einen langen Fließtext ein, gefolgt vom externen Link und erst darunter dem Eingabe-/Kopierfeld für die Rücksprungadresse. Das eigentliche Werkzeug (die URL mit Kopier-Button) rutscht dadurch optisch zu weit nach unten.
- **Vertikale Ausrichtung von Schritt 3:** Die Checkbox *„Anmeldung erlauben“* sitzt in einem eigenen dunklen Container, der im Vergleich zu den Feldern in Schritt 2 optisch sehr dominant wirkt, obwohl es sich lediglich um einen einzelnen Umschalter handelt.

### 3. Strukturierte Empfehlungen nach Priorität

#### Wirklich wichtig (Klarer Usability- & Hierarchie-Gewinn)
1. **Statusbadge direkt an den Reiter binden:**
   Entfernen Sie die gedoppelten Badges oben rechts aus dem Header-Bereich. Platzieren Sie den Status stattdessen direkt in den jeweiligen Tab-Reiter (z. B. `[Google | Aktiv]` und `[Microsoft | Aktiv]`). Dadurch sieht der Administrator auf einen Blick den Status beider Anbieter, ohne dass der Panel-Kopf überfrachtet wird.
2. **Schritt 1 straffen & Handlungsfokus setzen:**
   Der Fließtext vor der Rücksprungadresse sollte auf das Wesentliche gekürzt oder typografisch untergeordnet werden. Die Reihenfolge: Zuerst das Kopierfeld der Rücksprungadresse prominent anbieten, darunter der Link *„Anbieter-Verwaltung öffnen ↗“*. Das entspricht dem realen Arbeitsfluss (URL kopieren -> Konsole öffnen -> dort einfügen).

#### Sinnvoll (Visuelle Ruhe & Konsistenz)
3. **Schritt 3 vereinfachen:**
   Verzichten Sie auf den großen separaten Kasten für die Checkbox. Ein schlanker Toggle-Switch oder eine Standard-Checkboxzeile mit Label auf gleicher Einrückungslinie wie die Felder in Schritt 2 beruhigt das Gesamtbild erheblich.
4. **Schritt-Indikatoren vereinheitlichen:**
   Die nummerierten Badges `(1)`, `(2)`, `(3)` heben sich durch ihre Linienführung etwas unruhig von den Vaadin-Standardlabels ab. Eine rein typografische Zwischenüberschrift (z. B. 12px Versalien in Cyan/Muted) schafft mehr Ruhe.

#### Reine Geschmackssache (Optionale Nuancen)
5. **Hinweistext im Fußbereich:**
   Die Info-Box vor den Speicherbuttons ist sachlich richtig. Ob sie als volle Rahmenbox oder als schlanke Zeile mit vorangestelltem Info-Icon dargestellt wird, ist sekundär.

### Fazit Teil A
Ein fachlich bereits sehr reifer, logischer Administrationsdialog. Die größte Hebelwirkung liegt darin, die Statusbadges oben rechts aufzulösen und direkt in die Tabs zu integrieren sowie Schritt 1 visuell kompakter zu strukturieren.

---

# TEIL B: Anmeldung mit ODER-Trenner (Screen 13517430824104027733)

### 1. Detaillierte Wirkung des aktuellen Trenners im Screenshot
- **Störende Pillen-Optik („Badge“-Charakter):** Das umrandete, zentrierte `ODER` besitzt eine abgerundete Kapselform mit hellem Rahmen. Dadurch wirkt es auf den ersten Blick wie ein eigenständiges, klickbares UI-Element (wie ein Tag, Chip oder Button) und konkurriert mit den tatsächlichen Interaktionselementen.
- **Höhenkonflikt der Trennlinie:** Das linke Formularpanel ist deutlich höher als das rechte Schnellzugangs-Panel. Die vertikale Trennlinie orientiert sich in ihrer Länge an der Gesamthöhe, endet aber nach oben und unten unvermittelt im leeren Raum. Da das rechte Panel vertikal zentriert daneben „schwebt“, wirkt die harte Trennlinie wie ein Fremdkörper, der die asymmetrische Balance der beiden Boxen eher betont als harmonisiert.
- **Ablenkung vom Hauptfokus:** Das Auge wird durch den starken Kontrast des ODER-Ovals unwillkürlich in die Mitte gezogen – weg vom eigentlichen Login-Formular und weg vom Google-Button.

### 2. Klare Richtungsentscheidung & Begründung

**Empfohlene Richtung: Vereinfachen (Pille entfernen, dezent typografisch trennen bzw. auflösen).**

**Begründung:**
Ein vollständiges Weglassen der Trennung birgt bei zwei nebeneinander stehenden dunklen Boxen das Risiko, dass der rechte Kasten isoliert und unverbunden wirkt. Eine massive Linie mit ODER-Pille ist jedoch visueller Ballast.
Die beste Lösung für diese asymmetrische 2-Panel-Komposition:
- **Keine eigenständige Kapsel:** Das Wort „oder“ (bzw. kein Text) wird nicht in eine Badge eingefasst.
- **Keine starre Mittelachse:** Wenn überhaupt eine Linie gewünscht ist, reicht eine hauchdünne, nach oben und unten transparent auslaufende Linie (Fading Gradient in Border-Farbe).
- **Noch besser und natürlicher:** Die Trennung erfolgt primär über den bestehenden Panel-Aufbau selbst. Da der rechte Kasten bereits eine eigene Überschrift (*SCHNELLZUGANG / Alternativ fortfahren*) besitzt, ist die inhaltliche Abgrenzung redundant doppelt gemoppelt. Ein schlichter horizontaler Abstand (z. B. 32px oder 40px Whitespace) zwischen den beiden Panels reicht völlig aus. Falls eine sprachliche Trennung gewünscht bleibt, genügt ein dezenter, kleiner Text `oder` in Sekundärfarbe (`#94a3b8`) ohne Rahmen und ohne Hintergrund.

### 3. Übertragung auf die gestapelte Ansicht (untereinander)
Bei schmalen Displays bzw. gestapelter Ansicht (z. B. Smartphone oder responsiver Umbruch):
- Das lokale Formular steht oben, die Absendeaktion schließt es sauber ab.
- Darunter folgt eine schlichte horizontale Trennlinie mit dezent zentriertem Text `oder` in Formatsprache des Sekundärtextes (12px, keine Box, keine Pille).
- Darunter folgt die Anbieter-Zone (mit Google und ggf. Microsoft).
- Das garantiert einen natürlichen Lesefluss von oben nach unten, ohne dass der Eindruck entsteht, man müsse erst etwas „überwinden“ oder auswählen.

### Fazit Teil B
Das aktuelle ODER-Element mit Kapsel und harter Linie wirkt technisch und wie ein drittes Bedienelement. Das Entfernen der Umrandung und die Reduktion auf reinen Weißraum oder eine ganz zarte, auslaufende Linie beruhigt die Anmeldeseite sofort und lässt die beiden Panels harmonisch nebeneinander wirken.

---

### Gesamtresümee
Beide Screens besitzen ein solides, konsistentes Fundament im Cosmos-Design. Beide profitieren vor allem von visueller Entschlackung: in **Screen A** durch Zusammenführen von Tab und Status sowie Straffung von Schritt 1; in **Screen B** durch Abschaffen der aufdringlichen ODER-Kapsel zugunsten von ruhigem Whitespace.
