package com.example.interncert

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class CertificateDetailActivity : AppCompatActivity() {

    private lateinit var tvCertId: TextView
    private lateinit var tvVerifyUrl: TextView
    private lateinit var ivQrCode: ImageView
    private lateinit var ivCertificatePreview: ImageView
    private lateinit var btnGenerateQR: Button

    // Class-level variables to track the loaded certificate details
    private var currentCertId: String = ""
    private var currentFileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate_detail)

        tvCertId = findViewById(R.id.tvCertId)
        tvVerifyUrl = findViewById(R.id.tvVerifyUrl)
        ivQrCode = findViewById(R.id.ivQrCode)
        ivCertificatePreview = findViewById(R.id.ivCertificatePreview)
        btnGenerateQR = findViewById<Button>(R.id.btnGenerateQR)

        val passedCertId = if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            intent.data?.getQueryParameter("id") ?: intent.data?.lastPathSegment
        } else {
            intent.getStringExtra("CERT_ID")
        }

        loadCertificateFromFirestore(passedCertId)

        btnGenerateQR.setOnClickListener {
            if (currentCertId.isNotEmpty() && currentCertId != "ICERT-2026-XXXXX") {
                val qrPayload = "--- INTERNCERT VERIFIED ---\n" +
                        "Recipient: $currentFileName\n" +
                        "ID: $currentCertId\n" +
                        "Status: Official Verified Record"

                tvVerifyUrl.text = "ID: $currentCertId"
                generateQRCode(qrPayload)

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "InternCert Verification Details:\n$qrPayload")
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share Certificate Details"))
            } else {
                Toast.makeText(this, "No valid certificate loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
        setupBottomNavigation()
    }

    private fun loadCertificateFromFirestore(targetCertId: String?) {
        val db = FirebaseFirestore.getInstance()
        val query = if (targetCertId != null) {
            db.collection("certificates").whereEqualTo("certId", targetCertId).limit(1)
        } else {
            db.collection("certificates").orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
        }

        query.get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                val doc = documents.documents[0]
                currentCertId = doc.getString("certId") ?: "ICERT-2026-00000"
                val rawFileName = doc.getString("fileName") ?: "Internship Certificate"
                currentFileName = rawFileName.substringBeforeLast(".")
                val status = doc.getString("status") ?: "Pending Verification"

                tvCertId.text = currentCertId

                if (status == "Verified") {
                    val qrPayload = "--- INTERNCERT VERIFIED ---\n" +
                            "Recipient: $currentFileName\n" +
                            "ID: $currentCertId\n" +
                            "Status: Official Verified Record"

                    tvVerifyUrl.text = "ID: $currentCertId (Verified)"
                    ivCertificatePreview.setImageBitmap(generateCertificateBitmap(currentFileName, currentCertId, "Status: Official Verified Record"))
                    generateQRCode(qrPayload) // Encodes details directly into QR matrix
                    btnGenerateQR.isEnabled = true
                } else {
                    tvVerifyUrl.text = "Status: Pending Admin Approval"
                    ivCertificatePreview.setImageBitmap(generateCertificateBitmap(currentFileName, currentCertId, "Status: PENDING APPROVAL"))
                    ivQrCode.setImageBitmap(null) // Clear QR code
                    btnGenerateQR.isEnabled = false
                    Toast.makeText(this, "This certificate is awaiting admin verification.", Toast.LENGTH_SHORT).show()
                }
            } else {
                tvCertId.text = "ICERT-2026-XXXXX"
                tvVerifyUrl.text = "No record found"
                btnGenerateQR.isEnabled = false
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateCertificateBitmap(title: String, certId: String, statusText: String): Bitmap {
        val width = 800
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.parseColor("#FDF8F0"))

        val paintBorder = Paint().apply {
            color = Color.parseColor("#C6923B")
            style = Paint.Style.STROKE
            strokeWidth = 12f
        }
        canvas.drawRect(15f, 15f, width - 15f, height - 15f, paintBorder)

        val paintText = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 30f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("CERTIFICATE OF COMPLETION", (width / 2).toFloat(), 90f, paintText)

        paintText.textSize = 18f
        paintText.color = Color.parseColor("#666666")
        canvas.drawText("File: $title", (width / 2).toFloat(), 140f, paintText)

        val boldPaint = Paint(paintText).apply {
            textSize = 24f
            color = Color.parseColor("#C6923B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ID: $certId", (width / 2).toFloat(), 210f, boldPaint)

        paintText.textSize = 20f
        paintText.color = if (statusText.contains("VERIFIED")) Color.parseColor("#2E7D32") else Color.parseColor("#D32F2F")
        canvas.drawText(statusText, (width / 2).toFloat(), 320f, paintText)

        return bitmap
    }

    private fun generateQRCode(text: String) {
        try {
            val writer = MultiFormatWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            ivQrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navUpload).setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navVerify).setOnClickListener {
            startActivity(Intent(this, VerificationActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navQr).setOnClickListener {
            // Already here
        }
       /* findViewById<LinearLayout>(R.id.navEmail).setOnClickListener {
            startActivity(Intent(this, EmailActivity::class.java))
            finish()
        }*/
        findViewById<LinearLayout>(R.id.navEmail).setOnClickListener {
            val intent = Intent(this, EmailActivity::class.java).apply {
                putExtra("CERT_ID", currentCertId)
                putExtra("CERT_NAME", currentFileName)
            }
            startActivity(intent)
            finish()
        }
    }
}