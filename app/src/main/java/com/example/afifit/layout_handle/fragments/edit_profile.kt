package com.example.afifit.layout_handle.fragments
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.afifit.R
import com.example.afifit.databinding.FragmentEditProfileBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class edit_profile : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageView: ImageView
    private lateinit var editTextName: EditText
    private lateinit var editTextOccupation: EditText
    private lateinit var editTextAbout: EditText

    private lateinit var databaseReference: DatabaseReference
    private lateinit var buttonSelectImage: TextView
    private lateinit var buttonPushData: Button

    private companion object {
        const val REQUEST_IMAGE_PICK = 1
        const val NOTIFICATION_CHANNEL_ID = "channel_id"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        databaseReference = FirebaseDatabase.getInstance().reference.child("userProfiles")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = binding.profileImage
        editTextName = binding.nameEditText
        editTextOccupation = binding.occupation
        editTextAbout = binding.EditAbout

        buttonSelectImage = binding.EditProfile
        buttonPushData = binding.btnUpdate

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        buttonPushData.setOnClickListener {
            if (!isNetworkAvailable()) {
                Toast.makeText(context, "No network available. Please check your connection.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = editTextName.text.toString()
            val occupation = editTextOccupation.text.toString()
            val about = editTextAbout.text.toString()

            if (imageView.drawable == null) {
                Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            } else if (TextUtils.isEmpty(name) || TextUtils.isEmpty(occupation) || TextUtils.isEmpty(about)) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Use lifecycleScope to launch a coroutine tied to the fragment's lifecycle
                lifecycleScope.launch {
                    // Get the image URL from the selected image
                    val imageUrl = getImageUrlFromUri(imageView.tag as Uri)

                    val userProfile = UserProfile(name, occupation, about, imageUrl)

                    // Use a background thread to perform the database transaction
                    withContext(Dispatchers.IO) {
                        pushDataToFirebase(userProfile)
                        showNotification("Profile Update", "Your profile has successfully updated")
                    }

                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.profileBack.setOnClickListener {
            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun pushDataToFirebase(userProfile: UserProfile) {
        // Push the data to Firebase
        val userId = databaseReference.push().key
        if (userId != null) {
            databaseReference.child(userId).setValue(userProfile)
        }
        else{
            showNotification("Profile Update", "an error occured check your connection")

        }
    }

    private fun getImageUrlFromUri(imageUri: Uri): String {
        // Convert the image URI to URL (this is a simple example, you may need to handle this differently)
        return imageUri.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            // Handle the selected image
            val selectedImageUri = data.data
            // Load the selected image into the ImageView
            imageView.setImageURI(selectedImageUri)
            // Store the selected image URI in the tag for later use
            imageView.tag = selectedImageUri
        }
    }

    // notification handling
    private fun showNotification(title: String, message: String) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Profile",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Update profile"
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        // Create a notification builder
        val builder = NotificationCompat.Builder(requireContext(), NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Build the notification
        val notification = builder.build()

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Use a Handler to delay the removal of the notification after 10 seconds
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            notificationManager.cancel(NOTIFICATION_ID)
        }, 10000) // 10000 milliseconds (10 seconds)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
