package blockchain

import java.security.MessageDigest
import kotlin.math.pow
import kotlin.random.Random.Default.nextLong
import kotlin.system.measureTimeMillis

const val BLOCK_COUNT = 5
const val NUMBER_OF_MINERS = 10
const val NUMBER_OF_SENDERS = 10
const val MILLIS_TO_SECOND = 1000.0
const val MAX_DELAY_MILLIS: Long = 30
const val PRINT_IMMEDIATELY = false

fun main() {
    Blockchain.initializeChain()

    Mine.createMiner(NUMBER_OF_MINERS)
    Clients.createSender(NUMBER_OF_SENDERS)
    Mine.startAll()
    Clients.startAll()

    Clients.joinAll()
    Mine.joinAll()

    Blockchain.printChain()
}

class Message(val userName: String, val content: String)

class Block(val id: Long = 1, val hashPrev: String = "0", var hashLeadingZeros: Int = 0, val minerId: Long) {
    private companion object {
        const val EXTRA_DIGITS = 2
    }

    @Volatile
    var data = mutableListOf<Message>()
    val timeCreatedAt: Long = System.currentTimeMillis()
    var magicNumber: Long = 0
    var secondsElapsed: Double = 0.0
    val hashBlock: String = generateHash()
    var nextLeadingZeros: Int = hashLeadingZeros

    private fun generateHash(): String {
        var hash: String

        secondsElapsed = measureTimeMillis {
            do {
                magicNumber = nextLong(1, 10f.pow(hashLeadingZeros + EXTRA_DIGITS).toLong())
                hash = applySha256(id.toString() + timeCreatedAt.toString() + hashPrev + magicNumber.toString())
            } while (hash.slice(0 until hashLeadingZeros) != "0".repeat(hashLeadingZeros))
        } / MILLIS_TO_SECOND

        return hash
    }
}

object Blockchain {
    val chain: MutableList<Block> = mutableListOf()
    private const val SECONDS_INCREASE_DIFFICULTY = 15
    private const val SECONDS_DECREASE_DIFFICULTY = 60
    private val pendingData: MutableList<Message> = mutableListOf()

    @Synchronized
    fun getMessage(userName: String, content: String) {
        pendingData.add(Message(userName, content))
    }

    private fun appendToChain(block: Block) {
        when {
            block.secondsElapsed < SECONDS_INCREASE_DIFFICULTY -> ++block.nextLeadingZeros
            block.secondsElapsed >= SECONDS_DECREASE_DIFFICULTY -> --block.nextLeadingZeros
            else -> block.nextLeadingZeros
        }
        block.data.addAll(pendingData)
        chain.add(block)
        if (PRINT_IMMEDIATELY) {
            printBlock(chain.lastIndex)
        }
        pendingData.clear()
    }

    fun initializeChain() {
        val threadId = Thread.currentThread().threadId()
        appendToChain(Block(minerId = threadId))
    }

    fun generateBlock() {
        val threadId = Thread.currentThread().threadId()
        var pendingBlock: Block

        do {
            val blockId = chain.last().id + 1
            val hashPrev = chain.last().hashBlock

            pendingBlock = Block(blockId, hashPrev, chain.last().nextLeadingZeros, threadId)

            synchronized(chain) {
                if (pendingBlock.hashPrev == chain.last().hashBlock && chain.size < BLOCK_COUNT) {
                    appendToChain(pendingBlock)
                }
            }
        } while (chain.size < BLOCK_COUNT)
    }

    private fun printBlock(index: Int) {
        val block = chain[index]
        val zerosChange = when {
            block.nextLeadingZeros > block.hashLeadingZeros -> "N was increased to ${block.nextLeadingZeros}"
            block.nextLeadingZeros < block.hashLeadingZeros -> "N was decreased to ${block.nextLeadingZeros}"
            else -> "N stays the same"
        }
        var dataString = ""
        if (block.data.isEmpty()) {
            dataString = "no messages"
        } else {
            block.data.forEach { dataString += "\n${it.userName}: ${it.content}" }
        }
        println(
            """
Block:
Created by miner # ${block.minerId}
Id: ${block.id}
Timestamp: ${block.timeCreatedAt}
Magic number: ${block.magicNumber}
Hash of the previous block:
${block.hashPrev}
Hash of the block:
${block.hashBlock}
Block data: $dataString
Block was generating for ${block.secondsElapsed} seconds
$zerosChange

""".trimIndent()
        )
    }

    fun printChain() {
        for (index in 0..chain.lastIndex) {
            printBlock(index)
        }
    }
}

class Miner : Thread() {
    override fun run() {
        while (Blockchain.chain.size < BLOCK_COUNT) {
            Blockchain.generateBlock()
        }
    }
}

object Mine {
    private val miners: MutableList<Miner> = mutableListOf()

    fun createMiner(count: Int = 1) {
        repeat(count) { miners.add(Miner()) }
    }

    fun startAll() {
        miners.forEach { it.start() }
    }

    fun joinAll() {
        miners.forEach { it.join() }
    }
}

class Sender : Thread() {
    override fun run() {
        var messageCount = 1
        while (Blockchain.chain.size < BLOCK_COUNT) {
            val content = "You there? ${currentThread().threadId()}, $messageCount"
            Blockchain.getMessage("Test", content)
            sleep(nextLong(1, MAX_DELAY_MILLIS))
            ++messageCount
        }
    }
}

object Clients {
    private val senders: MutableList<Sender> = mutableListOf()

    fun createSender(count: Int = 1) {
        repeat(count) { senders.add(Sender()) }
    }

    fun startAll() {
        senders.forEach { it.start() }
    }

    fun joinAll() {
        senders.forEach { it.join() }
    }
}

fun applySha256(input: String): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
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
