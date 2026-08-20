package com.retailpos.app.core.staff

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StaffSessionStoreTest {
    @After
    fun tearDown() {
        StaffSessionStore.clear()
    }

    @Test
    fun storesAndClearsAuthenticatedSession() {
        val session = StaffSession("staff-1", "Cashier", StaffRole.CASHIER, 100L)
        StaffSessionStore.set(session)

        assertEquals(session, StaffSessionStore.current())

        StaffSessionStore.clear()
        assertNull(StaffSessionStore.current())
    }
}
