package blockchain

import java.security.MessageDigest

class Block(val id: Long = 1, val hashPrev: String = "0") {
    val timeCreatedAt: Long = System.currentTimeMillis()
    val hashBlock: String = applySha256(id.toString() + timeCreatedAt.toString() + hashPrev)
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
            println("Block:")
            println("Id: ${block.id}")
            println("Timestamp: ${block.timeCreatedAt}")
            println("Hash of the previous block:\n${block.hashPrev}")
            println("Hash of the block:\n${block.hashBlock}\n")
        }
    }
}

fun main() {
    val chain = Blockchain()

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