package com.example.triptogether

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.adapters.DocumentAdapter
import com.example.triptogether.databinding.ActivityDocumentsBinding
import com.example.triptogether.model.SharedItem
import com.google.firebase.database.*

class DocumentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentsBinding
    private lateinit var adapter: DocumentAdapter
    private val documentList = mutableListOf<SharedItem>()

    private lateinit var tripId: String
    private val dbRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getStringExtra("tripId") ?: run {
            Toast.makeText(this, "Trip ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = DocumentAdapter(documentList)
        binding.rvDocuments.layoutManager = LinearLayoutManager(this)
        binding.rvDocuments.adapter = adapter

        loadDocuments()
    }

    private fun loadDocuments() {
        dbRef.child("tripDocuments").child(tripId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    documentList.clear()
                    for (docSnap in snapshot.children) {
                        val doc = docSnap.getValue(SharedItem::class.java)
                        if (doc != null) {
                            documentList.add(doc)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DocumentsActivity, "Failed to load documents", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
