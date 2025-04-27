package com.jfh.drivy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AlcaldiasPasajeroActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alcaldias_pasajero)

        // Inicializar la referencia de la base de datos
        database = FirebaseDatabase.getInstance().reference

        // Mostrar el nombre del usuario
        mostrarNombreUsuario()

        // Configurar los botones de las alcaldías
        configurarBotones()

        val botonCerrarSesion = findViewById<ImageView>(R.id.cerrar_sesion)
        botonCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Cerrar sesión en Firebase
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpiar la pila de actividades
            startActivity(intent)
        }
    }

    private fun mostrarNombreUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val textViewNombre = findViewById<TextView>(R.id.nombre_usuario)

        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = database.child("Usuarios").child(uid)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChild("Nombre")) {
                        val nombre = snapshot.child("Nombre").value.toString()
                        textViewNombre.text = "¡Hola, $nombre!"
                    } else {
                        textViewNombre.text = "¡Hola, Usuario!"
                        Toast.makeText(
                            this@AlcaldiasPasajeroActivity,
                            "No se encontró el nombre del usuario.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    textViewNombre.text = "¡Hola, Usuario!"
                    Toast.makeText(
                        this@AlcaldiasPasajeroActivity,
                        "Error al obtener los datos: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            textViewNombre.text = "¡Hola, Usuario!"
        }
    }

    private fun configurarBotones() {
        // Mapa de IDs de los botones con sus respectivas alcaldías
        val botones = mapOf(
            R.id.boton_ALCALDIA1 to "Alvaro Obregón",
            R.id.boton_ALCALDIA2 to "Azcapotzalco",
            R.id.boton_ALCALDIA3 to "Benito Juárez",
            R.id.boton_ALCALDIA4 to "Coyoacán",
            R.id.boton_ALCALDIA5 to "Cuajimalpa de Morelos",
            R.id.boton_ALCALDIA6 to "Cuauhtémoc",
            R.id.boton_ALCALDIA7 to "Gustavo A. Madero",
            R.id.boton_ALCALDIA8 to "Iztacalco",
            R.id.boton_ALCALDIA9 to "Iztapalapa",
            R.id.boton_ALCALDIA10 to "Magdalena Contreras",
            R.id.boton_ALCALDIA11 to "Miguel Hidalgo",
            R.id.boton_ALCALDIA12 to "Milpa Alta",
            R.id.boton_ALCALDIA13 to "Tláhuac",
            R.id.boton_ALCALDIA14 to "Tlalpan",
            R.id.boton_ALCALDIA15 to "Venustiano Carranza",
            R.id.boton_ALCALDIA16 to "Xochimilco"
        )

        // Asignar listeners a los botones
        for ((id, alcaldia) in botones) {
            findViewById<Button>(id).setOnClickListener {
                navigateToVerRutas(alcaldia)
            }
        }
    }

    private fun navigateToVerRutas(alcaldia: String) {
        val intent = Intent(this, VerRutasPasajeroActivity::class.java)
        intent.putExtra("ALCALDIA_SELECCIONADA", alcaldia)
        startActivity(intent)
    }
}
