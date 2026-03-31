package com.albionradar.photon

/**
 * Photon Protocol Type Codes
 */
object PhotonTypes {
    const val TYPE_NULL: Byte = 0x42       // 'B'
    const val TYPE_BYTE: Byte = 0x62       // 'b'
    const val TYPE_BOOLEAN: Byte = 0x6B    // 'k'
    const val TYPE_SHORT: Byte = 0x73      // 's' (overlaps with string, context determines)
    const val TYPE_INTEGER: Byte = 0x69    // 'i'
    const val TYPE_LONG: Byte = 0x6C       // 'l'
    const val TYPE_FLOAT: Byte = 0x66      // 'f'
    const val TYPE_DOUBLE: Byte = 0x64     // 'd'
    const val TYPE_STRING: Byte = 0x73     // 's'
    const val TYPE_BYTE_ARRAY: Byte = 0x78 // 'x'
    const val TYPE_INT_ARRAY: Byte = 0x6E  // 'n'
    const val TYPE_ARRAY: Byte = 0x79      // 'y'
    const val TYPE_HASHTABLE: Byte = 0x68  // 'h'
    const val TYPE_DICTIONARY: Byte = 0x61 // 'a'
    const val TYPE_EVENT_DATA: Byte = 0x65 // 'e'
    const val TYPE_OPERATION_REQUEST: Byte = 0x71 // 'q'
    const val TYPE_OPERATION_RESPONSE: Byte = 0x70 // 'p'
}

/**
 * Photon Message Types
 */
object MessageTypes {
    const val TYPE_REQUEST = 2
    const val TYPE_RESPONSE = 3
    const val TYPE_EVENT = 4
}

/**
 * Photon Command Types
 */
object CommandTypes {
    const val TYPE_DISCONNECT = 4
    const val TYPE_RELIABLE = 6
    const val TYPE_UNRELIABLE = 7
}

/**
 * Albion Event Codes
 * Event type is in parameter[252], NOT the Photon event code!
 */
object AlbionEvents {
    const val LEAVE = 1                           // Entity despawn
    const val MOVE = 3                            // Position update
    const val HEALTH_UPDATE = 6                   // Single HP change
    const val HEALTH_UPDATES = 7                  // Batch HP changes
    const val NEW_CHARACTER = 29                  // Player spawn
    const val NEW_SIMPLE_HARVESTABLE_LIST = 38   // Batch resources
    const val NEW_HARVESTABLE = 40               // Single resource
    const val HARVESTABLE_CHANGE_STATE = 46      // Resource size update
    const val MOB_CHANGE_STATE = 47              // Mob enchantment
    const val NEW_MOB = 123                      // Mob spawn
    const val COMBAT_STATE_UPDATE = 273          // Combat state
    const val NEW_DUNGEON_EXIT = 319             // Dungeon entrance
    const val NEW_LOOT_CHEST = 387               // Chest spawn
    const val NEW_TREASURE_CHEST = 388           // Treasure chest
    const val NEW_FISHING_ZONE = 389             // Fishing spot
    const val NEW_MIST_PORTAL = 525              // Mist portal
}

/**
 * Albion Operation Codes
 */
object AlbionOperations {
    const val OP_JOIN = 2                        // Join map (local player info)
    const val OP_CHANGE_CLUSTER = 4              // Zone change
    const val OP_MOVE = 21                       // Player movement request
}

/**
 * Resource Type Numbers
 */
object ResourceTypeNumbers {
    // Wood: 0-5
    const val WOOD_START = 0
    const val WOOD_END = 5
    
    // Rock: 6-10
    const val ROCK_START = 6
    const val ROCK_END = 10
    
    // Fiber: 11-15
    const val FIBER_START = 11
    const val FIBER_END = 15
    
    // Hide: 16-22
    const val HIDE_START = 16
    const val HIDE_END = 22
    
    // Ore: 23-27
    const val ORE_START = 23
    const val ORE_END = 27
    
    fun getTypeName(typeNumber: Int): String {
        return when {
            typeNumber in WOOD_START..WOOD_END -> "WOOD"
            typeNumber in ROCK_START..ROCK_END -> "ROCK"
            typeNumber in FIBER_START..FIBER_END -> "FIBER"
            typeNumber in HIDE_START..HIDE_END -> "HIDE"
            typeNumber in ORE_START..ORE_END -> "ORE"
            else -> "UNKNOWN"
        }
    }
    
    fun isResource(typeNumber: Int): Boolean {
        return typeNumber in 0..27
    }
}

/**
 * Player Faction Values
 */
object PlayerFactions {
    const val PASSIVE = 0
    const val BRIDGEWATCH = 1
    const val MARTLOCK = 2
    const val THETFORD = 3
    const val FORTSTERLING = 4
    const val LYMHURST = 5
    const val CAERLEON = 6
    const val HOSTILE = 255
    
    fun isHostile(faction: Int): Boolean = faction == HOSTILE
    fun isFaction(faction: Int): Boolean = faction in 1..6
    fun isPassive(faction: Int): Boolean = faction == PASSIVE
}
