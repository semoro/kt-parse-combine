package me.semoro.parsekt.bench

import me.semoro.parsekt.StrDataSource
import me.semoro.parsekt.TraceGrammar
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

const val input = "Wtfkek (II)I(II)I; kekekek; lol(IIII)I;"

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class SimpleBenchmark {

    val source = StrDataSource(input)
    val grammar = TraceGrammar(source)
    @Benchmark
    fun benchmarkTrace(bh: Blackhole) {
        reset()
        val res = grammar.matchTrace()
        bh.consume(res)
    }


    fun reset() {
        source.setLimit(Int.MAX_VALUE)
        source.seekAbs(0)
    }
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class PatternBenchmark {

    val pattern = "(([^/.\\[;]+?)(\\([I]+\\)[I])?;)+".toRegex()

    @Benchmark
    fun benchmarkPattern(bh: Blackhole) {
        bh.consume(pattern.matchEntire(input)!!.groupValues[0])
    }
}