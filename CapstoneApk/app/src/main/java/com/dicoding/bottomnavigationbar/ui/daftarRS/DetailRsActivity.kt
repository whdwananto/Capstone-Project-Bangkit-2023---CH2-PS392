package com.dicoding.bottomnavigationbar.ui.daftarRS

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dicoding.bottomnavigationbar.R
import com.dicoding.bottomnavigationbar.data.ars.Rs
import com.dicoding.bottomnavigationbar.databinding.ActivityDetailRsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Suppress("DEPRECATION")
class DetailRsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDetailRsBinding
    private lateinit var mMap: GoogleMap
    private var rsData : Rs? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext)
        binding = ActivityDetailRsBinding.inflate(layoutInflater)
        setContentView( binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        rsData = intent.getParcelableExtra("DATA")

        if (rsData != null) {
            binding.tvNamaRs.text = rsData!!.nama
            binding.tvDescRs.text = rsData!!.descRS
            binding.tvAlamatRs.text = rsData!!.alamatRS
            binding.tvJmlhDokter.text = rsData!!.jmlhDokter
            binding.tvJamKerja.text = rsData!!.jamBuka
            binding.tvTelp.text = rsData!!.kontak

            Glide.with(this)
                .load(rsData!!.photo)
                .error(R.drawable.baseline_error_24)
                .into(binding.ivDetailRs)

            binding.tvHubungiWa.setOnClickListener {
                val formattedPhoneNumber = replaceLeading0With62(rsData!!.kontak)
                if (isValidPhoneNumber(formattedPhoneNumber)) {
                    openWhatsApp(formattedPhoneNumber)
                } else {
                    Toast.makeText(this, "Nomor telepon tidak valid", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Data tidak valid", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        val lat = rsData?.lat?.toDoubleOrNull()
        val lon = rsData?.lon?.toDoubleOrNull()

        if (lat != null && lon != null) {
            val markerLatLng = LatLng(lat, lon)

            val markerOptions = MarkerOptions()
                .position(markerLatLng)
                .title(rsData!!.nama)
            val location = CameraUpdateFactory.newLatLngZoom(markerLatLng, DEFAULT_ZOOM)

            mMap.animateCamera(location)
            mMap.addMarker(markerOptions)
        } else {
            println("Nilai Latitude atau Longitude tidak valid")
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.length >= 9 && phoneNumber.all { it.isDigit() }
    }
    private fun replaceLeading0With62(phoneNumber: String): String {
        return if (phoneNumber.startsWith("0")) {
            "62" + phoneNumber.substring(1)
        } else {
            phoneNumber
        }
    }
    private fun openWhatsApp(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://wa.me/$phoneNumber")
        startActivity(intent)
    }
    companion object {
        const val DEFAULT_ZOOM = 10.0f
    }
}