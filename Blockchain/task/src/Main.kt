package blockchain

import java.security.MessageDigest
import kotlin.random.Random.Default.nextLong
import kotlin.math.pow
import kotlin.system.measureTimeMillis

const val BLOCK_COUNT = 5

class Block(val id: Long = 1, val hashPrev: String = "0") {
    companion object {
        private var hashLeadingZeros: Int = 0
        fun inputLeadingZeros() {
            print("Enter how many zeros the hash must start with: ")
            hashLeadingZeros = readln().toInt()
        }
    }

    val timeCreatedAt: Long = System.currentTimeMillis()
    var magicNumber: Long = 0
    var secondsElapsed: Double = 0.0
    val hashBlock: String = generateHash()

    private fun generateHash(): String {
        var hash: String

        secondsElapsed = measureTimeMillis {
            do {
                magicNumber = nextLong(1, 10f.pow(hashLeadingZeros + 3).toLong())
                hash = applySha256(id.toString() + timeCreatedAt.toString() + hashPrev + magicNumber.toString())
            } while (hash.slice(0 until hashLeadingZeros) != "0".repeat(hashLeadingZeros))
        } / 1000.0

        return hash
    }
}

class Blockchain {
    private val chain: MutableList<Block> = mutableListOf(Block())

    fun appendChain(count: Int = 1) {
        repeat(count) {
            chain.add(Block(chain.last().id + 1, chain.last().hashBlock))
        }
    }

    fun printChain() {
        for (block in chain) {
            println(
                """
            Block:
            Id: ${block.id}
            Timestamp: ${block.timeCreatedAt}
            Magic number: ${block.magicNumber}
            Hash of the previous block:
            ${block.hashPrev}
            Hash of the block:
            ${block.hashBlock}
            Block was generating for ${block.secondsElapsed} seconds
            
            """.trimIndent()
            )
        }
    }
}

fun main() {
    Block.inputLeadingZeros()

    val chain = Blockchain()

    chain.appendChain(BLOCK_COUNT - 1)

    chain.printChain()
}

fun applySha256(input: String): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        /* Applies sha256 to input */
        val hash = digest.digest(input.toByteArray(charset("UTF-8")))
        val hexString = StringBuilder()
        for (elem in hash) {
            val hex = Integer.toHexString(0xff and elem.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        hexString.toString()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}