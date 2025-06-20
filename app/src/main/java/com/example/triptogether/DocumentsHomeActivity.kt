package com.example.triptogether

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.triptogether.model.SharedItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class DocumentsHomeActivity : AppCompatActivity() {

    private val FILE_PICKER_REQUEST = 1002

    private lateinit var cardDocuments: CardView
    private lateinit var cardImages: CardView
    private lateinit var btnUploadFile: Button

    private lateinit var tripId: String
    private lateinit var createdBy: String

    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents_home)

        cardDocuments = findViewById(R.id.cardDocuments)
        cardImages = findViewById(R.id.cardImages)
        btnUploadFile = findViewById(R.id.btnUploadFile)

        tripId = intent.getStringExtra("tripId") ?: return
        createdBy = intent.getStringExtra("createdBy") ?: return

        btnUploadFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, FILE_PICKER_REQUEST)
        }

        cardDocuments.setOnClickListener {
            val intent = Intent(this, DocumentsActivity::class.java)
            intent.putExtra("tripId", tripId)
            intent.putExtra("createdBy", createdBy)
            startActivity(intent)
        }

        cardImages.setOnClickListener {
            val intent = Intent(this, ImagesListActivity::class.java)
            intent.putExtra("tripId", tripId)
            intent.putExtra("createdBy", createdBy)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val fileUri = data.data!!
            val fileType = contentResolver.getType(fileUri) ?: ""

            val isImage = fileType.startsWith("image")
            val folder = if (isImage) "images" else "documents"
            val dbFolder = if (isImage) "tripImages" else "tripDocuments"

            val fileName = System.currentTimeMillis().toString()
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType) ?: "file"
            val fullName = "$fileName.$extension"

            val storageRef = storage.getReference("tripFiles/$createdBy/$tripId/$folder/$fullName")

            storageRef.putFile(fileUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val fileUrl = uri.toString()
                        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
                        val sharedItem = SharedItem(
                            name = fullName,
                            url = fileUrl,
                            uploadedBy = userEmail,
                            timestamp = System.currentTimeMillis()
                        )

                        val dbRef = FirebaseDatabase.getInstance().getReference(dbFolder)
                        dbRef.child(tripId).push().setValue(sharedItem)

                        Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show()

                        val nextIntent = if (isImage) {
                            Intent(this, ImagesListActivity::class.java)
                        } else {
                            Intent(this, DocumentsActivity::class.java)
                        }
                        nextIntent.putExtra("tripId", tripId)
                        nextIntent.putExtra("createdBy", createdBy)
                        startActivity(nextIntent)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
