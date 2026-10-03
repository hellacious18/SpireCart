package com.hellacious.spirecart.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellacious.spirecart.core.network.NetworkResult
import com.hellacious.spirecart.domain.repository.CartRepository
import com.hellacious.spirecart.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailsViewModel(
    private val productId: Long,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailsUiState(isLoading = true))
    val uiState: StateFlow<ProductDetailsUiState> = _uiState.asStateFlow()

    init {
        loadProductDetails()
        observeCartItem()
    }

    private fun observeCartItem() {
        cartRepository.getCartItem(productId)
            .onEach { cartItem ->
                _uiState.update {
                    it.copy(cartQuantity = cartItem?.quantity ?: 0)
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadProductDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = productRepository.getProductDetails(productId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            userMessage = "You are offline. Unable to fetch fresh product details."
                        )
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun incrementQuantityToAdd() {
        val current = _uiState.value.selectedQuantityToAdd
        val stock = _uiState.value.product?.stock ?: 99
        if (current < stock) {
            _uiState.update { it.copy(selectedQuantityToAdd = current + 1) }
        }
    }

    fun decrementQuantityToAdd() {
        val current = _uiState.value.selectedQuantityToAdd
        if (current > 1) {
            _uiState.update { it.copy(selectedQuantityToAdd = current - 1) }
        }
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return
        val qty = _uiState.value.selectedQuantityToAdd
        viewModelScope.launch {
            cartRepository.addToCart(product, quantity = qty)
            _uiState.update { it.copy(isAddedToCartSnackbar = true) }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(isAddedToCartSnackbar = false) }
    }

    fun retry() {
        loadProductDetails()
    }
}
