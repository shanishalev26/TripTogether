package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.triptogether.adapters.ImageAdapter
import com.example.triptogether.databinding.ActivityImagesListBinding
import com.example.triptogether.model.SharedItem
import com.google.firebase.database.*

class ImagesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagesListBinding
    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<SharedItem>()

    private lateinit var tripId: String
    private val dbRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getStringExtra("tripId") ?: return

        adapter = ImageAdapter(imageList) { imageUrl ->
            val intent = Intent(this, ImagePreviewActivity::class.java)
            intent.putExtra("imageUrl", imageUrl)
            startActivity(intent)
        }

        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
        binding.rvImages.adapter = adapter

        loadImages()
    }

    private fun loadImages() {
        dbRef.child("tripImages").child(tripId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    imageList.clear()
                    for (docSnap in snapshot.children) {
                        val doc = docSnap.getValue(SharedItem::class.java)
                        if (doc != null && isImage(doc.url)) {
                            imageList.add(doc)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ImagesListActivity, "Failed to load images", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun isImage(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return listOf(".jpg", ".jpeg", ".png").any { ext -> lowerUrl.contains(ext) }
    }

}
