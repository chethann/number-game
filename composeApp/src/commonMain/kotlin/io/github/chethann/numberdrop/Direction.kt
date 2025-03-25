package io.github.chethann.numberdrop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

enum class Direction { UP, DOWN, RIGHT, LEFT }

val KeyEvent.direction: Direction?
    get() = when (key) {
        Key.DirectionUp, Key.W -> Direction.UP
        Key.DirectionLeft, Key.A -> Direction.LEFT
        Key.DirectionDown, Key.S -> Direction.DOWN
        Key.DirectionRight, Key.D -> Direction.RIGHT
        else -> null
}