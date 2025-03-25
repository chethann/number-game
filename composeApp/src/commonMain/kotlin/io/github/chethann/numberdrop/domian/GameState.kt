package io.github.chethann.numberdrop.domian

import kotlinx.serialization.Serializable

const val GAME_STATE_FILE_NAME = "game_state.json"

@Serializable
data class GameState(
    val cells: List<Cell>,
    val score: Int,
    val bestScore: Int,
    val fallingNumberValue: Int,
    val fallingNumberY: Int,
    val fallingNumberColumn: Int
) {
    companion object {
        val EMPTY_OBJECT = GameState(
            cells = listOf(),
            score = 0,
            bestScore = 0,
            fallingNumberValue = 0,
            fallingNumberY = 0,
            fallingNumberColumn = 0
        )
    }
}
