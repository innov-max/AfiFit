package com.example.afifit.layout_handle.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.afifit.R
import com.example.afifit.databinding.FragmentDashFrag1Binding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import data.HealthData
import data.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class dash_frag1 : Fragment() {

    private var binding: FragmentDashFrag1Binding? = null
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use View Binding to inflate the layout
        binding = FragmentDashFrag1Binding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayGreeting()
        databaseReference = FirebaseDatabase.getInstance().reference.child("userProfiles")
        databaseReference = FirebaseDatabase.getInstance().reference.child("healthData")

        binding?.profileImage?.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()

            // You can add the transaction to the back stack if you want to allow users to navigate back
            transaction.addToBackStack(null)
            transaction.replace(R.id.mainfragContaier, profile())
            transaction.commit()
        }

        //retrieving image from database

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //userProfile
                val userProfile = dataSnapshot.children.firstOrNull()?.getValue(UserProfile::class.java)
                userProfile?.let { updateUI(it) }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //userProfile
                val healthData = dataSnapshot.getValue(HealthData::class.java)
                healthData?.let {
                    // Update UI with retrieved health data
                    updateHealthUI(it)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })
    }
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun displayGreeting() {
        val currentTime = Calendar.getInstance().time
        val formattedTime = SimpleDateFormat("HH", Locale.getDefault()).format(currentTime).toInt()

        val greeting = when (formattedTime) {
            in 6..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..23, in 0..5 -> "Good Evening"
            else -> "Hello"
        }

        binding?.greeting?.text = greeting
    }
    private fun updateUI(userProfile: UserProfile) {
        // Update other UI elements with data (e.g., name, occupation, about)
        binding?.UserName?.text = userProfile.name

        // Load the image using Glide and force it to load a new image
        binding?.let {


            Glide.with(requireContext())
                .load(userProfile.imageUrl)
                .skipMemoryCache(true)  // Skip memory cache
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // Skip disk cache
                .into(it.profileImage)
        }
    }

    private fun updateHealthUI(healthData: HealthData) {
        binding?.bpm?.text = healthData.bpm.toString()
        binding?.TempValue?.text = "${healthData.temperature} °C"
        binding?.OxyValue?.text = "${healthData.oxygenLevel}%"
        binding?.BreathValue?.text = "${healthData.breathingRate} breaths/min"
    }
}











