package uz.openlocation.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var status: TextView
    private lateinit var cameraStatus: TextView
    private lateinit var codeInput: EditText
    private lateinit var locationManager: LocationManager
    private lateinit var previewView: PreviewView
    private var sharing = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
        }
        layout.addView(TextView(this).apply {
            text = "Ochiq Lokatsiya"
            textSize = 26f
        })
        layout.addView(TextView(this).apply {
            text = "Ulashish kodi (ikkala telefonda bir xil):"
            textSize = 16f
        })
        codeInput = EditText(this).apply {
            hint = "Masalan: FAMILY-2026"
            setSingleLine(true)
        }
        layout.addView(codeInput)
        status = TextView(this).apply {
            text = "GPS: o‘chirilgan"
            textSize = 15f
            setPadding(0, 12, 0, 12)
        }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "Joylashuvni ulashishni boshlash"
            setOnClickListener { startSharing() }
        })
        layout.addView(Button(this).apply {
            text = "Joylashuv ulashishni to‘xtatish"
            setOnClickListener { stopSharing() }
        })

        layout.addView(TextView(this).apply {
            text = "Kamera — faqat shu telefonda ko‘rinadigan jonli preview"
            textSize = 17f
            setPadding(0, 22, 0, 8)
        })
        previewView = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 300
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        layout.addView(previewView)

        cameraStatus = TextView(this).apply {
            text = "Kamera: o‘chirilgan"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        layout.addView(cameraStatus)
        layout.addView(Button(this).apply {
            text = "Kamerani ochish"
            setOnClickListener { requestCameraAndStart() }
        })
        layout.addView(Button(this).apply {
            text = "Old / orqa kamerani almashtirish"
            setOnClickListener {
                if (cameraRunning) {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    startCameraPreview()
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Kamerani to‘xtatish"
            setOnClickListener { stopCameraPreview() }
        })
        layout.addView(TextView(this).apply {
            text = "Eslatma: kamera preview faqat shu qurilmada ko‘rinadi; boshqa telefonga video uzatilmaydi. Kamera yashirin yoqilmaydi."
            textSize = 12f
            setPadding(0, 12, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun requestCameraAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCameraPreview()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 202)
        }
    }

    private fun startCameraPreview() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cameraProvider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, selector, preview)
                cameraRunning = true
                cameraStatus.text = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                    "Kamera: ORQA kamera faol" else "Kamera: OLD kamera faol"
            } catch (e: Exception) {
                cameraStatus.text = "Kamera xatosi: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCameraPreview() {
        cameraProvider?.unbindAll()
        cameraRunning = false
        cameraStatus.text = "Kamera: to‘xtatildi"
        previewView.setBackgroundColor(android.graphics.Color.BLACK)
    }

    private fun startSharing() {
        val code = codeInput.text.toString().trim()
        if (code.length < 6) {
            Toast.makeText(this, "Kamida 6 belgili ulashish kodini kiriting", Toast.LENGTH_LONG).show()
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 101)
            return
        }
        sharing = true
        status.text = "GPS: ulashishga ulanmoqda…"
        FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, this)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000L, 20f, this)
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocationChanged(it) }
            } catch (e: SecurityException) {
                status.text = "GPS ruxsati kerak"
            }
        }.addOnFailureListener {
            sharing = false
            status.text = "Firebase ulanish xatosi: ${it.localizedMessage}"
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!sharing) return
        val code = codeInput.text.toString().trim()
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracyMeters" to location.accuracy,
            "updatedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
        )
        FirebaseDatabase.getInstance().reference.child("sharedLocations")
            .child(code).setValue(data)
            .addOnSuccessListener {
                status.text = "GPS ulashilmoqda\nLat: ${location.latitude}\nLon: ${location.longitude}\nAniqlik: ${location.accuracy.toInt()} m"
            }
            .addOnFailureListener { status.text = "Firebase yozish xatosi: ${it.localizedMessage}" }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun stopSharing() {
        sharing = false
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        status.text = "GPS: ulashish to‘xtatildi"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 202) cameraStatus.text = "Kamera ruxsati berilmadi"
            if (requestCode == 101) status.text = "GPS ruxsati berilmadi"
            return
        }
        when (requestCode) {
            202 -> startCameraPreview()
            101 -> startSharing()
        }
    }

    override fun onStop() {
        stopCameraPreview()
        super.onStop()
    }

    override fun onDestroy() {
        stopSharing()
        stopCameraPreview()
        super.onDestroy()
    }
}
