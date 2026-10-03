package com.hellacious.spirecart.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hellacious.spirecart.R
import com.hellacious.spirecart.core.network.ConnectivityObserver
import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.domain.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val connectivityObserver: ConnectivityObserver? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState(isLoading = true))
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // Reactive Paging 3 Stream
    private val _filterParams = MutableStateFlow<Pair<String?, String?>>(null to null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedProducts: Flow<PagingData<Product>> = _filterParams
        .flatMapLatest { (category, query) ->
            productRepository.getProductsPaged(category = category, query = query)
        }
        .cachedIn(viewModelScope)

    init {
        observeCartCount()
        observeNetworkConnectivity()
        loadCategories()
        loadProducts()
    }

    private fun observeCartCount() {
        cartRepository.getCartItemCount()
            .onEach { count ->
                _uiState.update { it.copy(cartItemCount = count) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeNetworkConnectivity() {
        connectivityObserver?.isConnected
            ?.onEach { isOnline ->
                val wasOffline = !_uiState.value.isOnline
                _uiState.update { it.copy(isOnline = isOnline) }

                // Automatically auto-retry when connectivity is restored
                if (isOnline && wasOffline) {
                    _uiState.update {
                        it.copy(userMessageResId = R.string.back_online_syncing)
                    }
                    loadCategories()
                    loadProducts()
                }
            }
            ?.launchIn(viewModelScope)
    }

    fun loadCategories() {
        viewModelScope.launch {
            when (val result = productRepository.getCategories()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(categories = result.data) }
                }
                is NetworkResult.Error -> {
                    // Non-fatal
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val selectedCategory = _uiState.value.selectedCategory
            _filterParams.value = selectedCategory to _uiState.value.searchQuery.takeIf { it.isNotBlank() }

            val result = if (selectedCategory.isNullOrBlank()) {
                productRepository.getProducts(limit = 100, skip = 0)
            } else {
                productRepository.getProductsByCategory(category = selectedCategory, limit = 100, skip = 0)
            }

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            userMessageResId = R.string.no_internet_connection
                        )
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false) }
            _filterParams.value = _uiState.value.selectedCategory to null
            loadProducts()
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce search queries
            _uiState.update { it.copy(isLoading = true, isSearching = true, errorMessage = null) }
            _filterParams.value = _uiState.value.selectedCategory to query.trim()

            when (val result = productRepository.searchProducts(query = query.trim())) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            userMessageResId = R.string.no_internet_connection
                        )
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun onCategorySelected(categorySlug: String?) {
        val newCategory = if (_uiState.value.selectedCategory == categorySlug) null else categorySlug
        _uiState.update { it.copy(selectedCategory = newCategory, searchQuery = "", isSearching = false) }
        _filterParams.value = newCategory to null
        loadProducts()
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessageResId = null) }
    }

    fun retry() {
        if (_uiState.value.searchQuery.isNotBlank()) {
            onSearchQueryChanged(_uiState.value.searchQuery)
        } else {
            loadCategories()
            loadProducts()
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            cartRepository.addToCart(product, quantity = 1)
        }
    }
}
