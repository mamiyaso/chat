package com.mamiyaso.chat

import java.util.Date

data class Message(
    val username: String = "",
    val text: String = "",
    val time: Date? = null
)