package com.albionradar.data

import com.albionradar.photon.PhotonValue

/**
 * Represents a parsed game event from the network
 */
data class GameEvent(
    val eventCode: Int,
    val entityId: Int,
    val parameters: Map<Int, PhotonValue>
) {
    /**
     * Get parameter as Integer
     */
    fun getInt(key: Int): Int? = parameters[key]?.asInt()
    
    /**
     * Get parameter as Float
     */
    fun getFloat(key: Int): Float? = parameters[key]?.asFloat()
    
    /**
     * Get parameter as String
     */
    fun getString(key: Int): String? = parameters[key]?.asString()
    
    /**
     * Get parameter as ByteArray
     */
    fun getByteArray(key: Int): ByteArray? = parameters[key]?.asByteArray()
    
    /**
     * Get parameter as IntArray
     */
    fun getIntArray(key: Int): IntArray? = parameters[key]?.asIntArray()
    
    /**
     * Get parameter as List
     */
    fun getArray(key: Int): List<PhotonValue>? = parameters[key]?.asArray()
    
    /**
     * Check if parameter exists
     */
    fun hasParameter(key: Int): Boolean = parameters.containsKey(key)
}
