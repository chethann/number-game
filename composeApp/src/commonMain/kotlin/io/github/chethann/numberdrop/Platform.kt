package io.github.chethann.numberdrop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform