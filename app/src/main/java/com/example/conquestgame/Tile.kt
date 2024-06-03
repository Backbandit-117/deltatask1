package com.example.conquestgame

class Tile {
    var points: Int = 0
    var player: Int = 0

    fun incrementPoints() {
        points++
    }

    fun reset() {
        points = 0
        player = 0
    }
}
