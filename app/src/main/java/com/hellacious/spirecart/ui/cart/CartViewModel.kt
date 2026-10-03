package com.hellacious.spirecart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hellacious.spirecart.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState(isLoading = true))
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        observeCartSummary()
    }

    private fun observeCartSummary() {
        cartRepository.getCartSummary()
            .onEach { summary ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cartSummary = summary
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun increaseQuantity(productId: Long) {
        viewModelScope.launch {
            cartRepository.increaseQuantity(productId)
        }
    }

    fun decreaseQuantity(productId: Long) {
        viewModelScope.launch {
            cartRepository.decreaseQuantity(productId)
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch {
            cartRepository.removeFromCart(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }

    fun checkout() {
        viewModelScope.launch {
            cartRepository.clearCart()
            _uiState.update { it.copy(isCheckoutSuccess = true) }
        }
    }

    fun dismissCheckoutMessage() {
        _uiState.update { it.copy(isCheckoutSuccess = false) }
    }
}
