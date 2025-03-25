package io.github.chethann.numberdrop.domian

import kotlinx.serialization.Serializable

@Serializable
data class Cell(
    val row: Int,
    val column: Int,
    val number: Int
)