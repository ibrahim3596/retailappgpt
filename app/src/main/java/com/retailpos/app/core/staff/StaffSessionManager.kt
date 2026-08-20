package com.retailpos.app.core.staff

/** Process-local session holder. Do not persist PINs or authenticated sessions to plain preferences. */
class StaffSessionManager {
    var current: StaffSession? = null
        private set

    fun signIn(session: StaffSession) {
        current = session
    }

    fun signOut() {
        current = null
    }

    fun requireSession(): StaffSession = current ?: error("No staff is signed in.")
}
