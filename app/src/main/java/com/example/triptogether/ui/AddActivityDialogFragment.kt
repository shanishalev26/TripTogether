package com.example.triptogether.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.triptogether.R
import com.example.triptogether.model.ActivityItem
import com.google.firebase.database.FirebaseDatabase

class AddActivityDialogFragment(
    private val tripId: String,
    private val createdBy: String,
    private val dayIndex: Int,
    private val onActivityAdded: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_activity, null)
        val etTime = view.findViewById<EditText>(R.id.etTime)
        val etEmoji = view.findViewById<EditText>(R.id.etEmoji)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Add Activity")
            .setCancelable(true)
            .create().also { dialog ->
                view.findViewById<Button>(R.id.btnEnter).setOnClickListener {
                    val time = etTime.text.toString().trim()
                    val emoji = etEmoji.text.toString().trim()
                    val description = etDescription.text.toString().trim()

                    if (time.isEmpty() || emoji.isEmpty() || description.isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (!isValidTimeFormat(time)) {
                        Toast.makeText(requireContext(), "Please enter a valid time (HH:mm)", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val activity = ActivityItem(time, emoji, description)
                    val dayKey = "day$dayIndex"
                    val tripPlanRef = FirebaseDatabase.getInstance().getReference("trips")
                        .child(createdBy)
                        .child(tripId)
                        .child("plan")
                        .child(dayKey)

                    tripPlanRef.push().setValue(activity).addOnSuccessListener {
                        onActivityAdded()
                        dismiss()
                    }
                }
            }
    }

    private fun isValidTimeFormat(input: String): Boolean {
        val regex = Regex("^([01]?\\d|2[0-3]):[0-5]\\d$")
        return input.matches(regex)
    }
}
