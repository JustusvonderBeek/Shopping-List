package com.cloudsheeptech.shoppinglist.data.recipe

import android.net.Uri

interface IBinaryFileHandler {
    suspend fun readImageFromFile(imageUri: String): ByteArray?

    suspend fun readImagesFromFiles(imageUris: List<String>): List<ByteArray>

    suspend fun storeImageToFile(imageByteArray: ByteArray): Uri
}
