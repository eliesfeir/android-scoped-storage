package com.example.scopedstorage.utils

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

fun Bitmap.toByteArray(imageType: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(imageType, 100, stream)
    return stream.toByteArray()
}