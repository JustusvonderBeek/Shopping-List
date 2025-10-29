package com.cloudsheeptech.shoppinglist.recipe.repo

import android.util.Log
import com.cloudsheeptech.shoppinglist.list.model.UserNotAuthenticatedException
import com.cloudsheeptech.shoppinglist.network.Networking
import com.cloudsheeptech.shoppinglist.network.UrlProviderEnum
import com.cloudsheeptech.shoppinglist.recipe.model.ApiRecipe
import com.cloudsheeptech.shoppinglist.recipe.model.RecipeMetadata
import com.cloudsheeptech.shoppinglist.util.OffsetDateTimeFormatHandler
import io.ktor.client.statement.HttpResponse
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.internal.toImmutableList
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

        private enum class MultipartContentStatusType {
            START,
            JSON,
            IMAGE,
        }

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

        private suspend fun parseAllRecipesAndImagesInResponse(response: HttpResponse): List<Pair<ApiRecipe, List<ByteArray>>> {
            var recipesAndImages = mutableListOf<Pair<ApiRecipe, List<ByteArray>>>()
            // TODO: Include file compression and decompression
            withContext(Dispatchers.IO) {
                val contentType = response.headers["Content-Type"] ?: return@withContext
                // Maybe make adaptive parsing, but would require more sophisticated handling
                try {
                    val fullBody = response.readBytes()
                    if (fullBody.isEmpty()) {
                        Log.i("RecipeRemoteDataSource", "No remote recipes found")
                        return@withContext
                    }
                    val multipartDataSource = ByteArrayDataSource(fullBody, contentType)
                    val multipart = MimeMultipart(multipartDataSource)
                    if (multipart.count < 1) {
                        Log.e("RecipeRemoteDataSource", "Recipe response contains no useful data")
                        return@withContext
                    }
                    var previousBodyType = MultipartContentStatusType.START
                    var currentRecipe: ApiRecipe? = null
                    val rawImages = mutableListOf<ByteArray>()
                    for (bodyPartIndex in 0..<multipart.count) {
                        val bodyPart = multipart.getBodyPart(bodyPartIndex)

                        if (bodyPart.contentType.equals(ContentType.Application.Json.toString())) {
                            Log.d(
                                "RecipeRemoteDataSource",
                                "Received JSON in body part: $bodyPartIndex",
                            )
                            if (previousBodyType != MultipartContentStatusType.START) {
                                if (currentRecipe == null) {
                                    Log.e("RecipeRemoteDataSource", "Received empty recipe object")
                                    return@withContext
                                }
                                val newRecipe = Pair(currentRecipe, rawImages.toImmutableList())
                                recipesAndImages.add(newRecipe)
                                currentRecipe = null
                                rawImages.clear()
                            }
                            previousBodyType = MultipartContentStatusType.JSON
                            val objectContent =
                                bodyPart.dataHandler.inputStream
                                    .bufferedReader(Charsets.UTF_8)
                                    .readText()
                            currentRecipe = json.decodeFromString<ApiRecipe>(objectContent)
                        } else if (bodyPart.contentType.substringBefore("/").equals(
                                ContentType.Image.Any
                                    .toString()
                                    .substringBefore("/"),
                            )
                        ) {
                            Log.d(
                                "RecipeRemoteDataSource",
                                "Received image in body part: $bodyPartIndex",
                            )
                            val rawImage = bodyPart.inputStream.buffered(1024).readBytes()
                            if (rawImage.isNotEmpty()) {
                                rawImages.add(rawImage)
                            }
                            previousBodyType = MultipartContentStatusType.IMAGE
                        } else {
                            Log.e(
                                "RecipeRemoteDataSource",
                                "Received unknown body part type: ${bodyPart.contentType}",
                            )
                            // We don't want to propagate data if the response is incorrect
                            recipesAndImages.clear()
                            return@withContext
                        }
                        if (bodyPartIndex + 1 == multipart.count) {
                            Log.d("RecipeRemoteDataSource", "Received last body part")
                            if (currentRecipe == null) {
                                Log.e("RecipeRemoteDataSource", "Received empty recipe object")
                                return@withContext
                            }
                            // Copy the elements so that we don't have the same images in all recipes
                            val newRecipe = Pair(currentRecipe, rawImages.toImmutableList())
                            recipesAndImages.add(newRecipe)
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(
                        "RecipeRemoteDataSource",
                        "Unknown exception while parsing all recipes: $ex",
                    )
                }
            }
            return recipesAndImages
        }

        suspend fun readFull(
            recipeId: Long,
            createdBy: Long,
        ): Pair<ApiRecipe?, List<ByteArray>> {
            var recipe: ApiRecipe? = null
            val rawImageList = mutableListOf<ByteArray>()
            networking.get("${UrlProviderEnum.RECIPE.url}/$recipeId/full?createdBy=$createdBy") { response ->
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
                    if (!rawRecipe.contentType.equals(ContentType.Application.Json.toString())) {
                        Log.e("RecipeRemoteDataSource", "Received recipe response in wrong format")
                        return@get
                    }
                    val objectContent =
                        rawRecipe.dataHandler.inputStream
                            .bufferedReader(Charsets.UTF_8)
                            .readText()
                    recipe = json.decodeFromString<ApiRecipe>(objectContent)
                    for (i in 1..<multipart.count) {
                        val imagePart = multipart.getBodyPart(i)
                        if (!imagePart.contentType.substringBefore("/").equals(
                                ContentType.Image.Any
                                    .toString()
                                    .substringBefore("/"),
                            )
                        ) {
                            Log.e(
                                "RecipeRemoteDataSource",
                                "The type of image is incorrect: ${imagePart.contentType}",
                            )
                            return@get
                        }
                        val rawImage = imagePart.inputStream.buffered(1024).readBytes()
                        if (rawImage.isNotEmpty()) {
                            rawImageList.add(rawImage)
                        } else {
                            Log.e(
                                "RecipeRemoteDataSource",
                                "Image $i content is empty",
                            )
                        }
                    }
                } catch (ex: MessagingException) {
                    Log.e("RecipeRemoteDataSource", "Failed to parse all body parts: $ex")
                } catch (ex: ParseException) {
                    Log.e("RecipeRemoteDataSource", "Received recipe response in wrong format: $ex")
                } catch (ex: OutOfMemoryError) {
                    Log.e("RecipeRemoteDataSource", "The received message cannot be parse at once: $ex")
                } catch (ex: Exception) {
                    Log.e("RecipeRemoteDataSource", "Unknown exception while reading recipe: $ex")
                }
            }
            return Pair(recipe, rawImageList)
        }

        suspend fun readAllRecipesFull(): List<Pair<ApiRecipe, List<ByteArray>>> {
            val remoteRecipes = mutableListOf<Pair<ApiRecipe, List<ByteArray>>>()
            withContext(Dispatchers.IO) {
                networking.get("${UrlProviderEnum.RECIPE.url}/full") { response ->
                    if (response.status != HttpStatusCode.OK) {
                        Log.e("RecipeRemoteDataSource", "Failed to get all recipes online")
                        return@get
                    }
                    try {
                        val recipesAndImages = parseAllRecipesAndImagesInResponse(response)
                        if (recipesAndImages.isEmpty()) {
                            Log.e("RecipeRemoteDataSource", "Failed to parse all recipes online")
                            return@get
                        }
                        remoteRecipes.addAll(recipesAndImages)
                    } catch (ex: IOException) {
                    } catch (ex: SerializationException) {
                    } catch (ex: Exception) {
                        Log.e(
                            "RecipeRemoteDataSource",
                            "Unknown exception while reading all recipes: $ex",
                        )
                    }
                }
            }
            return remoteRecipes
        }

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
