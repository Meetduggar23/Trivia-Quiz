<div align="center">

<img src="appicon.png" alt="Trivia Quiz Logo" width="150" />

# Trivia Quiz

A modern Android trivia quiz app built with **HttpURLConnection**, **org.json**, and **Kotlin Coroutines**.

Fetched live from [Open Trivia DB](https://opentdb.com/) — no API key required.

</div>

---

## 📋 Features

- **10 live trivia questions** fetched from the internet at runtime
- **4 multiple-choice answers** per question, shuffled each time
- **Score tracking** with real-time display
- **Loading, Error, Quiz, and Result** UI states
- **Retry & Play Again** — always fetches fresh questions
- **Pink + Brown theme** — clean, modern design

## 🏗 Architecture

```
MainActivity
  → QuizRepository (suspend + Dispatchers.IO)
    → NetworkClient (HttpURLConnection)
      → Open Trivia DB API
    → JsonParser (JSONObject / JSONArray)
      → List<Question>
  → UI (View Binding)
```

## 📁 Project Structure

```
com.example.triviaquiz/
├── MainActivity.kt
├── model/
│   └── Question.kt
├── network/
│   ├── NetworkUtils.kt
│   ├── NetworkResult.kt
│   └── NetworkClient.kt
└── data/
    ├── JsonParser.kt
    └── QuizRepository.kt
```

## 🛠 Tech Stack

| Technology | Purpose |
|------------|---------|
| `HttpURLConnection` | Raw HTTP networking |
| `org.json` | JSON object & array parsing |
| Kotlin Coroutines | Background networking via `Dispatchers.IO` |
| View Binding | Type-safe view references |
| Material Design 3 | UI components |
| Open Trivia DB | Free trivia API |

## ⚙️ Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on an emulator or device (min SDK 24 / Android 7.0)

## 📸 Screenshots

| Loading | Quiz | Result |
|---------|------|--------|
| Loading spinner | Question + 4 answers | Final score + Play Again |

## 📄 License

This project was built as an **Advanced Android Programming — Networking Assignment**.
