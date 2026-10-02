package com.hellacious.spirecart.core.network

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val code: Int? = null, val cause: Throwable? = null) : NetworkResult<Nothing>
    object Loading : NetworkResult<Nothing>
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: UnknownHostException) {
        NetworkResult.Error("No internet connection. Please check your network and retry.", cause = e)
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error("Request timed out. Please try again.", cause = e)
    } catch (e: retrofit2.HttpException) {
        val errorMessage = when (e.code()) {
            404 -> "Requested item not found."
            500, 502, 503 -> "Server error. Please try again later."
            else -> "HTTP error ${e.code()}: ${e.message()}"
        }
        NetworkResult.Error(errorMessage, code = e.code(), cause = e)
    } catch (e: IOException) {
        NetworkResult.Error("Network error: ${e.localizedMessage ?: "Unknown I/O error"}", cause = e)
    } catch (e: Exception) {
        NetworkResult.Error("An unexpected error occurred: ${e.localizedMessage ?: "Unknown error"}", cause = e)
    }
}
