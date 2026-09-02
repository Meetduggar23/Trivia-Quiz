package com.example.triviaquiz.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility object to check network connectivity status.
 * Uses Android's ConnectivityManager and NetworkCapabilities API.
 */
object NetworkUtils {

    /**
     * Checks if the device has an active network connection with Internet capability.
     * This must be called BEFORE attempting any network requests.
     *
     * @param context Application or Activity context
     * @return true if Internet is available, false otherwise
     */
    fun isInternetAvailable(context: Context): Boolean {
        // Get the ConnectivityManager system service
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the active network (may be null if no network is connected)
        val activeNetwork = connectivityManager.activeNetwork ?: return false

        // Get network capabilities for the active network
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return false

        // Check if the network has Internet capability
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
