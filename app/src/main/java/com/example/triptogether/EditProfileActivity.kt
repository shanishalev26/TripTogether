package com.example.triptogether

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivProfileImage: ImageView
    private lateinit var btnChooseImage: Button
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etRole: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnChangePassword: Button

    private var selectedImageUri: Uri? = null
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")
    private val storageRef = FirebaseStorage.getInstance().reference.child("profile_images")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etRole = findViewById(R.id.etRole)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        loadUserProfile()

        btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1001)
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        btnChangePassword.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.email?.let { email ->
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loadUserProfile() {
        val uid = currentUser?.uid ?: return
        dbRef.child(uid).get().addOnSuccessListener { snapshot ->
            val fullName = snapshot.child("name").getValue(String::class.java)
            val firstName = snapshot.child("firstName").getValue(String::class.java)
                ?: fullName?.split(" ")?.firstOrNull() ?: ""
            val lastName = snapshot.child("lastName").getValue(String::class.java)
                ?: fullName?.split(" ")?.getOrNull(1) ?: ""

            val role = snapshot.child("role").getValue(String::class.java) ?: ""

            val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                ?: snapshot.child("photoUrl").getValue(String::class.java)

            etFirstName.setText(firstName)
            etLastName.setText(lastName)
            etRole.setText(role)

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this).load(imageUrl).into(ivProfileImage)
            }
        }
    }

    private fun saveProfile() {
        val uid = currentUser?.uid ?: return
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val role = etRole.text.toString().trim()

        if (selectedImageUri != null) {
            val fileRef = storageRef.child("$uid.jpg")
            fileRef.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Upload failed")
                    }
                    fileRef.downloadUrl
                }
                .addOnSuccessListener { uri ->
                    saveToDatabase(uid, firstName, lastName, role, uri.toString())
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            dbRef.child(uid).get().addOnSuccessListener { snapshot ->
                val existingImageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                    ?: snapshot.child("photoUrl").getValue(String::class.java)
                saveToDatabase(uid, firstName, lastName, role, existingImageUrl)
            }
        }
    }

    private fun saveToDatabase(uid: String, firstName: String, lastName: String, role: String, imageUrl: String?) {
        val updates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "role" to role,
            "imageUrl" to (imageUrl ?: "")
        )
        dbRef.child(uid).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            ivProfileImage.setImageURI(selectedImageUri)
        }
    }
}
