package com.androdevlinux.satschat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform