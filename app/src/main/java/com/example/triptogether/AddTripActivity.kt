package com.example.triptogether

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.triptogether.databinding.ActivityAddTripBinding
import com.example.triptogether.model.Trip
import com.example.triptogether.utilities.Constants
import com.example.triptogether.utilities.LocationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.*

class AddTripActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripBinding

    private var selectedImageUri: Uri? = null
    private val imageRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etStartDate.setOnClickListener {
            showDatePicker { selectedDate ->
                binding.etStartDate.setText(selectedDate)
            }
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker { selectedDate ->
                binding.etEndDate.setText(selectedDate)
            }
        }

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, imageRequestCode)
        }

        binding.btnSaveTrip.setOnClickListener {
            saveTripToFirebase()
        }

    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, y, m, d ->
                val dateString = String.format(buildString {
                    append("%02d/%02d/%04d")
                }, d, m + 1, y)
                onDateSelected(dateString)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun saveTripToFirebase() {
        val country = binding.etCountry.text.toString().trim()
        val tripName = binding.etTripName.text.toString().trim()
        val startDate = binding.etStartDate.text.toString().trim()
        val endDate = binding.etEndDate.text.toString().trim()
        val membersText = binding.etMembers.text.toString().trim()

        if (tripName.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }


        val emailList = membersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        fetchUserIdsByEmails(emailList) { memberUids ->
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@fetchUserIdsByEmails


            val allMembers = memberUids.toMutableSet().apply { add(currentUid) }.toList()

            val tripId = FirebaseDatabase.getInstance().getReference("trips").push().key ?: return@fetchUserIdsByEmails
            val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(currentUid)

            lifecycleScope.launch {
                val coordinates = LocationUtils.getCoordinatesFromLocationName(country, Constants.GEOCODING_API_KEY)
                val lat = coordinates?.first ?: 0.0
                val lng = coordinates?.second ?: 0.0

                val defaultImageUrl: String? = null

                // אם נבחרה תמונה → מעלים ל-Firebase Storage
                if (selectedImageUri != null) {
                    val imageRef = FirebaseStorage.getInstance().reference.child("tripProfileImages/$tripId.jpg")
                    imageRef.putFile(selectedImageUri!!)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let { throw it }
                            }
                            imageRef.downloadUrl
                        }.addOnCompleteListener { task ->
                            val imageUrl = if (task.isSuccessful) task.result.toString() else defaultImageUrl
                            uploadTripData(tripRef, tripId, tripName, country, startDate, endDate, currentUid, imageUrl, allMembers, lat, lng)
                        }
                } else {
                    uploadTripData(tripRef, tripId, tripName, country, startDate, endDate, currentUid, null, allMembers, lat, lng)

                }
            }
        }
    }


    private fun uploadTripData(
        dbRef: DatabaseReference,
        tripId: String,
        name: String,
        country: String,
        startDate: String,
        endDate: String,
        creatorId: String,
        imageUrl: String?,
        members: List<String>,
        latitude: Double,
        longitude: Double
    ) {
        val trip = Trip(
            id = tripId,
            name = name,
            country = country,
            startDate = startDate,
            endDate = endDate,
            createdBy = creatorId,
            imageUrl = imageUrl,
            isFavorite = false,
            members = members,
            latitude = latitude,
            longitude = longitude
        )

        dbRef.child(tripId).setValue(trip)
            .addOnSuccessListener {
                for (memberId in members) {
                    FirebaseDatabase.getInstance().getReference("userTrips")
                        .child(memberId)
                        .child(tripId)
                        .setValue(true)
                }

                Toast.makeText(this, "Trip saved!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to save trip.", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper to convert emails to user IDs (UIDs)
    private fun fetchUserIdsByEmails(emails: List<String>, onResult: (List<String>) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        val userIds = mutableListOf<String>()
        val remaining = emails.toMutableList()

        database.get().addOnSuccessListener { snapshot ->
            for (email in emails) {
                val matchedUid = snapshot.children.firstOrNull { userSnapshot ->
                    val userEmail = userSnapshot.child("email").value as? String
                    userEmail.equals(email, ignoreCase = true)
                }?.key

                if (matchedUid != null) {
                    userIds.add(matchedUid)
                    remaining.remove(email)
                }
            }

            if (remaining.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "The following users aren't registered: ${remaining.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }

            onResult(userIds)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user IDs", Toast.LENGTH_SHORT).show()
            onResult(emptyList())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imageRequestCode && resultCode == RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            binding.ivTripImage.setImageURI(selectedImageUri)
        }
    }

}
