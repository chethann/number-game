package io.github.chethann.numberdrop

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chethann.numberdrop.domian.Cell
import io.github.chethann.numberdrop.domian.FallingNumber
import io.github.chethann.numberdrop.domian.GameState
import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2

const val HORIZONTAL_GRIDS = 5
const val VERTICAL_GRIDS = 6

@Composable
fun NumberDropLayout(
    modifier: Modifier = Modifier,
    gridSize: Dp = 360.dp,
    tileMargin: Dp = 4.dp,
    tileRadius: Dp = 4.dp,
    gameStateStore: KStore<GameState>? = null
) {

    val density = LocalDensity.current
    val tileSize = ((gridSize - tileMargin * (HORIZONTAL_GRIDS - 1)) / HORIZONTAL_GRIDS).coerceAtLeast(0.dp)
    val tileOffsetDp = tileSize + tileMargin
    val tileOffset = with(density) { tileOffsetDp.toPx() }
    var isInFastMode by remember { mutableStateOf(false) }
    var swipeAngle by remember { mutableDoubleStateOf(0.0) }
    var score by remember { mutableStateOf(0) }
    var pause by remember { mutableStateOf(false) }
    var showSaveState by remember { mutableStateOf(true) }
    var isInitialised by remember { mutableStateOf(false) }

    val emptyTileColor = getEmptyTileColor(isSystemInDarkTheme())

    var isGameOver by remember { mutableStateOf(false) }
    var currentCellValues by remember { mutableStateOf(listOf<Cell>()) }
    var fallingNumber by remember { mutableStateOf<FallingNumber?>(null) }
    var nextSetOfFallingCells by remember { mutableStateOf(mutableListOf<Cell>()) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(gameStateStore) {
        if (gameStateStore == null) {
            isInitialised = true
            return@LaunchedEffect
        }

        val storeValue = withContext(Dispatchers.Default) { // todo: Move to IO dispatcher
            gameStateStore.get()
        }

        if (storeValue == null) {
            isInitialised = true
            return@LaunchedEffect
        } else {
            storeValue.let { gameState ->
                if (gameState.fallingNumberValue != -1 && gameState.fallingNumberValue != 0) {
                    fallingNumber = FallingNumber(
                        value = gameState.fallingNumberValue,
                        targetColumn = gameState.fallingNumberColumn,
                        y = with(density) { gameState.fallingNumberY.toDp() }
                    )
                    currentCellValues = gameState.cells
                    pause = true
                    isInitialised = true
                    score = gameState.score
                } else {
                    fallingNumber = null
                    score = 0
                    pause = false
                    isInitialised = true
                }
                showSaveState = false
            }
        }
    }

    fun resetStore() {
        CoroutineScope(Dispatchers.Default).launch {
            gameStateStore?.reset()
        }
    }

    LaunchedEffect(pause, isInitialised) {
        if(!pause && isInitialised) {
            println("resetStore called")
            resetStore()
        }
    }

    if (isGameOver) {
        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("game over")
            Text("Score $score")
            Text("You can do better, start over!")
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                isGameOver = false
                currentCellValues = listOf()
                fallingNumber = null
                score = 0
                pause = false
            }) {
                Text("Start Again")
            }
        }
        return
    }

    if (!isInitialised) {
        Text("Initialising!!!")
        return
    }

    fun saveState() {
        val fallingNumberY = with(density) { fallingNumber?.y?.toPx() }
        CoroutineScope(Dispatchers.Default).launch {
            gameStateStore?.set(
                GameState(
                    cells = currentCellValues,
                    score = score,
                    bestScore = -1,
                    fallingNumberValue = fallingNumber?.value ?: -1,
                    fallingNumberY = fallingNumberY?.toInt() ?: -1,
                    fallingNumberColumn = fallingNumber?.targetColumn ?: -1
                )
            )
        }
    }

    fun handleDirectionInput(direction: Direction?) {
        if (pause || !isInitialised) return
        if (direction == Direction.RIGHT) {
            // The current y position of falling item should be less that the top item of the right column, else ignore

            if ((fallingNumber?.targetColumn ?: Int.MAX_VALUE) < HORIZONTAL_GRIDS - 1) {

                val occupiedCellsInRightColumn =
                    currentCellValues.filter { it.column - 1 == fallingNumber?.targetColumn }

                val minRow = if (occupiedCellsInRightColumn.isNotEmpty()) {
                    occupiedCellsInRightColumn.minOf { it.row } - 1
                } else {
                    VERTICAL_GRIDS - 1
                }

                // disable key event if current y of fallingNumber is below some row with a number
                if (tileOffsetDp.times(minRow).compareTo(fallingNumber?.y ?: 0.dp) < 0) {
                    return
                }

                fallingNumber = fallingNumber?.copy(
                    targetColumn = fallingNumber!!.targetColumn + 1
                )
            }
        } else if (direction == Direction.LEFT) {
            if ((fallingNumber?.targetColumn ?: Int.MIN_VALUE) > 0) {

                val occupiedCellsInLeftColumn =
                    currentCellValues.filter { it.column + 1 == fallingNumber?.targetColumn }

                val minRow = if (occupiedCellsInLeftColumn.isNotEmpty()) {
                    occupiedCellsInLeftColumn.minOf { it.row } - 1
                } else {
                    VERTICAL_GRIDS - 1
                }

                // disable key event if current y of fallingNumber is below some row with a number
                if (tileOffsetDp.times(minRow).compareTo(fallingNumber?.y ?: 0.dp) < 0) {
                    return
                }

                fallingNumber = fallingNumber?.copy(
                    targetColumn = fallingNumber!!.targetColumn - 1
                )
            }
        } else if (direction == Direction.DOWN) {
            if (fallingNumber != null) {
                val occupiedCellsInTargetColumn =
                    currentCellValues.filter { it.column == fallingNumber?.targetColumn }
                val targetRow = if (occupiedCellsInTargetColumn.isNotEmpty()) {
                    occupiedCellsInTargetColumn.minOf { it.row } - 1
                } else {
                    VERTICAL_GRIDS - 1
                }
                fallingNumber = fallingNumber?.copy(
                    y = tileOffsetDp.times(targetRow)
                )
            }
        }
    }


    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = (-50).dp) // Move up by 50dp
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown) {
                    handleDirectionInput(it.direction)
                }
                return@onKeyEvent true
            }.then(
                if (shouldDetectSwipes()) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                swipeAngle =
                                    with(dragAmount) { (atan2(-y, x) * 180 / PI + 360) % 360 }
                            },
                            onDragEnd = {
                                val direction = when {
                                    45 <= swipeAngle && swipeAngle < 135 -> Direction.UP
                                    135 <= swipeAngle && swipeAngle < 225 -> Direction.LEFT
                                    225 <= swipeAngle && swipeAngle < 315 -> Direction.DOWN
                                    else -> Direction.RIGHT
                                }
                                handleDirectionInput(direction)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .focusRequester(focusRequester)
            .focusable()
    ) {

        Score(score)
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = {
                pause = !pause
                showSaveState = true
            }) {
                Text(if (pause) "Play" else "Pause")
            }

            if (pause && showSaveState) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    saveState()
                }) {
                    Text("save state")
                }
            }
        }



        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = modifier
                .width(gridSize)
                .height(gridSize.times(VERTICAL_GRIDS).div(HORIZONTAL_GRIDS))
                .drawBehind {
                    // Draw the background empty tiles.
                    for (row in 0 until VERTICAL_GRIDS) {
                        for (col in 0 until HORIZONTAL_GRIDS) {
                            drawRoundRect(
                                color = emptyTileColor,
                                topLeft = Offset(col * tileOffset, row * tileOffset),
                                size = Size(tileSize.toPx(), tileSize.toPx()),
                                cornerRadius = CornerRadius(tileRadius.toPx()),
                            )
                        }
                    }
                },
        ) {

            // This block generates falling numbers
            LaunchedEffect(fallingNumber, currentCellValues, pause, isInitialised) {
                if (pause || !isInitialised) return@LaunchedEffect
                if (currentCellValues.filter { it.column == 2 }.size == VERTICAL_GRIDS) {
                    isGameOver = true
                }

                if (fallingNumber == null) {
                    if (nextSetOfFallingCells.isNotEmpty()) {
                        val cell = nextSetOfFallingCells.removeFirst()

                        currentCellValues = currentCellValues.filter { it != cell }

                        fallingNumber = FallingNumber(
                            y = tileOffsetDp.times(cell.row),
                            targetColumn = cell.column,
                            value = cell.number
                        )
                        isInFastMode = true
                        return@LaunchedEffect
                    }
                    fallingNumber = FallingNumber(
                        y = 0.dp,
                        targetColumn = 2,
                        value = listOf(2, 4, 8, 16).random()
                    )
                    isInFastMode = false
                }
            }


            // This block moves things
            LaunchedEffect(fallingNumber, isInFastMode, pause, isInitialised) {
                if (pause || !isInitialised) return@LaunchedEffect

                // just the column logic is not enough when the numbers fall from in between
                val occupiedCellsInTargetColumn = currentCellValues.filter {
                    it.column == fallingNumber?.targetColumn &&
                            tileOffsetDp.times(it.row) > (fallingNumber?.y ?: 0.dp)
                }
                val targetRow = if (occupiedCellsInTargetColumn.isNotEmpty()) {
                    occupiedCellsInTargetColumn.minOf { it.row } - 1
                } else {
                    VERTICAL_GRIDS - 1
                }

                while (fallingNumber != null && (fallingNumber?.y?.compareTo(
                        tileOffsetDp.times(
                            targetRow
                        )
                    ) ?: 0) < 0
                ) {
                    withFrameMillis {
                        fallingNumber = fallingNumber?.copy(
                            y = fallingNumber!!.y.plus(if (isInFastMode) 4.dp else 1.dp)
                        )
                    }
                }

                val sameColumnBelowItem =
                    currentCellValues.filter { it.column == fallingNumber?.targetColumn && it.row == targetRow + 1 && it.number == fallingNumber?.value }
                        .getOrNull(0)
                val sameRowLeftItem =
                    currentCellValues.filter { it.row == targetRow && it.number == fallingNumber?.value && it.column + 1 == fallingNumber?.targetColumn }
                        .getOrNull(0)
                val sameRowRightItem =
                    currentCellValues.filter { it.row == targetRow && it.number == fallingNumber?.value && it.column - 1 == fallingNumber?.targetColumn }
                        .getOrNull(0)

                val matchingList =
                    listOf(sameColumnBelowItem, sameRowLeftItem, sameRowRightItem).filterNotNull()

                if (sameRowLeftItem != null) {
                    nextSetOfFallingCells.addAll(currentCellValues.filter { it.column == sameRowLeftItem.column && it.row < sameRowLeftItem.row })
                }
                if (sameRowRightItem != null) {
                    nextSetOfFallingCells.addAll(currentCellValues.filter { it.column == sameRowRightItem.column && it.row < sameRowRightItem.row }
                        .sortedByDescending { it.row })
                }

                if (matchingList.isNotEmpty()) {
                    currentCellValues = currentCellValues.filter { !matchingList.contains(it) }
                    val matchingListSize = matchingList.size
                    val multiplier =
                        if (matchingListSize == 1) 2 else if (matchingListSize == 2) 4 else 8
                    fallingNumber = fallingNumber?.copy(
                        value = fallingNumber!!.value * multiplier,
                        targetColumn = fallingNumber!!.targetColumn
                    )
                    fallingNumber?.let {
                        score += it.value
                    }
                    isInFastMode = true

                } else {
                    fallingNumber?.let {
                        currentCellValues = currentCellValues + Cell(
                            row = targetRow,
                            column = it.targetColumn,
                            number = it.value
                        )
                    }

                    fallingNumber = null
                }
            }

            currentCellValues.forEach {
                CellWithNumber(
                    it,
                    modifier = Modifier
                        .offset(
                            x = tileOffsetDp.times(it.column),
                            y = tileOffsetDp.times(it.row)
                        ),
                    tileColor = getTileColor(it.number, false),
                    tileSize = tileSize,
                    tileRadius = tileRadius,
                )
            }

            if (fallingNumber != null) {
                FallingNumber(
                    fallingNumber!!,
                    modifier = Modifier
                        .offset(
                            x = tileOffsetDp.times(fallingNumber!!.targetColumn),
                            y = fallingNumber!!.y
                        ),
                    tileColor = getTileColor(fallingNumber!!.value, false),
                    tileSize = tileSize,
                    tileRadius = tileRadius,
                )
            }

        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CellWithNumber(
    cell: Cell, modifier: Modifier,
    tileSize: Dp = 80.dp,
    fontSize: TextUnit = 24.sp,
    tileRadius: Dp = 4.dp,
    tileColor: Color = Color.Black,
    fontColor: Color = Color.White
) {
    Text(
        text = cell.number.toString(),
        modifier = modifier
            .background(tileColor, RoundedCornerShape(tileRadius))
            .size(tileSize)
            .wrapContentSize(),
        color = fontColor,
        fontSize = fontSize,
    )
}

@Composable
fun FallingNumber(
    fallingNumber: FallingNumber, modifier: Modifier,
    tileSize: Dp = 80.dp,
    fontSize: TextUnit = 24.sp,
    tileRadius: Dp = 4.dp,
    tileColor: Color = Color.Black,
    fontColor: Color = Color.White
) {
    Text(
        text = fallingNumber.value.toString(),
        modifier = modifier
            .background(tileColor, RoundedCornerShape(tileRadius))
            .size(tileSize)
            .wrapContentSize(),
        color = fontColor,
        fontSize = fontSize,
    )
}

@Composable
fun Score(score: Int,
          modifier: Modifier = Modifier,
          fontSize: TextUnit = 24.sp) {

    Crossfade(targetState = score.toString()) { currentText ->
        Text(
            text = score.toString(),
            modifier = modifier,
            fontSize = fontSize
        )
    }
}

fun getTileColor(num: Int, isDarkTheme: Boolean): Color {
    return when (num) {
        2 -> Color(if (isDarkTheme) 0xff4e6cef else 0xff50c0e9)
        4 -> Color(if (isDarkTheme) 0xff3f51b5 else 0xff1da9da)
        8 -> Color(if (isDarkTheme) 0xff8e24aa else 0xffcb97e5)
        16 -> Color(if (isDarkTheme) 0xff673ab7 else 0xffb368d9)
        32 -> Color(if (isDarkTheme) 0xffc00c23 else 0xffff5f5f)
        64 -> Color(if (isDarkTheme) 0xffa80716 else 0xffe92727)
        128 -> Color(if (isDarkTheme) 0xff0a7e07 else 0xff92c500)
        256 -> Color(if (isDarkTheme) 0xff056f00 else 0xff7caf00)
        512 -> Color(if (isDarkTheme) 0xffe37c00 else 0xffffc641)
        1024 -> Color(if (isDarkTheme) 0xffd66c00 else 0xffffa713)
        2048 -> Color(if (isDarkTheme) 0xffcf5100 else 0xffff8a00)
        4096 -> Color(if (isDarkTheme) 0xff80020a else 0xffcc0000)
        8192 -> Color(if (isDarkTheme) 0xff303f9f else 0xff0099cc)
        16384 -> Color(if (isDarkTheme) 0xff512da8 else 0xff9933cc)
        else -> Color.Black
    }
}

fun getEmptyTileColor(isDarkTheme: Boolean): Color {
    return Color(if (isDarkTheme) 0xff444444 else 0xffdddddd)
}

internal expect fun shouldDetectSwipes(): Boolean
