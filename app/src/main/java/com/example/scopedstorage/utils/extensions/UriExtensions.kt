package com.example.scopedstorage.utils.extensions

import android.content.Context
import android.net.Uri
import java.io.IOException

@Throws(IOException::class)
 fun Uri.toByteArray(context: Context): ByteArray? =
    context.contentResolver.openInputStream(this)?.buffered()?.use { it.readBytes()}