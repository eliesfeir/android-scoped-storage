package com.example.scopedstorage.utils

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import com.example.scopedstorage.R
import java.io.File

class Utils {

    companion object {
        fun getAppName(context: Context) = context.getString(R.string.app_name)

         fun ensureDirExists(directory: String): String {
            val dir = File(directory)
            if (!dir.exists()) dir.mkdirs()
            return dir.path
        }

        fun getRawUri(context: Context, @RawRes rawResource: Int): Uri? {
            return Uri.parse("android.resource://" + context.packageName + "/" + rawResource)
        }

    }
}