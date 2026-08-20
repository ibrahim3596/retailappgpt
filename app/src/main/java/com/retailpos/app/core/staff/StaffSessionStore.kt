package com.retailpos.app.core.staff

/** Process-local authenticated staff session. It is intentionally cleared when the app process dies. */
object StaffSessionStore {
    @Volatile
    private var session: StaffSession? = null

    fun set(value: StaffSession) {
        session = value
    }

    fun current(): StaffSession? = session

    fun clear() {
        session = null
    }
}
