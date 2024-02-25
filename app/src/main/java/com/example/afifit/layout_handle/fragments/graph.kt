package com.example.afifit.layout_handle.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.afifit.databinding.FragmentGraphBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import data.HealthData


class graph : Fragment() {



    private var _binding: FragmentGraphBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseReference: DatabaseReference




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseReference = FirebaseDatabase.getInstance().reference.child("healthData")


        // Attach a listener to read the data
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val healthDataList = mutableListOf<HealthData>()

                for (dataSnapshotChild in dataSnapshot.children) {
                    val healthData = dataSnapshotChild.getValue(HealthData::class.java)
                    healthData?.let {
                        healthDataList.add(it)
                    }
                }

                // Update line chart with retrieved health data
                updateLineChart(healthDataList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
            }
        })

    }
    private fun updateLineChart(healthDataList: List<HealthData>) {
        val lineChart: LineChart = binding.lineChart
        val entries: MutableList<Entry> = mutableListOf()

        for ((index, healthData) in healthDataList.withIndex()) {
            // Assuming you want to show the BPM in the graph
            entries.add(Entry(index.toFloat(), healthData.bpm.toFloat()))
        }

        val dataSet = LineDataSet(entries, "BPM Progress")
        val lineDataSets: MutableList<ILineDataSet> = mutableListOf()
        lineDataSets.add(dataSet)

        val lineData = LineData(lineDataSets)
        lineChart.data = lineData

        // Customize the line chart appearance
        lineChart.description = Description().apply { text = "BPM Progress Over Time" }
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        // Refresh the chart
        lineChart.invalidate()
    }


}