package com.example.triptogether.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.triptogether.R
import com.example.triptogether.model.SharedItem

class ImageAdapter(
    private val images: List<SharedItem>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivImageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = images[position]

        Glide.with(holder.itemView.context)
            .load(imageItem.url)
            .centerCrop()
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClick(imageItem.url)
        }
    }

    override fun getItemCount(): Int = images.size
}
