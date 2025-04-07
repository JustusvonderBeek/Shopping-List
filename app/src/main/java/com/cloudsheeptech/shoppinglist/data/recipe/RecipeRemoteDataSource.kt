package com.cloudsheeptech.shoppinglist.data.recipe

import android.util.Log
import com.cloudsheeptech.shoppinglist.data.typeConverter.OffsetDateTimeFormatHandler
import com.cloudsheeptech.shoppinglist.exception.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.ParseException
import jakarta.mail.util.ByteArrayDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRemoteDataSource
@Inject
constructor(
    private val networking: Networking,
    private val certificate: InputStream,
) {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            serializersModule =
                SerializersModule {
                    contextual(OffsetDateTime::class, OffsetDateTimeFormatHandler())
                }
        }

    @OptIn(InternalSerializationApi::class)
    suspend fun create(
        recipe: ApiRecipe,
        recipeImages: List<ByteArray>,
    ): Boolean {
        var success = false
        withContext(Dispatchers.IO) {
            try {
                val encodedRecipe = json.encodeToString(recipe)
                networking.MULTIFORM_POST("/v1/recipe", encodedRecipe, recipeImages) { response ->
                    if (response.status != HttpStatusCode.Created) {
                        Log.e("RecipeRemoteDataSource", "Failed to create remote receipt")
                        return@MULTIFORM_POST
                    }
                    success = true
                }
            } catch (ex: SerializationException) {
                Log.e(
                    "RecipeRemoteDataSource",
                    "Recipe cannot be serialized: $ex",
                )
            } catch (ex: IOException) {
                Log.e("RecipeRemoteDataSource", "Failed to connect to remote: $ex")
            } catch (ex: ConnectException) {
                Log.e("RecipeRemoteDataSource", "Failed to connect to remote: $ex")
            } catch (ex: UserNotAuthenticatedException) {
                Log.e("RecipeRemoteDataSource", "User not authenticated: $ex")
            } catch (ex: Exception) {
                Log.e("RecipeRemoteDataSource", "Unknown exception while creating recipe: $ex")
            }
        }
        return success
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun read(
        recipeId: Long,
        createdBy: Long,
    ): ApiRecipe? {
        var onlineReceipt: ApiRecipe? = null
        networking.get("/v1/recipe/$recipeId?createdBy=$createdBy") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e(
                    "ReceiptRemoteDataSource",
                    "Failed to get receipt $recipeId from $createdBy online",
                )
                return@get
            }
            val rawBody = response.bodyAsText(Charsets.UTF_8)
            if (rawBody.isEmpty() || rawBody == "null") {
                Log.w("ReceiptRemoteDataSource", "List $recipeId from $createdBy not found online")
                return@get
            }
            val decoded = json.decodeFromString<ApiRecipe>(rawBody)
            onlineReceipt = decoded
            Log.d(
                "ReceiptRemoteDataSource",
                "Found receipt $recipeId with ${onlineReceipt?.ingredients?.size} online",
            )
        }
        return onlineReceipt
    }

    suspend fun readImages(
        recipeId: Long,
        createdBy: Long,
        imagesMetadata: RecipeMetadata,
    ): List<ByteArray> {
        val images = mutableListOf<ByteArray>()
        // TODO: Get the total number of bytes and the individual bytes
        val imageBuffer = ByteBuffer.allocate(1024)
        networking.get("${UrlProviderEnum.BASE_URL}${UrlProviderEnum.RECIPE}$recipeId/images") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e(
                    "ReceiptRemoteDataSource",
                    "Failed to get receipt $recipeId from $createdBy online",
                )
                return@get
            }
            val rawBody = response.bodyAsChannel()
            rawBody.readFully(imageBuffer)
        }
        var offset = 0
        for (i in 0..imagesMetadata.numOfImages) {
            val sizeOfImage = imagesMetadata.sizeOfImages.get(i)
            val imageSlice = ByteArray(sizeOfImage)
            imageBuffer.get(imageSlice, offset, sizeOfImage)
            images.add(imageSlice)
        }
        return images
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    suspend fun readFull(
        recipeId: Long,
        createdBy: Long,
    ): Pair<ApiRecipe?, List<ByteArray>> {
        var recipe: ApiRecipe? = null
        val rawImageList = mutableListOf<ByteArray>()
        networking.get("${UrlProviderEnum.BASE_URL}${UrlProviderEnum.RECIPE}$recipeId/full") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e(
                    "ReceiptRemoteDataSource",
                    "Failed to get receipt $recipeId from $createdBy online",
                )
                return@get
            }
            try {
                val contentType = response.headers["Content-Type"] ?: return@get
                val fullBody = response.readBytes()
                val multipartDataSource = ByteArrayDataSource(fullBody, contentType)
                val multipart = MimeMultipart(multipartDataSource)
                val rawRecipe = multipart.getBodyPart(0)
                if (!rawRecipe.contentType.equals(ContentType.Application.Json)) {
                    Log.e("RecipeRemoteDataSource", "Received recipe response in wrong format")
                    return@get
                }
                recipe = json.decodeFromString<ApiRecipe>(rawRecipe.content.toString())
                for (i in 1..multipart.count) {
                    val imagePart = multipart.getBodyPart(i)
                    if (!imagePart.contentType.equals(ContentType.Image.Any)) {
                        Log.e(
                            "RecipeRemoteDataSource",
                            "The type of image is incorrect: ${imagePart.contentType}"
                        )
                        return@get
                    }
                    val rawImage = imagePart.content
                    if (rawImage is ByteArray) {
                        rawImageList.add(rawImage)
                    } else {
                        Log.e(
                            "RecipeRemoteDataSource",
                            "Image content-cype is ${imagePart.content.javaClass}"
                        )
                    }
                }
            } catch (ex: MessagingException) {
                Log.e("RecipeRemoteDataSource", "Failed to parse all body parts: $ex")
            } catch (ex: ParseException) {
                Log.e("RecipeRemoteDataSource", "Received recipe response in wrong format: $ex")
            } catch (ex: OutOfMemoryError) {
                Log.e("RecipeRemoteDataSource", "The received message cannot be parse at once: $ex")
            }
        }
        return Pair(recipe, rawImageList)
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun update(
        recipe: ApiRecipe,
        recipeImages: List<ByteArray>,
    ): Boolean {
        var success = false
        val encodedRecipe = json.encodeToString(recipe)
        networking.PUT_MULTIPART(
            "/v1/recipe/${recipe.onlineId}?createdBy=${recipe.createdBy.onlineId}",
            encodedRecipe,
            recipeImages,
        ) { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e(
                    "ReceiptRemoteDataSource",
                    "Failed to update receipt ${recipe.onlineId} online",
                )
                return@PUT_MULTIPART
            }
            success = true
        }
        return success
    }

    suspend fun delete(
        recipeId: Long,
        createdBy: Long,
    ): Boolean {
        var success = false
        networking.DELETE("/v1/recipe/$recipeId?createdBy=$createdBy") { response ->
            if (response.status != HttpStatusCode.OK) {
                Log.e("ReceiptRemoteDataSource", "Failed to delete receipt $recipeId online")
                return@DELETE
            }
            success = true
        }
        return success
    }
}
