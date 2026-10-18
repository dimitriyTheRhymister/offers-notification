# offers-notification

Монорепозиторий с двумя Spring Boot сервисами: `offers-service` и `notification-service`, реализующими паттерны **Outbox** и **Inbox** для гарантированной доставки событий.

## Архитектура

┌──────────────┐     POST /api/v1/offers    ┌──────────────────┐
│   Client     │ ────────────────────────►  │  offers-service  │
└──────────────┘                            └────────┬─────────┘
│
┌──────────▼──────────┐
│  offers_db (PG)     │
│  - offers           │
│  - outbox           │
└──────────┬──────────┘
│
OutboxWorker (@Scheduled)
│
▼ POST /internal/api/v1/events
┌────────────────────────┐
│ notification-service   │
└───────────┬────────────┘
│
┌───────────▼────────────┐
│ notification_db (PG)   │
│  - inbox (unique)      │
│  - notifications       │
└────────────────────────┘

**Сервисы:**
- **offers-service** (порт 8080) — CRUD заявок, запись события `OfferCreated` в `outbox` в одной транзакции с заявкой, фоновый worker для отправки.
- **notification-service** (порт 8081) — приём событий, сохранение в `inbox` (с идемпотентностью по `event_id`), создание уведомлений `EMAIL`/`TELEGRAM`.

## Технологии

- Java 21, Spring Boot 3.2.5, Maven
- PostgreSQL 16 + Liquibase
- Spring Data JPA + Hibernate
- WebClient (offers → notification)
- Docker + Docker Compose
- Kubernetes (minikube) + nginx-ingress
- JUnit 5, Testcontainers, RestAssured

## Структура

offers-notification/
|-- offers-service/          # CRUD + Outbox
|-- notification-service/    # Inbox + notifications
|-- k8s/                     # Kubernetes manifests
|-- scripts/                 # build.sh, deploy.sh, delete.sh
|-- api-tests/offers.http    # IntelliJ HTTP-client collection
|-- docker-compose.yaml      # Local dev
`-- pom.xml                  # Parent Maven POM

## Локальный запуск (Docker Compose)

docker compose up -d --build

**Проверка:**
curl -X POST http://localhost:8080/api/v1/offers \
-H "Content-Type: application/json" \
-d '{"customerName":"Ivan","customerEmail":"ivan@example.com","customerTelegram":"@ivan","title":"Test","description":"Test"}'

Затем через 5 секунд:
curl http://localhost:8081/api/v1/notifications

Должны появиться 2 уведомления (EMAIL + TELEGRAM).

## Сборка Docker-образов

docker compose build

Или через скрипт:
./scripts/build.sh

## Развёртывание в Kubernetes

**Требования:** minikube, kubectl, Docker.

# 1. Запустить minikube
minikube start --driver=docker --cpus=2 --memory=2500 --disk-size=4g

# 2. Включить ingress addon
minikube addons enable ingress

# 3. Собрать образы и загрузить в minikube
docker compose build
minikube image load offers-notification-offers-service:latest
minikube image load offers-notification-notification-service:latest

# 4. Задеплоить
./scripts/deploy.sh

# 5. Проверить
kubectl get pods -n offers-notification

**Доступ к API через Ingress:**
curl -H "Host: offers-notification.local" http://$(minikube ip)/api/v1/offers

**Удалить всё:**
./scripts/delete.sh

## API-тесты

**Интеграционные тесты (JUnit 5 + Testcontainers):**

# Требуется Docker
mvn test

**Или только по одному сервису:**
mvn -pl offers-service test
mvn -pl notification-service test

**Через `.http`-файл (IDEA):**
- Открой `api-tests/offers.http`
- Клик по ▶ у каждого запроса.

**Ожидаемые результаты:**
- `OfferFlowTest`: 3 теста (создание → outbox, get by id → 200, get unknown → 404).
- `EventIdempotencyTest`: 3 теста (первый приём → 202, повтор → 200 без дублей, без telegram → только EMAIL).

## REST API

### offers-service

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/offers` | Создать заявку (201) |
| GET | `/api/v1/offers/{id}` | Получить по ID (200 / 404) |
| GET | `/api/v1/offers?page=0&size=20` | Список с пагинацией |
| PUT | `/api/v1/offers/{id}` | Обновить |
| DELETE | `/api/v1/offers/{id}` | Удалить (204) |

### notification-service

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/internal/api/v1/events` | Принять событие (202 новый / 200 дубликат) |
| GET | `/api/v1/events` | Список принятых событий |
| GET | `/api/v1/notifications` | Список созданных уведомлений |

## Outbox и идемпотентность

### Как работает Outbox

1. **POST /offers** в одной `@Transactional` транзакции:
    - `INSERT INTO offers` — заявка.
    - `INSERT INTO outbox` со статусом `NEW` — событие `OfferCreated`.
2. Если вторая запись падает — **обе откатываются**. Атомарность.
3. **`@Scheduled(fixedDelay=5000)`** worker:
    - Берёт `NEW`/`FAILED` с `next_attempt_at <= now()`.
    - Ставит `PROCESSING`.
    - Отправляет `POST /internal/api/v1/events` в notification-service.
    - Успех → `SENT`. Ошибка → `FAILED`, `retry_count++`, экспоненциальная задержка.

### Как работает Inbox

1. При приёме события — `findByEventId`.
2. Если событие **уже есть** → **`200 OK`**, ничего не создаётся.
3. Если **новое**:
    - `INSERT INTO inbox` (с `event_id`, `payload`).
    - `INSERT INTO notifications` — 1–2 уведомления (EMAIL + TELEGRAM, если заполнены).
    - `UPDATE inbox SET processed_at = now()`.
4. Транзакция — атомарная.

**Идемпотентность обеспечивается:**
- Уникальным индексом `inbox.event_id`.
- Проверкой `findByEventId` до вставки.
- Возвратом `200 OK` при дубликате.

### At-least-once vs At-most-once

- Worker гарантирует **at-least-once** — событие доставится, если notification-service вернётся.
- При ошибке — retry с экспоненциальной задержкой (2 → 4 → 8 → ... → 60 сек).
- Notification-service **идемпотентен** — повторная доставка не создаёт дублей.

## Известные ограничения

- **Email/Telegram не отправляются реально** — только сохраняются в БД (по заданию).
- **БД в K8s используют `emptyDir`** — данные не сохраняются между перезапусками Pod. Для прода — PersistentVolumeClaim.
- **Без аутентификации** — по заданию.