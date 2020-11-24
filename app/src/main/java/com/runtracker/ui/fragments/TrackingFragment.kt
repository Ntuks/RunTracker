package com.runtracker.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.PolylineOptions
import com.runtracker.R
import com.runtracker.services.Polyline
import com.runtracker.services.TrackingService
import com.runtracker.ui.viewmodels.MainViewModel
import com.runtracker.utils.Constants.ACTION_PAUSE_SERVICE
import com.runtracker.utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.runtracker.utils.Constants.MAP_ZOOM
import com.runtracker.utils.Constants.POLYLINE_COLOR
import com.runtracker.utils.Constants.POLYLINE_WIDTH
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*


@AndroidEntryPoint
class TrackingFragment: Fragment(R.layout.fragment_tracking) , OnMapReadyCallback {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var huaweiMap: HuaweiMap? = null
    private var huaweiMapView: MapView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnToggleRun.setOnClickListener{
            toggleRun()
        }

        huaweiMapView = view.findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle("MapViewBundleKey")
        }
        huaweiMapView?.onCreate(mapViewBundle)
        huaweiMapView?.getMapAsync{
            huaweiMap = it
            addAllPolylines()
        }
        subscribeToObservers()
    }

    override fun onMapReady(hMap: HuaweiMap?) {
            huaweiMap = hMap
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            addMarker()
            moveCameraToUser()
        })
    }

    private fun toggleRun() {
        if (isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            btnToggleRun.text = "Start"
            btnFinishRun.visibility = View.VISIBLE
        } else {
            btnToggleRun.text = "Stop"
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            huaweiMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private var mMarker: Marker? = null

    private fun addMarker() {
        if (null != mMarker) {
            mMarker!!.remove()
        }
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            val options = MarkerOptions()
                .position(
                    LatLng(
                        pathPoints.last().last().latitude,
                        pathPoints.last().last().longitude
                    )
                )
                .title("Hello Huawei Map")
                .snippet("This is a snippet!")
            mMarker = huaweiMap?.addMarker(options)
        }
    }

    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            huaweiMap?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val prelastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(prelastLatLng)
                .add(lastLatLng)
            huaweiMap?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle: Bundle? = outState.getBundle("MapViewBundleKey")
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle("MapViewBundleKey", mapViewBundle)
        }

        huaweiMapView?.onSaveInstanceState(mapViewBundle)
    }

    override fun onStart() {
        super.onStart()
        huaweiMapView?.onStart()
    }

    override fun onPause() {
        super.onPause()
        huaweiMapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        huaweiMapView?.onResume()
    }

    override fun onStop() {
        super.onStop()
        huaweiMapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        huaweiMapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        huaweiMapView?.onLowMemory()
    }
}
