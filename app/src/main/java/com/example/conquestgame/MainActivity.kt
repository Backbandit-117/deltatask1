package com.example.conquestgame

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var turnIndicator: TextView
    private lateinit var gameMessage: TextView
    private lateinit var resetButton: Button
    private lateinit var player1WinsView: TextView
    private lateinit var player2WinsView: TextView

    private var currentPlayer = 1
    private var firstTurnPlayer1 = true
    private var firstTurnPlayer2 = true
    private var gameOver = false

    private val tiles: Array<Array<Tile>> = Array(5) { Array(5) { Tile() } }

    private var player1Wins = 0
    private var player2Wins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.gridLayout)
        turnIndicator = findViewById(R.id.turnIndicator)
        gameMessage = findViewById(R.id.gameMessage)
        resetButton = findViewById(R.id.resetButton)
        player1WinsView = findViewById(R.id.player1Wins)
        player2WinsView = findViewById(R.id.player2Wins)

        loadWins()
        initializeGrid()
        updateTurnIndicator()

        resetButton.setOnClickListener {
            resetGame()
        }

        showRulesDialog()
    }

    private fun initializeGrid() {
        gridLayout.removeAllViews()
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                val button = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 200
                        height = 200
                        setMargins(5, 5, 5, 5)
                    }
                    tag = "$i,$j"
                    setOnClickListener {
                        handleTileClick(this)
                    }
                }
                gridLayout.addView(button)
            }
        }
    }

    private fun handleTileClick(button: Button) {
        if (gameOver) return

        val coordinates = (button.tag as String).split(",")
        val x = coordinates[0].toInt()
        val y = coordinates[1].toInt()

        val tile = tiles[x][y]
        val player = tile.player
        val points = tile.points

        if (currentPlayer == 1 && (firstTurnPlayer1 || player == 1)) {
            if (firstTurnPlayer1) firstTurnPlayer1 = false
            tile.player = 1
            tile.points = if (points == 0) 3 else points + 1
        } else if (currentPlayer == 2 && (firstTurnPlayer2 || player == 2)) {
            if (firstTurnPlayer2) firstTurnPlayer2 = false
            tile.player = 2
            tile.points = if (points == 0) 3 else points + 1
        } else {
            return
        }

        updateButton(button, tile)
        checkForExpansion()
        checkForWin()

        // Check for a draw if all tiles are filled
        if (!gameOver && isGridFilled()) {
            gameMessage.text = "It's a draw!"
            gameOver = true
        }

        currentPlayer = if (currentPlayer == 1) 2 else 1
        updateTurnIndicator()
    }

    private fun updateButton(button: Button, tile: Tile) {
        val player = tile.player
        val points = tile.points

        button.setBackgroundColor(
            if (player == 1) ContextCompat.getColor(this, R.color.colorPlayer1)
            else if (player == 2) ContextCompat.getColor(this, R.color.colorPlayer2)
            else Color.TRANSPARENT
        )
        button.text = if (points > 0) points.toString() else ""
    }

    private fun checkForExpansion() {
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (tiles[i][j].points >= 4) {
                    expandTile(i, j)
                }
            }
        }
    }

    private fun expandTile(x: Int, y: Int) {
        val tile = tiles[x][y]
        val player = tile.player

        tile.reset()
        updateButton(gridLayout.getChildAt(x * 5 + y) as Button, tile)

        val directions = arrayOf(
            intArrayOf(0, -1), intArrayOf(0, 1),
            intArrayOf(-1, 0), intArrayOf(1, 0)
        )

        for (direction in directions) {
            val newX = x + direction[0]
            val newY = y + direction[1]

            if (newX in 0 until 5 && newY in 0 until 5) {
                val adjacentTile = tiles[newX][newY]
                if (adjacentTile.player != player) {
                    adjacentTile.player = player
                    adjacentTile.points = 1
                } else {
                    adjacentTile.incrementPoints()
                }
                updateButton(gridLayout.getChildAt(newX * 5 + newY) as Button, adjacentTile)
                if (adjacentTile.points >= 4) {
                    expandTile(newX, newY)
                }
            }
        }
    }

    private fun checkForWin() {
        var player1Tiles = 0
        var player2Tiles = 0

        for (i in 0 until 5) {
            for (j in 0 until 5) {
                when (tiles[i][j].player) {
                    1 -> player1Tiles++
                    2 -> player2Tiles++
                }
            }
        }

        if (player1Tiles == 0 && player2Tiles > 1) {
            gameMessage.text = "Player 2 wins!"
            player2Wins++
            saveWins()
            updateWinsDisplay()
            showGameOverDialog("Player 2 wins!")
            gameOver = true
        } else if (player2Tiles == 0 && player1Tiles > 1) {
            gameMessage.text = "Player 1 wins!"
            player1Wins++
            saveWins()
            updateWinsDisplay()
            showGameOverDialog("Player 1 wins!")
            gameOver = true
        }
    }

    private fun isGridFilled(): Boolean {
        for (row in tiles) {
            for (tile in row) {
                if (tile.player == 0) {
                    return false
                }
            }
        }
        return true
    }

    private fun updateTurnIndicator() {
        turnIndicator.text = if (currentPlayer == 1) "Player 1's Turn" else "Player 2's Turn"
        turnIndicator.setBackgroundColor(
            if (currentPlayer == 1) ContextCompat.getColor(this, R.color.colorPlayer1)
            else ContextCompat.getColor(this, R.color.colorPlayer2)
        )
    }

    private fun resetGame() {
        tiles.forEach { row ->
            row.forEach { tile ->
                tile.reset()
            }
        }
        gameMessage.text = ""
        currentPlayer = 1
        firstTurnPlayer1 = true
        firstTurnPlayer2 = true
        gameOver = false
        initializeGrid()
        updateTurnIndicator()
    }

    private fun loadWins() {
        val sharedPref = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        player1Wins = sharedPref.getInt("player1_wins", 0)
        player2Wins = sharedPref.getInt("player2_wins", 0)
        updateWinsDisplay()
    }

    private fun saveWins() {
        val sharedPref = getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("player1_wins", player1Wins)
            putInt("player2_wins", player2Wins)
            apply()
        }
    }

    private fun updateWinsDisplay() {
        player1WinsView.text = "Player 1 Wins: $player1Wins"
        player2WinsView.text = "Player 2 Wins: $player2Wins"
    }

    private fun showRulesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Game Rules")
            .setMessage(
                "1st Turn: Players can choose any tile on the grid and get 3 points.\n" +
                        "Subsequent Turns: Click on your colored tile to add 1 point.\n" +
                        "Conquest and Expansion: When a tile reaches 3 points, it expands to adjacent tiles.\n" +
                        "Objective: Eliminate the opponent's color from the grid."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showGameOverDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
