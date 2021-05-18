package com.example.scopedstorage.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.example.scopedstorage.R
import kotlinx.android.synthetic.main.item_file.view.*

class DocumentsAdapter(private val c: Context, private val files: ArrayList<DocumentFile>) :
    RecyclerView.Adapter<DocumentsAdapter.ViewHolder>() {



    override fun getItemCount(): Int {
        return files.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(c).inflate(R.layout.item_file, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]

        holder.tvFileName.text = file.name

        holder.tvFileType.text = file.type
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName = view.tvFileName
        val tvFileType = view.tvFileType
    }
}