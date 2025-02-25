package com.cloudsheeptech.shoppinglist.network

/**
 * An interface providing essential networking methods to
 * send and receive HTTP data to the server
 */
interface INetworking {

    /**
     * HTTP GET Request
     */
    suspend fun get(
        requestUrlPath: String,
        responseHandler: IHttpResponseHandler
    ): Boolean

    /**
     * HTTP POST Request
     */
    suspend fun post(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler,
    ): Boolean

    /**
     * HTTP POST Request with multiform data
     */
    suspend fun post(
        requestUrlPath: String,
        data: String?,
        binaryContent: ByteArray?,
        responseHandler: IHttpResponseHandler
    ): Boolean

    /**
     * HTTP PUT Request
     */
    suspend fun put(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler,
    ): Boolean

    /**
     * HTTP Patch Request
     */
    suspend fun patch(
        requestUrlPath: String,
        data: String?,
        responseHandler: IHttpResponseHandler,
    ): Boolean

    /**
     * HTTP DELETE Request
     */
    suspend fun delete(
        requestUrlPath: String,
        responseHandler: IHttpResponseHandler,
    ): Boolean
}