package me.semoro.parsekt

import java.util.*
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


fun defCSet(vararg char: Char): BitSet = BitSet().apply { for(c in char) { set(c.toInt()) } }

class TraceGrammar(source: DataSource) : GrammarMatcher(source) {

    val pChars = defCSet('[', '/', ';', '.')

    suspend inline fun name(): MatchRange {
        return requireMultipleLazy(cmin = 1) {
            requireOnceR {
                !pChars[it.toInt()]
            }
        }
    }

    suspend inline fun type() = ranged {
        c('I')
    }

    suspend inline fun signature(): String {
        c('(')
        val argumentTypes = requireMultiple { type() }
        c(')')
        val resultType = type()
        return with(source) {
            "("+argumentTypes.joinToString { it.text() } + "): ${resultType.text()}"
        }
    }

    suspend inline fun frame(): String {
        val name = name()
        val sig = requireOptional { signature() }
        c(';')


        return with(source) {
            "'${name.text()}', '$sig'"
        }
        // name?sig?
    }

    suspend inline fun trace(): String {
        return requireMultiple { frame() }.joinToString { "($it)" }
    }

    fun matchTrace() = match {
        val res = trace()
        res
    }
}

// ([a-z])+


@ExperimentalTime
class StatCounter {
    var min: Duration? = null
    var max: Duration? = null
    var sum: Double = 0.0
    var count = 0
    val ave get() = sum / count

    fun accept(duration: Duration) {
        min = minOf(min ?: Duration.INFINITE, duration)
        max = maxOf(max ?: Duration.ZERO, duration)
        sum += duration.inMilliseconds
        count++
    }

    operator fun plusAssign(duration: Duration) {
        accept(duration)
    }
}

@ExperimentalTime
fun main() {

    val pattern = "(([^/.\\[;]+?)(\\([I]+\\)[I])?;)+".toRegex()

    val str = "Wtfkek (II)I(II)I; kekekek; lol(IIII)I;"
    // 'Wtfkek (II)I', '(I, I): I'

    var count = 0
    var count2 = 0
    var timeTotal = 0L
    var timeReg = 0L
    val pass = 1

    val collector = StatCounter()

    repeat(pass) {
        collector += measureTime {
            with(TraceGrammar((StrDataSource(str)))) {
                val matchTrace = matchTrace()
                count += matchTrace.length
                if (pass == 1) println(matchTrace)
            }
        }
//        timeReg += measureNanoTime {
//            val matchEntire = pattern.matchEntire(str)
//            require(matchEntire != null)
//            count2 += matchEntire.groupValues[0].length
//        }
    }

    println(perClassCache.size)



    println(count)
    println(count2)
    println("Time is ${collector.ave} ms, (${collector.min!!.inMilliseconds}/${collector.max!!.inMilliseconds}), ${timeReg / pass * 1e-6} ms")



}