package com.example.afifit.layout_handle.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
    private lateinit var databaseReferenceUser: DatabaseReference
    private lateinit var databaseReferenceHealth: DatabaseReference
    private var bpmTextView: TextView? = null
    private var avgBpmTextView: TextView? = null
    private var bloodOxygenTextView: TextView? = null
    private var isWifiConnected: Boolean = false

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                isWifiConnected = checkWifiStatus()

                if (!isWifiConnected) {
                    showNotification("No Connection", "Connect to wifi or cellular data")
                } else {
                    showNotification("Yaay", "We are back online")
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private companion object {
        private const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashFrag1Binding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDashFrag1Binding.bind(view)

        bpmTextView = binding?.bpm
        avgBpmTextView = binding?.brr
        bloodOxygenTextView = binding?.OxyValue

        displayGreeting()
        databaseReferenceUser = FirebaseDatabase.getInstance().reference.child("userProfiles")
        databaseReferenceHealth = FirebaseDatabase.getInstance().reference.child("healthData")

        binding?.profileImage?.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.addToBackStack(null)
            transaction.replace(R.id.mainfragContaier, profile())
            transaction.commit()
        }

        isWifiConnected = checkWifiStatus()

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(connectivityReceiver, filter)

        // Check for READ_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
        } else {
            // Permission is already granted, proceed with image loading
            loadImageWithGlide()
        }

        if (!isWifiConnected) {
            showNotification("No Connection", "Connect to wifi or cellular data")
        }

        databaseReferenceUser.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userProfile = dataSnapshot.children.firstOrNull()?.getValue(UserProfile::class.java)
                userProfile?.let { updateUI(it) }

                readDataFromFirebaseAndDisplay(bpmTextView!!, avgBpmTextView, bloodOxygenTextView)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })

        databaseReferenceHealth.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val healthData = dataSnapshot.getValue(HealthData::class.java)
                healthData?.let {
                    // Additional logic for health data
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(connectivityReceiver)
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

    private fun checkWifiStatus(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(connectivityManager.activeNetwork)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager?.activeNetworkInfo
            return activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                edit_profile.NOTIFICATION_CHANNEL_ID,
                "Wifi connection",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Internet Connection"
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        // Create a notification builder
        val builder = NotificationCompat.Builder(requireContext(),
            edit_profile.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Build the notification
        val notification = builder.build()

        // Show the notification
        notificationManager.notify(edit_profile.NOTIFICATION_ID, notification)

        // Use a Handler to delay the removal of the notification after 10 seconds
        handler.postDelayed({
            notificationManager.cancel(edit_profile.NOTIFICATION_ID)
        }, 10000) // 10000 milliseconds (10 seconds)
    }

    private fun updateUI(userProfile: UserProfile) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("userProfiles")

        binding?.UserName?.text = userProfile.name
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userProfile = dataSnapshot.getValue(UserProfile::class.java)

                // Check if userProfile is not null and has a valid imageUrl
                if (userProfile != null && userProfile.imageUrl.isNotEmpty()) {
                    activity?.runOnUiThread {
                        binding?.let {
                                it1 ->
                            Glide.with(it1.profileImage)
                                .load(userProfile.imageUrl)
                                .into(it1.profileImage)
                        }

                    }
                } else {
                    // Handle the case where the image URL is not available
                    // You can set a default image or handle it in another way
                    // For example:
                    binding?.let {
                        Glide.with(requireContext())
                            .load(R.drawable.anne) // Replace with your default image resource
                            .into(it.profileImage)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })
    }


    private fun loadImageWithGlide() {
        // Code to load the image using Glide
        // Use this code where you are currently using Glide in your fragment
        // For example, in the updateUI method
        // ...

        // Example:
        // Glide.with(requireContext())
        //     .load(userProfile.imageUrl)
        //     .into(binding?.profileImage!!)
    }

    private fun readDataFromFirebaseAndDisplay(
        bpmTextView: TextView,
        avgBpmTextView: TextView?,
        bloodOxygenTextView: TextView?
    ) {
        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("/bpm")

        databaseReference.orderByChild("timestamp").limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val latestUserId = dataSnapshot.children.lastOrNull()?.key

                        if (latestUserId != null) {
                            val latestUserData = dataSnapshot.child(latestUserId)
                            val bpm = latestUserData.child("bpm").getValue(Float::class.java)
                            val avgBpm = latestUserData.child("avgBpm").getValue(Int::class.java)
                            val bloodOxygen = latestUserData.child("bloodOxygen").getValue(Int::class.java)
                            val timestamp = latestUserData.child("timestamp").getValue(Long::class.java)

                            val bpmText = "BPM: ${bpm ?: 0.0f}"
                            val avgBpmText = "${avgBpm ?: 0} Per Min"
                            val bloodOxygenText = " ${bloodOxygen ?: 0}%"

                            val formattedTime =
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(timestamp ?: 0L)

                            handler.post {
                                bpmTextView.text = bpmText
                                avgBpmTextView?.text = avgBpmText
                                bloodOxygenTextView?.text = bloodOxygenText
                                binding?.time?.text = "Time:$formattedTime"
                            }
                        } else {
                            handler.post {
                                bpmTextView.text = "No data available"
                                avgBpmTextView?.text = "No data available"
                                bloodOxygenTextView?.text = "No data available"
                                binding?.time?.text = "Last Update: N/A"
                            }
                        }
                    } else {
                        handler.post {
                            bpmTextView.text = "No data available"
                            avgBpmTextView?.text = "No data available"
                            binding?.time?.text = "Last Update: N/A"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    handler.post {
                        bpmTextView.text = "Failed to read data: ${error.message}"
                        avgBpmTextView?.text = "Failed to read data: ${error.message}"
                        bloodOxygenTextView?.text = "Failed to read data: ${error.message}"
                    }
                }
            })
    }
}
