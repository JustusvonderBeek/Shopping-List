package com.cloudsheeptech.shoppinglist.network

import javax.net.ssl.SSLSession

class HostnameVerification {

    companion object {

        private const val APPLICATION_PORT = 46152
        private val APPLICATION_ENDPOINTS = listOf("10.0.2.2", "shop.cloudsheeptech.com")

        fun verifyHostname(hostname: String, sslSession: SSLSession) : Boolean {
//            Log.d("HostVerfication", "SSL Host: ${sslSession.peerHost}, SSL Port: ${sslSession.peerPort}")
            if (sslSession.peerPort != APPLICATION_PORT)
                return false
            if (sslSession.peerHost != hostname || !APPLICATION_ENDPOINTS.contains(sslSession.peerHost))
                return false
            if (!sslSession.isValid)
                return false
            return true
        }
    }
}