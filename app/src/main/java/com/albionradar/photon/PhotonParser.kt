package com.albionradar.photon

import android.util.Log
import com.albionradar.data.GameEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses Photon protocol packets from Albion Online
 * Based on Protocol16 deserialization
 */
class PhotonParser {

    companion object {
        private const val TAG = "PhotonParser"
        private const val PHOTON_HEADER_SIZE = 12
        private const val COMMAND_HEADER_SIZE = 12
    }

    /**
     * Parse a raw UDP payload into a list of game events
     */
    fun parsePacket(payload: ByteArray): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        
        try {
            val buffer = ByteBuffer.wrap(payload)
            buffer.order(ByteOrder.BIG_ENDIAN)
            
            // Parse Photon header
            if (buffer.remaining() < PHOTON_HEADER_SIZE) {
                return events
            }
            
            val peerId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.get().toInt() and 0xFF
            val commandCount = buffer.get().toInt() and 0xFF
            val timestamp = buffer.int
            val challenge = buffer.int
            
            // Parse each command
            repeat(commandCount) {
                if (buffer.remaining() < COMMAND_HEADER_SIZE) {
                    return@repeat
                }
                
                val command = parseCommand(buffer)
                if (command != null) {
                    val commandEvents = processCommand(command)
                    events.addAll(commandEvents)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing packet", e)
        }
        
        return events
    }

    private fun parseCommand(buffer: ByteBuffer): PhotonCommand? {
        val commandType = buffer.get().toInt() and 0xFF
        val channelId = buffer.get().toInt() and 0xFF
        val reserved1 = buffer.get().toInt() and 0xFF
        val reserved2 = buffer.get().toInt() and 0xFF
        val commandLength = buffer.int
        val reliableSequenceNumber = buffer.int
        
        if (buffer.remaining() < commandLength - 8) {
            return null
        }
        
        val payloadStart = buffer.position()
        val payloadEnd = payloadStart + commandLength - 8
        
        return when (commandType) {
            CommandTypes.TYPE_RELIABLE, CommandTypes.TYPE_UNRELIABLE -> {
                val payload = ByteArray(commandLength - 8)
                buffer.get(payload)
                PhotonCommand(commandType, payload)
            }
            else -> {
                buffer.position(payloadEnd)
                null
            }
        }
    }

    private fun processCommand(command: PhotonCommand): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        
        try {
            val buffer = ByteBuffer.wrap(command.payload)
            buffer.order(ByteOrder.BIG_ENDIAN)
            
            if (buffer.remaining() < 2) return events
            
            val messageType = buffer.get().toInt() and 0xFF
            val operationCode = buffer.get().toInt() and 0xFF
            
            when (messageType) {
                MessageTypes.TYPE_EVENT -> {
                    val event = parseEvent(buffer, operationCode)
                    if (event != null) {
                        events.add(event)
                    }
                }
                MessageTypes.TYPE_RESPONSE -> {
                    val event = parseResponse(buffer, operationCode)
                    if (event != null) {
                        events.add(event)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
        }
        
        return events
    }

    private fun parseEvent(buffer: ByteBuffer, operationCode: Int): GameEvent? {
        try {
            // Parse event data using Protocol16
            val parameters = parseParameters(buffer)
            
            // Get the actual Albion event code from parameter 252
            val albionEventCode = parameters[252]?.asInt() ?: operationCode
            
            // Extract entity ID from parameter 0 (common pattern)
            val entityId = parameters[0]?.asInt() ?: 0
            
            return GameEvent(
                eventCode = albionEventCode,
                entityId = entityId,
                parameters = parameters
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event", e)
            return null
        }
    }

    private fun parseResponse(buffer: ByteBuffer, operationCode: Int): GameEvent? {
        try {
            // Skip return code (2 bytes)
            if (buffer.remaining() < 2) return null
            val returnCode = buffer.short.toInt()
            
            // Skip debug message
            val debugMessageLength = buffer.short.toInt() and 0xFFFF
            if (buffer.remaining() < debugMessageLength) return null
            buffer.position(buffer.position() + debugMessageLength)
            
            val parameters = parseParameters(buffer)
            
            return GameEvent(
                eventCode = operationCode or 0x1000, // Mark as response
                entityId = parameters[0]?.asInt() ?: 0,
                parameters = parameters
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            return null
        }
    }

    private fun parseParameters(buffer: ByteBuffer): Map<Int, PhotonValue> {
        val parameters = mutableMapOf<Int, PhotonValue>()
        
        try {
            if (buffer.remaining() < 2) return parameters
            
            val parameterCount = buffer.short.toInt() and 0xFFFF
            
            repeat(parameterCount) {
                if (buffer.remaining() < 3) return@repeat
                
                val parameterCode = buffer.get().toInt() and 0xFF
                val valueType = buffer.get().toInt() and 0xFF
                
                val value = parseValue(buffer, valueType)
                if (value != null) {
                    parameters[parameterCode] = value
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing parameters", e)
        }
        
        return parameters
    }

    private fun parseValue(buffer: ByteBuffer, type: Int): PhotonValue? {
        return when (type.toByte()) {
            PhotonTypes.TYPE_NULL -> PhotonValue.NullValue()
            PhotonTypes.TYPE_BYTE -> {
                if (buffer.remaining() < 1) null
                else PhotonValue.ByteValue(buffer.get())
            }
            PhotonTypes.TYPE_BOOLEAN -> {
                if (buffer.remaining() < 1) null
                else PhotonValue.BooleanValue(buffer.get() != 0.toByte())
            }
            PhotonTypes.TYPE_SHORT -> {
                if (buffer.remaining() < 2) null
                else PhotonValue.ShortValue(buffer.short)
            }
            PhotonTypes.TYPE_INTEGER -> {
                if (buffer.remaining() < 4) null
                else PhotonValue.IntegerValue(buffer.int)
            }
            PhotonTypes.TYPE_LONG -> {
                if (buffer.remaining() < 8) null
                else PhotonValue.LongValue(buffer.long)
            }
            PhotonTypes.TYPE_FLOAT -> {
                if (buffer.remaining() < 4) null
                else {
                    // Albion uses LITTLE ENDIAN for floats in move events
                    val savedOrder = buffer.order()
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    val value = buffer.float
                    buffer.order(savedOrder)
                    PhotonValue.FloatValue(value)
                }
            }
            PhotonTypes.TYPE_DOUBLE -> {
                if (buffer.remaining() < 8) null
                else PhotonValue.DoubleValue(buffer.double)
            }
            PhotonTypes.TYPE_STRING -> {
                if (buffer.remaining() < 2) null
                else {
                    val length = buffer.short.toInt() and 0xFFFF
                    if (buffer.remaining() < length) null
                    else {
                        val bytes = ByteArray(length)
                        buffer.get(bytes)
                        PhotonValue.StringValue(String(bytes, Charsets.UTF_8))
                    }
                }
            }
            PhotonTypes.TYPE_BYTE_ARRAY -> {
                if (buffer.remaining() < 4) null
                else {
                    val length = buffer.int
                    if (buffer.remaining() < length) null
                    else {
                        val bytes = ByteArray(length)
                        buffer.get(bytes)
                        PhotonValue.ByteArrayValue(bytes)
                    }
                }
            }
            PhotonTypes.TYPE_INT_ARRAY -> {
                if (buffer.remaining() < 4) null
                else {
                    val length = buffer.int
                    if (buffer.remaining() < length * 4) null
                    else {
                        val array = IntArray(length)
                        repeat(length) { array[it] = buffer.int }
                        PhotonValue.IntArrayValue(array)
                    }
                }
            }
            PhotonTypes.TYPE_ARRAY -> parseArray(buffer)
            PhotonTypes.TYPE_HASHTABLE -> parseHashtable(buffer)
            else -> null
        }
    }

    private fun parseArray(buffer: ByteBuffer): PhotonValue? {
        if (buffer.remaining() < 5) return null
        
        val valueType = buffer.get().toInt() and 0xFF
        val length = buffer.int
        
        if (length > 10000) return null // Sanity check
        
        val values = mutableListOf<PhotonValue>()
        repeat(length) {
            val value = parseValue(buffer, valueType)
            if (value != null) {
                values.add(value)
            }
        }
        
        return PhotonValue.ArrayValue(values)
    }

    private fun parseHashtable(buffer: ByteBuffer): PhotonValue? {
        if (buffer.remaining() < 6) return null
        
        val keyType = buffer.get().toInt() and 0xFF
        val valueType = buffer.get().toInt() and 0xFF
        val length = buffer.int
        
        if (length > 10000) return null // Sanity check
        
        val map = mutableMapOf<PhotonValue, PhotonValue>()
        repeat(length) {
            val key = parseValue(buffer, keyType)
            val value = parseValue(buffer, valueType)
            if (key != null && value != null) {
                map[key] = value
            }
        }
        
        return PhotonValue.HashtableValue(map)
    }

    /**
     * Extract position from Move event (Event 3)
     * Albion uses ByteArray with LE floats for position
     */
    fun extractPositionFromMove(parameters: Map<Int, PhotonValue>): Pair<Float, Float>? {
        // Try direct parameters first
        val xParam = parameters[4]?.asFloat()
        val yParam = parameters[5]?.asFloat()
        
        if (xParam != null && yParam != null) {
            // Validate - reject obviously invalid coordinates
            if (isValidCoordinate(xParam, yParam)) {
                return Pair(xParam, yParam)
            }
        }
        
        // Try ByteArray extraction
        val byteArray = parameters[1]?.asByteArray()
        if (byteArray != null && byteArray.size >= 17) {
            val buffer = ByteBuffer.wrap(byteArray)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            val x = buffer.getFloat(9)
            val y = buffer.getFloat(13)
            
            if (isValidCoordinate(x, y)) {
                return Pair(x, y)
            }
        }
        
        return null
    }

    private fun isValidCoordinate(x: Float, y: Float): Boolean {
        // Reject (0, 0) - likely invalid
        if (x == 0f && y == 0f) return false
        // Reject coordinates outside reasonable game world bounds
        if (Math.abs(x) > 50000 || Math.abs(y) > 50000) return false
        // Reject NaN or Infinite
        if (x.isNaN() || y.isNaN() || x.isInfinite() || y.isInfinite()) return false
        return true
    }
}

data class PhotonCommand(
    val type: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhotonCommand) return false
        return type == other.type && payload.contentEquals(other.payload)
    }
    
    override fun hashCode(): Int = 31 * type + payload.contentHashCode()
}
