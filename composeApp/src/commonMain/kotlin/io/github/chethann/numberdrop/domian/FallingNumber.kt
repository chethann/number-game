package io.github.chethann.numberdrop.domian

import androidx.compose.ui.unit.Dp

data class FallingNumber(
    val value: Int,
    val targetColumn: Int = 0,
    val y: Dp,
)