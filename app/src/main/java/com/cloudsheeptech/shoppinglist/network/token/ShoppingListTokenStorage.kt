package com.cloudsheeptech.shoppinglist.network.token

import android.util.Log
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

class ShoppingListTokenStorage {
    companion object : ITokenStorage {
        private val TOKEN_CHARSET = Charsets.UTF_8
        private const val TOKEN_FOLDER = "tokens/"

        /**
         * @return True if the token was stored successfully, otherwise false
         */
        @OptIn(InternalSerializationApi::class)
        override fun storeTokenToDisk(
            appFileDir: String,
            fileName: String,
            token: String,
        ): Boolean = storeTokenToDisk(appFileDir, fileName, BearerTokens(token, token))

        /**
         * @return True if the token was stored successfully, otherwise false
         */
        @OptIn(InternalSerializationApi::class)
        override fun storeTokenToDisk(
            appFileDir: String,
            fileName: String,
            token: BearerTokens,
        ): Boolean {
            if (fileName.isEmpty()) {
                Log.e("ShoppingListTokenStorage", "The given filename is empty")
                return false
            }
            try {
                val tokenInFileformat = NetworkToken(token.accessToken)
                val encodedToken = Json.encodeToString(tokenInFileformat)
                // Overwriting the file in case it does exist
                val fileNameInFolder = Path(appFileDir, TOKEN_FOLDER, fileName)
                Log.d("ShoppingListTokenStorage", "Writing token to $fileNameInFolder")
                fileNameInFolder.createParentDirectories()
                fileNameInFolder.toFile().writeText(encodedToken, TOKEN_CHARSET)
                return true
            } catch (ex: IOException) {
                Log.e("ShoppingListTokenStorage", "Writing token not possible: $ex")
            } catch (ex: SerializationException) {
                Log.e("ShoppingListTokenStorage", "Token for storage cannot be parsed: $ex")
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListTokenStorage", "Token for storage in incorrect format: $ex")
            }
            return false
        }

        @OptIn(InternalSerializationApi::class)
        override fun readTokenFromDisk(
            appFileDir: String,
            fileName: String,
        ): BearerTokens? {
            var token: BearerTokens? = null
            if (fileName.isEmpty()) {
                Log.w("ShoppingListTokenStorage", "Given tokenFile value is empty")
                return null
            }
            val fileNameInFolder = Path(appFileDir, TOKEN_FOLDER, fileName)
            if (!fileNameInFolder.toFile().exists()) {
                Log.d(
                    "ShoppingListTokenStorage",
                    "Token File '$fileNameInFolder' does not exist",
                )
                return null
            }
            val content = fileNameInFolder.toFile().readText(TOKEN_CHARSET)
            try {
                val decodedToken = Json.decodeFromString<NetworkToken>(content)
                token = BearerTokens(decodedToken.token, decodedToken.token)
            } catch (ex: IOException) {
                Log.e("ShoppingListTokenStorage", "Reading token not possible: $ex")
                return null
            } catch (ex: IllegalArgumentException) {
                Log.e("ShoppingListTokenStorage", "Stored token in incorrect format: $ex")
                return null
            } catch (ex: SerializationException) {
                Log.d("Networking", "Stored token cannot be parsed: $ex")
                return null
            }
            return token
        }

        override fun resetTokens(): Int {
            val tokenPath = Path(TOKEN_FOLDER).toFile()
            if (!tokenPath.exists()) {
                return 0
            }
            val numberOfTokens = tokenPath.list()?.size ?: 0
            tokenPath.deleteRecursively()
            return numberOfTokens
        }
    }
}
