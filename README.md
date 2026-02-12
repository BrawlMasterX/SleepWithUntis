# SleepWithUntis

An Android alarmclock app that wakes you up automatically based on your WebUntis schedule.

---

Copyright & License Notice

Copyright (c) 2026 BrawlMasterX

All Rights Reserved.

This software and its source code are provided for educational and personal use only. Unauthorized copying, modification, distribution, or publication of this software, including but not limited to uploading the application to the Google Play Store or any other app distribution platform, is strictly prohibited without prior written permission from the author.

Restrictions:

    You may not sell this software or use it for commercial purposes.

    You may not republish the source code or the application under your own name.

    You are welcome to study the code and adapt it for private, non-commercial use only.

For inquiries or permission requests, please contact me on github.

---

## ✨ Features
* **Auto-Sync:** Synchronizes directly with the WebUntis timetable.
* **Smart Alarm:** Calculates the wake-up time based on the first actual lesson of the day.
* **Customizable:** Users can study the code and adapt it for private, non-commercial use (e.g., adding individual travel time buffers).
* **SmartHome/Tasker:** I have a function in the updateWorker which triggers an openhab LightAlarm. You can edit it or use tasker which is easier

---

## 🛠️ Tech Stack
* **Language:** Kotlin for App / Java for API
* **Platform:** Android
* **API Connection:** [WebUntisAPI by Keule0010](https://github.com/Keule0010/WebUntisAPI) I've edited
  * ## Technical Modifications

    I have made specific adjustments to the original WebUntisAPI to ensure compatibility with the Android environment:

    * **JSON Parameter Fix:** Modified the `json` calls by removing the second `null` parameter. This was necessary because the standard Android JSON implementation only accepts a single parameter, and the extra null caused a signature mismatch.
    * **Exception Handling:** Added explicit `Exception` handling and `throws` declarations to the core methods to ensure the app compiles correctly under Android's strict error-handling requirements.

---

## 💡 Integrating Tasker with SleepWithUntis
This app sends two different broadcast intents that Tasker can listen for. This allows you to automate your smart home or device settings perfectly synchronized with your school schedule.

**Prerequisites**
* Open Tasker.
* Tap the three dots (top right) → Preferences.
* Go to the Misc tab.
* Enable "Allow External Access".

**Available Triggers**
* **Profile:** Tap + → Event → System → Intent Received
* **Action:** com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE
    * 5 Minutes before alarm
* **Action:** com.sleepwithuntis.app.ACTION_ALARM_NOW
    * when the alarm rings
* **Task:** Create a task with actions to run exactly when the alarm goes off.

---

## 🙏 Credits & Third-Party Resources
This app utilizes the following resources:

* **API:** A huge thanks to [Keule0010](https://github.com/Keule0010) for providing the [WebUntisAPI](https://github.com/Keule0010/WebUntisAPI) (Licensed under the MIT License).
* **Icons:** All icons were sourced from [SVG Repo](https://www.svgrepo.com/) and are used under the CC0 License:
    * [Home Icon](https://www.svgrepo.com/svg/22031/home-icon-silhouette)
    * [Go Back Icon](https://www.svgrepo.com/svg/376074/go-back)
    * [Clock Icon](https://www.svgrepo.com/svg/535304/clock)
* **Audio:** The `alarm_sound.mp3` is sourced from a royalty-free sound library.

---

## Development Journey & Status
This is my very first Android application. The project started using a Python-based API but was later migrated to the **WebUntisAPI (Kotlin)** to implement the school search feature.

As this is a learning project:
* **Current Status:** Technical Alpha / Proof of Concept.
* **Development:** Built with a mix of manual coding, documentation research, and AI assistance.
* **Design:** I've just made the things i've nedded like buttons. Then send all .xml files to gemini, which made this clean design.
* **Known Issues:** Since I am still learning Kotlin and Android development, there might be redundant code or minor bugs. I am continuously working on cleaning up the codebase and optimizing performance.

---

## ⚠️ Disclaimer
This is an unofficial application and is not affiliated with, authorized, or endorsed by Untis GmbH. Use of this app is at your own risk. The developer is not responsible for any missed classes or appointments due to technical failures.
