package com.example.interncert

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class EmailActivity : AppCompatActivity() {

    private lateinit var etRecipient: EditText
    private lateinit var etSubject: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSendEmail: Button

    private var certId: String = ""
    private var certName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email)

        etRecipient = findViewById(R.id.etRecipient)
        etSubject = findViewById(R.id.etSubject)
        etMessage = findViewById(R.id.etMessage)
        btnSendEmail = findViewById(R.id.btnSendEmail)

        // Retrieve passed certificate details from the previous activity
        certId = intent.getStringExtra("CERT_ID") ?: "ICERT-2026-XXXXX"
        certName = intent.getStringExtra("CERT_NAME") ?: "Internship Certificate"


        btnSendEmail.setOnClickListener {
            val recipient = etRecipient.text.toString().trim()
            val subject = etSubject.text.toString().trim()
            val message = etMessage.text.toString().trim()

            if (recipient.isEmpty()) {
                Toast.makeText(this, "Please enter recipient email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if it's a valid email format (must contain '@' and '.')
            if (!recipient.contains("@") || !recipient.contains(".")) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            launchEmailClient(recipient, subject, message)
        }
        setupBottomNavigation()
    }

    private fun launchEmailClient(recipient: String, subject: String, message: String) {
        try {
            // 1. Generate the certificate bitmap on the fly
            val bitmap = generateCertificateBitmap(certName, certId, "Status: Official Verified Record")

            // 2. Save bitmap to cache directory securely
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "$certId.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // 3. Get secure content URI using FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            // 4. Create intent targeting Gmail directly with explicit ClipData
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                setPackage("com.google.android.gm")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_STREAM, fileUri)

                // CRITICAL: Explicitly grant read permission via ClipData for targeted packages
                clipData = android.content.ClipData.newUri(contentResolver, "Certificate Attachment", fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                // Fallback to system chooser if Gmail package isn't found
                emailIntent.setPackage(null)
                emailIntent.clipData = null
                startActivity(Intent.createChooser(emailIntent, "Send Email via..."))
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing attachment: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
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
        paintText.color = Color.parseColor("#2E7D32")
        canvas.drawText(statusText, (width / 2).toFloat(), 320f, paintText)

        return bitmap
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
            startActivity(Intent(this, CertificateDetailActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navEmail).setOnClickListener {
            // Already here
        }
    }
}