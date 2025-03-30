package com.cloudsheeptech.shoppinglist.data.recipe

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BinaryFileHandler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : IBinaryFileHandler {
        private fun loadFileFromContent(location: String): ByteArray? {
            if (location.isEmpty() || !location.startsWith("content://")) {
                return null
            }
            try {
                // Resolve the location of the content:// file and read the raw binary data from disk
                val rawImage =
                    context.contentResolver.openInputStream(location.toUri())?.use { inputStream ->
                        val buffer = ByteArrayOutputStream()
                        val data = ByteArray(1024)
                        var bytesRead: Int
                        while (inputStream.read(data).also { bytesRead = it } != -1) {
                            buffer.write(data, 0, bytesRead)
                        }
                        buffer.toByteArray()
                    }
                return rawImage
            } catch (ex: IOException) {
                Log.e("BinaryFileHandler", "Failed to read $location file: $ex")
            } catch (ex: FileNotFoundException) {
                Log.e("BinaryFileHandler", "File $location not found: $ex")
            }
            return null
        }

        private fun resolveContentTypeAndReadImage(location: String): ByteArray? =
            when {
                location.startsWith("content://") -> {
                    loadFileFromContent(location)
                }
                else -> {
                    null
                }
            }

        override suspend fun readImageFromFile(imageLocation: String): ByteArray? {
            var binaryImage: ByteArray? = null
            withContext(Dispatchers.IO) {
                try {
                    binaryImage = resolveContentTypeAndReadImage(imageLocation)
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
