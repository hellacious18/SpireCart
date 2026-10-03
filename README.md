# SpireCart - Product Catalog & Offline Cart

SpireCart is a shopping Android app built with Jetpack Compose. It lets users browse and search products from DummyJSON and manage a shopping cart that works completely offline.

---

## 📱 APK & Demo Recording

- **Google Drive Folder (APK & Screen Recording)**: [Download APK & Watch Demo](https://drive.google.com/drive/folders/1lRLYMHgo_ZHDTkVRmtFOTWOyLhn-tojJ?usp=sharing)

---

## ✨ Features

- **Product Catalog**: Browse products with images, prices, ratings, and discounts.
- **Search & Categories**: Search products in real-time or filter by category chips.
- **Product Details**: View product descriptions, multiple images, stock status, ratings, reviews, and policies.
- **Offline Shopping Cart**:
  - Add items from product lists or the details screen.
  - Increase/decrease quantities or remove items.
  - View total item count and live order price.
  - **Works 100% offline**: All cart actions work without internet and persist when closing/reopening the app.
- **Auto-Sync & Offline Status**: Notifies when the app is offline and automatically refreshes when back online.

---

## 🏛️ Architecture & Project Structure

SpireCart is built following **Modern Android Architecture (MVVM / Clean Architecture)** with **Unidirectional Data Flow (UDF)** and **Offline-First** principles:

```
┌────────────────────────────────────────────────────────┐
│               UI Layer (Jetpack Compose)               │
│      ProductListScreen │ ProductDetailsScreen │ Cart   │
└───────────────────────────▲────────────────────────────┘
                            │ (StateFlow / Events)
┌───────────────────────────┴────────────────────────────┐
│                  Presentation Layer                    │
│      ProductListViewModel │ DetailsVM │ CartViewModel  │
└───────────────────────────▲────────────────────────────┘
                            │ (Coroutines Flow)
┌───────────────────────────┴────────────────────────────┐
│                     Domain Layer                       │
│    Models (Product, CartItem) │ Repository Interfaces  │
└───────────────────────────▲────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
┌───────────────────────────┐ ┌───────────────────────────┐
│     Remote Data Source    │ │     Local Data Source     │
│  Retrofit + DummyJSON API │ │ Room DB + RemoteMediator  │
└───────────────────────────┘ └───────────────────────────┘
```

### 📂 Package Structure
```text
com.hellacious.spirecart/
├── core/
│   ├── di/                 # Manual Dependency Injection (AppContainer)
│   └── network/            # NetworkResult wrapper & ConnectivityObserver
├── data/
│   ├── local/              # Room Database, DAOs & Entities (Cart, Products, RemoteKeys)
│   ├── remote/             # Retrofit ApiClient, ApiService & DTOs
│   ├── paging/             # Paging 3 ProductRemoteMediator
│   └── repository/         # Repository Implementations (ProductRepositoryImpl, CartRepositoryImpl)
├── domain/
│   ├── model/              # Pure Domain Models (Product, CartItem, CartSummary)
│   └── repository/         # Repository Interfaces
└── ui/
    ├── cart/               # CartScreen, CartViewModel, CartUiState
    ├── details/            # ProductDetailsScreen, ProductDetailsViewModel, ProductDetailsUiState
    ├── products/           # ProductListScreen, ProductListViewModel, ProductListUiState
    ├── components/         # Reusable Compose widgets (ProductCard, CartItemCard, CategoryChips, ErrorView)
    ├── navigation/         # Navigation Graph & Destinations (SpireCartNavGraph, Screen)
    └── theme/              # Material 3 Color Schemes, Typography & Shapes
```

---

## 📦 Libraries Used

- **Kotlin & Coroutines**: Reactive asynchronous programming with `Flow` & `StateFlow`.
- **Jetpack Compose (Material 3)**: Modern declarative, adaptive UI design.
- **Room Database**: Local SQLite storage for cart persistence, product caching, and paging remote keys.
- **Paging 3 + RemoteMediator**: Smooth, reactive infinite scrolling with offline database cache.
- **Retrofit & OkHttp**: Networking client with JSON parsing and logging interceptors.
- **Coil**: Asynchronous image loading with disk caching for offline display.
- **Navigation Compose**: Type-safe single-activity screen navigation.

---

## 💾 Local Storage (Offline Cart & Data)

1. **Cart Persistence**:
   - Cart items are saved into Room database (`cart_items` table).
   - Adding, modifying quantities, or removing items updates the database directly.
   - The cart remains saved even after quitting or restarting the app.

2. **Offline Products & Images**:
   - Product information is cached in Room database when fetched.
   - Images are saved locally so they continue to show when the device is in airplane mode.

---

## 💡 Important Design Decisions

1. **Offline First Cart**: Cart operations do not rely on network calls, guaranteeing zero delay and full offline support.
2. **Search Debouncing**: A short delay (350ms) is applied while typing to prevent unnecessary API calls.
3. **Automatic Reconnection**: The app detects when the internet comes back online and automatically refreshes data.
4. **Stock Limit Protection**: Prevents users from adding more items to the cart than currently available in stock.

---

## 🛠️ How to Build and Run

1. Open the project in **Android Studio**.
2. Make sure you have **JDK 17** selected in Settings $\rightarrow$ Build Tools $\rightarrow$ Gradle.
3. Sync Gradle and run on an Android device or emulator (Android 11+ / API 30+).
4. Run tests:
   ```bash
   ./gradlew test
   ```

---

## ⚠️ Known Limitations

- The DummyJSON API is a mock service, so remote checkout is not processed on a real payment gateway.
- Product images need to be loaded at least once with internet before they become available offline.
