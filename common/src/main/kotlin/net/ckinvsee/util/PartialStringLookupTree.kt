package net.ckinvsee.util

import java.util.*
import kotlin.collections.ArrayList

class PartialStringLookupTree(initialSet: HashSet<String>) : Collection<String> {

    private var strings: ArrayList<String> = ArrayList(initialSet)
    private var lookup: Branch = Branch('\u0000')

    init {
        // insert into lookup table
        strings.forEachIndexed { id, string ->
            val lstring = string.lowercase()
            val (indent, closestBranch) = tryFindClosestBranch(lstring)
            if(indent == -1) {
                closestBranch.leafs.add(id)
            }
            else {
                buildBranches(closestBranch, indent, lstring).leafs.add(id)
            }
        }
    }

    fun insert(str: String) {
        val lstring = str.lowercase()
        val closest = tryFindClosestBranch(lstring)

        if(closest.first == -1) {
            // valid is not within leafs already
            if(closest.second.leafs.any { id -> strings[id] == str }) { return }
            strings.add(str)
            closest.second.leafs.add(strings.size-1)
            return
        }

        strings.add(str)
        buildBranches(closest.second, closest.first, lstring).leafs.add(strings.size-1)
    }

    fun partialLookup(str: String) : Iterable<String> {
        if(str.isEmpty()) {
            return BranchLeafIterable(this, lookup)
        }

        val lstring = str.lowercase()
        val result = tryFindClosestBranch(lstring)

        return if(result.first == -1) {
            result.second.collectLeaves(this)
        } else {
            emptySet()
        }
    }
    private fun tryFindClosestBranch(str: String): Pair<Int, Branch> {
        var current = lookup
        var pos = 0
        do {
            current.subBranches.firstOrNull { branch ->
                branch.denominator == str[pos]
            }?.let { branch ->
                pos += 1
                current = branch
            } ?: run {
                return Pair(pos, current)
            }
        } while (pos != str.length)
        return Pair(-1, current)
    }
    private fun buildBranches(closestBranch: Branch, indent: Int, lstring: String) : Branch {
        var current = closestBranch
        var pos = indent
        do {
            val newBranch = Branch(lstring[pos])
            current.subBranches.add(newBranch)
            current = newBranch
            pos += 1
        } while(pos != lstring.length)
        return current
    }


    private class Branch(val denominator: Char) {
        val subBranches: ArrayList<Branch> = ArrayList()
        val leafs: ArrayList<Int> = ArrayList()

        fun collectLeaves(set: PartialStringLookupTree): Iterable<String> {
            return BranchLeafIterable(set, this)
        }
    }
    private class BranchLeafIterable(val set: PartialStringLookupTree, val branch: Branch) : Iterable<String> {

        class BranchLeafIterator(val set: PartialStringLookupTree, branch: Branch) : Iterator<String> {
            var branchWalker: Stack<Pair<Int, Branch>> = Stack()
            var branchIndex = 0
            var currentBranch = branch
            var leafIndex = 0

            init {
                // walk to first leaf
                if(currentBranch.leafs.isEmpty()) {
                    do {
                        // move into branch
                        branchWalker.add(Pair(branchIndex+1, currentBranch))
                        currentBranch = currentBranch.subBranches[0]
                        if (currentBranch.subBranches.isEmpty()) {
                            throw Exception("illegal branch tree!")
                        }
                    } while (currentBranch.leafs.isEmpty())
                }
            }

            override fun hasNext(): Boolean {
                if(leafIndex < currentBranch.leafs.size) { return true }
                if(branchIndex < currentBranch.subBranches.size) {
                    branchWalker.push(Pair(branchIndex +1, currentBranch))
                    currentBranch = currentBranch.subBranches[branchIndex]
                    branchIndex = 0
                    while (currentBranch.leafs.isEmpty()) {
                        branchWalker.add(Pair(branchIndex+1, currentBranch))
                        currentBranch = currentBranch.subBranches[0]
                    }
                    leafIndex = 0
                    return true
                }

                while (!branchWalker.empty()) {
                    val prev = branchWalker.pop()
                    if (prev.second.subBranches.size > prev.first)
                    {
                        currentBranch = prev.second.subBranches[prev.first]
                        branchIndex = 0
                        leafIndex = 0
                        branchWalker.push(Pair(prev.first+1, prev.second))
                        while (currentBranch.leafs.isEmpty()) {
                            branchWalker.add(Pair(branchIndex+1, currentBranch))
                            currentBranch = currentBranch.subBranches[0]
                        }
                        return true
                    }
                }
                return false
            }

            override fun next(): String {
                val ret = currentBranch.leafs[leafIndex]
                leafIndex += 1
                return set.strings[ret]
            }
        }

        override fun iterator(): Iterator<String> {
            return BranchLeafIterator(set, branch)
        }
    }

    override val size: Int
        get() = strings.size

    override fun isEmpty(): Boolean {
        return strings.isEmpty()
    }

    override fun iterator(): Iterator<String> {
        return if(strings.isEmpty()) {
            Collections.emptyIterator()
        }
        else {
            BranchLeafIterable.BranchLeafIterator(this, lookup)
        }
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        return elements.all { str -> contains(str) }
    }

    override fun contains(element: String): Boolean {
        val result = tryFindClosestBranch(element)
        return result.first == -1 && result.second.leafs.map { id -> strings[id] }.contains(element)
    }
}