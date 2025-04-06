package com.jfh.drivy

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import java.util.Locale

class AgregarParadaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var database: DatabaseReference
    private var diasSeleccionados: MutableList<String> = mutableListOf()
    private lateinit var map: GoogleMap

    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_parada)

        checkPermissions()
        createMapFragment()
        setupSearchView()

        database = FirebaseDatabase.getInstance().reference

        val spinnerHorario = findViewById<Spinner>(R.id.spinner_horario)
        val textViewDias = findViewById<TextView>(R.id.text_dias_actividad)
        val editTextReferencia = findViewById<EditText>(R.id.ingresar_referencia)
        val spinnerTolerancia = findViewById<Spinner>(R.id.spinner_tolerancia)
        val spinnerAlcaldia = findViewById<Spinner>(R.id.spinner_alcaldia)
        val spinnerVehiculo = findViewById<Spinner>(R.id.spinner_vehiculo)
        val botonRegistrarParada = findViewById<Button>(R.id.registrar_parada)

        val flechaRegreso = findViewById<ImageView>(R.id.flecha_regreso)
        flechaRegreso.setOnClickListener { finish() }

        val alcaldias = listOf(
            "Seleccione una alcaldía", "Álvaro Obregón", "Azcapotzalco", "Benito Juárez", "Coyoacán",
            "Cuajimalpa de Morelos", "Cuauhtémoc", "Gustavo A. Madero", "Iztacalco", "Iztapalapa",
            "Magdalena Contreras", "Miguel Hidalgo", "Milpa Alta", "Tláhuac", "Tlalpan",
            "Venustiano Carranza", "Xochimilco", "Otra"
        )
        val adapterAlcaldias = ArrayAdapter(this, android.R.layout.simple_spinner_item, alcaldias)
        adapterAlcaldias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAlcaldia.adapter = adapterAlcaldias

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val vehiculosRef = database.child("Vehiculos")
            vehiculosRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val vehiculos = mutableListOf("Seleccione un vehículo")
                    snapshot.children.forEach { vehiculoSnapshot ->
                        val uidUsuario = vehiculoSnapshot.child("uidUsuario").value.toString()
                        if (uidUsuario == uid) {
                            val marca = vehiculoSnapshot.child("marca").value.toString()
                            vehiculos.add(marca)
                        }
                    }
                    val adapterVehiculos = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehiculos)
                    adapterVehiculos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerVehiculo.adapter = adapterVehiculos
                } else {
                    Toast.makeText(this, "No tienes vehículos registrados", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Error al obtener los vehículos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val horarios = (0..23).map { hora -> String.format("%02d:00", hora) }
        val adapterHorarios = ArrayAdapter(this, android.R.layout.simple_spinner_item, horarios)
        adapterHorarios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHorario.adapter = adapterHorarios

        val tolerancias = listOf("5 minutos", "10 minutos", "15 minutos")
        val adapterTolerancias = ArrayAdapter(this, android.R.layout.simple_spinner_item, tolerancias)
        adapterTolerancias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTolerancia.adapter = adapterTolerancias

        val diasSemana = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val diasSeleccionadosIndices = BooleanArray(diasSemana.size)
        textViewDias.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Selecciona los días de actividad")
            builder.setMultiChoiceItems(diasSemana, diasSeleccionadosIndices) { _, which, isChecked ->
                if (isChecked) {
                    diasSeleccionados.add(diasSemana[which])
                } else {
                    diasSeleccionados.remove(diasSemana[which])
                }
            }
            builder.setPositiveButton("OK") { _, _ ->
                textViewDias.text = diasSeleccionados.joinToString(", ")
            }
            builder.setNegativeButton("Cancelar", null)
            builder.show()
        }

        botonRegistrarParada.setOnClickListener {
            val horario = spinnerHorario.selectedItem.toString()
            val dias = diasSeleccionados.joinToString(", ")
            val referencia = editTextReferencia.text.toString().trim()
            val tolerancia = spinnerTolerancia.selectedItem.toString()
            val alcaldia = spinnerAlcaldia.selectedItem.toString()
            val vehiculo = spinnerVehiculo.selectedItem?.toString()

            if (horario.isNotEmpty() && dias.isNotEmpty() && referencia.isNotEmpty()
                && tolerancia.isNotEmpty() && alcaldia != "Seleccione una alcaldía"
                && vehiculo != "Seleccione un vehículo"
                && latitudSeleccionada != null && longitudSeleccionada != null
            ) {
                registrarParada(
                    horario, dias, referencia, tolerancia,
                    alcaldia, vehiculo!!, latitudSeleccionada!!, longitudSeleccionada!!
                )
            } else {
                Toast.makeText(this, "Completa todos los campos y busca una ubicación válida en el mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //maps

    private fun createMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentMap) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                } else {
                    Toast.makeText(applicationContext, "Ingrese una dirección", Toast.LENGTH_SHORT).show()
                }
                return false
            }

            override fun onQueryTextChange(newText: String?) = false
        })
    }

    private fun searchLocation(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList.isNullOrEmpty()) {
                Toast.makeText(this, "Ubicación no encontrada", Toast.LENGTH_SHORT).show()
                return
            }

            val address = addressList[0]
            val latLng = LatLng(address.latitude, address.longitude)

            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title(locationName))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            // Guarda coordenadas seleccionadas
            latitudSeleccionada = latLng.latitude
            longitudSeleccionada = latLng.longitude

        } catch (e: IOException) {
            Log.e("AgregarParada", "Error al buscar ubicación: ${e.message}")
            Toast.makeText(this, "Error al conectar con el servicio de mapas", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("AgregarParada", "Excepción: ${e.message}")
            Toast.makeText(this, "Error inesperado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        val mexicoCity = LatLng(19.432608, -99.133209)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 10f))

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun registrarParada(
        horario: String,
        dias: String,
        referencia: String,
        tolerancia: String,
        alcaldia: String,
        vehiculo: String,
        latitud: Double,
        longitud: Double
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = database.child("Usuarios").child(uid)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.hasChild("Nombre")) {
                    val nombreUsuario = snapshot.child("Nombre").value.toString()

                    val paradaId = database.child("Paradas").push().key
                    if (paradaId != null) {
                        val paradaData = mapOf(
                            "id" to paradaId,
                            "horario" to horario,
                            "dias" to dias,
                            "referencia" to referencia,
                            "tolerancia" to tolerancia,
                            "alcaldia" to alcaldia,
                            "vehiculo" to vehiculo,
                            "agregadoPor" to nombreUsuario,
                            "uidUsuario" to uid,
                            "latitud" to latitud,
                            "longitud" to longitud
                        )

                        database.child("Paradas").child(paradaId).setValue(paradaData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Parada registrada correctamente", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(this, "Error al registrar la parada: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "No se pudo obtener el nombre del usuario", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(this, "Error al obtener los datos del usuario: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }
}
