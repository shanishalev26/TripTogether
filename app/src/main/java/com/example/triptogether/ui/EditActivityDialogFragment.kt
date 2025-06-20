package com.example.triptogether.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.triptogether.R
import com.example.triptogether.model.ActivityItem
import com.google.firebase.database.FirebaseDatabase

class EditActivityDialogFragment(
    private val tripId: String,
    private val createdBy: String,
    private val dayIndex: Int,
    private val activityKey: String,
    private val currentActivity: ActivityItem,
    private val onUpdated: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_activity, null)
        dialog.setContentView(view)

        val etTime = view.findViewById<EditText>(R.id.etTime)
        val etEmoji = view.findViewById<EditText>(R.id.etEmoji)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val btnEnter = view.findViewById<Button>(R.id.btnEnter)

        etTime.setText(currentActivity.time)
        etEmoji.setText(currentActivity.emoji)
        etDescription.setText(currentActivity.description)

        btnEnter.setOnClickListener {
            val newTime = etTime.text.toString().trim()
            val newEmoji = etEmoji.text.toString().trim()
            val newDescription = etDescription.text.toString().trim()

            if (newTime.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedActivity = ActivityItem(newTime, newEmoji, newDescription)
            val dayKey = "day$dayIndex"

            val ref = FirebaseDatabase.getInstance()
                .getReference("trips")
                .child(createdBy)
                .child(tripId)
                .child("plan")
                .child(dayKey)
                .child(activityKey)


            ref.setValue(updatedActivity).addOnSuccessListener {
                Toast.makeText(requireContext(), "Activity updated", Toast.LENGTH_SHORT).show()
                onUpdated()
                dismiss()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }
}
