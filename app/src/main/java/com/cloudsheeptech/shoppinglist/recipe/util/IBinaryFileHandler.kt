package com.cloudsheeptech.shoppinglist.recipe.util

import java.net.URI

interface IBinaryFileHandler {
    suspend fun readImageFromFile(imageUri: String): ByteArray?

    suspend fun readImagesFromFiles(imageUris: List<String>): List<ByteArray>

    suspend fun storeImageToFile(
        imageByteArray: ByteArray,
        fileName: String,
        compress: Boolean = false,
    ): URI

    suspend fun deleteImage(imageUri: String): Boolean

    suspend fun deleteImagesForRecipe(imageUris: List<String>): Int
}
