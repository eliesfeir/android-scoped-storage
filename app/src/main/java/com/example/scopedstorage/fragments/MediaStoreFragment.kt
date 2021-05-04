package com.example.scopedstorage.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scopedstorage.R
import com.example.scopedstorage.utils.MediaStoreOperations
import com.example.scopedstorage.utils.MediaStoreOperations.Companion.getFileList
import com.example.scopedstorage.utils.Utils.Companion.getRawUri
import com.example.scopedstorage.utils.extensions.toByteArray
import com.example.scopedstorage.utils.toByteArray
import kotlinx.android.synthetic.main.fragment_media_store.*


class MediaStoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (grantExternalStoragePermission())
            init()

    }

    private fun init() {
        setupListeners()
    }

    fun setupListeners() {

        setupButtons()

        setupRecyclerView()
    }

    private fun setupButtons() {
        // ============== Image ===================
        BtnSaveImage.setOnClickListener {
            saveImage()
        }

        BtnReadImage.setOnClickListener {
            getFileList(requireContext(), MediaStoreOperations.MediaStoreFileType.IMAGE)
        }

        // ============== Video ===================

        BtnSaveVideo.setOnClickListener {
            saveVideo()
        }

        BtnReadVideo.setOnClickListener {
            getFileList(requireContext(), MediaStoreOperations.MediaStoreFileType.VIDEO)
        }

        // ============== Audio ===================

        BtnSaveAudio.setOnClickListener {
            saveAudio()
        }

        BtnReadAudio.setOnClickListener {
            getFileList(requireContext(), MediaStoreOperations.MediaStoreFileType.AUDIO)
        }
    }

    private fun saveImage() {
        val icon = BitmapFactory.decodeResource(
            requireContext().getResources(),
            R.drawable.android
        )
        MediaStoreOperations.createFile(
            context = requireContext(),
            fileName = "temp_image.jpg",
            mimeType = "jpeg",
            fileType = MediaStoreOperations.MediaStoreFileType.IMAGE,
            fileContents = icon.toByteArray()
        )
    }

    private fun saveVideo() {
        val video = getRawUri(requireContext(), R.raw.android_studio)
        video?.toByteArray(requireContext())?.let { video ->
            MediaStoreOperations.createFile(
                context = requireContext(),
                fileName = "temp_video.mp4",
                mimeType = "mp4",
                fileType = MediaStoreOperations.MediaStoreFileType.VIDEO,
                fileContents = video
            )
        }
    }

    private fun saveAudio() {
        val audio = getRawUri(requireContext(), R.raw.android_studio_audio)
        audio?.toByteArray(requireContext())?.let { audio ->
            MediaStoreOperations.createFile(
                context = requireContext(),
                fileName = "temp_audio.mp3",
                mimeType = "mp3",
                fileType = MediaStoreOperations.MediaStoreFileType.AUDIO,
                fileContents = audio
            )
        }
    }

    private fun setupRecyclerView() {
        rvList.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun grantExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("test", "Permission is granted")
                true
            } else {
                Log.d("test", "Permission is revoked")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
                false
            }
        } else {
            Toast.makeText(
                requireContext(),
                "External Storage Permission is Grant",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("test", "External Storage Permission is Grant ")
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("test", "Permission: ${permissions[0]} was ${grantResults[0]}")
            init()
        }
    }

}