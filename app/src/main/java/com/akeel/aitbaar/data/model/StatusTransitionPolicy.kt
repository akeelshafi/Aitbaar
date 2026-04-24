package com.akeel.aitbaar.data.model

object StatusTransitionPolicy {

    private val allowedTransitions = mapOf(
        Status.PENDING to setOf(Status.ACCEPTED, Status.REJECTED),
        Status.ACCEPTED to setOf(Status.PAID),
        Status.REJECTED to emptySet(),
        Status.PAID to emptySet()
    )

    fun canTransition(from: Status, to: Status): Boolean {
        return allowedTransitions[from]?.contains(to) == true
    }
}
