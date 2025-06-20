package com.example.triptogether.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.R
import com.example.triptogether.model.SharedItem
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val documentList: List<SharedItem>
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val fileName: TextView = itemView.findViewById(R.id.tvFileName)
        val fileInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documentList[position]

        holder.fileName.text = document.name

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(document.timestamp))
        holder.fileInfo.text = buildString {
        append("Uploaded by ")
        append(document.uploadedBy)
        append(" • ")
        append(dateStr)
    }

        val extension = MimeTypeMap.getFileExtensionFromUrl(document.url).lowercase(Locale.ROOT)

        val iconRes = when (extension) {
            "pdf" -> R.drawable.ic_pdf
            "jpg", "jpeg", "png" -> R.drawable.ic_image
            else -> R.drawable.ic_file
        }
        holder.fileIcon.setImageResource(iconRes)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(document.url), getMimeType(extension))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = documentList.size

    private fun getMimeType(extension: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
