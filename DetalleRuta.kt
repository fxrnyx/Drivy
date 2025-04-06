package com.jfh.drivy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.jfh.drivy.databinding.ActivityDetalleRutaBinding

class DetalleRuta : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var database: DatabaseReference
    private lateinit var map: GoogleMap
    private var latitud: Double? = null
    private var longitud: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_ruta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val flechaRegreso = findViewById<ImageView>(R.id.flecha_regreso)
        flechaRegreso.setOnClickListener {
            finish()
        }

        database = FirebaseDatabase.getInstance().reference

        val origenPantalla = intent.getStringExtra("origenPantalla") ?: ""
        val paradaId = intent.getStringExtra("paradaId")

        if (paradaId != null) {
            cargarDetallesParada(paradaId)
            if (origenPantalla == "VerRutasPasajero") {
                Toast.makeText(this, "Cargando detalles de parada", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No se pudo obtener la información de la parada", Toast.LENGTH_SHORT).show()
            finish()
        }

        checkPermissions()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentMap) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        // Si ya se cargó latitud y longitud, muestra el marcador
        if (latitud != null && longitud != null) {
            val ubicacion = LatLng(latitud!!, longitud!!)
            map.addMarker(MarkerOptions().position(ubicacion).title("Parada"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15f))
        } 
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun cargarDetallesParada(paradaId: String) {
        val paradaRef = database.child("Paradas").child(paradaId)
        paradaRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val horario = snapshot.child("horario").value?.toString() ?: "No especificado"
                val referencia = snapshot.child("referencia").value?.toString() ?: "No disponible"
                val agregadoPor = snapshot.child("agregadoPor").value?.toString() ?: "Anónimo"
                val vehiculoSeleccionado = snapshot.child("vehiculo").value?.toString() ?: "Sin vehículo"
                val uidUsuario = snapshot.child("uidUsuario").value?.toString() ?: ""

                latitud = snapshot.child("latitud").value as? Double
                longitud = snapshot.child("longitud").value as? Double

                findViewById<TextView>(R.id.text_horario).text = "Horario: $horario"
                findViewById<TextView>(R.id.text_referencia).text = "Referencia: $referencia"
                findViewById<TextView>(R.id.text_conductor).text = "Conductor: $agregadoPor"

                // Si el mapa ya está inicializado, mostrar la ubicación
                if (latitud != null && longitud != null && ::map.isInitialized) {
                    val ubicacionParada = LatLng(latitud!!, longitud!!)
                    map.clear()
                    map.addMarker(MarkerOptions().position(ubicacionParada).title("Parada"))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionParada, 15f))
                }

                cargarDetallesVehiculo(uidUsuario, vehiculoSeleccionado)
            } else {
                Toast.makeText(this, "La parada no existe", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Error al cargar los datos: ${error.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cargarDetallesVehiculo(uidUsuario: String, vehiculoSeleccionado: String) {
        val vehiculosRef = database.child("Vehiculos")
        vehiculosRef.orderByChild("uidUsuario").equalTo(uidUsuario)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (vehiculoSnapshot in snapshot.children) {
                            val marca = vehiculoSnapshot.child("marca").value?.toString() ?: ""
                            if (marca == vehiculoSeleccionado) {
                                val modelo = vehiculoSnapshot.child("modelo").value?.toString() ?: "Desconocido"
                                val color = vehiculoSnapshot.child("color").value?.toString() ?: "Desconocido"
                                val placa = vehiculoSnapshot.child("placa").value?.toString() ?: "Desconocida"

                                findViewById<TextView>(R.id.text_marca_auto).text = "Marca de auto: $marca"
                                findViewById<TextView>(R.id.text_modelo_auto).text = "Modelo de auto: $modelo"
                                findViewById<TextView>(R.id.text_color).text = "Color: $color"
                                findViewById<TextView>(R.id.text_placa).text = "Placa: $placa"
                                break
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DetalleRuta, "Error al cargar datos del vehículo: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}





