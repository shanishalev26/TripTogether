package com.example.triptogether

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triptogether.adapters.ActivityAdapter
import com.example.triptogether.model.ActivityItem
import com.example.triptogether.model.Trip
import com.example.triptogether.ui.AddActivityDialogFragment
import com.example.triptogether.ui.DeleteActivityDialogFragment
import com.example.triptogether.ui.EditActivityDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TripPlanActivity : AppCompatActivity() {

    private lateinit var trip: Trip
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var tvTripTitle: TextView
    private lateinit var rvActivities: RecyclerView
    private lateinit var btnAddActivity: Button
    private lateinit var btnChange: Button
    private lateinit var btnBack: ImageView
    private lateinit var layoutDays: LinearLayout
    private lateinit var btnPrevDays: ImageView
    private lateinit var btnNextDays: ImageView

    private lateinit var dayList: List<String>
    private var selectedDayIndex = 0
    private var currentDayWindowStart = 0
    private lateinit var activityAdapter: ActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_plan)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("trips")
        trip = intent.getSerializableExtra("trip") as Trip

        tvTripTitle = findViewById(R.id.tvTripTitle)
        rvActivities = findViewById(R.id.rvActivities)
        btnAddActivity = findViewById(R.id.btnAddActivity)
        btnChange = findViewById(R.id.btnChange)
        btnBack = findViewById(R.id.btnBack)
        layoutDays = findViewById(R.id.dayTabsContainer)
        btnPrevDays = findViewById(R.id.btnPrevDays)
        btnNextDays = findViewById(R.id.btnNextDays)

        tvTripTitle.text = trip.name
        dayList = generateDateRange(trip.startDate, trip.endDate)

        setupDayTabs()

        activityAdapter = ActivityAdapter { activity, key ->
            showEditDeleteOptions(activity, key)
        }

        rvActivities.layoutManager = LinearLayoutManager(this)
        rvActivities.adapter = activityAdapter

        loadActivitiesForDay(selectedDayIndex)

        btnAddActivity.setOnClickListener {
            AddActivityDialogFragment(
                tripId = trip.id,
                createdBy = trip.createdBy,
                dayIndex = selectedDayIndex,
                onActivityAdded = {
                    loadActivitiesForDay(selectedDayIndex)
                }
            ).show(supportFragmentManager, "AddActivityDialog")
        }

        btnChange.setOnClickListener {
            val options = arrayOf("Edit existing activity", "Delete existing activity")
            AlertDialog.Builder(this)
                .setTitle("What would you like to do?")
                .setItems(options) { _, which ->
                    if (which == 0) promptEditActivity() else promptDeleteActivity()
                }
                .show()
        }

        btnBack.setOnClickListener { finish() }

        btnPrevDays.setOnClickListener {
            if (currentDayWindowStart >= 3) {
                currentDayWindowStart -= 3
                setupDayTabs()
            }
        }

        btnNextDays.setOnClickListener {
            if (currentDayWindowStart + 3 < dayList.size) {
                currentDayWindowStart += 3
                setupDayTabs()
            }
        }
    }

    private fun setupDayTabs() {
        layoutDays.removeAllViews()

        val inflater = layoutInflater
        val end = minOf(currentDayWindowStart + 3, dayList.size)

        for (i in currentDayWindowStart until end) {
            val view = inflater.inflate(R.layout.item_day, layoutDays, false)
            val tvDayNumber = view.findViewById<TextView>(R.id.tvDayNumber)
            val tvDayDate = view.findViewById<TextView>(R.id.tvDayDate)

            tvDayNumber.text = "Day ${i + 1}"
            tvDayDate.text = formatDate(dayList[i])

            view.setBackgroundResource(
                if (i == selectedDayIndex)
                    R.drawable.bg_day_selected
                else
                    R.drawable.bg_day_unselected
            )

            view.setOnClickListener {
                selectedDayIndex = i
                setupDayTabs()
                loadActivitiesForDay(i)
            }

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutDays.addView(view, params)
        }

        btnPrevDays.visibility = if (currentDayWindowStart > 0) View.VISIBLE else View.INVISIBLE
        btnNextDays.visibility = if (end < dayList.size) View.VISIBLE else View.INVISIBLE
    }

    private fun loadActivitiesForDay(dayIndex: Int) {
        val dayKey = "day$dayIndex"
        val ref = database.child(trip.createdBy).child(trip.id).child("plan").child(dayKey)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activities = mutableListOf<ActivityItem>()
                val keys = mutableListOf<String>()

                for (child in snapshot.children) {
                    val item = child.getValue(ActivityItem::class.java)
                    val key = child.key
                    if (item != null && key != null) {
                        activities.add(item)
                        keys.add(key)
                    }
                }

                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val sorted = activities.zip(keys).sortedBy { (a, _) -> formatter.parse(a.time) }
                val sortedActivities = sorted.map { it.first }
                val sortedKeys = sorted.map { it.second }

                activityAdapter.updateData(sortedActivities, sortedKeys)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun promptEditActivity() {
        val items = activityAdapter.getCurrentItems()
        if (items.isEmpty()) {
            showToast("No activities to edit.")
            return
        }
        val options = items.map { "${it.first.time} - ${it.first.description}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose activity to edit:")
            .setItems(options) { _, index ->
                showEditActivityDialog(items[index].first, items[index].second)
            }.show()
    }

    private fun promptDeleteActivity() {
        val items = activityAdapter.getCurrentItems()
        if (items.isEmpty()) {
            showToast("No activities to delete.")
            return
        }
        val options = items.map { "${it.first.time} - ${it.first.description}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose activity to delete:")
            .setItems(options) { _, index ->
                showDeleteActivityDialog(items[index].second)
            }.show()
    }

    private fun showEditActivityDialog(activity: ActivityItem, key: String) {
        EditActivityDialogFragment(
            tripId = trip.id,
            createdBy = trip.createdBy,
            dayIndex = selectedDayIndex,
            activityKey = key,
            currentActivity = activity,
            onUpdated = {
                loadActivitiesForDay(selectedDayIndex)
            }
        ).show(supportFragmentManager, "EditActivityDialog")
    }

    private fun showDeleteActivityDialog(key: String) {
        DeleteActivityDialogFragment(
            tripId = trip.id,
            createdBy = trip.createdBy,
            dayIndex = selectedDayIndex,
            activityKey = key,
            onDeleted = {
                showToast("Activity deleted successfully.")
                loadActivitiesForDay(selectedDayIndex)
            }
        ).show(supportFragmentManager, "DeleteActivityDialog")
    }

    private fun generateDateRange(startDate: String, endDate: String): List<String> {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val start = sdf.parse(startDate) ?: return emptyList()
        val end = sdf.parse(endDate) ?: return emptyList()

        val dates = mutableListOf<String>()
        val cal = Calendar.getInstance().apply { time = start }

        while (!cal.time.after(end)) {
            dates.add(sdf.format(cal.time))
            cal.add(Calendar.DATE, 1)
        }
        return dates
    }

    private fun formatDate(date: String): String {
        val input = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM", Locale.getDefault())
        return try {
            output.format(input.parse(date) ?: return date)
        } catch (e: Exception) {
            date
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showEditDeleteOptions(activity: ActivityItem, key: String) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Choose action")
            .setItems(options) { _, which ->
                if (which == 0) showEditActivityDialog(activity, key)
                else showDeleteActivityDialog(key)
            }.show()
    }
}
