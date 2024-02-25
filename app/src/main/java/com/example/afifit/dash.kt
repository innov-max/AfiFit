package com.example.afifit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.example.afifit.databinding.ActivityDashBinding
import com.example.afifit.layout_handle.fragments.drugs
import com.example.afifit.layout_handle.fragments.exercise
import com.example.afifit.layout_handle.fragments.nutrition

import kotlinx.coroutines.DelicateCoroutinesApi

class dash : AppCompatActivity() {

    private lateinit var binding: ActivityDashBinding
    val drugs = drugs()
    val nutrition = nutrition()
    val exercise = exercise()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
                    true
                }
                R.id.navigation_nutrition -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mainfragContaier, nutrition)
                        .commit()
                    true
                }
                R.id.navigation_prescription -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mainfragContaier, drugs)
                        .commit()
                    true
                }
                R.id.navigation_exercise -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mainfragContaier, exercise)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
