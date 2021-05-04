package com.example.scopedstorage.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scopedstorage.utils.Utils.Companion.ensureDirExists
import com.example.scopedstorage.utils.Utils.Companion.getAppName
import com.example.scopedstorage.utils.Utils.Companion.showDialog
import dev.dnights.scopedstoragesample.mediastore.data.MediaFileData
import java.io.FileOutputStream
import java.util.*

class MediaStoreOperations {

    enum class MediaStoreFileType(
        val externalContentUri: Uri,
        var mimeType: String,
        val pathByDCIM: String
    ) {
        IMAGE(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/", ""),// ex: /images
        AUDIO(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/", ""),// ex: /audios
        VIDEO(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/", ""); // ex: /videos

    }

    companion object {

        fun createFile(
            context: Activity,
            fileName: String,
            mimeType: String,
            fileType: MediaStoreFileType,
            fileContents: ByteArray
        ): Uri? {
            var isError = false
            var uri: Uri? = null
            try {
                val contentValues = ContentValues()

                /**
                 * image allowed directories are [DCIM, Pictures]
                 * audio allowed directories are [Alarms, Music, Notifications, Podcasts, Ringtones]
                 * video allowed directories are [DCIM, Movies]
                 */
                val filePath = "/" + getAppName(context) + fileType.pathByDCIM
                when (fileType) {
                    MediaStoreFileType.IMAGE -> {
                        setImageValues(contentValues, filePath, fileName)
                    }
                    MediaStoreFileType.AUDIO -> {
                        setAudioValues(contentValues, filePath, fileName)
                    }
                    MediaStoreFileType.VIDEO -> {
                        setVideoValues(contentValues, filePath, fileName)
                    }
                }

                contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                contentValues.put(MediaStore.Files.FileColumns.MIME_TYPE, fileType.mimeType + mimeType)
                if (isScopedStorage()) {
                    contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                }

                uri = context.contentResolver.insert(
                    fileType.externalContentUri,
                    contentValues
                )

                val parcelFileDescriptor =
                    context.contentResolver.openFileDescriptor(uri!!, "w", null)

                val fileOutputStream = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
                fileOutputStream.write(fileContents)
                fileOutputStream.close()

                if (isScopedStorage()) {
//                    contentValues.clear()
                    contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (ex: Exception) {
                isError = true
                showDialog(context, message = ex.message.orEmpty(), isError = true)
            }
            if (!isError)
                showDialog(context)
            return uri
        }

        private fun setVideoValues(
            contentValues: ContentValues,
            filePath: String,
            fileName: String
        ) {
            if (isScopedStorage()) {
                contentValues.put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + filePath
                )
            } else {
                contentValues.put(
                    MediaStore.MediaColumns.DATA,
                    ensureDirExists("${Environment.getExternalStorageDirectory().path}/${Environment.DIRECTORY_MOVIES}$filePath") + "/$fileName"
                )
            }
        }

        private fun setAudioValues(
            contentValues: ContentValues,
            filePath: String,
            fileName: String
        ) {
            if (isScopedStorage()) {
                contentValues.put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + filePath
                )
            } else {
                contentValues.put(
                    MediaStore.MediaColumns.DATA,
                    ensureDirExists("${Environment.getExternalStorageDirectory().path}/${Environment.DIRECTORY_MUSIC}$filePath") + "/$fileName"
                )
            }
        }

        private fun setImageValues(
            contentValues: ContentValues,
            filePath: String,
            fileName: String
        ) {
            if (isScopedStorage()) {
                contentValues.put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + filePath
                )
            } else {
                contentValues.put(
                    MediaStore.MediaColumns.DATA,
                    ensureDirExists("${Environment.getExternalStorageDirectory().path}/${Environment.DIRECTORY_PICTURES}$filePath") + "/$fileName"
                )
            }
        }

        private fun isScopedStorage(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }

        suspend fun getFileList(
            context: Activity,
            type: MediaStoreFileType,
            fileName: String = ""
        ): List<MediaFileData> {
            val fileList = mutableListOf<MediaFileData>()
            try {

                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )

                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

                val selection = getSelection(type)
                val selectionArgs = getSelectionArgs(context, fileName)

                val cursor = context.contentResolver.query(
                    type.externalContentUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val dateTakenColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val displayNameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val dateTaken = Date(cursor.getLong(dateTakenColumn))
                        val displayName = cursor.getString(displayNameColumn)
                        val contentUri = Uri.withAppendedPath(
                            type.externalContentUri,
                            id.toString()
                        )

                        Log.d(
                                "test",
                                "id: $id, display_name: $displayName, date_modified: $dateTaken, content_uri: $contentUri\n"
                        )

                        fileList.add(MediaFileData(id, dateTaken, displayName, contentUri))
                    }
                }


            } catch (ex: Exception) {
                showDialog(context, message = ex.message.orEmpty(), isError = true)
            }
            return fileList
        }

        private fun getSelectionArgs(context: Context, fileName: String = "") =

            if (isScopedStorage()) {
                arrayOf("%${getAppName(context)}%", "%${fileName}%")

            } else {
                arrayOf("%${getAppName(context)}/${fileName}%")
            }


        private fun getSelection(fileType: MediaStoreFileType) =

            if (isScopedStorage()) {
                getRelativePath(fileType) + " like ? and ${MediaStore.Images.Media.DISPLAY_NAME} like ? "
            } else {
                MediaStore.Images.Media.DATA + " like ? "
            }


        @RequiresApi(Build.VERSION_CODES.Q)
        private fun getRelativePath(fileType: MediaStoreFileType) =
            when (fileType) {
                MediaStoreFileType.IMAGE -> {
                    MediaStore.Images.Media.RELATIVE_PATH
                }
                MediaStoreFileType.AUDIO -> {
                    MediaStore.Audio.Media.RELATIVE_PATH
                }
                MediaStoreFileType.VIDEO -> {
                    MediaStore.Video.Media.RELATIVE_PATH
                }
                else -> ""
            }


        /**
         * @return true if file is deleted, false otherwise
         * @param fileType Image,Video,Audio
         * @param fileName with or without extension
         */
        suspend fun removeFileIfExists(activity: Activity, fileType: MediaStoreOperations.MediaStoreFileType, fileName: String): Boolean {
            try {
                val list = getFileList(activity, type = fileType, fileName = fileName)
                if (list.size > 0) {
                    removeMediaFile(activity, list.get(0).uri)
                }
                return true
            } catch (ex: Exception) {
                return false
            }
        }

        /**
         * @return true if file deleted successfully, false otherwise
         * @param uri uri of a file
         */
        fun removeMediaFile(context: Context, uri: Uri): Boolean {
            try {

                uri.let {
                    context.contentResolver.delete(uri, null, null)
                    return true
                    Log.d("test", "Removed MediaStore: $it")
                }
            } catch (ex: Exception) {
                return false
            }
        }

        suspend fun getFileByType(activity: Activity,
                                  type: MediaStoreFileType,
                                  fileName: String): MediaFileData? {
            val list = getFileList(activity, type = type, fileName = fileName)
            if (list.size > 0)
                return list.get(0)
            return null
        }
    }
}