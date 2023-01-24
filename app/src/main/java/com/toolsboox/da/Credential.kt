package com.toolsboox.da

import java.util.*

data class Credential(
    val userId: UUID,

    val created: Date,
    val lastLogin: Date
)
