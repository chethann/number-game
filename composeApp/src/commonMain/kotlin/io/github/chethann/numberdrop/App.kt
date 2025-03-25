package io.github.chethann.numberdrop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chethann.numberdrop.domian.GameState
import io.github.xxfast.kstore.KStore

@Composable
fun App(gameStateStore: KStore<GameState>? = null) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NumberDropLayout(
                gridSize = 360.dp,
                gameStateStore = gameStateStore
            )
        }
    }
}