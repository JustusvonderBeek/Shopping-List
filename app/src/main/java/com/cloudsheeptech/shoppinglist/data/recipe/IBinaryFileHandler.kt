package com.cloudsheeptech.shoppinglist.data.recipe

import android.net.Uri

interface IBinaryFileHandler {

    suspend fun readImageFromFile(imageUri: Uri): ByteArray?

    suspend fun readImagesFromFiles(imageUris: List<Uri>): List<ByteArray>

    suspend fun storeImageToFile(imageByteArray: ByteArray): Uri

}