package com.cloudsheeptech.shoppinglist.network

interface UserCreationPayloadProvider {
    /**
     * This method turns the user or other authentication method
     * into a string which can be used by the creation mechanism
     * to create a new account which can the be used to authenticate
     */
    suspend fun provideUserCreationPayload(): String?

    /**
     * This method processes the response from the server and
     * extracts the information contained.
     */
    suspend fun processUserCreationResponse(payload: String): Unit

    /**
     * This method provides the string payload sent to the
     * server when authenticating for login
     */
    fun provideLoginPayload(): Pair<String, Long>
}
