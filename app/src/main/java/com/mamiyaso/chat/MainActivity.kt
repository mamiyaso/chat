package com.mamiyaso.chat

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        db = Firebase.firestore

        username = getUsername()

        messageAdapter = MessageAdapter(mutableListOf())
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageEditText.text.clear()
            }
        }

        listenForMessages()
    }

    private fun getUsername(): String {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        var username = sharedPref.getString("username", null)

        if (username == null) {
            val builder = AlertDialog.Builder(this)
            val input = EditText(this)
            builder.setTitle("Kullanıcı Adınızı Girin")
            builder.setView(input)
            builder.setPositiveButton("Tamam") { _, _ ->
                username = input.text.toString()
                with(sharedPref.edit()) {
                    putString("username", username)
                    apply()
                }
            }
            builder.setCancelable(false)
            builder.show()
        }

        return username ?: "Anonim"
    }

    private fun sendMessage(messageText: String) {
        val message = hashMapOf(
            "username" to username,
            "text" to messageText,
            "time" to java.util.Date()
        )

        db.collection("messages")
            .add(message)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Mesaj başarıyla eklendi: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Mesaj eklenirken hata oluştu", e)
            }
    }

    private fun listenForMessages() {
        db.collection("messages")
            .orderBy("time")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Mesajlar dinlenirken hata oluştu", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = mutableListOf<Message>()
                    for (document in snapshot) {
                        val message = Message(
                            document.getString("username") ?: "",
                            document.getString("text") ?: "",
                            document.getTimestamp("time")?.toDate()
                        )
                        messages.add(message)
                    }
                    messageAdapter.updateMessages(messages)
                    messageRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }
}