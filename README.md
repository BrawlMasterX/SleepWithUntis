# SleepWithUntis

<p align="center">
  <a href="README.de.md">
    <img src="https://img.shields.io/badge/Language-Deutsch-red?style=for-the-badge" alt="Deutsch">
  </a>
  <a href="README.md">
    <img src="https://img.shields.io/badge/Language-English-blue?style=for-the-badge" alt="English">
  </a>
</p>

---

An Android alarm clock app that wakes you up automatically based on your WebUntis schedule.

---

### ✨ Features & Preview

| Home Screen | Settings |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/6a8c9bfb-749e-47bc-957b-b2a597f9252f" width="350"> | <img src="https://github.com/user-attachments/assets/806cb7d3-2313-4d2a-9bc7-c09080bef178" width="350"> |
| **Minimalist Design**: Tap the wake-up time to see a toast notification showing exactly how much time remains until your next alarm. | **Full Control**: Quickly customize app behavior and preferences through the clean settings menu. |

---

| Login & Sync | Alarm Setup |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/42e60006-3ea8-4f14-a492-c3340f676291" width="350"> | <img src="https://github.com/user-attachments/assets/c37a4489-c3e3-4e1b-8316-3351dbc0b6fb" width="350"> |
| **Easy Access**: Simply log in with your credentials and search for your school to sync your timetable. | **Personalized Wake-up**: Adjust snooze durations or choose multiple alarm sounds—the app will pick a random one to keep your mornings fresh. |

---

### Flexible Wake-up Modes

Switch between a general preparation time or specific times for every lesson using the toggle in the top right.

| Preparation (Early Minutes) | Individual Lesson Times |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/4ffa48a7-5bc1-4774-8eb8-c27771d683d7" width="350"> | <img src="https://github.com/user-attachments/assets/a8ef6992-f1af-4014-a498-3fc24e379429" width="350"> |
| Set a fixed number of "Early Minutes" you need before your first school hour starts. | Define a custom wake-up time for every single hour of the school day for maximum precision. |

---

### 🛠️ Tech Stack
* **Language:** Kotlin for the App / Java for the API
* **Platform:** Android
* **API Connection:** [WebUntisAPI by Keule0010](https://github.com/Keule0010/WebUntisAPI) (Modified by me)

#### Technical Modifications
I have made specific adjustments to the original WebUntisAPI to ensure compatibility with the Android environment:
* **JSON Parameter Fix:** Modified the `json` calls by removing the second `null` parameter. This was necessary because the standard Android JSON implementation only accepts a single parameter, and the extra null caused a signature mismatch.
* **Exception Handling:** Added explicit `Exception` handling and `throws` declarations to the core methods to ensure the app compiles correctly under Android's strict error-handling requirements.

---

### 💡 Tasker Integration
This app sends two different broadcast intents that Tasker can listen for. This allows you to automate your smart home or device settings perfectly synchronized with your school schedule.

**Prerequisites in Tasker:**
* Tap the three dots (top right) → Preferences.
* Go to the "Misc" tab.
* Enable "Allow External Access".

**Available Triggers:**
* **Profile:** Event → System → Intent Received
* **Action:** `com.sleepwithuntis.app.ACTION_ALARM_5_MINUTE` (5-minute lead-up, e.g., for light alarms)
* **Action:** `com.sleepwithuntis.app.ACTION_ALARM_NOW` (Upon alarm trigger, e.g., for roller shutters)

---

### 📜 Copyright & License
Copyright (c) 2026 BrawlMasterX. All Rights Reserved.
This software and its source code are provided for educational and personal use only. Unauthorized copying, modification, or publication of this software, including but not limited to uploading the application to the Google Play Store or any other app distribution platform, is strictly prohibited without prior written permission from the author.

---

### ⚠️ Disclaimer
This is an unofficial application and is not affiliated with, authorized, or endorsed by Untis GmbH. Use of this app is at your own risk. The developer is not responsible for any missed classes or appointments due to technical failures.
