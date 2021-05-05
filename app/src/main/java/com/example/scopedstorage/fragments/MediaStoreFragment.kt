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
import com.example.scopedstorage.adapters.MediaFileAdapter
import com.example.scopedstorage.utils.MediaStoreOperations
import com.example.scopedstorage.utils.MediaStoreOperations.Companion.getFileByType
import com.example.scopedstorage.utils.MediaStoreOperations.Companion.getFileList
import com.example.scopedstorage.utils.MediaStoreOperations.Companion.isScopedStorage
import com.example.scopedstorage.utils.MediaStoreOperations.Companion.removeFileIfExists
import com.example.scopedstorage.utils.Utils.Companion.getRawUri
import com.example.scopedstorage.utils.Utils.Companion.showDialog
import com.example.scopedstorage.utils.extensions.toByteArray
import com.example.scopedstorage.utils.toByteArray
import kotlinx.android.synthetic.main.fragment_media_store.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MediaStoreFragment : Fragment() {

    private var adapter: MediaFileAdapter? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_store, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isScopedStorage() && grantExternalStoragePermission()) {
            init()
        } else {
            init()
        }
    }

    private fun init() {
        setupListeners()
    }

    fun setupListeners() {

        setupButtons()

        setupRecyclerView()
    }

    private fun setupButtons() {
        setupImageActions()

        setupVideoActions()

        setupAudioActions()
    }

    private fun setupAudioActions() {
        // ============== Audio ===================

        val audioFileName = "temp_audio"
        BtnSaveAudio.setOnClickListener {
            saveAudio(audioFileName)
        }

        BtnReadAudio.setOnClickListener {
            readSpecificMedia(type = MediaStoreOperations.MediaStoreFileType.AUDIO, fileName = audioFileName)
        }

        BtnDeleteAudio.setOnClickListener {
            deleteMedia(fileType = MediaStoreOperations.MediaStoreFileType.AUDIO, fileName = audioFileName)
        }

        BtnGetListAudio.setOnClickListener {
            getAllFilesOfType(MediaStoreOperations.MediaStoreFileType.AUDIO)
        }
    }

    private fun setupVideoActions() {
        // ============== Video ===================

        val videoFileName = "temp_video"
        BtnSaveVideo.setOnClickListener {
            saveVideo(videoFileName)
        }

        BtnReadVideo.setOnClickListener {
            readSpecificMedia(type = MediaStoreOperations.MediaStoreFileType.VIDEO, fileName = videoFileName)
        }

        BtnDeleteVideo.setOnClickListener {
            deleteMedia(fileType = MediaStoreOperations.MediaStoreFileType.VIDEO, fileName = videoFileName)
        }

        BtnGetListVideo.setOnClickListener {
            getAllFilesOfType(MediaStoreOperations.MediaStoreFileType.VIDEO)
        }
    }

    private fun setupImageActions() {
        // ============== Image ===================
        val imageFileName = "temp_image"
        BtnSaveImage.setOnClickListener {
            saveImage(imageFileName)
        }

        BtnReadImage.setOnClickListener {
            readSpecificMedia(type = MediaStoreOperations.MediaStoreFileType.IMAGE, fileName = imageFileName)
        }

        BtnDeleteImage.setOnClickListener {

            deleteMedia(fileType = MediaStoreOperations.MediaStoreFileType.IMAGE, fileName = imageFileName)

        }

        BtnGetListImage.setOnClickListener {
            getAllFilesOfType(MediaStoreOperations.MediaStoreFileType.IMAGE)
        }
    }

    private fun readSpecificMedia(type: MediaStoreOperations.MediaStoreFileType,
                                  fileName: String) {
        GlobalScope.launch {
            val media = getFileByType(requireActivity(), type, fileName = fileName)
            if (media == null) {
                showDialog(requireActivity(), "Not Found", isError = true)
            } else {
                showDialog(requireActivity(), media.toString(), isError = false)
            }
        }
    }

    private fun getAllFilesOfType(filetype: MediaStoreOperations.MediaStoreFileType) {
        GlobalScope.launch {
            val list = getFileList(requireActivity(), filetype)
            requireActivity().runOnUiThread {
                rvList.adapter = adapter
                adapter?.setFileList(list)
                if (list.size == 0)
                    showDialog(requireActivity(), "Not Results Found", isError = false)
            }
        }
    }

    private fun deleteMedia(fileType: MediaStoreOperations.MediaStoreFileType, fileName: String) {
        GlobalScope.launch {
            val isSuccess = removeFileIfExists(activity = requireActivity(), fileType = fileType, fileName = fileName)
            if (isSuccess)
                showDialog(requireActivity())
            else
                showDialog(requireActivity(), message = "Error", isError = true)
        }
    }

    private fun saveImage(imageFileName: String) {
        GlobalScope.launch {
            val icon = BitmapFactory.decodeResource(
                    requireContext().resources,
                    R.drawable.android
            )

            removeFileIfExists(activity = requireActivity(), fileType = MediaStoreOperations.MediaStoreFileType.IMAGE, fileName = imageFileName)
            MediaStoreOperations.createFile(
                    context = requireActivity(),
                    fileName = "${imageFileName}.jpg",
                    mimeType = "jpeg",
                fileType = MediaStoreOperations.MediaStoreFileType.IMAGE,
                fileContents = icon.toByteArray()
            )
        }
    }


    private fun saveVideo(videoFileName: String) {
        GlobalScope.launch {

            val video = getRawUri(requireContext(), R.raw.android_studio)

            removeFileIfExists(activity = requireActivity(), fileType = MediaStoreOperations.MediaStoreFileType.VIDEO, fileName = videoFileName)

            video?.toByteArray(requireContext())?.let { video ->
                MediaStoreOperations.createFile(
                        context = requireActivity(),
                        fileName = "${videoFileName}.mp4",
                        mimeType = "mp4",
                    fileType = MediaStoreOperations.MediaStoreFileType.VIDEO,
                    fileContents = video
                )
            }

        }
    }

    private fun saveAudio(audioFileName: String) {
        GlobalScope.launch {
            val audio = getRawUri(requireContext(), R.raw.android_studio_audio)

            removeFileIfExists(activity = requireActivity(), fileType = MediaStoreOperations.MediaStoreFileType.AUDIO, fileName = audioFileName)

            audio?.toByteArray(requireContext())?.let { audio ->
                MediaStoreOperations.createFile(
                        context = requireActivity(),
                        fileName = "${audioFileName}.mp3",
                        mimeType = "mp3",
                    fileType = MediaStoreOperations.MediaStoreFileType.AUDIO,
                    fileContents = audio
                )
            }
        }
    }

    private fun setupRecyclerView() {
        rvList.layoutManager = LinearLayoutManager(requireContext())
        adapter = MediaFileAdapter()
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