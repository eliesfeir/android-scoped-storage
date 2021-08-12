package com.example.scopedstorage.fragments

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.example.scopedstorage.R
import com.example.scopedstorage.adapters.DocumentsAdapter
import com.example.scopedstorage.utils.Utils
import kotlinx.android.synthetic.main.fragment_non_media.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class NonMediaFragment : Fragment() {

    var root:View? = null

    val CREATE_REQUEST_CODE = 4
    val OPEN_DOCUMENT_REQUEST_CODE = 5
    val OPEN_DIRECTORY_REQUEST_CODE = 6

    var fileList = ArrayList<DocumentFile>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_non_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews(){
        btnCreateFile.setOnClickListener {
            checkAndCreateFile()
        }

        btnOpenPdf.setOnClickListener {
            openPdfFile(it)
        }

        btnDownloadPdf.setOnClickListener {
            downalodPDf(it)
        }

        btnGetDocuments.setOnClickListener {
            openDocTree(it)
        }

    }


    private fun checkAndCreateFile(){
        if(etContent.text.toString().trim().isEmpty() || etFileName.text.toString().trim().isEmpty()){
            Toast.makeText(activity,"Fill all values", Toast.LENGTH_LONG).show()
            return
        }
        createFile()
    }

    private fun createFile(){
        val fileName = etFileName.text.toString().trim().isEmpty()

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)

        startActivityForResult(intent, CREATE_REQUEST_CODE)
    }

    private fun writeFileContent(uri: Uri?) {
        try {
            val pfd = uri?.let { activity?.contentResolver?.openFileDescriptor(it, "w") }

            val fileOutputStream = FileOutputStream(
                pfd!!.fileDescriptor
            )

            val textContent = etContent.text.toString().trim()

            fileOutputStream.write(textContent.toByteArray())

            fileOutputStream.close()
            pfd.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun openPdfFile(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }

    @SuppressLint("NewApi")
    fun downalodPDf(view: View) {
        // create a new document
        val document = PdfDocument()
        // crate a page description
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        // start a page
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        paint.setColor(Color.RED)
        canvas.drawCircle(50F, 50F, 30F, paint)
        paint.setColor(Color.BLACK)
        canvas.drawText("Rizk PDF Test", 80F, 50F, paint)
        // finish the page
        document.finishPage(page)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "demopdf.pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = activity?.contentResolver
            // Log.i("TAG","${MediaStore.getExternalVolumeNames(this)}")

            val volumeNames = MediaStore.getExternalVolumeNames(activity!!)


            val collection = MediaStore.Downloads.getContentUri(volumeNames.elementAt(1))

            val item = resolver?.insert(collection, values)


            if (item != null) {
                resolver.openOutputStream(item).use { out ->
                    document.writeTo(out);
                }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            item?.let { resolver?.update(it, values, null, null) }

            Toast.makeText(
                activity?.applicationContext,
                "Download successfully to ${item?.path}",
                Toast.LENGTH_LONG
            ).show()
        }else{
            if (checkSelfPermission(activity!!,WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                val mypath = File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"demopdf.pdf");
                document.writeTo( FileOutputStream(mypath));

                document.close()
                Toast.makeText(
                    activity?.applicationContext,
                    "Download successfully to ${mypath}",
                    Toast.LENGTH_LONG
                ).show()
            }else{
                ActivityCompat.requestPermissions(activity!!,  arrayOf(WRITE_EXTERNAL_STORAGE), 100)
            }

        }



    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downalodPDf(view!!)
        }
    }

    fun openDocTree(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
    }

    fun loadDirectory(directoryUri: Uri) {
        val documentsTree = DocumentFile.fromTreeUri(activity!!, directoryUri) ?: return
        val childDocuments = documentsTree.listFiles().asList()


        for (i in 0 until childDocuments.size) {
            Log.i("TAG", "${childDocuments[i].type}")
            fileList.add(childDocuments[i])

        }
        rvDocuments.adapter = DocumentsAdapter(activity!!,fileList)

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CREATE_REQUEST_CODE) {
                if (data != null) {
                    var currentUri = data.data
                    writeFileContent(currentUri)
                }
            }

        }

        if (requestCode == OPEN_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { documentUri ->
                activity?.contentResolver?.takePersistableUriPermission(
                    documentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                tvPdfName.text = "PDF Name: " + documentUri.path.toString()
                OpenPDFFile(Utils.getPath(activity!!,documentUri)?:"")
            }
        }

        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            fileList.clear()
            val directoryUri = data?.data ?: return

            activity?.contentResolver?.takePersistableUriPermission(
                directoryUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            loadDirectory(directoryUri)


        }
    }

    fun OpenPDFFile(path:String) {
        val pdfFile = File(
            path
        )
        if (pdfFile.exists()) //Checking for the file is exist or not
        {
            val path = Uri.fromFile(pdfFile)
            val objIntent = Intent(Intent.ACTION_VIEW)
            objIntent.setDataAndType(path, "application/pdf")
            objIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(objIntent) //Staring the pdf viewer
        }else{
            Toast.makeText(activity,"Path not found",Toast.LENGTH_LONG).show()
        }
    }

}