package com.example.afifit.layout_handle.fragments

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
                    showWifiNotification()
                }
            }
        }
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

        // Check Wi-Fi connection initially
        isWifiConnected = checkWifiStatus()

        // Register a BroadcastReceiver for connectivity changes
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(connectivityReceiver, filter)

        if (!isWifiConnected) {
            showWifiNotification()
        } else {
            // Additional logic when Wi-Fi is connected
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
        // Unregister the BroadcastReceiver
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

    private fun showWifiNotification() {
        val channelId = "WifiNotificationChannel"
        val notificationId = 1

        // Create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Wifi Notification Channel"
            val descriptionText = "Notify when Wi-Fi is not connected"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.logo) // Replace with your notification icon
            .setContentTitle("No Network Connection")
            .setContentText("Please connect to Wi-Fi or cellular data")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireContext())) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            notify(notificationId, builder.build())
        }
    }

    private fun updateUI(userProfile: UserProfile) {
        binding?.UserName?.text = userProfile.name

        Glide.with(requireContext())
            .load(userProfile.imageUrl)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding?.profileImage!!)
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

                            bpmTextView.text = bpmText
                            avgBpmTextView?.text = avgBpmText
                            bloodOxygenTextView?.text = bloodOxygenText

                            val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(timestamp ?: 0L)
                            binding?.time?.text = "Time:$formattedTime"
                        } else {
                            bpmTextView.text = "No data available"
                            avgBpmTextView?.text = "No data available"
                            bloodOxygenTextView?.text = "No data available"
                            binding?.time?.text = "Last Update: N/A"
                        }
                    } else {
                        bpmTextView.text = "No data available"
                        avgBpmTextView?.text = "No data available"
                        binding?.time?.text = "Last Update: N/A"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    bpmTextView.text = "Failed to read data: ${error.message}"
                    avgBpmTextView?.text = "Failed to read data: ${error.message}"
                    bloodOxygenTextView?.text = "Failed to read data: ${error.message}"
                }
            })
    }
}
