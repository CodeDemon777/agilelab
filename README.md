# AgileLab - Full-Stack Java Spring Boot Real-Time Chat & Video Web Application

A full-stack, enterprise-grade real-time chat, group messaging, and WebRTC video/voice calling web application built with **Java 21/25**, **Spring Boot 3.4**, **Spring Security 6**, **Spring Data JPA**, **Spring WebSocket (STOMP)**, **Thymeleaf**, and **WebRTC**.

---

## 🌟 Key Features

1. **Real-time Messaging (STOMP WebSockets)**:
   - Instant direct (1-to-1) messaging and team group chats.
   - Live typing indicators ("Alice is typing...").
   - Online/Offline presence detection in real time.
   - Chat search and instant filtering.

2. **WebRTC Video & Voice Calling**:
   - Peer-to-peer real-time high-fidelity video & voice calling via WebRTC.
   - WebSocket-based signaling channel for SDP Offer/Answer and ICE candidate exchange.
   - In-call controls: Mute/Unmute microphone, toggle video camera, screen sharing, and call duration timer.
   - Incoming call notification banners with one-click Accept / Decline.

3. **Notifications**:
   - Live notification push over user WebSocket queue (`/user/queue/notifications`).
   - Unread notification count badge in navigation bar.
   - Interactive notification feed with "Mark as read" and direct links to relevant chats.

4. **User & Profile Management**:
   - Full authentication (Sign up, Log in, Session management, Log out) powered by Spring Security with BCrypt password hashing.
   - Interactive profile viewer and editor (custom status quote e.g. *"Living the dream"*, avatar customization, email, full name).
   - Public teammate profiles with quick-action buttons for messaging and video calls.

5. **Data Persistence & Zero-Config Setup**:
   - Spring Data JPA with embedded H2 database (`./data/agilelabdb`).
   - Persists data across application restarts with zero external database configuration required.
   - Embedded H2 Console at `/h2-console` for inspection.
   - Automated data seeding on startup with realistic demo users and sample conversations.

---

## 👥 Demo Accounts

The application automatically initializes with demo accounts (password: `password123` for all):

| Full Name | Username | Email | Role / Status Quote |
|---|---|---|---|
| **John Doe** | `johndoe` | `johndoe@example.com` | *"Living the dream"* |
| **Alice Smith** | `alice` | `alice@example.com` | *"Building great things 🚀"* |
| **Bob Wilson** | `bob` | `bob@example.com` | *"Available for quick syncs 🎧"* |
| **Emma Davis** | `emma` | `emma@example.com` | *"Coding Spring Boot & WebRTC ✨"* |

*The login page also includes 1-click demo buttons to automatically populate credentials!*

---

## 🚀 Getting Started

### Prerequisites
- **Java**: JDK 17, 21, or 25 installed.
- **Maven**: Bundled Maven wrapper script `mvnw.cmd` included in project.

### Running the Application

1. **Start the application**:
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```
   Or run the packaged JAR directly:
   ```cmd
   java -jar target/chatapp-1.0.0.jar
   ```

2. **Open in Browser**:
   - **Application URL**: [http://localhost:8088](http://localhost:8088)
   - **H2 Database Console**: [http://localhost:8088/h2-console](http://localhost:8088/h2-console)
     - JDBC URL: `jdbc:h2:file:./data/agilelabdb`
     - Username: `sa`
     - Password: *(blank)*

3. **Testing Real-Time Multi-User Chat**:
   - Open [http://localhost:8088](http://localhost:8088) in one browser tab and log in as `johndoe`.
   - Open [http://localhost:8088](http://localhost:8088) in an Incognito / private window or a second browser and log in as `alice`.
   - Send messages between each other — observe instant real-time delivery, typing indicator, and incoming call alerts!

---

## 🏗️ Project Architecture

```
d:\harees_projects\agilelab\
├── pom.xml                               # Maven project descriptor & dependencies
├── mvnw.cmd                              # Maven wrapper script
├── README.md                             # Project documentation
├── agile lab\                            # Original prototype HTML & CSS design assets
└── src\
    ├── main\
    │   ├── java\com\agilelab\chatapp\
    │   │   ├── AgileLabApplication.java  # Spring Boot Main Entry Point
    │   │   ├── config\
    │   │   │   ├── SecurityConfig.java   # Spring Security & BCrypt filter chain
    │   │   │   ├── WebSocketConfig.java  # STOMP & SockJS WebSocket configuration
    │   │   │   └── DataInitializer.java  # Seed data for demo accounts & chats
    │   │   ├── controller\
    │   │   │   ├── ViewController.java   # Thymeleaf page routing
    │   │   │   ├── AuthController.java   # Registration & auth endpoints
    │   │   │   ├── ChatRestController.java # REST API for chats, rooms & users
    │   │   │   ├── ChatWebSocketController.java # STOMP messaging & WebRTC signaling
    │   │   │   └── NotificationRestController.java # Notifications REST API
    │   │   ├── dto\                      # Data Transfer Objects (DTOs)
    │   │   ├── model\                    # JPA Entities (User, ChatRoom, ChatMessage, etc.)
    │   │   ├── repository\               # Spring Data JPA Repositories
    │   │   └── service\                  # Business logic services
    │   └── resources\
    │       ├── application.properties    # Server & database configuration
    │       ├── static\
    │       │   ├── css\                  # Preserved & enhanced stylesheets
    │       │   ├── js\                   # WebRTC, STOMP & chat scripts
    │       │   └── images\               # SVG avatars & icons
    │       └── templates\                # Thymeleaf HTML pages
    │           ├── fragments\navbar.html # Global sticky navigation bar
    │           ├── landing.html          # Landing page
    │           ├── login.html            # Login page with demo pills
    │           ├── signup.html           # User registration page
    │           ├── chats.html            # Dual-panel Chat List & Chat Room
    │           ├── call.html             # WebRTC Video & Voice Call page
    │           ├── notifications.html    # Live notification center
    │           └── profile.html          # User profile view & edit modal
    └── test\
        └── java\com\agilelab\chatapp\
            └── AgileLabApplicationTests.java # Automated integration tests
```

---

## 📡 REST API & WebSocket Summary

### REST Endpoints
- `POST /signup`: Register a new user
- `POST /login`: Form authentication
- `GET /api/auth/current`: Get logged-in user profile
- `GET /api/chats`: List user conversations
- `GET /api/chats/{roomId}/messages`: Fetch room message history
- `POST /api/chats/direct`: Get or initiate 1-to-1 conversation
- `POST /api/chats/group`: Create a new group chat
- `POST /api/chats/{roomId}/members`: Invite members to group
- `GET /api/users/search?q=...`: Search users by name/username
- `POST /api/users/profile`: Update user profile (name, status quote, avatar)
- `GET /api/notifications`: Retrieve user notifications
- `POST /api/notifications/{id}/read`: Mark notification as read

### WebSocket Channels (STOMP)
- `SUBSCRIBE /topic/room.{roomId}`: Real-time room messages
- `SUBSCRIBE /topic/room.{roomId}.typing`: Real-time typing indicators
- `SUBSCRIBE /user/queue/notifications`: Instant personal notification alerts
- `SUBSCRIBE /user/queue/call`: WebRTC incoming call offers, answers, candidates
- `SEND /app/chat.sendMessage`: Dispatch chat message
- `SEND /app/chat.typing`: Notify typing state
- `SEND /app/call.signal`: WebRTC peer exchange signaling
