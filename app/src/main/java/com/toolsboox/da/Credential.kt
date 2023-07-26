package com.toolsboox.da

import java.util.*

data class Credential(
    val userId: UUID,
    val username: String,

    val created: Date,
    val lastLogin: Date,

    val accessToken: String,
    val refreshToken: String
)
