# AgileLab – Full-Stack Real-Time Collaboration & AI-Powered Web Application

A modern, enterprise-grade, full-stack collaboration platform built with **Java 21/25**, **Spring Boot 3.4**, **Spring Security 6**, **Spring Data JPA**, **Spring WebSocket (STOMP)**, **WebRTC**, and **Spring AI / RAG Semantic Search**.

AgileLab goes far beyond standard chat by combining **real-time dual-panel messaging**, **peer-to-peer video/voice calling**, and an **Autonomous AI Smart Collaboration Assistant** that turns everyday conversations and meetings into actionable sprint deliverables.

---

## 🌟 Key Application Features

### 1. 🤖 AI-Powered Smart Collaboration Assistant (Spring AI & RAG)
- **AI Chat Summarization**:
  - Summarizes hundreds of conversation messages into an **Executive Summary**, **Key Discussion Points**, **Consensus/Decisions Made**, and **Team Sentiment** (e.g. *Highly Positive & Collaborative 🚀*).
  - One-click "✨ AI Actions" dropdown in the active chat header and full view in the AI Workspace.
  - One-click "Copy Summary" for sprint reports or standup updates.
- **RAG Chat Assistant (Semantic Search & Question Answering)**:
  - Users can ask natural language questions about team discussions, sprint commitments, decisions, and roadblocks.
  - Retrieval-Augmented Generation (RAG) pipeline computes semantic cosine similarity across conversation history, ranks matching messages, and generates grounded answers citing specific team members, timestamps, and message quotes.
  - Room-scoped or workspace-wide search mode.
- **Automatic Action Item Extraction & Kanban**:
  - Scans chat conversations and meeting minutes for commitments, tasks, assignees, deadlines ("by tomorrow", "by Friday", "by next week"), and priorities (`URGENT`, `HIGH`, `MEDIUM`, `LOW`).
  - Automatically deduplicates and persists tasks to the Action Items Kanban hub.
  - Interactive status transitions: `PENDING` ➔ `IN_PROGRESS` ➔ `COMPLETED` with progress tracking.
- **AI Meeting Transcription & Minutes**:
  - Live in-call Speech-to-Text via browser Web Speech API (`webkitSpeechRecognition`) during WebRTC audio/video calls.
  - "✨ Meeting Notes" button converts transcripts into structured minutes (Executive Summary, Agenda Points Covered, Strategic Decisions, and Follow-up Action Items).
  - Standalone transcript ingestion interface in the AI Workspace.
- **Multilingual AI Translation**:
  - Instant translation of team messages into Spanish, French, German, Hindi, Japanese, and more.
  - In-chat translation preview and dedicated translation studio in the workspace.
- **Team Velocity & Collaboration Analytics**:
  - Visual metrics: total messages grounded, active action items, completion rate gauge, and member communication distribution.

---

### 2. 💬 Real-Time Messaging & Group Chat (STOMP WebSockets)
- **Dual-Panel Responsive Chat Layout**: Sidebar conversation list on the left with active chat view on the right.
- **Instant Messaging**: Low-latency message delivery powered by Spring WebSocket STOMP with SockJS fallback.
- **Dual Broadcast Channels**:
  - `/topic/room.{roomId}`: Updates the active conversation history in real time.
  - `/topic/messages`: Updates conversation previews and timestamps in the sidebar for all participants simultaneously.
- **Live Typing Indicators**: Broadcasts when a teammate is typing (`/topic/room.{roomId}.typing`) with auto-dismiss timers.
- **Live User Presence**: Instant online/offline dot indicators across chat items and headers (`/topic/presence`).
- **Group Conversations**: Create named group chats with multi-member selector and member invitations.

---

### 3. 📹 WebRTC Video & Voice Calling
- **Peer-to-Peer Calling**: High-definition video and crisp audio calling directly in the browser via WebRTC.
- **Real-Time Signaling Channel**: Handles SDP Offers, SDP Answers, and ICE Candidates via STOMP user queues (`/user/queue/call`).
- **Full Call Controls**:
  - Mute / Unmute Microphone
  - Turn Camera On / Off (video calls)
  - Screen Sharing (`getDisplayMedia`)
  - Live call duration timer
  - **🎙️ Live AI Transcript drawer**
  - **✨ Generate Meeting Notes**
- **Incoming Call Banners**: Real-time notifications for incoming calls with 1-click Accept / Decline.

---

### 4. 🔔 Notifications Center
- Real-time notification delivery over user-specific WebSocket queues (`/user/queue/notifications`).
- Unread badge counter in the top navigation bar.
- Notification feed with "Mark as Read" and direct links to the relevant chat room or meeting.

---

### 5. 👤 Profile & Authentication Management
- Secure user signup, login, session management, and logout backed by **Spring Security 6** and **BCrypt**.
- User profile viewer and editor with custom status quote (e.g. *"Building great things 🚀"*), avatar selection, full name, and email.
- Teammate profile cards with quick-action buttons for messaging and calling.

---

### 6. 💾 Zero-Config Persistent Database
- Spring Data JPA with an embedded file-backed H2 database (`./data/agilelabdb`).
- Data persists across restarts with zero external database configuration required.
- Built-in web console accessible at `/h2-console`.
- Automated startup data initializer with realistic demo users, sprint messages, action items, and meeting notes.

---

## 👥 Demo User Accounts

The application automatically seeds 4 demo accounts (Password: `password123` for all):

| Full Name | Username | Email | Status Quote |
|---|---|---|---|
| **John Doe** | `johndoe` | `johndoe@example.com` | *"Living the dream"* |
| **Alice Smith** | `alice` | `alice@example.com` | *"Building great things 🚀"* |
| **Bob Wilson** | `bob` | `bob@example.com` | *"Available for quick syncs 🎧"* |
| **Emma Davis** | `emma` | `emma@example.com` | *"Coding Spring Boot & WebRTC ✨"* |

*The login page also provides 1-click pills to instantly populate credentials!*

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java**: JDK 17, 21, or 25 installed.
- **Maven**: Bundled Maven wrapper (`mvnw.cmd` / `./mvnw`) included in the project root.

### Running the Application

1. **Start via Maven wrapper**:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Or run the pre-built executable JAR:
   ```powershell
   java -jar target/chatapp-1.0.0.jar
   ```

2. **Access in Browser**:
   - **Main Web Application**: [http://localhost:8088](http://localhost:8088)
   - **AI Workspace**: [http://localhost:8088/ai-workspace](http://localhost:8088/ai-workspace)
   - **H2 Database Console**: [http://localhost:8088/h2-console](http://localhost:8088/h2-console)
     - JDBC URL: `jdbc:h2:file:./data/agilelabdb`
     - Username: `sa`
     - Password: *(leave blank)*

3. **Running the Automated Test Suite**:
   ```powershell
   .\mvnw.cmd test
   ```
   *(Executes 14 unit and integration tests covering WebSocket STOMP messaging, WebRTC signaling, AI summarization, RAG queries, and action item extraction.)*

---

## 🏗️ Project Architecture & Directory Structure

```
d:\harees_projects\agilelab\
├── pom.xml                                  # Maven dependencies (Spring Boot, WebSocket, JPA, Security, Thymeleaf)
├── mvnw.cmd / mvnw                          # Maven wrapper executables
├── README.md                                # Comprehensive documentation
├── data\agilelabdb.mv.db                    # File-persisted H2 database
└── src\
    ├── main\
    │   ├── java\com\agilelab\chatapp\
    │   │   ├── AgileLabApplication.java     # Spring Boot application entry point
    │   │   ├── config\
    │   │   │   ├── DataInitializer.java     # Seeds demo users, sprint messages, tasks & summaries
    │   │   │   ├── SecurityConfig.java      # Spring Security 6 authentication & CSRF rules
    │   │   │   └── WebSocketConfig.java     # STOMP broker, SockJS & endpoint registration
    │   │   ├── controller\
    │   │   │   ├── AiRestController.java    # REST API for Summarization, RAG, Action Items & Notes
    │   │   │   ├── AuthController.java      # User registration & session endpoints
    │   │   │   ├── ChatRestController.java  # REST API for rooms, messages & user search
    │   │   │   ├── ChatWebSocketController.java # STOMP handlers for messaging & WebRTC signaling
    │   │   │   ├── NotificationRestController.java # Notifications REST API
    │   │   │   └── ViewController.java      # Thymeleaf page routing (/chats, /ai-workspace, /call, etc.)
    │   │   ├── dto\                         # DTOs: ActionItemDto, ChatSummaryDto, AiAskRequest/Response, etc.
    │   │   ├── model\                       # JPA Entities: User, ChatRoom, ChatMessage, ActionItem, MeetingSummary
    │   │   ├── repository\                  # JPA Repositories: ActionItemRepository, MeetingSummaryRepository, etc.
    │   │   └── service\
    │   │       ├── CallService.java         # Call history management
    │   │       ├── ChatService.java         # Room creation & message routing
    │   │       ├── NotificationService.java # Notification dispatch
    │   │       ├── UserService.java         # User authentication & profile updates
    │   │       └── ai\
    │   │           ├── AiClient.java        # Interface for LLM / AI engines
    │   │           ├── AiService.java       # Orchestrates RAG, Summaries, Action Items & Notes
    │   │           ├── ExternalLlmClient.java # Client for OpenAI / Ollama integration
    │   │           └── SmartLocalAiClient.java # Built-in Semantic NLP engine (zero external dependencies)
    │   └── resources\
    │       ├── application.properties       # Port 8088, H2 database URL, AI provider configuration
    │       ├── static\
    │       │   ├── css\                     # app.css, ChatList.css, ChatRoom.css, CallPage.css
    │       │   ├── js\                      # app.js, chat.js, call.js, ai-workspace.js, stomp.min.js
    │       │   └── images\                  # Default avatar SVG and icons
    │       └── templates\                   # Thymeleaf HTML Templates
    │           ├── fragments\navbar.html    # Navigation header with Chats, AI Workspace, Notifications
    │           ├── ai-workspace.html        # AI Workspace (RAG Assistant, Kanban, Meetings, Translation)
    │           ├── chats.html               # Dual-panel Chat List & Room with AI Actions dropdown
    │           ├── call.html                # WebRTC video/voice call with live AI transcript & notes
    │           ├── landing.html             # Product landing page
    │           ├── login.html               # Login page with demo user selector
    │           ├── signup.html              # Registration form
    │           ├── notifications.html       # Notifications inbox
    │           └── profile.html             # Profile view & editor
    └── test\
        └── java\com\agilelab\chatapp\
            ├── AgileLabApplicationTests.java # Core application context & service tests
            ├── AiCollaborationIntegrationTests.java # AI Summaries, RAG Assistant & Action Items tests
            └── WebSocketMessagingIntegrationTests.java # STOMP broker & frame delivery tests
```

---

## 📡 Complete API Catalog

### 1. AI Collaboration Endpoints (`/api/ai`)
| Method | Endpoint | Description |
|---|---|---|
| `GET / POST` | `/api/ai/summarize/{roomId}` | Generates executive summary, decisions, action items & sentiment for a room |
| `POST` | `/api/ai/ask` | Queries RAG chat assistant with natural language query & optional room scope |
| `POST` | `/api/ai/action-items/extract/{roomId}` | Automatically extracts tasks, assignees & deadlines from room messages |
| `GET` | `/api/ai/action-items` | Retrieves action items with filters (`roomId`, `status`, `assignedTo`) |
| `POST` | `/api/ai/action-items` | Manually creates a new action item |
| `POST` | `/api/ai/action-items/{id}/status` | Updates status (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |
| `DELETE` | `/api/ai/action-items/{id}` | Deletes an action item |
| `POST` | `/api/ai/meeting-notes` | Ingests call transcript & generates structured meeting minutes |
| `GET` | `/api/ai/meeting-notes` | Lists all recorded meeting summaries |
| `GET` | `/api/ai/meeting-notes/{id}` | Fetches details for a specific meeting summary |
| `POST` | `/api/ai/translate` | Translates text into target language (`spanish`, `french`, `german`, etc.) |
| `GET` | `/api/ai/insights` | Returns workspace-wide collaboration metrics & contributor activity |

### 2. Chat & User REST Endpoints (`/api/chat`, `/api/users`, `/api/auth`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/chat/rooms` | Lists active conversations for the authenticated user |
| `GET` | `/api/chat/rooms/{roomId}/messages` | Fetches historical messages for a room |
| `POST` | `/api/chat/direct` | Finds or creates a direct conversation with a teammate |
| `POST` | `/api/chat/group` | Creates a new named group conversation |
| `POST` | `/api/chat/rooms/{roomId}/members` | Adds members to an existing group chat |
| `GET` | `/api/users/all` | Lists eligible teammates for group chat creation |
| `GET` | `/api/users/search?q=...` | Searches teammates by name or username |
| `POST` | `/api/users/profile` | Updates user full name, status quote, and avatar URL |
| `GET` | `/api/auth/current` | Returns currently authenticated user details |
| `POST` | `/api/auth/register` | Registers a new user via JSON API |

### 3. WebSocket STOMP Channels (`/ws`)
| Channel Type | Destination | Purpose |
|---|---|---|
| `SUBSCRIBE` | `/topic/room.{roomId}` | Receives real-time messages posted to a specific room |
| `SUBSCRIBE` | `/topic/room.{roomId}.typing` | Receives live typing state changes for a room |
| `SUBSCRIBE` | `/topic/messages` | Receives global conversation updates to refresh sidebar previews |
| `SUBSCRIBE` | `/topic/presence` | Receives live user online/offline status changes |
| `SUBSCRIBE` | `/user/queue/notifications` | Receives private notification alerts for the current user |
| `SUBSCRIBE` | `/user/queue/call` | Receives WebRTC signaling packets (Offer, Answer, ICE Candidates, Hangup) |
| `SEND` | `/app/chat.sendMessage` | Dispatches a new chat message |
| `SEND` | `/app/chat.typing` | Broadcasts user typing activity |
| `SEND` | `/app/call.signal` | Sends WebRTC peer signaling to initiate, answer, or end a call |

---

## 💡 Highlights for Interviews & Portfolio Evaluations

- **Full-Stack Proficiency**: Demonstrates complete end-to-end architecture from raw WebSocket frames and WebRTC peer negotiation to secure JPA persistence and reactive Thymeleaf rendering.
- **Spring AI & RAG Engineering**: Shows how to construct a semantic context retrieval pipeline, ground answers on real chat history, and gracefully support both external LLMs and smart local NLP fallbacks with 100% offline reliability.
- **Real-Time Distributed Event Handling**: Implements STOMP message brokers with topic-based pub-sub channels and user-specific message queues.
- **Peer-to-Peer Media Streaming**: Seamlessly bridges WebSocket signaling with browser WebRTC media streams (`getUserMedia`, `getDisplayMedia`) and Web Speech API.
- **Enterprise Security Practices**: BCrypt password encoding, CSRF protection with selective API ignores, authenticated principal propagation, and secure frame options.
