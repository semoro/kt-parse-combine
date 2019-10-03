package me.semoro.parsekt.ex

interface InstrumentedCopy {
    fun copy(): InstrumentedCopy
}