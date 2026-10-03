package com.hellacious.spirecart.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.domain.model.Product
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.domain.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState(isLoading = true))
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeCartCount()
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

    fun loadCategories() {
        viewModelScope.launch {
            when (val result = productRepository.getCategories()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(categories = result.data) }
                }
                is NetworkResult.Error -> {
                    // Non-fatal, category chips can remain empty or use cached categories
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val selectedCategory = _uiState.value.selectedCategory
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
                    val toast = if (_uiState.value.products.isNotEmpty()) {
                        "You are offline. Showing cached products."
                    } else {
                        "You are offline. Unable to fetch products."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            userMessage = toast
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
            loadProducts()
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce search queries
            _uiState.update { it.copy(isLoading = true, isSearching = true, errorMessage = null) }
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
                            userMessage = "You are offline. Unable to search remote catalog."
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
        loadProducts()
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
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
