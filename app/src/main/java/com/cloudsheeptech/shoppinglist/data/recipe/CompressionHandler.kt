package com.cloudsheeptech.shoppinglist.data.recipe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import net.jpountz.lz4.LZ4Factory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.min

class CompressionHandler
    @Inject
    constructor() {
        private val compressionFactory = LZ4Factory.fastestJavaInstance()

        private val targetWidth = 256
        private val targetHeight = 256

        private fun scaleImageToMaximumAppSize(image: ByteArray): ByteArray = scaleImageToSize(image, targetWidth, targetHeight)

        fun scaleImageToSize(
            image: ByteArray,
            targetWidth: Int,
            targetHeight: Int,
            outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            outputQuality: Int = 85,
        ): ByteArray {
            val byteInputStream = ByteArrayInputStream(image)
            val exifInterface = ExifInterface(byteInputStream)

            val originalBitmap =
                BitmapFactory.decodeByteArray(image, 0, image.size)
                    ?: throw IllegalArgumentException("Unable to decode image")

            val newWidth = min(originalBitmap.width, targetWidth)
            val newHeight = min(originalBitmap.height, targetHeight)

            val fixedBitmap = fixOrientation(originalBitmap, exifInterface)
            val scaledBitmap = fixedBitmap.scale(newWidth, newHeight)

            val outputStream = ByteArrayOutputStream()
            // Convert all images to JPEG to reduce file size
            scaledBitmap.compress(outputFormat, outputQuality, outputStream)

            return outputStream.toByteArray()
        }

        private fun fixOrientation(
            bitmap: Bitmap,
            exif: ExifInterface,
        ): Bitmap {
            val orientation =
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                // Add more if needed
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        fun compress(data: ByteArray): ByteArray {
            val compressor = compressionFactory.highCompressor()
            val maxCompressedLength = compressor.maxCompressedLength(data.size)
            val compressed = ByteArray(maxCompressedLength)
            val compressedLength =
                compressor.compress(data, 0, data.size, compressed, 0, maxCompressedLength)
            Log.d("CompressionHandler", "Compressed from ${data.size} to $compressedLength")
            return compressed
        }

        fun scaleAndCompress(
            data: ByteArray,
            targetWidth: Int,
            targetHeight: Int,
        ): ByteArray {
            val compressor = compressionFactory.highCompressor()
            val scaledData = scaleImageToSize(data, targetWidth, targetHeight)
            val maxCompressedLength = compressor.maxCompressedLength(scaledData.size)
            val compressed = ByteArray(maxCompressedLength)
            val compressedLength =
                compressor.compress(scaledData, 0, scaledData.size, compressed, 0, maxCompressedLength)
            Log.d(
                "CompressionHandler",
                "Scaled Image ${targetWidth}x$targetHeight and compressed from ${data.size} to $compressedLength bytes",
            )
            return compressed
        }

        fun decompress(
            data: ByteArray,
            decompressedLength: Int,
        ): ByteArray {
            val decompressor = compressionFactory.fastDecompressor()
            val decompressed = ByteArray(decompressedLength)
            val decompressedLength =
                decompressor.decompress(data, 0, decompressed, 0, decompressedLength)
            Log.d("CompressionHandler", "Decompressed from ${data.size} to $decompressedLength")
            return decompressed
        }
    }
