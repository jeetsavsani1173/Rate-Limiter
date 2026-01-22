# Implementation Documentation - SOLID Principles & Design Patterns

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [SOLID Principles Implementation](#solid-principles-implementation)
4. [Design Patterns Applied](#design-patterns-applied)
5. [Class-by-Class Analysis](#class-by-class-analysis)
6. [Data Flow](#data-flow)
7. [Extension Points](#extension-points)

---

## 🎯 Overview

This document provides an in-depth analysis of how SOLID principles and design patterns are implemented throughout the Rate Limiting application. Each component is designed with maintainability, testability, and extensibility in mind.

### **Key Design Goals:**
- ✅ Follow SOLID principles strictly
- ✅ Apply appropriate design patterns
- ✅ Enable easy testing
- ✅ Support future extensions
- ✅ Maintain clean separation of concerns

---

## 🏗️ Architecture

### **High-Level Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Request                           │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Cloud Gateway                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      TokenBucketRateLimiterFilter                     │  │
│  │  (Extracts Client ID, Checks Rate Limit)              │  │
│  └──────────────────────┬─────────────────────────────────┘  │
└─────────────────────────┼─────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              RateLimiterService (Facade)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      RateLimiterStrategy (Interface)                 │  │
│  │              │                                        │  │
│  │              ▼                                        │  │
│  │      TokenBucketStrategy                             │  │
│  │              │                                        │  │
│  │    ┌─────────┴─────────┐                            │  │
│  │    ▼                    ▼                            │  │
│  │ TokenBucketAlgorithm  TokenBucketRepository          │  │
│  │ (Pure Logic)          (Interface)                    │  │
│  │                            │                         │  │
│  │                            ▼                         │  │
│  │                    RedisTokenBucketRepository        │  │
│  │                            │                         │  │
│  │                            ▼                         │  │
│  │                         Redis (Jedis)                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### **Package Structure**

```
com.example.RateLimiting/
├── algorithm/          # Pure algorithm logic (SRP)
│   └── TokenBucketAlgorithm
├── config/             # Configuration classes
│   ├── GatewayConfig
│   ├── RateLimiterProperties
│   ├── RedisConfig
│   └── RedisProperties
├── controller/         # REST endpoints
│   ├── StatusController
│   └── UsersController
├── extractor/          # Client identification (SRP)
│   ├── ClientIdentifierExtractor (Interface)
│   └── IpBasedClientIdentifierExtractor
├── filter/             # Gateway filter
│   └── TokenBucketRateLimiterFilter
├── repository/         # Data access layer (Repository Pattern)
│   ├── TokenBucketRepository (Interface)
│   ├── TokenBucketState (Value Object)
│   ├── RedisKeyGenerator
│   └── RedisTokenBucketRepository
├── service/            # Service layer (Facade Pattern)
│   └── RateLimiterService
└── strategy/           # Strategy Pattern
    ├── RateLimiterStrategy (Interface)
    ├── TokenBucketStrategy
    └── RateLimiterStrategyFactory
```

---

## 🔷 SOLID Principles Implementation

### **1. Single Responsibility Principle (SRP)**

**Principle:** A class should have only one reason to change.

#### **✅ Implementation Examples:**

##### **TokenBucketAlgorithm**
```java
public class TokenBucketAlgorithm {
    // ONLY algorithm logic - no storage, no HTTP, no configuration
    public TokenBucketState refillTokens(...) { }
    public TokenBucketState consumeToken(...) { }
}
```
**Responsibility:** Pure token bucket algorithm calculations
**Why it changes:** Only when algorithm logic needs modification

##### **RedisTokenBucketRepository**
```java
public class RedisTokenBucketRepository implements TokenBucketRepository {
    // ONLY data persistence - no algorithm, no business logic
    public TokenBucketState getState(String clientId) { }
    public void saveState(String clientId, TokenBucketState state) { }
}
```
**Responsibility:** Redis data access operations
**Why it changes:** Only when storage mechanism changes

##### **RedisKeyGenerator**
```java
public class RedisKeyGenerator {
    // ONLY key generation - single responsibility
    public static String getTokenKey(String clientId) { }
    public static String getLastRefillKey(String clientId) { }
}
```
**Responsibility:** Redis key generation logic
**Why it changes:** Only when key naming convention changes

##### **IpBasedClientIdentifierExtractor**
```java
public class IpBasedClientIdentifierExtractor implements ClientIdentifierExtractor {
    // ONLY client ID extraction - no rate limiting logic
    public String extractClientId(ServerHttpRequest request) { }
}
```
**Responsibility:** Extract client identifier from HTTP request
**Why it changes:** Only when client identification method changes

##### **TokenBucketRateLimiterFilter**
```java
public class TokenBucketRateLimiterFilter {
    // ONLY HTTP request/response handling - delegates rate limiting
    public GatewayFilter apply(Config config) {
        // Extract client ID
        // Check rate limit (delegates to service)
        // Handle response
    }
}
```
**Responsibility:** HTTP request/response handling for rate limiting
**Why it changes:** Only when HTTP handling needs modification

**Before Refactoring (Violation):**
```java
// ❌ BAD: Multiple responsibilities
class RedisTokenBucketService {
    // Algorithm logic
    // Redis operations
    // Key management
    // Time calculations
    // All mixed together!
}
```

**After Refactoring (Compliant):**
```java
// ✅ GOOD: Single responsibility per class
TokenBucketAlgorithm        // Algorithm only
RedisTokenBucketRepository  // Storage only
RedisKeyGenerator          // Keys only
TokenBucketStrategy        // Orchestration only
```

---

### **2. Open/Closed Principle (OCP)**

**Principle:** Software entities should be open for extension but closed for modification.

#### **✅ Implementation Examples:**

##### **RateLimiterStrategy Interface**
```java
public interface RateLimiterStrategy {
    boolean isAllowed(String clientId);
    long getAvailableTokens(String clientId);
    long getCapacity();
}
```

**Extension (Adding New Algorithm):**
```java
// ✅ Can add new algorithm WITHOUT modifying existing code
public class SlidingWindowStrategy implements RateLimiterStrategy {
    // New implementation
}

// ✅ Factory can create it without changing existing code
public RateLimiterStrategy createStrategy(String type) {
    return switch(type) {
        case "token-bucket" -> createTokenBucketStrategy();
        case "sliding-window" -> new SlidingWindowStrategy(...); // NEW
        // No modification to existing code!
    };
}
```

**Benefits:**
- ✅ Add new algorithms without touching `TokenBucketStrategy`
- ✅ Add new strategies without modifying `RateLimiterService`
- ✅ Extend functionality without breaking existing code

##### **TokenBucketRepository Interface**
```java
public interface TokenBucketRepository {
    TokenBucketState getState(String clientId);
    void saveState(String clientId, TokenBucketState state);
}
```

**Extension (Adding New Storage):**
```java
// ✅ Can add new storage WITHOUT modifying existing code
public class DatabaseTokenBucketRepository implements TokenBucketRepository {
    // New implementation using database
}

// ✅ Strategy can use it without modification
TokenBucketStrategy strategy = new TokenBucketStrategy(
    algorithm,
    new DatabaseTokenBucketRepository(...) // NEW storage
);
```

##### **ClientIdentifierExtractor Interface**
```java
public interface ClientIdentifierExtractor {
    String extractClientId(ServerHttpRequest request);
}
```

**Extension (Adding New Extraction Method):**
```java
// ✅ Can add new extraction method WITHOUT modifying existing code
public class ApiKeyBasedExtractor implements ClientIdentifierExtractor {
    // Extract from API key header
}

// ✅ Filter can use it without modification
filter = new TokenBucketRateLimiterFilter(
    service,
    new ApiKeyBasedExtractor() // NEW extractor
);
```

**Before Refactoring (Violation):**
```java
// ❌ BAD: Need to modify existing code to add new algorithm
class RateLimiterService {
    public boolean isAllowed(String clientId) {
        // Hard-coded token bucket logic
        // To add sliding window, must modify this class!
    }
}
```

**After Refactoring (Compliant):**
```java
// ✅ GOOD: Extend via new implementations
interface RateLimiterStrategy { }
class TokenBucketStrategy implements RateLimiterStrategy { }
class SlidingWindowStrategy implements RateLimiterStrategy { } // NEW - no modification!
```

---

### **3. Liskov Substitution Principle (LSP)**

**Principle:** Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.

#### **✅ Implementation Examples:**

##### **RateLimiterStrategy Substitution**
```java
// ✅ Any implementation can replace another
RateLimiterStrategy strategy1 = new TokenBucketStrategy(...);
RateLimiterStrategy strategy2 = new SlidingWindowStrategy(...);

// Both can be used interchangeably
RateLimiterService service1 = new RateLimiterService(factory);
// service1 uses TokenBucketStrategy

// Can switch to different strategy without code changes
RateLimiterService service2 = new RateLimiterService(factory);
// service2 uses SlidingWindowStrategy

// Service code doesn't change - strategies are substitutable
```

##### **TokenBucketRepository Substitution**
```java
// ✅ Any repository implementation can replace another
TokenBucketRepository repo1 = new RedisTokenBucketRepository(...);
TokenBucketRepository repo2 = new DatabaseTokenBucketRepository(...);
TokenBucketRepository repo3 = new InMemoryTokenBucketRepository(...);

// All can be used in TokenBucketStrategy
TokenBucketStrategy strategy1 = new TokenBucketStrategy(algorithm, repo1);
TokenBucketStrategy strategy2 = new TokenBucketStrategy(algorithm, repo2);
TokenBucketStrategy strategy3 = new TokenBucketStrategy(algorithm, repo3);

// Strategy code doesn't change - repositories are substitutable
```

##### **ClientIdentifierExtractor Substitution**
```java
// ✅ Any extractor can replace another
ClientIdentifierExtractor ext1 = new IpBasedClientIdentifierExtractor();
ClientIdentifierExtractor ext2 = new ApiKeyBasedExtractor();
ClientIdentifierExtractor ext3 = new JwtBasedExtractor();

// All can be used in filter
TokenBucketRateLimiterFilter filter1 = new TokenBucketRateLimiterFilter(service, ext1);
TokenBucketRateLimiterFilter filter2 = new TokenBucketRateLimiterFilter(service, ext2);
TokenBucketRateLimiterFilter filter3 = new TokenBucketRateLimiterFilter(service, ext3);

// Filter code doesn't change - extractors are substitutable
```

**Real-World Example:**
```java
// In tests, can substitute with mock implementations
@Test
void testStrategy() {
    TokenBucketRepository mockRepo = mock(TokenBucketRepository.class);
    TokenBucketAlgorithm algorithm = new TokenBucketAlgorithm(10, 5, 1000);
    
    // Mock repository substitutes real repository
    TokenBucketStrategy strategy = new TokenBucketStrategy(algorithm, mockRepo);
    
    // Works exactly the same!
    assertTrue(strategy.isAllowed("client-1"));
}
```

---

### **4. Interface Segregation Principle (ISP)**

**Principle:** Clients should not be forced to depend on interfaces they don't use.

#### **✅ Implementation Examples:**

##### **Separate Interfaces for Different Concerns**

**RateLimiterStrategy Interface:**
```java
// ✅ Clients that only need rate limiting check
public interface RateLimiterStrategy {
    boolean isAllowed(String clientId);  // Core functionality
    long getAvailableTokens(String clientId);  // Status check
    long getCapacity();  // Configuration
}
```

**TokenBucketRepository Interface:**
```java
// ✅ Clients that only need data access
public interface TokenBucketRepository {
    TokenBucketState getState(String clientId);  // Read
    void saveState(String clientId, TokenBucketState state);  // Write
}
```

**ClientIdentifierExtractor Interface:**
```java
// ✅ Clients that only need client identification
public interface ClientIdentifierExtractor {
    String extractClientId(ServerHttpRequest request);  // Single method
}
```

**Benefits:**
- ✅ `TokenBucketStrategy` doesn't depend on HTTP request details
- ✅ `TokenBucketRateLimiterFilter` doesn't depend on repository details
- ✅ Each interface is focused and minimal

**Before Refactoring (Violation):**
```java
// ❌ BAD: Fat interface forcing clients to depend on unused methods
interface RateLimiterService {
    boolean isAllowed(String clientId);
    long getAvailableTokens(String clientId);
    void configureRateLimit(...);  // Not needed by filter
    void resetTokens(...);  // Not needed by filter
    void exportMetrics(...);  // Not needed by filter
}
```

**After Refactoring (Compliant):**
```java
// ✅ GOOD: Segregated interfaces
interface RateLimiterStrategy {
    boolean isAllowed(String clientId);
    long getAvailableTokens(String clientId);
}

interface RateLimiterConfigurator {
    void configureRateLimit(...);
}

interface RateLimiterMetrics {
    void exportMetrics(...);
}
```

---

### **5. Dependency Inversion Principle (DIP)**

**Principle:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

#### **✅ Implementation Examples:**

##### **Service Layer Dependencies**
```java
// ✅ HIGH-LEVEL: RateLimiterService depends on abstraction
@Service
public class RateLimiterService {
    private final RateLimiterStrategyFactory strategyFactory;  // Abstraction
    
    // Depends on factory interface, not concrete implementation
    private RateLimiterStrategy getStrategy() {
        return strategyFactory.createDefaultStrategy();  // Returns interface
    }
}
```

##### **Strategy Dependencies**
```java
// ✅ HIGH-LEVEL: TokenBucketStrategy depends on abstractions
public class TokenBucketStrategy implements RateLimiterStrategy {
    private final TokenBucketAlgorithm algorithm;  // Concrete (algorithm is stable)
    private final TokenBucketRepository repository;  // ✅ Interface (abstraction)
    
    // Depends on repository interface, not Redis implementation
}
```

##### **Repository Dependencies**
```java
// ✅ HIGH-LEVEL: Repository interface (abstraction)
public interface TokenBucketRepository {
    TokenBucketState getState(String clientId);
    void saveState(String clientId, TokenBucketState state);
}

// ✅ LOW-LEVEL: Redis implementation
public class RedisTokenBucketRepository implements TokenBucketRepository {
    // Implements interface, can be swapped
}
```

##### **Filter Dependencies**
```java
// ✅ HIGH-LEVEL: Filter depends on abstractions
@Component
public class TokenBucketRateLimiterFilter {
    private final RateLimiterService rateLimiterService;  // Service abstraction
    private final ClientIdentifierExtractor extractor;  // ✅ Interface (abstraction)
    
    // Depends on extractor interface, not IP-based implementation
}
```

**Dependency Hierarchy:**
```
RateLimiterService (High-Level)
    ↓ depends on
RateLimiterStrategyFactory (Abstraction)
    ↓ creates
RateLimiterStrategy (Interface - Abstraction)
    ↓ implemented by
TokenBucketStrategy (High-Level)
    ↓ depends on
TokenBucketRepository (Interface - Abstraction)
    ↓ implemented by
RedisTokenBucketRepository (Low-Level)
    ↓ uses
JedisPool (Low-Level)
```

**Before Refactoring (Violation):**
```java
// ❌ BAD: High-level depends on low-level directly
class RateLimiterService {
    private final RedisTokenBucketService redisService;  // Concrete dependency!
    // Tightly coupled to Redis implementation
}
```

**After Refactoring (Compliant):**
```java
// ✅ GOOD: High-level depends on abstraction
class RateLimiterService {
    private final RateLimiterStrategyFactory factory;  // Abstraction
    // Can work with any strategy implementation
}
```

---

## 🎨 Design Patterns Applied

### **1. Strategy Pattern** ⭐

**Purpose:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

#### **Implementation:**

**Interface:**
```java
public interface RateLimiterStrategy {
    boolean isAllowed(String clientId);
    long getAvailableTokens(String clientId);
    long getCapacity();
}
```

**Concrete Strategy:**
```java
public class TokenBucketStrategy implements RateLimiterStrategy {
    private final TokenBucketAlgorithm algorithm;
    private final TokenBucketRepository repository;
    
    @Override
    public boolean isAllowed(String clientId) {
        // Token bucket algorithm implementation
    }
}
```

**Context (Service):**
```java
@Service
public class RateLimiterService {
    private final RateLimiterStrategyFactory factory;
    private RateLimiterStrategy strategy;  // Uses strategy interface
    
    public boolean isRequestAllowed(String clientId) {
        return getStrategy().isAllowed(clientId);  // Delegates to strategy
    }
}
```

**Benefits:**
- ✅ Easy to add new algorithms (SlidingWindowStrategy, FixedWindowStrategy)
- ✅ Runtime algorithm selection
- ✅ Follows Open/Closed Principle
- ✅ Algorithms are interchangeable

**Usage Example:**
```java
// Can switch strategies without changing service code
RateLimiterStrategy tokenBucket = factory.createStrategy("token-bucket");
RateLimiterStrategy slidingWindow = factory.createStrategy("sliding-window");

// Both work with same service
service.setStrategy(tokenBucket);  // Use token bucket
service.setStrategy(slidingWindow);  // Switch to sliding window
```

---

### **2. Repository Pattern**

**Purpose:** Abstract data access logic from business logic.

#### **Implementation:**

**Interface:**
```java
public interface TokenBucketRepository {
    TokenBucketState getState(String clientId);
    void saveState(String clientId, TokenBucketState state);
}
```

**Concrete Repository:**
```java
public class RedisTokenBucketRepository implements TokenBucketRepository {
    private final JedisPool jedisPool;
    
    @Override
    public TokenBucketState getState(String clientId) {
        // Redis-specific implementation
    }
    
    @Override
    public void saveState(String clientId, TokenBucketState state) {
        // Redis-specific implementation
    }
}
```

**Usage in Strategy:**
```java
public class TokenBucketStrategy {
    private final TokenBucketRepository repository;  // Depends on interface
    
    public boolean isAllowed(String clientId) {
        TokenBucketState state = repository.getState(clientId);  // Uses abstraction
        // ... algorithm logic ...
        repository.saveState(clientId, newState);  // Uses abstraction
    }
}
```

**Benefits:**
- ✅ Can switch storage (Redis → Database → In-Memory)
- ✅ Business logic doesn't know about storage details
- ✅ Easy to test (mock repository)
- ✅ Follows Dependency Inversion Principle

**Extension Example:**
```java
// Can add new storage without changing strategy
public class DatabaseTokenBucketRepository implements TokenBucketRepository {
    // Database implementation
}

public class InMemoryTokenBucketRepository implements TokenBucketRepository {
    // In-memory implementation for testing
}

// Strategy works with any implementation
TokenBucketStrategy strategy1 = new TokenBucketStrategy(algorithm, redisRepo);
TokenBucketStrategy strategy2 = new TokenBucketStrategy(algorithm, dbRepo);
TokenBucketStrategy strategy3 = new TokenBucketStrategy(algorithm, memoryRepo);
```

---

### **3. Factory Pattern**

**Purpose:** Create objects without specifying the exact class of object that will be created.

#### **Implementation:**

**Factory:**
```java
@Component
public class RateLimiterStrategyFactory {
    private final RateLimiterProperties properties;
    private final JedisPool jedisPool;
    
    public RateLimiterStrategy createStrategy(String strategyType) {
        return switch (strategyType.toLowerCase()) {
            case "token-bucket" -> createTokenBucketStrategy();
            case "sliding-window" -> createSlidingWindowStrategy();  // Future
            default -> throw new IllegalArgumentException("Unsupported strategy");
        };
    }
    
    private RateLimiterStrategy createTokenBucketStrategy() {
        TokenBucketAlgorithm algorithm = new TokenBucketAlgorithm(
            properties.getCapacity(),
            properties.getRefillRate(),
            properties.getRefillInterval()
        );
        
        TokenBucketRepository repository = new RedisTokenBucketRepository(
            jedisPool,
            properties.getCapacity()
        );
        
        return new TokenBucketStrategy(algorithm, repository);
    }
}
```

**Benefits:**
- ✅ Centralized object creation
- ✅ Hides complex object construction
- ✅ Easy to extend with new strategies
- ✅ Configuration-driven creation

**Usage:**
```java
// Client doesn't know how strategy is created
RateLimiterStrategy strategy = factory.createStrategy("token-bucket");
// Factory handles all the complexity
```

---

### **4. Value Object Pattern**

**Purpose:** Represent a simple value that has no identity.

#### **Implementation:**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBucketState {
    private long tokenCount;
    private long lastRefillTime;
    
    public boolean hasTokens() {
        return tokenCount > 0;
    }
    
    public static TokenBucketState createInitialState(long capacity, long currentTime) {
        return new TokenBucketState(capacity, currentTime);
    }
}
```

**Benefits:**
- ✅ Immutable data container
- ✅ Type safety
- ✅ Clear data structure
- ✅ Easy to pass around

**Usage:**
```java
// Value object passed between layers
TokenBucketState state = repository.getState(clientId);
TokenBucketState newState = algorithm.refillTokens(state, currentTime);
repository.saveState(clientId, newState);
```

---

### **5. Facade Pattern**

**Purpose:** Provide a simplified interface to a complex subsystem.

#### **Implementation:**

```java
@Service
public class RateLimiterService {
    private final RateLimiterStrategyFactory strategyFactory;
    
    // Simple facade methods
    public boolean isRequestAllowed(String clientId) {
        return getStrategy().isAllowed(clientId);
    }
    
    public long getRemainingTokens(String clientId) {
        return getStrategy().getAvailableTokens(clientId);
    }
    
    public long getCapacity() {
        return getStrategy().getCapacity();
    }
}
```

**Benefits:**
- ✅ Simple API for clients
- ✅ Hides complexity of strategy selection
- ✅ Single entry point
- ✅ Easy to use

---

## 📦 Class-by-Class Analysis

### **Algorithm Layer**

#### **TokenBucketAlgorithm**
- **SOLID:** ✅ SRP (only algorithm logic)
- **Pattern:** Pure algorithm class (no pattern, just good design)
- **Responsibilities:**
  - Calculate token refills based on elapsed time
  - Consume tokens from bucket
  - No storage, no HTTP, no configuration

---

### **Repository Layer**

#### **TokenBucketRepository (Interface)**
- **SOLID:** ✅ DIP (abstraction), ✅ ISP (focused interface)
- **Pattern:** Repository Pattern
- **Responsibilities:**
  - Define data access contract
  - Abstract storage implementation

#### **RedisTokenBucketRepository**
- **SOLID:** ✅ SRP (only data access), ✅ DIP (implements interface)
- **Pattern:** Repository Pattern implementation
- **Responsibilities:**
  - Redis GET/SET operations
  - Key generation (delegates to RedisKeyGenerator)
  - No business logic

#### **RedisKeyGenerator**
- **SOLID:** ✅ SRP (only key generation)
- **Pattern:** Utility class
- **Responsibilities:**
  - Generate Redis keys consistently
  - Centralize key naming logic

#### **TokenBucketState**
- **SOLID:** ✅ Value object (immutable)
- **Pattern:** Value Object Pattern
- **Responsibilities:**
  - Hold token bucket state
  - Provide helper methods

---

### **Strategy Layer**

#### **RateLimiterStrategy (Interface)**
- **SOLID:** ✅ OCP (extensible), ✅ LSP (substitutable), ✅ ISP (focused)
- **Pattern:** Strategy Pattern interface
- **Responsibilities:**
  - Define rate limiting contract
  - Enable algorithm substitution

#### **TokenBucketStrategy**
- **SOLID:** ✅ SRP (orchestration), ✅ DIP (depends on interfaces)
- **Pattern:** Strategy Pattern implementation
- **Responsibilities:**
  - Orchestrate algorithm and repository
  - Implement token bucket strategy
  - No direct storage or HTTP handling

#### **RateLimiterStrategyFactory**
- **SOLID:** ✅ SRP (object creation), ✅ DIP (creates abstractions)
- **Pattern:** Factory Pattern
- **Responsibilities:**
  - Create strategy instances
  - Configure dependencies
  - Centralize creation logic

---

### **Service Layer**

#### **RateLimiterService**
- **SOLID:** ✅ DIP (depends on factory), ✅ SRP (service facade)
- **Pattern:** Facade Pattern
- **Responsibilities:**
  - Provide simple API
  - Delegate to strategy
  - Hide complexity

---

### **Filter Layer**

#### **TokenBucketRateLimiterFilter**
- **SOLID:** ✅ SRP (HTTP handling), ✅ DIP (depends on interfaces)
- **Pattern:** Gateway Filter (Spring Cloud Gateway pattern)
- **Responsibilities:**
  - Extract client ID
  - Check rate limit
  - Handle HTTP responses
  - Add rate limit headers

---

### **Extractor Layer**

#### **ClientIdentifierExtractor (Interface)**
- **SOLID:** ✅ ISP (single method), ✅ DIP (abstraction)
- **Pattern:** Strategy Pattern (for extraction)
- **Responsibilities:**
  - Define client identification contract
  - Enable different extraction methods

#### **IpBasedClientIdentifierExtractor**
- **SOLID:** ✅ SRP (only extraction), ✅ DIP (implements interface)
- **Pattern:** Strategy Pattern implementation
- **Responsibilities:**
  - Extract client ID from IP address
  - Handle X-Forwarded-For header
  - Fallback to direct connection IP

---

### **Configuration Layer**

#### **GatewayConfig**
- **SOLID:** ✅ SRP (routing configuration), ✅ DIP (depends on abstractions)
- **Pattern:** Configuration class
- **Responsibilities:**
  - Configure gateway routes
  - Apply filters
  - Define routing rules

#### **RateLimiterProperties**
- **SOLID:** ✅ SRP (configuration only)
- **Pattern:** Configuration Properties
- **Responsibilities:**
  - Hold rate limiter configuration
  - Load from application.properties

#### **RedisConfig**
- **SOLID:** ✅ SRP (Redis connection configuration)
- **Pattern:** Configuration class
- **Responsibilities:**
  - Create JedisPool bean
  - Configure connection pool

#### **RedisProperties**
- **SOLID:** ✅ SRP (Redis properties only)
- **Pattern:** Configuration Properties
- **Responsibilities:**
  - Hold Redis connection properties
  - Load from application.properties

---

### **Controller Layer**

#### **StatusController**
- **SOLID:** ✅ SRP (status endpoints)
- **Pattern:** REST Controller
- **Responsibilities:**
  - Health check endpoint
  - Rate limit status endpoint
  - No rate limiting logic (delegates to service)

#### **UsersController**
- **SOLID:** ✅ SRP (user endpoints)
- **Pattern:** REST Controller
- **Responsibilities:**
  - User list endpoint
  - Rate limited via gateway filter
  - No rate limiting logic

---

## 🔄 Data Flow

### **Request Flow (Rate Limited Endpoint)**

```
1. Client Request
   ↓
2. Spring Cloud Gateway
   ↓
3. TokenBucketRateLimiterFilter
   ├─→ ClientIdentifierExtractor.extractClientId()
   │   └─→ Returns: "192.168.1.1"
   │
   ├─→ RateLimiterService.isRequestAllowed("192.168.1.1")
   │   ├─→ RateLimiterStrategyFactory.createDefaultStrategy()
   │   │   ├─→ Creates TokenBucketAlgorithm
   │   │   ├─→ Creates RedisTokenBucketRepository
   │   │   └─→ Creates TokenBucketStrategy
   │   │
   │   └─→ TokenBucketStrategy.isAllowed("192.168.1.1")
   │       ├─→ TokenBucketRepository.getState("192.168.1.1")
   │       │   └─→ RedisTokenBucketRepository.getState()
   │       │       ├─→ RedisKeyGenerator.getTokenKey()
   │       │       ├─→ Redis GET "rate_limiter:tokens:192.168.1.1"
   │       │       └─→ Returns TokenBucketState
   │       │
   │       ├─→ TokenBucketAlgorithm.refillTokens(state, currentTime)
   │       │   └─→ Calculates new token count
   │       │
   │       ├─→ TokenBucketAlgorithm.consumeToken(refilledState)
   │       │   └─→ Decrements token count
   │       │
   │       └─→ TokenBucketRepository.saveState("192.168.1.1", newState)
   │           └─→ RedisTokenBucketRepository.saveState()
   │               └─→ Redis SET operations
   │
   └─→ If allowed: Forward to UsersController
       If not allowed: Return HTTP 429
```

### **State Management Flow**

```
Initial State:
  Redis: rate_limiter:tokens:192.168.1.1 = "10"
         rate_limiter:last_refill:192.168.1.1 = "1705932000000"

Request 1:
  1. Get state: tokenCount=10, lastRefillTime=1705932000000
  2. Refill: elapsedTime=5000ms, intervalsPassed=0, tokensToAdd=0
  3. Consume: tokenCount=9
  4. Save: rate_limiter:tokens:192.168.1.1 = "9"

Request 2:
  1. Get state: tokenCount=9, lastRefillTime=1705932000000
  2. Refill: elapsedTime=100ms, intervalsPassed=0, tokensToAdd=0
  3. Consume: tokenCount=8
  4. Save: rate_limiter:tokens:192.168.1.1 = "8"

... (continues until tokens exhausted)

Request 11:
  1. Get state: tokenCount=0, lastRefillTime=1705932000000
  2. Refill: elapsedTime=5000ms, intervalsPassed=0, tokensToAdd=0
  3. Consume: tokenCount=0 (no tokens available)
  4. Return: false (rate limit exceeded)
```

---

## 🔌 Extension Points

### **1. Adding New Rate Limiting Algorithm**

**Steps:**
1. Create new strategy class:
```java
public class SlidingWindowStrategy implements RateLimiterStrategy {
    // Implementation
}
```

2. Add to factory:
```java
public RateLimiterStrategy createStrategy(String type) {
    return switch(type) {
        case "sliding-window" -> new SlidingWindowStrategy(...);
        // ...
    };
}
```

**SOLID Compliance:**
- ✅ OCP: Extends without modifying existing code
- ✅ LSP: New strategy is substitutable
- ✅ DIP: Depends on same interfaces

---

### **2. Adding New Storage Backend**

**Steps:**
1. Create new repository:
```java
public class DatabaseTokenBucketRepository implements TokenBucketRepository {
    // Database implementation
}
```

2. Use in factory:
```java
TokenBucketRepository repo = new DatabaseTokenBucketRepository(...);
```

**SOLID Compliance:**
- ✅ OCP: New storage without modifying strategy
- ✅ LSP: Repository is substitutable
- ✅ DIP: Strategy depends on interface

---

### **3. Adding New Client Identification Method**

**Steps:**
1. Create new extractor:
```java
public class ApiKeyExtractor implements ClientIdentifierExtractor {
    // Extract from API key header
}
```

2. Use in filter:
```java
filter = new TokenBucketRateLimiterFilter(service, new ApiKeyExtractor());
```

**SOLID Compliance:**
- ✅ OCP: New extractor without modifying filter
- ✅ LSP: Extractor is substitutable
- ✅ ISP: Single method interface

---

## 📊 SOLID Principles Summary Table

| Class | SRP | OCP | LSP | ISP | DIP | Patterns |
|-------|-----|-----|-----|-----|-----|----------|
| TokenBucketAlgorithm | ✅ | ✅ | ✅ | ✅ | ✅ | Pure Algorithm |
| TokenBucketRepository | ✅ | ✅ | ✅ | ✅ | ✅ | Repository |
| RedisTokenBucketRepository | ✅ | ✅ | ✅ | ✅ | ✅ | Repository Impl |
| RateLimiterStrategy | ✅ | ✅ | ✅ | ✅ | ✅ | Strategy |
| TokenBucketStrategy | ✅ | ✅ | ✅ | ✅ | ✅ | Strategy Impl |
| RateLimiterStrategyFactory | ✅ | ✅ | ✅ | ✅ | ✅ | Factory |
| RateLimiterService | ✅ | ✅ | ✅ | ✅ | ✅ | Facade |
| TokenBucketRateLimiterFilter | ✅ | ✅ | ✅ | ✅ | ✅ | Gateway Filter |
| ClientIdentifierExtractor | ✅ | ✅ | ✅ | ✅ | ✅ | Strategy |
| IpBasedClientIdentifierExtractor | ✅ | ✅ | ✅ | ✅ | ✅ | Strategy Impl |

---

## 🎓 Key Takeaways

### **SOLID Principles:**
1. **SRP:** Each class has one clear responsibility
2. **OCP:** Easy to extend without modification
3. **LSP:** All implementations are substitutable
4. **ISP:** Interfaces are focused and minimal
5. **DIP:** Depend on abstractions, not concretions

### **Design Patterns:**
1. **Strategy:** Different algorithms, interchangeable
2. **Repository:** Abstract data access
3. **Factory:** Centralized object creation
4. **Value Object:** Immutable data containers
5. **Facade:** Simplified service interface

### **Benefits:**
- ✅ Highly testable (mockable interfaces)
- ✅ Easy to extend (new algorithms/storage)
- ✅ Maintainable (clear separation)
- ✅ Flexible (runtime selection)
- ✅ Production-ready (best practices)

---

## 📝 Conclusion

This implementation demonstrates a **production-grade** application of SOLID principles and design patterns. Every class follows at least one SOLID principle, and multiple design patterns work together to create a maintainable, testable, and extensible codebase.

The architecture allows for:
- ✅ Easy testing (mockable components)
- ✅ Simple extensions (new algorithms/storage)
- ✅ Runtime flexibility (strategy selection)
- ✅ Clear responsibilities (single purpose classes)
- ✅ Loose coupling (interface-based design)

**This codebase is ready for production use and serves as an excellent example for LLD interviews!** 🎉
