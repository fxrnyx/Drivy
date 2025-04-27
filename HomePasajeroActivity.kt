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

class HomePasajeroActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_pasajero)

        // Inicializar la referencia de la base de datos
        database = FirebaseDatabase.getInstance().reference

        // Mostrar el nombre del usuario
        mostrarNombreUsuario()

        // Configurar botones
        configurarBotones()

        val botonCerrarSesion = findViewById<ImageView>(R.id.cerrar_sesion)
        botonCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                            this@HomePasajeroActivity,
                            "No se encontró el nombre del usuario.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    textViewNombre.text = "¡Hola, Usuario!"
                    Toast.makeText(
                        this@HomePasajeroActivity,
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
        val botonAlcaldias = findViewById<Button>(R.id.boton_ALCALDIAS)
        val botonReservas = findViewById<Button>(R.id.boton_RESERVAS)

        botonAlcaldias.setOnClickListener {
            redirigirAlcaldias()
        }

        botonReservas.setOnClickListener {
            redirigirReservas()
        }
    }

    private fun redirigirAlcaldias() {
        val intent = Intent(this, AlcaldiasPasajeroActivity::class.java)
        startActivity(intent)
    }

    private fun redirigirReservas() {
        // De momento no hace nada. Método listo para usar en el futuro.
        Toast.makeText(this, "Funcionalidad de Reservas en desarrollo.", Toast.LENGTH_SHORT).show()
    }
}
