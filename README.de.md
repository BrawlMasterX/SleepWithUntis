# SleepWithUntis (Deutsch)

<p align="center">
  <a href="README.de.md">
    <img src="https://img.shields.io/badge/Language-Deutsch-red?style=for-the-badge" alt="Deutsch">
  </a>
  <a href="README.md">
    <img src="https://img.shields.io/badge/Language-English-blue?style=for-the-badge" alt="English">
  </a>
</p>

---

Eine Android-Wecker-App, die dich automatisch basierend auf deinem WebUntis-Stundenplan weckt.

---

### ✨ Funktionen & Vorschau

| Startbildschirm | Einstellungen |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/6a8c9bfb-749e-47bc-957b-b2a597f9252f" width="350"> | <img src="https://github.com/user-attachments/assets/806cb7d3-2313-4d2a-9bc7-c09080bef178" width="350"> |
| **Minimalistisches Design**: Tippe auf die Weckzeit, um eine Toast-Benachrichtigung zu sehen, die dir genau zeigt, wie viel Zeit bis zum nächsten Alarm verbleibt. | **Volle Kontrolle**: Passe das App-Verhalten und deine Vorlieben schnell über das übersichtliche Einstellungsmenü an. |

---

| Login & Synchronisation | Alarm-Einrichtung |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/42e60006-3ea8-4f14-a492-c3340f676291" width="350"> | <img src="https://github.com/user-attachments/assets/c37a4489-c3e3-4e1b-8316-3351dbc0b6fb" width="350"> |
| **Einfacher Zugriff**: Melde dich einfach mit deinen Zugangsdaten an und suche nach deiner Schule, um deinen Stundenplan zu synchronisieren. | **Personalisierter Weckruf**: Passe die Schlummerdauer an oder wähle mehrere Alarmtöne – die App wählt zufällig einen aus, um deine Morgenroutine frisch zu halten. |

---

### Flexible Weck-Modi

Wechsle zwischen einer allgemeinen Vorbereitungszeit oder spezifischen Zeiten für jede Unterrichtsstunde über den Schalter oben rechts.

| Vorbereitung (Early Minutes) | Individuelle Stundenzeiten |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/4ffa48a7-5bc1-4774-8eb8-c27771d683d7" width="350"> | <img src="https://github.com/user-attachments/assets/a8ef6992-f1af-4014-a498-3fc24e379429" width="350"> |
| Lege eine feste Anzahl an "Vorlaufminuten" fest, die du vor Beginn deiner ersten Schulstunde benötigst. | Definiere für jede einzelne Schulstunde eine eigene Weckzeit für maximale Präzision. |

---

### 🛠️ Tech Stack
* **Sprache:** Kotlin für die App / Java für die API
* **Plattform:** Android
* **API-Anbindung:** [WebUntisAPI von Keule0010](https://github.com/Keule0010/WebUntisAPI) (von mir modifiziert)

#### Technische Modifikationen
Ich habe spezifische Anpassungen an der originalen WebUntisAPI vorgenommen, um die Kompatibilität mit Android sicherzustellen:
* **JSON-Parameter Fix:** Die `json`-Aufrufe wurden korrigiert (Entfernung des zweiten `null`-Parameters), da die Android-Standard-JSON-Implementierung sonst Signatur-Fehler verursachte.
* **Fehlerbehandlung:** Explizite `Exception`-Handhabung und `throws`-Deklarationen zu den Kernmethoden hinzugefügt.

---

### 💡 Tasker-Integration
Diese App sendet zwei verschiedene Broadcast-Intents, die Tasker empfangen kann. Dies ermöglicht es dir, dein Smart Home oder deine Geräteeinstellungen perfekt synchron zum Stundenplan zu automatisieren.

**Voraussetzungen in Tasker:**
* Drei Punkte (oben rechts) → Einstellungen.
* Reiter "Diverse".
* "Externen Zugriff erlauben" aktivieren.

**Verfügbare Trigger:**
* **Profil:** Ereignis → System → Intent Empfangen
* **Aktion:** `com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE` (5 Minuten Vorlauf, z. B. für Lichtwecker)
* **Aktion:** `com.sleepwithuntis.app.ACTION_ALARM_NOW` (Beim Auslösen des Alarms, z. B. für Rolläden)

---

### 📜 Urheberrecht & Lizenz
Copyright (c) 2026 BrawlMasterX. Alle Rechte vorbehalten.
Diese Software und ihr Quellcode werden nur für Bildungszwecke und den persönlichen Gebrauch zur Verfügung gestellt. Das unbefugte Kopieren, Modifizieren oder Veröffentlichen (insbesondere das Hochladen in den Google Play Store) ist ohne vorherige schriftliche Genehmigung des Autors strengstens untersagt.

---

### ⚠️ Haftungsausschluss
Dies ist eine inoffizielle Anwendung und steht in keiner Verbindung zur Untis GmbH. Die Nutzung erfolgt auf eigene Gefahr. Der Entwickler haftet nicht für versäumten Unterricht oder Termine aufgrund technischer Fehler.
