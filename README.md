# Rate Limiting Service

A production-ready rate limiting service built with Spring Boot, implementing Token Bucket algorithm with Redis for distributed rate limiting.

## 🎯 Overview

This service provides rate limiting functionality using the Token Bucket algorithm. It intercepts requests at the gateway level, tracks token consumption per client (IP address), and enforces rate limits. The implementation follows SOLID principles and uses design patterns for maintainability.

**Key Features:**
- Token Bucket rate limiting algorithm
- Distributed rate limiting using Redis
- Per-client rate limiting (based on IP address)
- Spring Cloud Gateway integration
- RESTful APIs for health check and status

## 🛠️ Tech Stack

- **Java 21** - Programming language
- **Spring Boot 3.4.1** - Application framework
- **Spring Cloud Gateway** - API Gateway
- **Redis** - Distributed state storage
- **Jedis 5.2.0** - Redis Java client
- **Docker & Docker Compose** - Containerization
- **Gradle** - Build tool

## 📦 Dependencies

- `spring-boot-starter-data-redis` - Redis integration
- `spring-cloud-starter-gateway` - API Gateway
- `redis.clients:jedis:5.2.0` - Redis client library
- `lombok` - Code generation

## 🚀 Local Setup

### Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Git (optional)

### Step 1: Start Redis

```bash
docker compose up -d
```

Verify Redis is running:
```bash
docker ps
```

### Step 2: Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Step 3: Verify Setup

Test the health endpoint:
```bash
curl http://localhost:8080/gateway/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "Rate Limiter Gateway"
}
```

## 🧪 Testing APIs

### 1. Health Check

```bash
curl --location 'http://localhost:8080/gateway/health'
```

**Response:** Service status

---

### 2. Rate Limit Status

```bash
curl --location 'http://localhost:8080/gateway/rate-limit/status'
```

**Response:**
```json
{
  "clientId": "127.0.0.1",
  "capacity": 10,
  "availableTokens": 10
}
```

**Note:** This endpoint does NOT consume tokens.

---

### 3. Get Users (Rate Limited)

```bash
curl --location 'http://localhost:8080/api/users'
```

**Success (200 OK):**
```json
{
  "users": [...],
  "total": 5,
  "message": "Users retrieved successfully"
}
```

**Rate Limit Exceeded (429):**
```json
{
  "error": "Rate limit exceeded",
  "clientId": "127.0.0.1"
}
```

**Testing Rate Limiting:**
- Send 11+ requests rapidly
- First 10 requests: Status 200
- 11th request: Status 429 (rate limit exceeded)

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Rate Limiter Settings
rate-limiter.capacity=10          # Maximum tokens (burst limit)
rate-limiter.refill-rate=5        # Tokens per refill interval
rate-limiter.refill-interval=100000  # Refill interval in ms (100 seconds)

# Redis Settings
spring.redis.host=localhost
spring.redis.port=6379
```

**Default Behavior:**
- Allow 10 requests immediately
- Refill 5 tokens every 100 seconds

## 🏗️ Architecture

```
Client Request
    ↓
Spring Cloud Gateway
    ↓
Rate Limiter Filter (checks tokens)
    ↓
Rate Limiter Service
    ↓
Token Bucket Strategy
    ↓
Redis (stores token state)
```

**Key Components:**
- **Gateway Filter**: Intercepts and rate limits requests
- **Token Bucket Strategy**: Implements rate limiting algorithm
- **Redis Repository**: Stores token state per client
- **Algorithm**: Pure token bucket logic

## 📁 Project Structure

```
src/main/java/com/example/RateLimiting/
├── algorithm/          # Token bucket algorithm
├── config/             # Configuration classes
├── controller/         # REST endpoints
├── extractor/          # Client ID extraction
├── filter/             # Gateway filter
├── repository/         # Data access layer
├── service/            # Service layer
└── strategy/           # Rate limiting strategies
```

## 🛑 Stop Services

```bash
# Stop Redis
docker compose down

# Stop application
Ctrl + C (in terminal running bootRun)
```

## 📚 Documentation

For detailed implementation details, SOLID principles, and design patterns, see [IMPLEMENTATION.md](./IMPLEMENTATION.md)

## 🔧 Troubleshooting

**Redis connection error:**
```bash
docker compose up -d
docker ps  # Verify Redis is running
```

**Port 8080 already in use:**
- Change port in `application.properties`: `server.port=8081`

**Java version error:**
- Ensure Java 21 is installed: `java -version`

---

**Happy Coding! 🚀**
