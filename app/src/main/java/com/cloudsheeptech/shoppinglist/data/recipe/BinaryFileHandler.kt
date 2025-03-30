package com.cloudsheeptech.shoppinglist.data.recipe

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BinaryFileHandler : IBinaryFileHandler {

    override suspend fun readImageFromFile(imageUri: Uri): ByteArray? {
        var binaryImage: ByteArray? = null
        withContext(Dispatchers.IO) {
            try {
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

    override suspend fun readImagesFromFiles(imageUris: List<Uri>): List<ByteArray> {
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