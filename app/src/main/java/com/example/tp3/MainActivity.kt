package com.example.tp3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.tp3.ViewModels.UtilisateurViewModel
import com.example.tp3.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    lateinit var navController: NavController
    lateinit var parkingViewModel: ParkingViewModel
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var location: Location
    var isConnected: Boolean = false
    lateinit var pref: SharedPreferences
    lateinit var menuDecoonexion: Menu
    private lateinit var binding: ActivityMainBinding
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var utilisateurViewModel: UtilisateurViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // Intervalle de mise à jour en millisecondes
            fastestInterval = 500 // Intervalle le plus rapide en millisecondes
            priority = PRIORITY_HIGH_ACCURACY
            maxWaitTime = 100 // Temps d’attente max en millisecondes
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                location = p0.lastLocation!!
                if (location == null) {
                    Log.d("Erreur", "Emplacement invalid")
                } else {
                    var position = HashMap<String, Double>()
                    position["latitude"] = location.latitude
                    position["longitude"] = location.longitude
                    position["vitesse"] = location.speed.toDouble()
                    Log.d("Vitesse", location.speed.toString())
                    Log.d("Latitude", location.latitude.toString())
                    Log.d("Longitude", location.longitude.toString())
                    parkingViewModel.postion.value = position
                }
            }
        }
        getLastLocation()
        utilisateurViewModel = ViewModelProvider(this).get(UtilisateurViewModel::class.java)
        pref = getSharedPreferences("db_privee", Context.MODE_PRIVATE)
        parkingViewModel = ViewModelProvider(this).get(ParkingViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = navHostFragment.navController
        NavigationUI.setupWithNavController(binding.navBottom, navController)
        isConnected = pref.getBoolean("connected", false)
        binding.navBottom.setOnNavigationItemSelectedListener { item ->
            isConnected = pref.getBoolean("connected", false)
            when (item.itemId) {
                R.id.homeFragment -> {
                    findNavController(R.id.navHost)
                        .navigate(R.id.homeFragment)
                    true
                }
                R.id.mesReservationFragment -> {
                    if (!isConnected) {
                        findNavController(R.id.navHost)
                            .navigate(R.id.loginFragment)
                    } else {
                        findNavController(R.id.navHost)
                            .navigate(R.id.mesReservationFragment)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            utilisateurViewModel.menu = menu
        }
        menuInflater.inflate(R.menu.custom_app_bar, menu);
        return true;

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val itemMenu: MenuItem = menu!!.findItem(R.id.btnLogout)
        itemMenu.isVisible = isConnected
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        isConnected = pref.getBoolean("connected", false)
        return when (item.itemId) {
            R.id.btnLogout -> {
                val prefmodif = this.getSharedPreferences("db_privee", Context.MODE_PRIVATE)?.edit()
                prefmodif?.putBoolean("connected", false)
                prefmodif?.putString("email", "")
                prefmodif?.putString("password", "")
                prefmodif?.apply()
                isConnected = false
                item.isVisible = isConnected
                utilisateurViewModel.utilisateurs.value = null
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Status de connexion")
                builder.setMessage("Deconnexion effectuée avec succés")
                builder.setIcon(android.R.drawable.ic_dialog_info)
                builder.setNeutralButton("Ok") { dialogInterface, which ->
                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.setCancelable(false)
                alertDialog.show()
                true;
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissions(): Boolean {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback, Looper.getMainLooper()
                )
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    override fun onPause() {
        mFusedLocationClient.removeLocationUpdates(locationCallback)
        super.onPause()
    }
}