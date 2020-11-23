package com.runtracker.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.OnMapReadyCallback
import com.runtracker.R
import com.runtracker.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment: Fragment(R.layout.fragment_tracking) , OnMapReadyCallback {

    private val viewModel: MainViewModel by viewModels()

    private var huaweiMap: HuaweiMap? = null
    private var huaweiMapView: MapView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        huaweiMapView = view.findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle("MapViewBundleKey")
        }
        huaweiMapView?.onCreate(mapViewBundle)
        huaweiMapView?.getMapAsync(this)
    }

    override fun onMapReady(hMap: HuaweiMap?) {
            huaweiMap = hMap
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
