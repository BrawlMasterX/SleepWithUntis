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

### ✨ Funktionen & Vorschau

| Startbildschirm | Einstellungen |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/6a8c9bfb-749e-47bc-957b-b2a597f9252f" width="350"> | <img src="https://github.com/user-attachments/assets/806cb7d3-2313-4d2a-9bc7-c09080bef178" width="350"> |
| **Minimalistisches Design**: Tippe auf die Weckzeit, um eine Toast-Benachrichtigung zu sehen, die genau anzeigt, wie viel Zeit bis zum nächsten Alarm verbleibt. | **Volle Kontrolle**: Passe das App-Verhalten und deine Vorlieben schnell über das übersichtliche Einstellungsmenü an. |

---

| Login & Synchronisation | Alarm-Einrichtung |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/42e60006-3ea8-4f14-a492-c3340f676291" width="350"> | <img src="https://github.com/user-attachments/assets/c37a4489-c3e3-4e1b-8316-3351dbc0b6fb" width="350"> |
| **Einfacher Zugriff**: Melde dich einfach mit deinen Zugangsdaten an und suche nach deiner Schule, um deinen Stundenplan zu synchronisieren. | **Personalisiertes Aufwachen**: Passe die Schlummerdauer an oder wähle mehrere Alarmtöne – die App wählt zufällig einen aus, um deine Morgen abwechslungsreich zu gestalten. |

---

### Flexible Weckmodi

Wechsle über den Schalter oben rechts zwischen einer allgemeinen Vorbereitungszeit oder spezifischen Zeiten für jede Unterrichtsstunde.

| Vorbereitung (Frühe Minuten) | Individuelle Stundenzeiten |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/4ffa48a7-5bc1-4774-8eb8-c27771d683d7" width="350"> | <img src="https://github.com/user-attachments/assets/a8ef6992-f1af-4014-a498-3fc24e379429" width="350"> |
| Lege eine feste Anzahl an "Frühen Minuten" fest, die du vor Beginn deiner ersten Schulstunde benötigst. | Definiere für jede einzelne Schulstunde eine eigene Weckzeit für maximale Präzision. |

---

### 🛠️ Tech Stack
* **Sprache:** Kotlin für die App / Java für die API
* **Plattform:** Android
* **API-Anbindung:** [WebUntisAPI von Keule0010](https://github.com/Keule0010/WebUntisAPI) (Von mir modifiziert)

#### Technische Modifikationen
Ich habe spezifische Anpassungen an der ursprünglichen WebUntisAPI vorgenommen, um die Kompatibilität mit der Android-Umgebung sicherzustellen:
* **JSON-Parameter-Fix:** Die `json`-Aufrufe wurden durch Entfernen des zweiten `null`-Parameters angepasst. Dies war notwendig, da die Standard-Android-JSON-Implementierung nur einen einzigen Parameter akzeptiert und der zusätzliche Null-Wert zu einem Signaturfehler führte.
* **Fehlerbehandlung:** Explizite `Exception`-Behandlung und `throws`-Deklarationen wurden zu den Kernmethoden hinzugefügt, um sicherzustellen, dass die App unter den strengen Fehlerbehandlungsanforderungen von Android korrekt kompiliert.

---

### 💡 Tasker Integration
Diese App sendet zwei verschiedene Broadcast-Intents, auf die Tasker reagieren kann. Dies ermöglicht es dir, dein Smart Home oder deine Geräteeinstellungen perfekt synchronisiert mit deinem Stundenplan zu automatisieren.

**Voraussetzungen in Tasker:**
* Tippe auf die drei Punkte (oben rechts) → Einstellungen (Preferences).
* Go to the "Misc" tab.
* Enable "Allow External Access".

**Verfügbare Trigger:**
* **Profil:** Ereignis → System → Intent Empfangen
* **Aktion:** `com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE` (5-Minuten-Vorlauf)
* **Aktion:** `com.sleepwithuntis.app.ACTION_ALARM_NOW` (Bei Alarmauslösung)

---

### 🙏 Credits & Drittanbieter-Ressourcen

Diese App nutzt die folgenden Ressourcen:


* **API:** Ein großes Dankeschön an [Keule0010](https://github.com/Keule0010) für die Bereitstellung der [WebUntisAPI](https://github.com/Keule0010/WebUntisAPI) (Lizenziert unter der MIT-Lizenz).

* **Icons:** Alle Icons stammen von [SVG Repo](https://www.svgrepo.com/) und werden unter der CC0-Lizenz verwendet:

    * [Home Icon](https://www.svgrepo.com/svg/22031/home-icon-silhouette)

    * [Go Back Icon](https://www.svgrepo.com/svg/376074/go-back)

    * [Clock Icon](https://www.svgrepo.com/svg/535304/clock)

* **Audio:** Die Datei `alarm_sound.mp3` stammt aus einer lizenzfreien Sound-Bibliothek. 

---

### 🚀 Entwicklungsreise & Status
Dies ist meine allererste Android-Anwendung!
* **Status:** Technische Alpha / Proof of Concept.
* **Entwicklung:** Begann mit einer Python-API und wurde zur besseren Integration auf Kotlin/Java migriert.
* **Design:** Die funktionalen Elemente (Buttons/XML) wurden von mir erstellt und anschließend mit KI-Unterstützung verfeinert, um diesen modernen Look zu erzielen.
* **Lernprozess:** Als Lernprojekt arbeite ich kontinuierlich daran, redundanten Code zu entfernen und die Performance zu optimieren.

---

### Urheberrecht & Lizenzhinweis


Copyright (c) 2026 BrawlMasterX


Alle Rechte vorbehalten.


Diese Software und ihr Quellcode werden ausschließlich für Bildungszwecke und den persönlichen Gebrauch zur Verfügung gestellt. Die unbefugte Vervielfältigung, Änderung, Verbreitung oder Veröffentlichung dieser Software, einschließlich, aber nicht beschränkt auf das Hochladen der Anwendung in den Google Play Store oder auf andere App-Vertriebsplattformen, ist ohne vorherige schriftliche Genehmigung des Autors strengstens untersagt.


Einschränkungen:


* Du darfst diese Software nicht verkaufen oder für kommerzielle Zwecke nutzen.

* Du darfst den Quellcode oder die Anwendung nicht unter deinem eigenen Namen veröffentlichen.

* Es ist dir gestattet, den Code zu studieren und ihn ausschließlich für den privaten, nicht-kommerziellen Gebrauch anzupassen.


Für Anfragen oder Genehmigungswünsche kontaktiere mich bitte auf GitHub.

---

### ⚠️ Haftungsausschluss
Dies ist eine inoffizielle Anwendung und steht in keiner Verbindung zur Untis GmbH. Die Nutzung erfolgt auf eigene Gefahr. Der Entwickler ist nicht verantwortlich für versäumten Unterricht aufgrund technischer Fehler.
