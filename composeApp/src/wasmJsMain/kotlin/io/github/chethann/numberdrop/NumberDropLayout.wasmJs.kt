package io.github.chethann.numberdrop

import kotlinx.browser.window

internal actual fun shouldDetectSwipes(): Boolean {
    return window.navigator.maxTouchPoints > 0
}