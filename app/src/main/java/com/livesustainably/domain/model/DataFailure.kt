package com.livesustainably.domain.model

enum class DataFailure {
    SignedOut, PermissionDenied, Network, InvalidData, DemoNotConfigured, Unknown
}

class DataException(val reason: DataFailure, cause: Throwable? = null) : Exception(reason.name, cause)
