# Pubify - PDF to EPUB Converter & Smart Reader

**Pubify** is a feature-rich, modern Android application built with Jetpack Compose that allows users to convert PDF documents into clean, reflowable EPUB e-books with smart chapter detection, custom metadata, and a built-in reading environment enriched by AI insights and Text-to-Speech capabilities.

---

a## 🌟 Key Features

- **⚡ Unlimited PDF to EPUB Conversion**
  - **Structure & Metadata Extraction**: Automatically parses PDF structure, font hierarchy, table of contents, and cover images.
  - **Smart Chapter Detection**: Automatically detects chapter headings and splits content into clean EPUB chapters.
  - **Metadata Customization**: Edit book title, author, description, cover image, and chapter boundaries before building the EPUB file.

- **📖 Advanced EPUB Reader**
  - **Reflowable Layout & Customization**: Adjust font size, line spacing, margins, and typography (supports custom fonts like *EB Garamond*, *Literata*, *Lora*, and *Inter*).
  - **Custom Themes**: Includes Light, Dark, Sepia, and Glassmorphic reader themes.
  - **Bookmarks & Highlights**: Save reading progress, highlight important text, and add notes.

- **🤖 AI-Powered Assistant (Gemini)**
  - **In-Book Dictionary & Explanations**: Select words or paragraphs to fetch instant AI definitions, contextual summaries, or translations using Gemini.
  - **AI Reader Tools**: Quick insights and summaries directly inside the reading view.

- **🎙️ Text-to-Speech (TTS)**
  - Integrated hands-free audio playback with speech rate control, pitch adjustments, sentence highlighting, and media controls.

- **📊 Reading Statistics & Library Management**
  - **Local Room Database**: Organizes your collection of books, bookmarks, highlights, and reading stats offline.
  - **Analytics**: Track total reading time, daily reading streaks, pages read, and reading speed.

- **☁️ Cloud Sync & Security**
  - **Firebase Authentication & Firestore**: Sync your library, reading progress, and bookmarks across devices via Google Sign-In.
  - **Firebase App Check**: Protects API resources against unauthorized requests.

---

## 🛠️ Tech Stack & Architecture

- **Language & UI Framework**: [Kotlin](https://kotlinlang.org/) & [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles
- **PDF & EPUB Engine**: [PDFBox-Android](https://github.com/TomRoushed/pdfbox-android), Custom EPUB XML generator & ZIP unpacker/parser
- **Local Database**: [Room Database](https://developer.android.com/training/data-storage/room) with KSP
- **Asynchronous & Reactive**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Cloud & AI Integration**: Firebase SDK (Auth, Firestore, App Check), Google Gemini API
- **Networking**: Retrofit 2, Moshi, OkHttp 4
- **Image Loading**: Coil
- **Testing**: JUnit 4, Robolectric, Roborazzi (Screenshot Testing)

---

## 📁 Project Structure

```
Pubify/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── converter/            # PDF extraction & EPUB generation engines
│   │   ├── data/                 # Room database, Entities, DAOs, Firebase repositories
│   │   ├── reader/               # EPUB parser, pagination engine, TTS manager, Gemini AI helpers
│   │   └── ui/                   # Jetpack Compose UI screens, components, and design system
│   │       ├── components/       # Reusable UI components & custom text renderers
│   │       ├── screens/          # Converter, Reader, Library, Tools, Auth, Stats screens
│   │       ├── theme/            # Material 3 colors, typography, shapes, and themes
│   │       └── viewmodel/        # ViewModels managing state & business logic
├── gradle/                       # Dependency catalog (libs.versions.toml) & wrapper
└── build.gradle.kts              # Root build configuration
```

---

## 📜 License

This project is released under the [MIT License](LICENSE).
