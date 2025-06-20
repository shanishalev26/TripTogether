package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.adapters.TripAdapter
import com.example.triptogether.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteActivity : AppCompatActivity() {

    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var tripAdapter: TripAdapter
    private val tripList = mutableListOf<Trip>()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        recyclerFavorites = findViewById(R.id.recyclerFavorites)
        recyclerFavorites.layoutManager = LinearLayoutManager(this)
        tripAdapter = TripAdapter(tripList)
        recyclerFavorites.adapter = tripAdapter

        loadFavoriteTrips()
        setupBottomNav()
    }

    private fun loadFavoriteTrips() {
        val userId = auth.currentUser?.uid ?: return
        val favRef = database.child("favorites").child(userId)

        favRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()
                for (child in snapshot.children) {
                    val trip = child.getValue(Trip::class.java)
                    if (trip != null) {
                        trip.isFavorite = true
                        tripList.add(trip)
                    }
                }
                tripAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FavoriteActivity, "Failed to load favorites", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun setupBottomNav() {
        findViewById<ImageView>(R.id.homeIcon).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.heartIcon).setOnClickListener {
            Toast.makeText(this, "You're already on Favorites screen", Toast.LENGTH_SHORT).show()
        }

        ///findViewById<ImageView>(R.id.clockIcon).setOnClickListener {
            //Toast.makeText(this, "Future: calendar screen", Toast.LENGTH_SHORT).show()
        //}

        findViewById<ImageView>(R.id.userIcon).setOnClickListener {
            showBottomUserPopup()
        }
    }

    private fun showBottomUserPopup() {
        val popup = PopupMenu(this, findViewById(R.id.userIcon))
        popup.menuInflater.inflate(R.menu.bottom_user_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_add_trip -> {
                    startActivity(Intent(this, AddTripActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

}
