package uz.openlocation.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var codeInput: EditText
    private lateinit var status: TextView
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView

    private var sharing = false

    private val database by lazy {
        FirebaseDatabase.getInstance().reference
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        createInterface()
    }

    private fun createInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 20)
            setBackgroundColor(Color.rgb(16, 16, 24))
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "OPEN LOCATION"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 10)
        }

        root.addView(title)

        addText(root, "Ikki qurilmani bog‘lash tizimi", 15f)

        root.addView(createCard("🏠  BOSH SAHIFA"))

        status = addText(
            root,
            "Holat: Qurilma ulanmagan",
            15f
        )

        root.addView(createCard("🔗  QURILMANI ULASH"))

        codeInput = EditText(this).apply {
            hint = "Ulanish kodini kiriting"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(20, 12, 20, 12)
            setBackgroundColor(Color.rgb(35, 35, 48))
        }

        root.addView(codeInput)

        addButton(root, "JOYLANISHNI ULASHISH") {
            startSharing()
        }

        addButton(root, "ULANISHNI TO‘XTATISH") {
            stopSharing()
        }

        root.addView(createCard("📍  LOKATSIYA"))

        latitudeText = addText(
            root,
            "Latitude: --",
            15f
        )

        longitudeText = addText(
            root,
            "Longitude: --",
            15f
        )

        addButton(root, "LOKATSIYANI KO‘RISH") {
            readLocation()
        }

        root.addView(createCard("📷  KAMERA"))

        addText(
            root,
            "Kamera faqat ushbu telefonda, foydalanuvchi tugmani bosganda ochiladi.",
            13f
        )

        addButton(root, "KAMERA RUXSATINI TEKSHIRISH") {
            requestCameraPermission()
        }

        root.addView(createCard("⚙️  SOZLAMALAR"))

        addText(
            root,
            "Maxfiylik va ruxsatlarni telefon sozlamalaridan boshqarishingiz mumkin.",
            13f
        )

        addText(
            root,
            "Ochiq Lokatsiya v1.2",
            12f
        )

        setContentView(scroll)
    }

    private fun createCard(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(16, 22, 16, 22)
            setBackgroundColor(Color.rgb(40, 35, 60))
        }
    }

    private fun addText(
        parent: LinearLayout,
        text: String,
        size: Float
    ): TextView {

        val view = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(8, 12, 8, 12)
        }

        parent.addView(view)
        return view
    }

    private fun addButton(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }

        parent.addView(button)
    }

    private fun authenticate(action: () -> Unit) {

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {
                action()
            }
            .addOnFailureListener {
                status.text =
                    "Firebase xatosi: ${it.localizedMessage}"
            }
    }

    private fun startSharing() {

        val code = codeInput.text.toString().trim()

        if (code.length < 6) {
            status.text = "Kamida 6 belgili kod kiriting"
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                101
            )

            return
        }

        authenticate {

            sharing = true

            try {

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000L,
                    10f,
                    this
                )

                status.text = "Lokatsiya ulashish boshlandi"

            } catch (e: Exception) {

                status.text = "GPS xatosi: ${e.message}"
            }
        }
    }

    override fun onLocationChanged(location: Location) {

        if (!sharing) return

        val code = codeInput.text.toString().trim()

        latitudeText.text =
            "Latitude: ${location.latitude}"

        longitudeText.text =
            "Longitude: ${location.longitude}"

        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("sharedLocations")
            .child(code)
            .setValue(data)
            .addOnSuccessListener {
                status.text = "Lokatsiya ulashilmoqda"
            }
            .addOnFailureListener {
                status.text =
                    "Firebase yozish xatosi: ${it.message}"
            }
    }

    private fun readLocation() {

        val code = codeInput.text.toString().trim()

        if (code.length < 6) {
            status.text = "Ulanish kodini kiriting"
            return
        }

        authenticate {

            database.child("sharedLocations")
                .child(code)
                .get()
                .addOnSuccessListener { snapshot ->

                    if (!snapshot.exists()) {
                        status.text = "Lokatsiya topilmadi"
                        return@addOnSuccessListener
                    }

                    val lat = snapshot.child("latitude")
                        .getValue(Double::class.java)

                    val lon = snapshot.child("longitude")
                        .getValue(Double::class.java)

                    latitudeText.text = "Latitude: $lat"
                    longitudeText.text = "Longitude: $lon"

                    status.text = "Lokatsiya olindi"
                }
                .addOnFailureListener {
                    status.text = "O‘qish xatosi: ${it.message}"
                }
        }
    }

    private fun stopSharing() {

        sharing = false

        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {
        }

        status.text = "Lokatsiya ulashish to‘xtatildi"
    }

    private fun requestCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            Toast.makeText(
                this,
                "Kamera ruxsati berilgan",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                202
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            if (requestCode == 101) {
                startSharing()
            }

            if (requestCode == 202) {
                Toast.makeText(
                    this,
                    "Kamera ruxsati berildi",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {

            status.text = "Ruxsat berilmadi"
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        stopSharing()
        super.onDestroy()
    }
}
