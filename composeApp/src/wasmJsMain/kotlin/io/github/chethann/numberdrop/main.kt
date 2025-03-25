package io.github.chethann.numberdrop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.chethann.numberdrop.domian.GAME_STATE_FILE_NAME
import io.github.chethann.numberdrop.domian.GameState
import io.github.xxfast.kstore.storage.storeOf
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val store = storeOf(key = GAME_STATE_FILE_NAME, default = GameState.EMPTY_OBJECT)

    ComposeViewport(document.body!!) {
        App(store)
    }
}