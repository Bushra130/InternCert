package com.example.interncert

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UploadActivity : AppCompatActivity() {

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = "No file selected"
    private lateinit var listViewCertificates: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        listViewCertificates = findViewById(R.id.listViewCertificates)
        val btnSelectFile = findViewById<Button>(R.id.btnSelectFile)
        val btnUploadTemplate = findViewById<Button>(R.id.btnUploadTemplate)

        loadAllUploads()

        // File picker launcher
        val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedFileUri = uri
                    selectedFileName = getFileName(uri)
                    Toast.makeText(this, "Selected: $selectedFileName", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Tap to choose file
        btnSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpeg", "application/pdf"))
            }
            filePickerLauncher.launch(intent)
        }

        // Upload metadata to Firestore
        btnUploadTemplate.setOnClickListener {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val certId = "ICERT-2026-" + (10000..99999).random()
            val certificateData = hashMapOf(
                "certId" to certId,
                "fileName" to selectedFileName,
                "status" to "Pending Verification",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("certificates").document(certId)
                .set(certificateData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Uploaded! ID: $certId", Toast.LENGTH_LONG).show()
                    loadAllUploads() // Refresh the list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        setupBottomNavigation()
    }

    private fun loadAllUploads() {
        val db = FirebaseFirestore.getInstance()
        db.collection("certificates")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val certificateList = mutableListOf<String>()
                for (doc in documents) {
                    val fileName = doc.getString("fileName") ?: "File"
                    val certId = doc.getString("certId") ?: ""
                    certificateList.add("$fileName ($certId)")
                }

                if (certificateList.isNotEmpty()) {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, certificateList)
                    listViewCertificates.adapter = adapter

                    listViewCertificates.setOnItemClickListener { _, _, position, _ ->
                        val selectedText = certificateList[position]
                        val certId = selectedText.substringAfter("(").substringBefore(")")
                        val intent = Intent(this, CertificateDetailActivity::class.java).apply {
                            putExtra("CERT_ID", certId)
                            if (selectedFileUri != null) {
                                putExtra("IMAGE_URI", selectedFileUri.toString())
                            }
                        }
                        startActivity(intent)
                    }
                } else {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("No uploads yet"))
                    listViewCertificates.adapter = adapter
                }
            }
    }

    private fun getFileName(uri: Uri): String {
        var name = "certificate_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navUpload).setOnClickListener {
            // Already here
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
            startActivity(Intent(this, EmailActivity::class.java))
            finish()
        }
    }
}