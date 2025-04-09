package com.cloudsheeptech.shoppinglist.data.recipe

import java.net.URI

interface IBinaryFileHandler {
    suspend fun readImageFromFile(imageUri: String): ByteArray?

    suspend fun readImagesFromFiles(imageUris: List<String>): List<ByteArray>

    suspend fun storeImageToFile(imageByteArray: ByteArray, fileName: String): URI
}
