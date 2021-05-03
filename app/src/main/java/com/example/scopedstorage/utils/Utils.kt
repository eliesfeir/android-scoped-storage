package com.example.scopedstorage.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
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

        fun showDialog(context: Activity, message: String = "Success", isError: Boolean = false) {
            context.runOnUiThread {

                val builder = AlertDialog.Builder(context)
                //set message for alert dialog
                builder.setTitle("Result")
                builder.setMessage(message)
                builder.setIcon(if (!isError) R.drawable.ic_success else android.R.drawable.ic_dialog_info)

                //performing positive action
                builder.setPositiveButton("OK") { dialogInterface, which ->
                }

                // Create the AlertDialog
                val alertDialog: AlertDialog = builder.create()
                // Set other dialog properties
                alertDialog.setCancelable(true)
                alertDialog.show()

            }
        }

    }
}