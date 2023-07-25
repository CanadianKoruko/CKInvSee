package net.ckinvsee.util

data class IntRange(val first: Int, val last: Int) : Iterable<Int> {
    init {
        if (first > last) {
            throw IllegalArgumentException("first:$first is can not be greater than last:$last!!")
        }
    }

    /// gets the range element at the specified index (0-N)
    @Suppress("UNUSED")
    fun indexAt(index: Int): Int {
        if (index > (last - first)) {
            throw IndexOutOfBoundsException("$index is out of bounds of a range containing ${last - first} elements!")
        }
        return indexAtUnsafe(index)
    }

    /// gets the range element at the specified index (0-N), just without range checks
    fun indexAtUnsafe(index: Int): Int {
        return first + index
    }

    fun contains(index: Int): Boolean {
        return (index >= first) and (index <= last)
    }

    fun mapRanges(toOther: IntRange): Map<Int, Int> {
        // this.first == 0
        // this.last == 8
        // toOther.first = 27
        // toOther.last = 35

        val count = last - first + 1
        val otherCount = toOther.last - toOther.first + 1

        if (count != otherCount) {
            throw IllegalArgumentException("Ranges must be the same size")
        }
        return mapOf(pairs = Array(count) { x ->
            Pair(first + x, toOther.first + x)
        })
    }

    override fun iterator(): Iterator<Int> {
        return IntRangeIterator(first, last)
    }


    data class IntRangeIterator(val first: Int, val last: Int) : Iterator<Int> {
        private var position = first
        override fun hasNext(): Boolean {
            return position <= last
        }

        override fun next(): Int {
            if (position > last) {
                throw IndexOutOfBoundsException("end of iterator!")
            }
            return nextUnsafe()
        }

        /// just like next, except 0 safety checks
        fun nextUnsafe(): Int {
            val v = position
            position += 1
            return v
        }

    }
}