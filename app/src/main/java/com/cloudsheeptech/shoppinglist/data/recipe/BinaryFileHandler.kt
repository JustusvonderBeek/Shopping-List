package com.cloudsheeptech.shoppinglist.data.recipe

import android.content.Context
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.nio.file.StandardOpenOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

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

        private fun loadFileFromFileStorage(location: String): ByteArray? {
            if (location.isEmpty() || !location.startsWith("file://")) {
                return null
            }
            try {
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
            } catch (ex: FileNotFoundException) {
                Log.e("BinaryFileHandler", "File to read not found: $ex")
            } catch (ex: Exception) {
                Log.e("BinaryFileHandler", "Unknown error while reading file: $ex")
            }
            return null
        }

        private fun resolveContentTypeAndReadImage(location: String): ByteArray? =
            when {
                location.startsWith("content://") -> {
                    loadFileFromContent(location)
                }
                location.startsWith("file://") -> {
                    loadFileFromFileStorage(location)
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

        suspend fun persistTemporaryImagesInLocalStorage(
            recipeId: Long,
            userId: Long,
            images: List<String>,
        ): List<String> {
            return withContext(Dispatchers.IO) {
                val updatedImagePaths = mutableListOf<String>()
                var updatedImageIndex = 0
                for (image in images) {
                    if (!image.startsWith("content://")) {
                        Log.d("BinaryFileHandler", "Image '$image' already persisted, nothing to do")
                        updatedImagePaths.add(image)
                        continue
                    }
                    val rawImage = resolveContentTypeAndReadImage(image)
                    if (rawImage == null) {
                        Log.i("BinaryFileHandler", "Failed to load image $image, skipping step")
                        updatedImagePaths.add(image)
                        continue
                    }
                    val newPath = storeImageToFile(rawImage, "${recipeId}_${userId}_$updatedImageIndex.img")
                    updatedImageIndex++
                    updatedImagePaths.add(newPath.toString())
                }
                return@withContext updatedImagePaths
            }
        }

        override suspend fun storeImageToFile(
            imageByteArray: ByteArray,
            fileName: String,
        ): URI {
            val createdUri =
                withContext(Dispatchers.IO) {
                    val imagePath = context.filesDir.absolutePath
                    val newImagePath = Path(imagePath, fileName)
                    try {
                        newImagePath.writeBytes(
                            imageByteArray,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE,
                        )
                    } catch (ex: NoSuchFileException) {
                        Log.e("BinaryFileHandler", "Failed to create file $newImagePath: $ex")
                    } catch (ex: IOException) {
                        Log.e("BinaryFileHandler", "Failed to write to file $newImagePath: $ex")
                    }
                    return@withContext newImagePath.toUri()
                }
            return createdUri
        }

        suspend fun deleteImagesForRecipe(imageUris: List<String>): Int {
            var deletedImages = 0
            for (uri in imageUris) {
                if (deleteImage(uri)) {
                    deletedImages++
                }
            }
            return deletedImages
        }

        suspend fun deleteImage(imageUri: String): Boolean {
            var success = true
            withContext(Dispatchers.IO) {
                if (imageUri.startsWith("content://")) {
                    Log.i("BinaryFileHandler", "Cannot delete image in content://: $imageUri")
                    return@withContext
                }
                val imageFile = imageUri.toUri().toFile()
                if (!imageFile.isFile || !imageFile.exists()) {
                    Log.e("BinaryFileHandler", "File to delete not found: $imageUri")
                    return@withContext
                }
                success = imageFile.delete()
            }
            return success
        }
    }
