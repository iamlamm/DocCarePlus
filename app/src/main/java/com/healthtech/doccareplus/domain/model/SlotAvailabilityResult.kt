package com.healthtech.doccareplus.domain.model

sealed class SlotAvailabilityResult {
    object Available : SlotAvailabilityResult()
    object Unavailable : SlotAvailabilityResult()
    object AlreadyBookedByCurrentUser : SlotAvailabilityResult()
    object AlreadyBookedByOther : SlotAvailabilityResult()
}