package com.reflection.thecampus.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Utility for network connectivity detection and monitoring
 */
object NetworkUtils {
    
    /**
     * Check if network is currently available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Observe network connectivity changes
     * Returns LiveData<Boolean> where true = connected, false = disconnected
     */
    fun observeNetworkChanges(context: Context): LiveData<Boolean> {
        val networkLiveData = MutableLiveData<Boolean>()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    networkLiveData.postValue(true)
                }
                
                override fun onLost(network: Network) {
                    networkLiveData.postValue(false)
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    networkLiveData.postValue(hasInternet && isValidated)
                }
            }
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(request, networkCallback)
            
            // Set initial value
            networkLiveData.value = isNetworkAvailable(context)
        }
        
        return networkLiveData
    }
    
    /**
     * Get network type (WiFi, Cellular, etc.)
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            when (connectivityManager.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        }
    }
    
    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        OTHER,
        NONE
    }
}

/**
 * Extension function for Context to easily check network availability
 */
fun Context.isNetworkAvailable(): Boolean = NetworkUtils.isNetworkAvailable(this)
