package com.example.interncert

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class VerificationActivity : AppCompatActivity() {

    private lateinit var listViewVerification: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        listViewVerification = findViewById(R.id.listViewVerification)
        loadPendingCertificates()
        setupBottomNavigation()
    }

    private fun loadPendingCertificates() {
        val db = FirebaseFirestore.getInstance()
        db.collection("certificates")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val docsList = documents.documents

                if (docsList.isNotEmpty()) {
                    val adapter = VerificationAdapter(docsList) { docId, newStatus ->
                        db.collection("certificates").document(docId)
                            .update("status", newStatus)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Certificate marked as $newStatus", Toast.LENGTH_SHORT).show()
                                loadPendingCertificates() // Refresh list
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    listViewVerification.adapter = adapter
                } else {
                    listViewVerification.adapter = null
                    Toast.makeText(this, "No certificates found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    inner class VerificationAdapter(
        private val list: List<DocumentSnapshot>,
        private val onActionClick: (String, String) -> Unit
    ) : BaseAdapter() {

        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context)
                .inflate(R.layout.item_verification, parent, false)

            val doc = list[position]
            val docId = doc.id
            val fileName = doc.getString("fileName") ?: "File"
            val certId = doc.getString("certId") ?: ""
            val status = doc.getString("status") ?: "Pending"

            val tvFileName = view.findViewById<TextView>(R.id.tvFileName)
            val tvCertId = view.findViewById<TextView>(R.id.tvCertId)
            val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
            val btnApprove = view.findViewById<Button>(R.id.btnApprove)
            val btnReject = view.findViewById<Button>(R.id.btnReject)

            tvFileName.text = fileName
            tvCertId.text = "ID: $certId"
            tvStatus.text = "Status: $status"

            btnApprove.setOnClickListener {
                onActionClick(docId, "Verified")
            }

            btnReject.setOnClickListener {
                onActionClick(docId, "Rejected")
            }

            return view
        }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.navUpload).setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navVerify).setOnClickListener {
            // Already here
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