package com.aj.wafatti.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    private var connectedListener: ValueEventListener? = null

    fun initialize() {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                startPresence()
            } else {
                stopPresence()
            }
        }
    }

    private fun startPresence() {
        stopPresence()

        val connectedRef = database.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    val currentUid = firebaseAuth.currentUser?.uid ?: return
                    val userPresenceRef = database.getReference("presence/$currentUid")
                    val connectionRef = userPresenceRef.child("connections").push()
                    val lastSeenRef = userPresenceRef.child("lastSeen")

                    // 1. Register onDisconnect BEFORE marking connection online
                    connectionRef.onDisconnect().removeValue()
                    lastSeenRef.onDisconnect().setValue(ServerValue.TIMESTAMP)

                    // 2. Set connection to true
                    connectionRef.setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        connectedListener = listener
        connectedRef.addValueEventListener(listener)
    }

    private fun stopPresence() {
        connectedListener?.let {
            try {
                database.getReference(".info/connected").removeEventListener(it)
            } catch (_: Exception) {}
            connectedListener = null
        }
    }
}
