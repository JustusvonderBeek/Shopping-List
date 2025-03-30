package com.cloudsheeptech.shoppinglist.data.recipe

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BinaryFileHandler
    @Inject
    constructor() : IBinaryFileHandler {
        override suspend fun readImageFromFile(imageLocation: String): ByteArray? {
            var binaryImage: ByteArray? = null
            withContext(Dispatchers.IO) {
                try {
                    val imageUri = imageLocation.toUri()
                    if (!imageUri.toFile().exists()) {
                        Log.e("BinaryFileHandler", "File does not exist")
                        return@withContext
                    }
                    binaryImage = imageUri.toFile().readBytes()
                } catch (ex: IllegalArgumentException) {
                    Log.e("BinaryFileHandler", "Given URI in wrong format for local file: $ex")
                } catch (ex: IOException) {
                    Log.e("BinaryFileHandler", "Failed to read local file: $ex")
                } catch (ex: Exception) {
                    Log.e("BinaryFileHandler", "Unknown exception while reading local file: $ex")
                }
            }
            return binaryImage
        }

        override suspend fun readImagesFromFiles(imageUris: List<String>): List<ByteArray> {
            val binaryImages = mutableListOf<ByteArray>()
            for (uri in imageUris) {
                val image = readImageFromFile(uri)
                if (image != null) {
                    binaryImages.add(image)
                }
            }
            return binaryImages
        }

        override suspend fun storeImageToFile(imageByteArray: ByteArray): Uri {
            TODO("Not yet implemented")
        }
    }
