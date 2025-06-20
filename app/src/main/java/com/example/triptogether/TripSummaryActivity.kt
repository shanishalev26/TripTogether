package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.triptogether.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TripSummaryActivity : AppCompatActivity() {

    private lateinit var trip: Trip
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private var isFavorite = false

    private lateinit var tvMembers: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_summary)

        trip = intent.getSerializableExtra("trip") as Trip

        val tvTripName = findViewById<TextView>(R.id.tvTheTripName)
        val tvCountry = findViewById<TextView>(R.id.tvCountry)
        val tvDates = findViewById<TextView>(R.id.tvDates)
        tvMembers = findViewById(R.id.tvMembers)
        val icUsers = findViewById<ImageView>(R.id.icUsers)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val ivImage = findViewById<ImageView>(R.id.ivTripImage)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnFavorite = findViewById<ImageView>(R.id.btnFavorite)
        val btnEnterPlan = findViewById<Button>(R.id.btnEnterPlan)
        val mapTab = findViewById<TextView>(R.id.mapTab)
        val btnChat = findViewById<Button>(R.id.btnChat)
        val btnDocuments: Button = findViewById(R.id.btnDocuments)

        tvTripName.text = trip.name
        tvCountry.text = trip.country
        tvDates.text = "${trip.startDate} - ${trip.endDate}"
        tvMembers.text = trip.members.size.toString()

        if (!trip.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(trip.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivImage)
        } else {
            ivImage.setImageResource(R.drawable.default_trip_image)
        }


        val userRef = database.child("users").child(trip.createdBy)
        userRef.child("email").get().addOnSuccessListener { snapshot ->
            val email = snapshot.getValue(String::class.java) ?: trip.createdBy
            tvDescription.text = "Created by $email"
        }

        val userId = auth.currentUser?.uid ?: return
        val favRef = database.child("favorites").child(userId).child(trip.id)

        favRef.get().addOnSuccessListener { snapshot ->
            isFavorite = snapshot.exists()
            updateFavoriteIcon()
        }

        btnChat.setOnClickListener {
            val intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("tripId", trip.id)
            intent.putExtra("tripName", trip.name)
            startActivity(intent)
        }

        btnDocuments.setOnClickListener {
            val intent = Intent(this, DocumentsHomeActivity::class.java)
            intent.putExtra("tripId", trip.id)
            intent.putExtra("createdBy", trip.createdBy)
            startActivity(intent)
        }

        btnFavorite.setOnClickListener {
            val newFavoriteState = !isFavorite
            if (newFavoriteState) {
                favRef.setValue(trip)
            } else {
                favRef.removeValue()
            }

            isFavorite = newFavoriteState
            updateFavoriteIcon()

            Toast.makeText(
                this,
                if (isFavorite) "Added to favorites" else "Removed from favorites",
                Toast.LENGTH_SHORT
            ).show()
        }


        mapTab.setOnClickListener {
            val intent = Intent(this, TripMapActivity::class.java)
            intent.putExtra("tripLat", trip.latitude)
            intent.putExtra("tripLng", trip.longitude)
            intent.putExtra("country", trip.country)
            startActivity(intent)
        }

        icUsers.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            intent.putExtra("tripId", trip.id)
            intent.putExtra("createdBy", trip.createdBy)
            startActivity(intent)
        }

        btnEnterPlan.setOnClickListener {
            val intent = Intent(this, TripPlanActivity::class.java)
            intent.putExtra("trip", trip)
            startActivity(intent)
        }

        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        updateMemberCount()
    }

    private fun updateMemberCount() {
        val tripRef = database
            .child("trips")
            .child(trip.createdBy)
            .child(trip.id)

        tripRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedTrip = snapshot.getValue(Trip::class.java)
                val count = updatedTrip?.members?.size ?: 0
                tvMembers.text = count.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateFavoriteIcon() {
        val iconRes = if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_fav
        findViewById<ImageView>(R.id.btnFavorite).setImageResource(iconRes)
    }
}
