package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.triptogether.adapters.TripAdapter
import com.example.triptogether.databinding.ActivityMainBinding
import com.example.triptogether.model.Trip
import com.example.triptogether.ui.MainScreenUiHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private var currentTab: String = "upcoming"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadUserInfo()
    }

    private fun initViews() {
        MainScreenUiHelper.styleSelectedTab(this, binding.btnUpcoming, binding.btnPast)

        binding.btnUpcoming.setOnClickListener {
            if (currentTab != "upcoming") {
                loadUpcomingTrips()
                MainScreenUiHelper.styleSelectedTab(this, binding.btnUpcoming, binding.btnPast)
                currentTab = "upcoming"
            }
        }


        binding.btnPast.setOnClickListener {
            if (currentTab != "past") {
                loadPastTrips()
                MainScreenUiHelper.styleSelectedTab(this, binding.btnPast, binding.btnUpcoming)
                currentTab = "past"
            }
        }


        binding.homeIcon.setOnClickListener {
            if (currentTab != "upcoming") {
                loadUpcomingTrips()
                MainScreenUiHelper.styleSelectedTab(this, binding.btnUpcoming, binding.btnPast)
                currentTab = "upcoming"
            }
        }


        binding.heartIcon.setOnClickListener {
            startActivity(Intent(this, FavoriteActivity::class.java))
        }

        binding.userIconTop.setOnClickListener {
            showTopUserPopup()
        }

        binding.userIconBottom.setOnClickListener {
            showBottomUserPopup()
        }

        binding.searchIcon.setOnClickListener {
            val tripName = binding.searchPlaces.text.toString().trim()
            if (tripName.isNotEmpty()) {
                fetchDestinationsForTrip(tripName)
            }
        }
    }

    private fun loadUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)

                binding.greetingText.text = buildString {
        append("Hi ")
        append(firstName)
    }

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.sample_user_icon)
                        .circleCrop()
                        .into(binding.userIconTop)
                } else {
                    binding.userIconTop.setImageResource(R.drawable.sample_user_icon)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Failed to load user info", error.toException())
            }
        })
    }

    private fun loadUpcomingTrips() {
        loadTrips(filterPast = false)
    }

    private fun loadPastTrips() {
        loadTrips(filterPast = true)
    }

    private fun loadTrips(filterPast: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        val userTripsRef = database.child("userTrips").child(currentUid)
        val favRef = database.child("favorites").child(currentUid)

        favRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(favSnapshot: DataSnapshot) {
                val favoriteIds = favSnapshot.children.mapNotNull { it.key }.toSet()

                userTripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tripIds = snapshot.children.mapNotNull { it.key }

                        if (tripIds.isEmpty()) {
                            updateRecyclerView(emptyList())
                            return
                        }

                        val tripsRef = database.child("trips")
                        val tripList = mutableListOf<Trip>()

                        tripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(tripSnapshot: DataSnapshot) {
                                for (creatorSnapshot in tripSnapshot.children) {
                                    for (tripEntry in creatorSnapshot.children) {
                                        val trip = tripEntry.getValue(Trip::class.java)
                                        val tripId = tripEntry.key
                                        if (trip != null && tripId != null && tripIds.contains(tripId)) {
                                            val isFav = favoriteIds.contains(tripId)
                                            val shouldAdd = if (filterPast) isPast(trip.endDate) else !isPast(trip.endDate)
                                            if (shouldAdd) {
                                                tripList.add(trip.copy(id = tripId, isFavorite = isFav))
                                            }
                                        }
                                    }
                                }

                                updateRecyclerView(tripList)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Firebase", "Failed to read trips", error.toException())
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Failed to read userTrips", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read favorites", error.toException())
            }
        })
    }

    private fun updateRecyclerView(tripList: List<Trip>) {
        val sortedTrips = tripList.sortedBy {
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.startDate)
            } catch (e: Exception) {
                null
            }
        }

        val adapter = TripAdapter(sortedTrips)
        binding.tripRecycler.adapter = adapter
        binding.tripRecycler.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }


    private fun isPast(endDate: String): Boolean {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = format.parse(endDate)
            date != null && date.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchDestinationsForTrip(query: String) {
        val uid = auth.currentUser?.uid ?: return
        val tripsRef = database.child("trips")
        val userTripsRef = database.child("userTrips").child(uid)

        val normalizedQuery = normalizeText(query)

        userTripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tripIds = snapshot.children.mapNotNull { it.key }
                val foundTrips = mutableListOf<Trip>()

                tripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(tripSnapshot: DataSnapshot) {
                        for (creatorSnapshot in tripSnapshot.children) {
                            for (tripEntry in creatorSnapshot.children) {
                                val trip = tripEntry.getValue(Trip::class.java)
                                val tripId = tripEntry.key

                                if (trip != null && tripId != null && tripIds.contains(tripId)) {
                                    val countryNorm = normalizeText(trip.country)
                                    val nameNorm = normalizeText(trip.name)

                                    if (countryNorm.contains(normalizedQuery) || nameNorm.contains(normalizedQuery)) {
                                        foundTrips.add(trip.copy(id = tripId))
                                    }
                                }
                            }
                        }

                        when {
                            foundTrips.isEmpty() -> {
                                Toast.makeText(this@MainActivity, "No matching trips found", Toast.LENGTH_SHORT).show()
                            }
                            foundTrips.size == 1 -> {
                                val intent = Intent(this@MainActivity, TripSummaryActivity::class.java)
                                intent.putExtra("trip", foundTrips.first())
                                startActivity(intent)
                            }
                            else -> {
                                val intent = Intent(this@MainActivity, SearchResultsActivity::class.java)
                                intent.putExtra("trips", ArrayList(foundTrips))
                                startActivity(intent)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, "Error searching trips", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }




    private fun showTopUserPopup() {
        val popup = PopupMenu(this, binding.userIconTop)
        popup.menuInflater.inflate(R.menu.user_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit_profile -> {
                    startActivity(Intent(this, EditProfileActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showBottomUserPopup() {
        val popup = PopupMenu(this, binding.userIconBottom)
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

    override fun onResume() {
        super.onResume()
        currentTab = "upcoming"
        loadUpcomingTrips()
        MainScreenUiHelper.styleSelectedTab(this, binding.btnUpcoming, binding.btnPast)
    }

    private fun normalizeText(text: String): String {
        return text
            .lowercase(Locale.getDefault())
            .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "")
            .trim()
    }


}
