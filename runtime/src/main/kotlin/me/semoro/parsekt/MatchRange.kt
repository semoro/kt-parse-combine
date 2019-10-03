package me.semoro.parsekt

inline class MatchRange(val data: Long) {

    constructor(start: Int, end: Int): this(start.toLong() or (end.toLong() shl 32))
    val start: Int get() = (data and 0xFF_FF_FF_FFL).toInt()
    val end: Int get() = (data ushr 32).toInt()
}