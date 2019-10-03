package me.semoro.parsekt


class StrDataSource(val sequence: CharSequence) : DataSource() {
    private var pos = 0
    private var limit = Int.MAX_VALUE
    override fun seekAbs(position: Int) {
        pos = position
    }

    override fun position(): Int {
        return pos
    }

    override fun advance() {
        pos++
    }

    override fun hasNext(): Boolean {
        return pos < sequence.length && pos < limit
    }

    override fun next(): Char {
        return sequence[pos++]
    }

    override fun peek(): Char {
        return sequence[pos]
    }

    override fun MatchRange.text(): CharSequence {
        return sequence.subSequence(this.start, this.end)
    }

    override fun setLimit(limit: Int) {
        this.limit = limit
    }

}



class DebugDataSource(
    private val delegate: StrDataSource,
    private val debugPeek: Boolean = true,
    private val debugNext: Boolean = true,
    private val debugAdvance: Boolean = true,
    private val debugSeek: Boolean = true
): DataSource() {

    private fun printlnState() {
        println()
        for (trace in Thread.currentThread().stackTrace.drop(2)) {
            print("at ")
            println(trace)
        }

        println("---------------------------")
    }

    override fun seekAbs(position: Int) {

        if (debugSeek) {
            println(delegate.sequence)
            drawUnderline(position(), position)
            printlnState()
        }
        return delegate.seekAbs(position)

    }

    override fun position(): Int {
        return delegate.position()
    }

    fun drawUnderline(from: Int, to: Int) {
        print(" ".repeat(minOf(to, from)))
        when {
            from > to -> {
                print("┖")
                print("─".repeat(from - to - 1))
                println("┘")
            }
            from < to -> {
                print("└")
                print("─".repeat(to - from - 1))
                println("┚")
            }
            else -> {
                println("↺")
            }
        }
    }

    override fun next(): Char {
        if (debugNext) {
            println(delegate.sequence)
            drawUnderline(position(), position() + 1)
            printlnState()
        }
        return delegate.next()
    }

    override fun advance() {
        if (debugAdvance) {
            println(delegate.sequence)
            drawUnderline(position(), position() + 1)
            printlnState()
        }
        return delegate.advance()
    }

    override fun peek(): Char {
        if (debugPeek) {
            println(delegate.sequence)
            print(" ".repeat(position()))
            println("^")
            printlnState()
        }
        return delegate.peek()
    }

    override fun hasNext(): Boolean {
        return delegate.hasNext()
    }

    override fun setLimit(limit: Int) {
        delegate.setLimit(limit)
    }

    override fun MatchRange.text(): CharSequence {
        return with(delegate) { text() }
    }

}


abstract class RangeAccess {
    abstract fun MatchRange.text(): CharSequence
}

abstract class DataSource : RangeAccess() {
    abstract fun seekAbs(position: Int)
    abstract fun position(): Int
    abstract fun next(): Char
    abstract fun advance()
    abstract fun peek(): Char
    abstract fun hasNext(): Boolean
    abstract fun setLimit(limit: Int)
}
