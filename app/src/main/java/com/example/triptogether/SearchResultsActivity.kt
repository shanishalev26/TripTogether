package com.example.triptogether

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triptogether.adapters.TripAdapter
import com.example.triptogether.databinding.ActivitySearchResultsBinding
import com.example.triptogether.model.Trip

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val trips = intent.getSerializableExtra("trips") as? ArrayList<Trip> ?: return

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = TripAdapter(trips)

        binding.btnBackSearch.setOnClickListener {
            finish()
        }
    }
}
