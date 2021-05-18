package com.example.scopedstorage.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scopedstorage.R
import com.example.scopedstorage.utils.Utils.Companion.ensureDirExists
import com.example.scopedstorage.utils.Utils.Companion.getAppName
import com.example.scopedstorage.utils.Utils.Companion.showDialog
import dev.dnights.scopedstoragesample.mediastore.data.MediaFileData
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MediaStoreOperations {

    /**
     * enum class represents media types
     */
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

        /**
         * creates a media file using MediaStore
         * @param fileName send full file name with extension
         * @param mimeType jpeg,mp3,mp4...
         * @param fileType MediaStoreFileType.IMAGE,MediaStoreFileType.VIDEO,MediaStoreFileType.AUDIO
         * @param fileContents media file converted to ByteArray
         * @return Uri of media file created
         */
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
                    contentValues.clear()
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

        /**
         * set Video type content values according to SDK version
         */
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

        /**
         * set Audio type content values according to SDK version
         */
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

        /**
         * set Image type content values according to SDK version
         */
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

        /**
         * @return boolean that respresents if ScopedStorage supported by current SDK
         */
        fun isScopedStorage(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }

        /**
         * get all media files according to type
         * @return list of media files found
         */
        suspend fun getFileList(
                context: Activity,
                type: MediaStoreFileType,
                fileName: String = ""
        ): List<MediaFileData> {
            val fileList = mutableListOf<MediaFileData>()
            try {

                var projection = arrayOf(
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        MediaStore.Files.FileColumns.DATE_MODIFIED
                )
                if (!isScopedStorage()) {
                    projection = projection.plusElement(MediaStore.Files.FileColumns.DATA)
                }
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
                    val dateModifiedColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val displayNameColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    var pathColumn = -1
                    if (!isScopedStorage())
                        pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val dateModified = Date(cursor.getLong(dateModifiedColumn))
                        val displayName = cursor.getString(displayNameColumn)
                        val contentUri = Uri.withAppendedPath(
                                type.externalContentUri,
                                id.toString()
                        )
                        var filePath = ""
                        if (!isScopedStorage() && pathColumn >= 0) {
                            filePath = cursor.getString(pathColumn)
                        }


                        Log.d(
                                "test",
                                "id: $id, display_name: $displayName, date_modified: $dateModified, content_uri: $contentUri\n"
                        )

                        fileList.add(MediaFileData(id = id, dateModified = dateModified, displayName = displayName, uri = contentUri, path = filePath))
                    }
                }


            } catch (ex: Exception) {
                showDialog(context, message = ex.message.orEmpty(), isError = true)
            }
            return fileList
        }

        /**
         * get selection args for @see getFileList()
         */
        private fun getSelectionArgs(context: Context, fileName: String = "") =

                if (isScopedStorage()) {
                    arrayOf("%${getAppName(context)}%", "%${fileName}%")

                } else {
                    arrayOf("%${getAppName(context)}/${fileName}%")
                }


        /**
         * get selection query for @see getFileList()
         */
        private fun getSelection(fileType: MediaStoreFileType) =

                if (isScopedStorage()) {
                    getRelativePath(fileType) + " like ? and ${MediaStore.Images.Media.DISPLAY_NAME} like ? "
                } else {
                    MediaStore.Images.Media.DATA + " like ? "
                }


        /**
         * get media relative path used in scoped storage
         */
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
                    removeMediaFile(activity, list.get(0))
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
        fun removeMediaFile(context: Context, media: MediaFileData): Boolean {
            try {

                media.uri.let { uri ->
                    context.contentResolver.delete(uri, null, null)
                    if (!isScopedStorage()) {
                        if (!media.path.isNullOrBlank()) {
                            val file = File(media.path)
                            if (file.exists())
                                file.delete()
                        }
                    }
                    Log.d("test", "Removed MediaStore: $uri")
                    return true
                }
            } catch (ex: Exception) {
                return false
            }
        }

        /**
         * get specific media file according to type
         */
        suspend fun getFileByType(activity: Activity,
                                  type: MediaStoreFileType,
                                  fileName: String): MediaFileData? {
            val list = getFileList(activity, type = type, fileName = fileName)
            if (list.size > 0)
                return list.get(0)
            return null
        }

        fun getPickImageIntent(context: Context): Intent? {
            var chooserIntent: Intent? = null

            var intentList: MutableList<Intent> = ArrayList()

            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            intentList.add(pickIntent)

            if (intentList.size > 0) {
                chooserIntent = Intent.createChooser(
                        intentList.get(0),
                        context.getString(R.string.pick_image)
                )
                chooserIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        intentList.toTypedArray<Parcelable>()
                )
            }

            return chooserIntent
        }

        fun getCapturedImage(context: Context, selectedPhotoUri: Uri): Bitmap? {
            val bitmap = when {
                !isScopedStorage() -> MediaStore.Images.Media.getBitmap(
                        context.contentResolver,
                        selectedPhotoUri
                )
                isScopedStorage() -> {
                    val source = ImageDecoder.createSource(context.contentResolver, selectedPhotoUri)
                    ImageDecoder.decodeBitmap(source)
                }
                else -> null
            }

            return bitmap

        }
    }
}