package com.kotlin.distancetrackerapp.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.kotlin.distancetrackerapp.R
import com.kotlin.distancetrackerapp.databinding.FragmentMapsBinding
import com.kotlin.distancetrackerapp.model.Result
import com.kotlin.distancetrackerapp.service.TrackerService
import com.kotlin.distancetrackerapp.ui.maps.MapUtil.calculateElapsedTime
import com.kotlin.distancetrackerapp.ui.maps.MapUtil.calculateTheDistance
import com.kotlin.distancetrackerapp.ui.maps.MapUtil.setCameraPosition
import com.kotlin.distancetrackerapp.util.Constants.ACTION_SERVICE_START
import com.kotlin.distancetrackerapp.util.Constants.ACTION_SERVICE_STOP
import com.kotlin.distancetrackerapp.util.ExtensionFunctions.disable
import com.kotlin.distancetrackerapp.util.ExtensionFunctions.enabled
import com.kotlin.distancetrackerapp.util.ExtensionFunctions.hide
import com.kotlin.distancetrackerapp.util.ExtensionFunctions.show
import com.kotlin.distancetrackerapp.util.Permissions.hasBackgroundLocationPermission
import com.kotlin.distancetrackerapp.util.Permissions.requestBackgroundLocationPermission
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMarkerClickListener, EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val list = listOf(
        LatLng(-6.900396016397087, 107.68049116560313),
        LatLng(-6.900268202912309, 107.68154259152882),
        LatLng(-6.9010883387905775, 107.67946119735274),
        LatLng(-6.901652847076031, 107.6809203190432),
        LatLng(-6.901865868895579, 107.68093104787916),
        LatLng(-6.902014984112232, 107.67955775687638),
        LatLng(-6.901770009088656, 107.68071647115997),
        LatLng(-6.902004333026888, 107.6790213150784),
        LatLng(-6.902856419097122, 107.68117781110622),
        LatLng(-6.903367670032451, 107.68059845396472),
        LatLng(-6.90404933704765, 107.68008346983868),
        LatLng(-6.903889571429064, 107.67961140105645),
        LatLng(-6.904198451576324, 107.68115635343459),
        LatLng(-6.904858815352726, 107.67974014708797),
        LatLng(-6.904315612958742, 107.68105979391096),
        LatLng(-6.903463529515798, 107.67846341560882),
        LatLng(-6.90547657418012, 107.68024440237805),
        LatLng(-6.9018552178364425, 107.68188591427982),
        LatLng(-6.902643397752938, 107.67860289047627),
        LatLng(-6.9035274358272405, 107.68081303068391),
        LatLng(-6.901162896534937, 107.6802336735401),
        LatLng(-6.901077687692078, 107.68030877539182),
        LatLng(-6.903761758908072, 107.67845268676481),
        LatLng(-6.903506133737094, 107.67977233358782),
        LatLng(-6.905402017135775, 107.67991180845527),
        LatLng(-6.903005534043962, 107.67853851745248),
        LatLng(-6.903431576369555, 107.67977233358782),
        LatLng(-6.903815014134638, 107.6795899433765),
        LatLng(-6.904006732900677, 107.68084521718373),
        LatLng(-6.903825665179235, 107.67953629919671),
        LatLng(-6.9052529029864145, 107.67863507697612),
        LatLng(-6.903420925316092, 107.67951484152479),
        LatLng(-6.901226803157018, 107.6807379288302),
        LatLng(-6.9017700090963405, 107.68070574232232),
        LatLng(-6.901748706914378, 107.68016930052434),
        LatLng(-6.901876519991739, 107.68075938650212),
        LatLng(-6.902206703615154, 107.68013711401646),
        LatLng(-6.901983030863169, 107.6802336735401),
        LatLng(-6.902377120879056, 107.68050189443908),
        LatLng(-6.9000605058761515, 107.6804697079509),
        LatLng(-6.9025102593162, 107.68046970793816),
        LatLng(-6.904065313577077, 107.6798474354525),
        LatLng(-6.9051517183161275, 107.67892475556002),
        LatLng(-6.905300832497383, 107.67986889312442),
        LatLng(-6.903447552907137, 107.6819073719567),
        LatLng(-6.903234531799472, 107.67832394074631),
        LatLng(-6.905066510191451, 107.67628546191403),
        LatLng(-6.906046402698645, 107.68053408095392),
        LatLng(-6.902041611824648, 107.68113489576763)
    )

    //    private val list = listOf(
//        LatLng(-6.900396016397087, 107.68049116560313),
//        LatLng(-6.900268202912309, 107.68154259152882),
//        LatLng(-6.9010883387905775, 107.67946119735274),
//        LatLng(-6.901652847076031, 107.6809203190432),
//        LatLng(-6.901865868895579, 107.68093104787916),
//        LatLng(-6.902014984112232, 107.67955775687638),
//        LatLng(-6.901770009088656, 107.68071647115997),
//        LatLng(-6.902004333026888, 107.6790213150784),
//        LatLng(-6.902856419097122, 107.68117781110622),
//        LatLng(-6.903367670032451, 107.68059845396472),
//        LatLng(-6.90404933704765, 107.68008346983868),
//        LatLng(-6.903889571429064, 107.67961140105645),
//        LatLng(-6.904198451576324, 107.68115635343459),
//        LatLng(-6.904858815352726, 107.67974014708797),
//        LatLng(-6.904315612958742, 107.68105979391096),
//        LatLng(-6.903463529515798, 107.67846341560882),
//        LatLng(-6.90547657418012, 107.68024440237805),
//        LatLng(-6.9018552178364425, 107.68188591427982),
//        LatLng(-6.902643397752938, 107.67860289047627),
//        LatLng(-6.9035274358272405, 107.68081303068391),
//        LatLng(-6.901162896534937, 107.6802336735401),
//        LatLng(-6.901077687692078, 107.68030877539182),
//        LatLng(-6.903761758908072, 107.67845268676481),
//        LatLng(-6.903506133737094, 107.67977233358782),
//        LatLng(-6.905402017135775, 107.67991180845527),
//        LatLng(-6.903005534043962, 107.67853851745248),
//        LatLng(-6.903431576369555, 107.67977233358782),
//        LatLng(-6.903815014134638, 107.6795899433765),
//        LatLng(-6.904006732900677, 107.68084521718373),
//        LatLng(-6.903825665179235, 107.67953629919671),
//        LatLng(-6.9052529029864145, 107.67863507697612),
//        LatLng(-6.903420925316092, 107.67951484152479),
//        LatLng(-6.901226803157018, 107.6807379288302),
//        LatLng(-6.9017700090963405, 107.68070574232232),
//        LatLng(-6.901748706914378, 107.68016930052434),
//        LatLng(-6.901876519991739, 107.68075938650212),
//        LatLng(-6.902206703615154, 107.68013711401646),
//        LatLng(-6.901983030863169, 107.6802336735401),
//        LatLng(-6.902377120879056, 107.68050189443908),
//        LatLng(-6.9000605058761515, 107.6804697079509),
//        LatLng(-6.9025102593162, 107.68046970793816),
//        LatLng(-6.904065313577077, 107.6798474354525),
//        LatLng(-6.9051517183161275, 107.67892475556002),
//        LatLng(-6.905300832497383, 107.67986889312442),
//        LatLng(-6.903447552907137, 107.6819073719567),
//        LatLng(-6.903234531799472, 107.67832394074631),
//        LatLng(-6.905066510191451, 107.67628546191403),
//        LatLng(-6.906046402698645, 107.68053408095392),
//        LatLng(-6.902041611824648, 107.68113489576763)
//    )

    //    private val list = listOf(
//        LatLng(-6.900396016397087, 107.68049116560313),
//        LatLng(-6.900268202912309, 107.68154259152882),
//        LatLng(-6.9010883387905775, 107.67946119735274),
//        LatLng(-6.901652847076031, 107.6809203190432),
//        LatLng(-6.901865868895579, 107.68093104787916),
//        LatLng(-6.902014984112232, 107.67955775687638),
//        LatLng(-6.901770009088656, 107.68071647115997),
//        LatLng(-6.902004333026888, 107.6790213150784),
//        LatLng(-6.902856419097122, 107.68117781110622),
//        LatLng(-6.903367670032451, 107.68059845396472),
//        LatLng(-6.90404933704765, 107.68008346983868),
//        LatLng(-6.903889571429064, 107.67961140105645),
//        LatLng(-6.904198451576324, 107.68115635343459),
//        LatLng(-6.904858815352726, 107.67974014708797),
//        LatLng(-6.904315612958742, 107.68105979391096),
//        LatLng(-6.903463529515798, 107.67846341560882),
//        LatLng(-6.90547657418012, 107.68024440237805),
//        LatLng(-6.9018552178364425, 107.68188591427982),
//        LatLng(-6.902643397752938, 107.67860289047627),
//        LatLng(-6.9035274358272405, 107.68081303068391),
//        LatLng(-6.901162896534937, 107.6802336735401),
//        LatLng(-6.901077687692078, 107.68030877539182),
//        LatLng(-6.903761758908072, 107.67845268676481),
//        LatLng(-6.903506133737094, 107.67977233358782),
//        LatLng(-6.905402017135775, 107.67991180845527),
//        LatLng(-6.903005534043962, 107.67853851745248),
//        LatLng(-6.903431576369555, 107.67977233358782),
//        LatLng(-6.903815014134638, 107.6795899433765),
//        LatLng(-6.904006732900677, 107.68084521718373),
//        LatLng(-6.903825665179235, 107.67953629919671),
//        LatLng(-6.9052529029864145, 107.67863507697612),
//        LatLng(-6.903420925316092, 107.67951484152479),
//        LatLng(-6.901226803157018, 107.6807379288302),
//        LatLng(-6.9017700090963405, 107.68070574232232),
//        LatLng(-6.901748706914378, 107.68016930052434),
//        LatLng(-6.901876519991739, 107.68075938650212),
//        LatLng(-6.902206703615154, 107.68013711401646),
//        LatLng(-6.901983030863169, 107.6802336735401),
//        LatLng(-6.902377120879056, 107.68050189443908),
//        LatLng(-6.9000605058761515, 107.6804697079509),
//        LatLng(-6.9025102593162, 107.68046970793816),
//        LatLng(-6.904065313577077, 107.6798474354525),
//        LatLng(-6.9051517183161275, 107.67892475556002),
//        LatLng(-6.905300832497383, 107.67986889312442),
//        LatLng(-6.903447552907137, 107.6819073719567),
//        LatLng(-6.903234531799472, 107.67832394074631),
//        LatLng(-6.905066510191451, 107.67628546191403),
//        LatLng(-6.906046402698645, 107.68053408095392),
//        LatLng(-6.902041611824648, 107.68113489576763)
//    )

    //    private val list = listOf(
//        LatLng(-6.900396016397087, 107.68049116560313),
//        LatLng(-6.900268202912309, 107.68154259152882),
//        LatLng(-6.9010883387905775, 107.67946119735274),
//        LatLng(-6.901652847076031, 107.6809203190432),
//        LatLng(-6.901865868895579, 107.68093104787916),
//        LatLng(-6.902014984112232, 107.67955775687638),
//        LatLng(-6.901770009088656, 107.68071647115997),
//        LatLng(-6.902004333026888, 107.6790213150784),
//        LatLng(-6.902856419097122, 107.68117781110622),
//        LatLng(-6.903367670032451, 107.68059845396472),
//        LatLng(-6.90404933704765, 107.68008346983868),
//        LatLng(-6.903889571429064, 107.67961140105645),
//        LatLng(-6.904198451576324, 107.68115635343459),
//        LatLng(-6.904858815352726, 107.67974014708797),
//        LatLng(-6.904315612958742, 107.68105979391096),
//        LatLng(-6.903463529515798, 107.67846341560882),
//        LatLng(-6.90547657418012, 107.68024440237805),
//        LatLng(-6.9018552178364425, 107.68188591427982),
//        LatLng(-6.902643397752938, 107.67860289047627),
//        LatLng(-6.9035274358272405, 107.68081303068391),
//        LatLng(-6.901162896534937, 107.6802336735401),
//        LatLng(-6.901077687692078, 107.68030877539182),
//        LatLng(-6.903761758908072, 107.67845268676481),
//        LatLng(-6.903506133737094, 107.67977233358782),
//        LatLng(-6.905402017135775, 107.67991180845527),
//        LatLng(-6.903005534043962, 107.67853851745248),
//        LatLng(-6.903431576369555, 107.67977233358782),
//        LatLng(-6.903815014134638, 107.6795899433765),
//        LatLng(-6.904006732900677, 107.68084521718373),
//        LatLng(-6.903825665179235, 107.67953629919671),
//        LatLng(-6.9052529029864145, 107.67863507697612),
//        LatLng(-6.903420925316092, 107.67951484152479),
//        LatLng(-6.901226803157018, 107.6807379288302),
//        LatLng(-6.9017700090963405, 107.68070574232232),
//        LatLng(-6.901748706914378, 107.68016930052434),
//        LatLng(-6.901876519991739, 107.68075938650212),
//        LatLng(-6.902206703615154, 107.68013711401646),
//        LatLng(-6.901983030863169, 107.6802336735401),
//        LatLng(-6.902377120879056, 107.68050189443908),
//        LatLng(-6.9000605058761515, 107.6804697079509),
//        LatLng(-6.9025102593162, 107.68046970793816),
//        LatLng(-6.904065313577077, 107.6798474354525),
//        LatLng(-6.9051517183161275, 107.67892475556002),
//        LatLng(-6.905300832497383, 107.67986889312442),
//        LatLng(-6.903447552907137, 107.6819073719567),
//        LatLng(-6.903234531799472, 107.67832394074631),
//        LatLng(-6.905066510191451, 107.67628546191403),
//        LatLng(-6.906046402698645, 107.68053408095392),
//        LatLng(-6.902041611824648, 107.68113489576763)
//    )

    //    private val list = listOf(
//        LatLng(-6.900396016397087, 107.68049116560313),
//        LatLng(-6.900268202912309, 107.68154259152882),
//        LatLng(-6.9010883387905775, 107.67946119735274),
//        LatLng(-6.901652847076031, 107.6809203190432),
//        LatLng(-6.901865868895579, 107.68093104787916),
//        LatLng(-6.902014984112232, 107.67955775687638),
//        LatLng(-6.901770009088656, 107.68071647115997),
//        LatLng(-6.902004333026888, 107.6790213150784),
//        LatLng(-6.902856419097122, 107.68117781110622),
//        LatLng(-6.903367670032451, 107.68059845396472),
//        LatLng(-6.90404933704765, 107.68008346983868),
//        LatLng(-6.903889571429064, 107.67961140105645),
//        LatLng(-6.904198451576324, 107.68115635343459),
//        LatLng(-6.904858815352726, 107.67974014708797),
//        LatLng(-6.904315612958742, 107.68105979391096),
//        LatLng(-6.903463529515798, 107.67846341560882),
//        LatLng(-6.90547657418012, 107.68024440237805),
//        LatLng(-6.9018552178364425, 107.68188591427982),
//        LatLng(-6.902643397752938, 107.67860289047627),
//        LatLng(-6.9035274358272405, 107.68081303068391),
//        LatLng(-6.901162896534937, 107.6802336735401),
//        LatLng(-6.901077687692078, 107.68030877539182),
//        LatLng(-6.903761758908072, 107.67845268676481),
//        LatLng(-6.903506133737094, 107.67977233358782),
//        LatLng(-6.905402017135775, 107.67991180845527),
//        LatLng(-6.903005534043962, 107.67853851745248),
//        LatLng(-6.903431576369555, 107.67977233358782),
//        LatLng(-6.903815014134638, 107.6795899433765),
//        LatLng(-6.904006732900677, 107.68084521718373),
//        LatLng(-6.903825665179235, 107.67953629919671),
//        LatLng(-6.9052529029864145, 107.67863507697612),
//        LatLng(-6.903420925316092, 107.67951484152479),
//        LatLng(-6.901226803157018, 107.6807379288302),
//        LatLng(-6.9017700090963405, 107.68070574232232),
//        LatLng(-6.901748706914378, 107.68016930052434),
//        LatLng(-6.901876519991739, 107.68075938650212),
//        LatLng(-6.902206703615154, 107.68013711401646),
//        LatLng(-6.901983030863169, 107.6802336735401),
//        LatLng(-6.902377120879056, 107.68050189443908),
//        LatLng(-6.9000605058761515, 107.6804697079509),
//        LatLng(-6.9025102593162, 107.68046970793816),
//        LatLng(-6.904065313577077, 107.6798474354525),
//        LatLng(-6.9051517183161275, 107.67892475556002),
//        LatLng(-6.905300832497383, 107.67986889312442),
//        LatLng(-6.903447552907137, 107.6819073719567),
//        LatLng(-6.903234531799472, 107.67832394074631),
//        LatLng(-6.905066510191451, 107.67628546191403),
//        LatLng(-6.906046402698645, 107.68053408095392),
//        LatLng(-6.902041611824648, 107.68113489576763)
//    )

    //    private val list = listOf(
//        LatLng(-6.900396016397087, 107.68049116560313),
//        LatLng(-6.900268202912309, 107.68154259152882),
//        LatLng(-6.9010883387905775, 107.67946119735274),
//        LatLng(-6.901652847076031, 107.6809203190432),
//        LatLng(-6.901865868895579, 107.68093104787916),
//        LatLng(-6.902014984112232, 107.67955775687638),
//        LatLng(-6.901770009088656, 107.68071647115997),
//        LatLng(-6.902004333026888, 107.6790213150784),
//        LatLng(-6.902856419097122, 107.68117781110622),
//        LatLng(-6.903367670032451, 107.68059845396472),
//        LatLng(-6.90404933704765, 107.68008346983868),
//        LatLng(-6.903889571429064, 107.67961140105645),
//        LatLng(-6.904198451576324, 107.68115635343459),
//        LatLng(-6.904858815352726, 107.67974014708797),
//        LatLng(-6.904315612958742, 107.68105979391096),
//        LatLng(-6.903463529515798, 107.67846341560882),
//        LatLng(-6.90547657418012, 107.68024440237805),
//        LatLng(-6.9018552178364425, 107.68188591427982),
//        LatLng(-6.902643397752938, 107.67860289047627),
//        LatLng(-6.9035274358272405, 107.68081303068391),
//        LatLng(-6.901162896534937, 107.6802336735401),
//        LatLng(-6.901077687692078, 107.68030877539182),
//        LatLng(-6.903761758908072, 107.67845268676481),
//        LatLng(-6.903506133737094, 107.67977233358782),
//        LatLng(-6.905402017135775, 107.67991180845527),
//        LatLng(-6.903005534043962, 107.67853851745248),
//        LatLng(-6.903431576369555, 107.67977233358782),
//        LatLng(-6.903815014134638, 107.6795899433765),
//        LatLng(-6.904006732900677, 107.68084521718373),
//        LatLng(-6.903825665179235, 107.67953629919671),
//        LatLng(-6.9052529029864145, 107.67863507697612),
//        LatLng(-6.903420925316092, 107.67951484152479),
//        LatLng(-6.901226803157018, 107.6807379288302),
//        LatLng(-6.9017700090963405, 107.68070574232232),
//        LatLng(-6.901748706914378, 107.68016930052434),
//        LatLng(-6.901876519991739, 107.68075938650212),
//        LatLng(-6.902206703615154, 107.68013711401646),
//        LatLng(-6.901983030863169, 107.6802336735401),
//        LatLng(-6.902377120879056, 107.68050189443908),
//        LatLng(-6.9000605058761515, 107.6804697079509),
//        LatLng(-6.9025102593162, 107.68046970793816),
//        LatLng(-6.904065313577077, 107.6798474354525),
//        LatLng(-6.9051517183161275, 107.67892475556002),
//        LatLng(-6.905300832497383, 107.67986889312442),
//        LatLng(-6.903447552907137, 107.6819073719567),
//        LatLng(-6.903234531799472, 107.67832394074631),
//        LatLng(-6.905066510191451, 107.67628546191403),
//        LatLng(-6.906046402698645, 107.68053408095392),
//        LatLng(-6.902041611824648, 107.68113489576763)
//    )

    //    private val list = listOf(
//        LatLng(-6.900396016397087, 107.68049116560313),
//        LatLng(-6.900268202912309, 107.68154259152882),
//        LatLng(-6.9010883387905775, 107.67946119735274),
//        LatLng(-6.901652847076031, 107.6809203190432),
//        LatLng(-6.901865868895579, 107.68093104787916),
//        LatLng(-6.902014984112232, 107.67955775687638),
//        LatLng(-6.901770009088656, 107.68071647115997),
//        LatLng(-6.902004333026888, 107.6790213150784),
//        LatLng(-6.902856419097122, 107.68117781110622),
//        LatLng(-6.903367670032451, 107.68059845396472),
//        LatLng(-6.90404933704765, 107.68008346983868),
//        LatLng(-6.903889571429064, 107.67961140105645),
//        LatLng(-6.904198451576324, 107.68115635343459),
//        LatLng(-6.904858815352726, 107.67974014708797),
//        LatLng(-6.904315612958742, 107.68105979391096),
//        LatLng(-6.903463529515798, 107.67846341560882),
//        LatLng(-6.90547657418012, 107.68024440237805),
//        LatLng(-6.9018552178364425, 107.68188591427982),
//        LatLng(-6.902643397752938, 107.67860289047627),
//        LatLng(-6.9035274358272405, 107.68081303068391),
//        LatLng(-6.901162896534937, 107.6802336735401),
//        LatLng(-6.901077687692078, 107.68030877539182),
//        LatLng(-6.903761758908072, 107.67845268676481),
//        LatLng(-6.903506133737094, 107.67977233358782),
//        LatLng(-6.905402017135775, 107.67991180845527),
//        LatLng(-6.903005534043962, 107.67853851745248),
//        LatLng(-6.903431576369555, 107.67977233358782),
//        LatLng(-6.903815014134638, 107.6795899433765),
//        LatLng(-6.904006732900677, 107.68084521718373),
//        LatLng(-6.903825665179235, 107.67953629919671),
//        LatLng(-6.9052529029864145, 107.67863507697612),
//        LatLng(-6.903420925316092, 107.67951484152479),
//        LatLng(-6.901226803157018, 107.6807379288302),
//        LatLng(-6.9017700090963405, 107.68070574232232),
//        LatLng(-6.901748706914378, 107.68016930052434),
//        LatLng(-6.901876519991739, 107.68075938650212),
//        LatLng(-6.902206703615154, 107.68013711401646),
//        LatLng(-6.901983030863169, 107.6802336735401),
//        LatLng(-6.902377120879056, 107.68050189443908),
//        LatLng(-6.9000605058761515, 107.6804697079509),
//        LatLng(-6.9025102593162, 107.68046970793816),
//        LatLng(-6.904065313577077, 107.6798474354525),
//        LatLng(-6.9051517183161275, 107.67892475556002),
//        LatLng(-6.905300832497383, 107.67986889312442),
//        LatLng(-6.903447552907137, 107.6819073719567),
//        LatLng(-6.903234531799472, 107.67832394074631),
//        LatLng(-6.905066510191451, 107.67628546191403),
//        LatLng(-6.906046402698645, 107.68053408095392),
//        LatLng(-6.902041611824648, 107.68113489576763)
//    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClick()
        }
        binding.resetButton.setOnClickListener {
            onResetButtonClick()
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }
        observeTrackerService()

        addHeatMap()
    }

    private fun observeTrackerService() {
        TrackerService.locationList.observe(viewLifecycleOwner, {
            if (it != null) {
                locationList = it
                if (locationList.size > 1) {
                    binding.stopButton.enabled()
                    Log.d("LocationList", locationList.toString())
                }
                drawPolyline()
                followPolyline()
            }
        })
        TrackerService.started.observe(viewLifecycleOwner, {
            started.value = it
        })
        TrackerService.startTime.observe(viewLifecycleOwner, {
            startTime = it
        })
        TrackerService.stopTime.observe(viewLifecycleOwner, {
            stopTime = it
            if (stopTime != 0L) {
                showBiggerPicture()
                displayResult()
            }
        })
    }

    private fun drawPolyline() {
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polylineList.add(polyline)
    }

    private fun followPolyline() {
        if (locationList.isNotEmpty()) {
            map.animateCamera(
                (
                        CameraUpdateFactory.newCameraPosition(
                            setCameraPosition(locationList.last())
                        )
                        ), 1000, null
            )
        }
    }

    private fun addHeatMap() {
        val provider = HeatmapTileProvider.Builder()
            .data(list)
            .radius(50)
            .build()

        val overlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
    }

    private fun onStartButtonClicked() {
        if (hasBackgroundLocationPermission(requireContext())) {
            startCountDown()
            binding.startButton.disable()
            binding.startButton.hide()
            binding.stopButton.show()
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    private fun onStopButtonClick() {
        stopForegroundService()
        binding.stopButton.hide()
        binding.startButton.show()
    }

    private fun onResetButtonClick() {
        mapReset()
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopButton.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }

    private fun stopForegroundService() {
        binding.startButton.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    private fun sendActionCommandToService(action: String) {
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                100
            ), 2000, null
        )
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng) {
        val marker = map.addMarker(MarkerOptions().position(position))
        markerList.add(marker)
    }

    private fun displayResult() {
        val result = Result(
            calculateTheDistance(locationList),
            calculateElapsedTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val direction = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(direction)
            binding.startButton.apply {
                hide()
                enabled()
            }
            binding.stopButton.hide()
            binding.resetButton.show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(
                it.result.latitude,
                it.result.longitude
            )
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(lastKnownLocation)
                )
            )
            for (polyline in polylineList) {
                polyline.remove()
            }
            for (marker in markerList) {
                marker.remove()
            }
            locationList.clear()
            markerList.clear()
            binding.resetButton.hide()
            binding.startButton.show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.startButton.show()
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms[0])) {
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return true
    }
}