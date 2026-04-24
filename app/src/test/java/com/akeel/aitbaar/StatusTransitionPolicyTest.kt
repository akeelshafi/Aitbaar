package com.akeel.aitbaar

import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.StatusTransitionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusTransitionPolicyTest {

    @Test
    fun `pending can move to accepted or rejected`() {
        assertTrue(StatusTransitionPolicy.canTransition(Status.PENDING, Status.ACCEPTED))
        assertTrue(StatusTransitionPolicy.canTransition(Status.PENDING, Status.REJECTED))
    }

    @Test
    fun `pending cannot move directly to paid`() {
        assertFalse(StatusTransitionPolicy.canTransition(Status.PENDING, Status.PAID))
    }

    @Test
    fun `accepted can move to paid only`() {
        assertTrue(StatusTransitionPolicy.canTransition(Status.ACCEPTED, Status.PAID))
        assertFalse(StatusTransitionPolicy.canTransition(Status.ACCEPTED, Status.REJECTED))
    }

    @Test
    fun `rejected and paid are terminal`() {
        assertFalse(StatusTransitionPolicy.canTransition(Status.REJECTED, Status.ACCEPTED))
        assertFalse(StatusTransitionPolicy.canTransition(Status.PAID, Status.ACCEPTED))
    }
}
