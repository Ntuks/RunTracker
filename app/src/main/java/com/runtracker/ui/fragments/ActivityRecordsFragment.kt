package com.runtracker.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.runtracker.R
import com.runtracker.adapters.ActivityAdapter
import com.runtracker.services.TrackingService
import kotlinx.android.synthetic.main.fragment_activity_records.*


class ActivityRecordsFragment : Fragment(R.layout.fragment_activity_records) {

    private lateinit var activityAdapter: ActivityAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        fabBack.setOnClickListener{
            findNavController().navigate(R.id.action_activityRecordsFragment_to_trackingFragment)
        }

        TrackingService.activities.observe(viewLifecycleOwner, Observer {
            activityAdapter.submitList(it)
        })
    }

    private fun setUpRecyclerView() = rvActivityRecords.apply {
        activityAdapter = ActivityAdapter()
        adapter = activityAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }


}