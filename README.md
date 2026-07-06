# WhaBotPro

An intelligent WhatsApp business automation Android app with AI-powered customer service capabilities.

## Features

- **AI-Powered Chatbot**: Uses Groq (Llama 3.1) or Gemini for natural language processing
- **Knowledge Base Management**: Manage menu items, services, FAQs, deals, policies, events, and more
- **Order Management**: Track and manage customer orders with status updates
- **Contact Management**: Store and manage customer contacts
- **WhatsApp Integration**: Built-in WhatsApp engine for message handling
- **Bulk Messaging**: Send messages to multiple contacts at once
- **Data Import**: AI-powered raw data processing to populate your knowledge base
- **Business Info Management**: Complete business profile management

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with StateFlow
- **Storage**: Room Database + JSON file-based persistence
- **AI**: Groq API (Llama 3.1) and Google Gemini API
- **WhatsApp**: Baileys (Node.js embedded via J2V8)

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/whabotpro/
│   │   ├── ai/              # AI agents and clients (Groq, Gemini)
│   │   ├── data/
│   │   │   ├── db/         # Room database
│   │   │   ├── model/      # Data models
│   │   │   └── store/      # JSON store and repository
│   │   ├── engine/         # WhatsApp engine
│   │   ├── service/        # Android services
│   │   ├── ui/
│   │   │   ├── screen/     # Compose screens
│   │   │   └── component/  # Reusable UI components
│   │   └── viewmodel/      # ViewModels
│   └── assets/
│       └── nodejs-project/ # Embedded Node.js for WhatsApp
```

## AI Tools

The app includes 32 professional CRUD tools for AI execution:

### Read Tools (13)
- Business info, KB items, categories, orders, contacts, rules, logs

### Write Tools (19)
- Create, update, delete operations for all entities
- Toggle availability/status operations

## Setup

1. Clone the repository
2. Open in Android Studio
3. Configure API keys in `build.gradle.kts` or via Settings screen:
   - `GRADLE_API_GROQ_API_KEY` for Groq
   - `GRADLE_API_GEMINI_API_KEY` for Gemini
4. Build and run on Android device

## API Keys Configuration

Add to your `~/.gradle/gradle.properties`:

```properties
GRADLE_API_GROQ_API_KEY=your_groq_key_here
GRADLE_API_GEMINI_API_KEY=your_gemini_key_here
```

## License

Proprietary - All rights reserved
