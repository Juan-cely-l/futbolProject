<div align="center">
  <img src="Assets/celebracion.jpeg" alt="Futbix" width="100%" style="border-radius: 16px; margin-bottom: 20px;" />
  
  # ⚽ Futbix
  ### _Full-Stack Football Team & Player Management_
  
  <p align="center">
    <strong>Spring Boot 4</strong> ·
    <strong>React 19</strong> ·
    <strong>PostgreSQL</strong> ·
    <strong>Docker</strong>
  </p>
  
  <p align="center">
    <img src="https://img.shields.io/badge/java-21-%23ED8B00?style=flat-square&logo=openjdk" alt="Java 21" />
    <img src="https://img.shields.io/badge/spring_boot-4.0.6-%236DB33F?style=flat-square&logo=springboot" alt="Spring Boot 4" />
    <img src="https://img.shields.io/badge/react-19-%2358C4DC?style=flat-square&logo=react" alt="React 19" />
    <img src="https://img.shields.io/badge/postgresql-16-%234169E1?style=flat-square&logo=postgresql" alt="PostgreSQL 16" />
    <img src="https://img.shields.io/badge/tests-124_passing-%2322C55E?style=flat-square" alt="Tests: 124 passing" />
  </p>
</div>

---

## 📋 Overview

**Futbix** is a modern web application for managing football (soccer) teams and players. Built with a clean layered architecture, it provides a complete REST API backed by PostgreSQL and a responsive single-page frontend.

- **38 teams** and **56 players** ready to seed across 5 leagues
- Real-time search, pagination, and sorting
- Squad management with position-based pitch visualization
- Player efficiency metrics and market value tracking
- Fully containerized with Docker for one-command setup

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React 19)                      │
│  Vite 7 · React Router 7 · TanStack Query 5 · Tailwind 4   │
│  Recharts · Axios                                            │
│  Port: 5173 (dev) · Port: 80 (docker)                       │
└───────────────────────┬─────────────────────────────────────┘
                        │  /api/*  →  proxy
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Backend (Spring Boot 4 / Java 21)               │
│  Controller → Service Interface → Service Impl → Repository │
│  JPA / Hibernate · Jakarta Validation · SLF4J               │
│  Port: 8080                                                  │
└───────────────────────┬─────────────────────────────────────┘
                        │  JDBC
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL 16 (Alpine)                     │
│  Database: futbol_db · Port: 5432                            │
└─────────────────────────────────────────────────────────────┘
```

### Backend Structure

```
src/main/java/futbol/api/com/
├── controllers/       # REST endpoints (Team, Player, Seed)
├── models/            # JPA entities (Team, Player, Position enum)
├── repositories/      # Spring Data JPA with custom queries
├── dtos/              # Request/Response DTOs with validation
├── services/          # Business logic layer
├── seed/              # Data ingestion from JSON
└── exceptions/        # Global exception handler with logging
```

### Frontend Structure

```
frontend/src/
├── api/               # Axios instance & API modules
├── hooks/             # TanStack React Query v5 hooks
├── components/        # Reusable UI components (11)
├── pages/             # Route-level pages (6)
├── context/           # Toast notification context
└── utils/             # Formatters & helpers
```

---

## ✨ Features

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/futbix/v1/seed` | Seed database from JSON data |
| `POST` | `/futbix/v1/teams` | Create a team |
| `GET` | `/futbix/v1/teams` | List teams (paginated, searchable) |
| `GET` | `/futbix/v1/teams/name/{name}` | Get team by name |
| `GET` | `/futbix/v1/teams/{id}` | Get team by UUID |
| `GET` | `/futbix/v1/teams/{name}/squad` | Get team squad |
| `GET` | `/futbix/v1/teams/{name}/value` | Get squad market value |
| `PUT` | `/futbix/v1/teams/{id}` | Update team |
| `DELETE` | `/futbix/v1/teams/{id}` | Delete team + players |
| `POST` | `/futbix/v1/players` | Create a player |
| `GET` | `/futbix/v1/players` | List players (paginated, searchable) |
| `GET` | `/futbix/v1/players/{id}` | Get player by UUID |
| `PUT` | `/futbix/v1/players/{id}` | Update player |
| `DELETE` | `/futbix/v1/players/{id}` | Delete player |
| `GET` | `/futbix/v1/players/efficiency/{id}` | Player efficiency rating |

### Domain Model

```
 ┌───────────────┐          ┌───────────────────┐
 │     Team      │          │      Player       │
 ├───────────────┤          ├───────────────────┤
 │ id (UUID)     │◄─────────│ id (UUID)         │
 │ name (unique) │    FK    │ name              │
 │ budget        │          │ age               │
 │ city          │          │ position (enum)   │
 │ createdAt     │          │ goals             │
 └───────────────┘          │ assists           │
                            │ matches           │
                            │ valueMarket       │
                            │ team_id (FK)      │
                            └───────────────────┘
                            
                            Unique: (name, age, team_id)
```

### Frontend Pages

| Page | Route | Description |
|------|-------|-------------|
| **Dashboard** | `/dashboard` | 4 metric cards, top 5 players, recent teams, budget chart |
| **Teams** | `/teams` | Grid view, search, sort, create modal |
| **Team Detail** | `/teams/:name` | Squad tab, pitch view stats tab, edit/delete |
| **Players** | `/players` | Table/card toggle, search, position filter, sort |
| **Player Profile** | `/players/:id` | Jersey hero, stat grid, efficiency ring, bar chart |
| **404** | `*` | Not found page |

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Git
- Node.js 20+ _(for frontend dev)_
- Java 21+ _(for backend dev)_

### One-Command Setup

```bash
# 1. Clone
git clone https://github.com/juan-cely-l/futbolProject.git
cd futbolProject

# 2. Configure environment
cat > .env << EOF
POSTGRES_USER=postgres
POSTGRES_PASSWORD=futbolroot
POSTGRES_DB=futbol_db
EOF

# 3. Start everything
docker compose up -d

# 4. Seed the database
curl -X POST http://localhost:8080/futbix/v1/seed

# 5. Open the app
open http://localhost
```

### Local Development

```bash
# Terminal 1 — Database
docker compose up -d postgres-db

# Terminal 2 — Backend (port 8080)
POSTGRES_USER=postgres POSTGRES_PASSWORD=futbolroot \
  mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 3 — Frontend (port 5173)
cd frontend && npm install && npm run dev
```

---

## 🧪 Testing

```bash
# Run all 124 tests
mvn test

# Run a specific class
mvn test -Dtest=TeamControllerTest

# Run a specific method (JUnit 5 format)
mvn test -Dtest=PlayerServiceImplTest#createPlayer_validRequest_returnsCreated
```

| Test Suite | Type | Count |
|------------|------|-------|
| Controllers | `@ExtendWith(MockitoExtension.class)` | 4 files |
| Services | Mockito + PlayerMapper | 3 files |
| Repositories | `@SpringBootTest` + H2 (PG mode) | 2 files |
| DTOs | Jakarta Validation | 4 files |

---

## 🛠️ Tech Stack

### Backend

| Technology | Version |
|------------|---------|
| Java | 21 (Temurin) |
| Spring Boot | 4.0.6 |
| Spring Data JPA | 4.0.5 |
| Hibernate | 7.2.12 |
| PostgreSQL | 16 (Alpine) |
| Lombok | 1.18.46 |
| Jackson 3 | 3.1.2 |

### Frontend

| Technology | Version |
|------------|---------|
| React | 19 |
| React Router | 7 |
| TanStack Query | 5 |
| Vite | 7 |
| Tailwind CSS | 4 |
| Axios | 1.7 |
| Recharts | 2.15 |

---

## 🐳 Docker Stack

```
Services:
  ├── postgres-db    postgres:16-alpine     (healthcheck: pg_isready)
  ├── backend        eclipse-temurin:21     (healthcheck: /api/teams)
  └── frontend       nginx:alpine           (healthcheck: /)
```

```bash
# View logs
docker compose logs -f backend

# Rebuild after changes
docker compose build backend
docker compose up -d

# Stop everything
docker compose down
```

---

## 📁 Seed Data

The project includes real-world-inspired data for the 2025/26 season:

- **5 leagues** (La Liga, Premier League, Serie A, Bundesliga, Ligue 1)
- **38 teams** with realistic budgets
- **56 players** with positions, stats, and market values
- Spanish position mapping: `Portero` → `GOALKEEPER`, `Defensa` → `DEFENDER`, `Medio` → `MIDFIELDER`, `Delantero` → `FORWARD`

Trigger seeding via `POST /futbix/v1/seed` — fully idempotent.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`mvn test`) and verify frontend builds (`cd frontend && npm run build`)
5. Commit and push
6. Open a Pull Request

---

<div align="center">
  <p>
    <img src="Assets/futbol.jpeg" width="32" alt="ball" />
    Built with ❤️ for football and code
    <img src="Assets/futbol.jpeg" width="32" alt="ball" />
  </p>
</div>
