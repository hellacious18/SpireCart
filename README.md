# SpireCart - Product Catalog & Offline Shopping Cart

SpireCart is a modern Android application built with **Jetpack Compose**, **Kotlin Coroutines**, **Retrofit**, and **Room Database**. It fetches live product catalog data from the **DummyJSON Products API**, enables real-time search and category filtering, presents rich product details, and provides a **100% offline-resilient shopping cart** with local persistence.

---

## 📱 Features

- **Product Catalog Listing**:
  - Live product list fetched from DummyJSON API.
  - Displays high-resolution product thumbnails, titles, brand, category, rating stars, price, and discount percentage tags.
  - Seamless loading spinner, empty state views, and error states with retry mechanisms.

- **Real-Time Product Search & Category Filtering**:
  - Instant search with a 350ms debounce to prevent API thrashing and optimize network performance.
  - Horizontal scrollable category chips for quick filtering across all DummyJSON product categories.
  - Handles empty search results and invalid queries gracefully with reset actions.

- **Comprehensive Product Details**:
  - High-resolution hero image with thumbnail gallery switcher.
  - Stock availability status (`In Stock`, `Out of Stock`, `Low Stock`).
  - Full descriptions, SKU, brand, warranty information, shipping estimates, and return policies.
  - Customer review breakdown with reviewer names, star ratings, and comments.
  - Sticky bottom action bar with quantity stepper and dynamic price total.

- **Offline-Ready Shopping Cart**:
  - Local database persistence powered by **AndroidX Room**.
  - Add products directly from catalog cards or product details screen.
  - Quantity steppers (`+` and `-`) with stock limit validation.
  - Automatically removes items when quantity drops to 0.
  - Real-time cart badge counter across screens.
  - Complete order summary card with item breakdown, shipping status, and total calculation.
  - **Full Offline Availability**: Cart data, item manipulation, and totals remain 100% available and responsive even in airplane mode or with no internet connectivity.

---

## 🏗️ Architecture

SpireCart is built following **Clean Architecture** and **MVVM (Model-View-ViewModel)** design principles:

```
[ UI Layer (Jetpack Compose) ]
             ▲
             │ (StateFlow / Events)
[ Presentation Layer (ViewModels) ]
             ▲
             │ (Coroutines Flow)
[ Domain Layer (Models, Repositories, Business Logic) ]
      ▲                              ▲
      │                              │
[ Remote Data Source (Retrofit) ]    [ Local Data Source (Room Database) ]
```

### Layer Breakdown:
1. **Remote API Layer**:
   - `DummyJsonApiService` Retrofit interface defining REST endpoints.
   - `ApiClient` OkHttpClient configuration with timeouts and logging.
   - DTOs (`ProductDto`, `ProductsResponseDto`, `CategoryDto`) mapped to Domain models via extension mappers.
   - `safeApiCall` utility catching HTTP errors, timeouts, and network reachability issues.
2. **Local Persistence Layer**:
   - `CartItemEntity` Room entity with indexed primary keys and timestamps.
   - `CartDao` providing reactive SQLite `Flow` streams for real-time UI synchronization.
   - `SpireCartDatabase` singleton database instance.
3. **Repository Layer**:
   - `ProductRepository`: Manages catalog fetching, queries, and category lookups.
   - `CartRepository`: Encapsulates offline cart operations, quantity bounds, stock capping, and auto-removal.
4. **Presentation & UI Layer**:
   - Built 100% in **Jetpack Compose** with Material 3.
   - Unidirectional Data Flow (UDF) with `StateFlow` and immutable UI states.

---

## 📦 Libraries & Tech Stack

| Component | Library | Version | Purpose |
|---|---|---|---|
| **Language** | Kotlin | 2.2.10 | Core programming language |
| **UI Toolkit** | Jetpack Compose (BOM) | 2026.02.01 | Declarative native UI |
| **Architecture** | AndroidX Lifecycle & ViewModel Compose | 2.11.0 | MVVM state management & lifecycle handling |
| **Navigation** | Navigation Compose | 2.8.8 | Single-activity Compose navigation graph |
| **Networking** | Retrofit 2 & OkHttp 3 | 2.11.0 / 4.12.0 | REST API client & HTTP logging interceptor |
| **Serialization** | Google Gson | 2.11.0 | JSON parsing and serialization |
| **Local Persistence** | AndroidX Room | 2.6.1 | SQLite ORM database for offline cart persistence |
| **Image Loading** | Coil Compose | 2.7.0 | Asynchronous image loading with memory caching |
| **Asynchrony** | Kotlinx Coroutines | 1.10.1 | Reactive streams and background threads |
| **Unit Testing** | JUnit 4 & Coroutines Test | 4.13.2 / 1.10.1 | Unit test verification of logic and ViewModels |

---

## 💾 Local Storage Approach

The shopping cart persistence is implemented using **AndroidX Room**:
- **Entity**: `CartItemEntity` stores `productId` (PrimaryKey), `title`, `price`, `thumbnail`, `quantity`, `stock`, `category`, and `addedAt` timestamp.
- **Reactive Streams**: The DAO exposes `Flow<List<CartItemEntity>>` and `Flow<Int>` which automatically emit new values whenever the underlying SQLite database changes.
- **Stock Guarding**: Cart additions and increments check `stock` limits to ensure users cannot add more items than available in stock.
- **Zero-Quantity Cleanup**: Decrementing an item when quantity is 1 immediately deletes the row from the database.

---

## 💡 Important Design Decisions

1. **Inside-Out Architecture**: Built from Data Layer $\rightarrow$ Room DB $\rightarrow$ ViewModels $\rightarrow$ Compose UI. This prevented mismatches between API schemas and UI assumptions.
2. **DTO & Domain Model Separation**: Domain entities are decoupled from backend JSON schemas, shielding ViewModels and Compose screens from API changes.
3. **Coroutines Search Debounce**: Product search uses a 350ms debounce with Job cancellation to avoid unnecessary HTTP requests during rapid typing.
4. **Reactive Single Source of Truth**: The cart badge count in the top bar and the cart screen observe the exact same Room database `Flow`, guaranteeing instant synchronization across all screens.

---

## 🛠️ Setup & Build Instructions

### Prerequisites:
- **Android Studio Ladybug / Koala / Meerkat** (or newer).
- **JDK 17** (or compatible JDK 17+ toolchain).
- **Android SDK Platform 35 / 37**.

### Build & Run:
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd SpireCart
   ```
2. Build the Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run all Unit Tests:
   ```bash
   ./gradlew test
   ```
4. Install on a connected device / emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## ⚠️ Known Limitations

- The DummyJSON Products API is a mock backend; changes made to the remote catalog (like mock checkouts) are simulated locally.
- Product catalog browsing requires an active internet connection on initial load; however, once items are added to the cart, the shopping cart is 100% accessible and editable offline.
