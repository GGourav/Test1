package com.albionradar.data

/**
 * Types of entities in Albion Online
 */
enum class EntityType {
    RESOURCE,
    MOB,
    PLAYER,
    DUNGEON,
    CHEST,
    FISHING,
    MIST_PORTAL
}

/**
 * Base entity interface
 */
interface Entity {
    val id: Int
    val type: EntityType
    val x: Float
    val y: Float
    val timestamp: Long
}

/**
 * Resource entity (harvestable)
 */
data class ResourceEntity(
    override val id: Int,
    val typeNumber: Int,
    val typeName: String,
    val tier: Int,
    val enchantment: Int,
    var size: Int,
    override val x: Float,
    override val y: Float,
    val isLiving: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.RESOURCE
    
    /**
     * Get color for this resource based on tier and enchantment
     */
    fun getColor(): Int {
        return when (enchantment) {
            4 -> 0xFFFF9800.toInt() // Orange (E4)
            3 -> 0xFF9C27B0.toInt() // Purple (E3)
            2 -> 0xFF2196F3.toInt() // Blue (E2)
            1 -> 0xFF4CAF50.toInt() // Green (E1)
            else -> getTierColor(tier)
        }
    }
    
    private fun getTierColor(tier: Int): Int {
        return when (tier) {
            1 -> 0xFF808080.toInt() // Gray
            2 -> 0xFF4CAF50.toInt() // Green
            3 -> 0xFF2196F3.toInt() // Blue
            4 -> 0xFF9C27B0.toInt() // Purple
            5 -> 0xFFFF9800.toInt() // Orange
            6 -> 0xFF607D8B.toInt() // Blue Gray
            7 -> 0xFF00BCD4.toInt() // Cyan
            8 -> 0xFFE91E63.toInt() // Pink
            else -> 0xFFFFFFFF.toInt()
        }
    }
    
    /**
     * Get display name for this resource
     */
    fun getDisplayName(): String {
        val enchantStr = if (enchantment > 0) ".${enchantment}" else ""
        return "T$tier$enchantStr $typeName"
    }
}

/**
 * Mob entity
 */
data class MobEntity(
    override val id: Int,
    val typeNumber: Int,
    val name: String,
    val tier: Int,
    val category: String, // normal, boss, veteran, mistboss, drone
    var enchantment: Int,
    var healthPercent: Float,
    override val x: Float,
    override val y: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.MOB
    
    fun isBoss(): Boolean = category == "boss" || category == "mistboss"
    fun isVeteran(): Boolean = category == "veteran"
    fun isEnchanted(): Boolean = enchantment > 0
    
    fun getColor(): Int {
        return when {
            isBoss() -> 0xFFF44336.toInt() // Red
            isVeteran() -> 0xFFFF9800.toInt() // Orange
            isEnchanted() -> 0xFF9C27B0.toInt() // Purple
            else -> 0xFF9E9E9E.toInt() // Gray
        }
    }
}

/**
 * Player entity
 */
data class PlayerEntity(
    override val id: Int,
    val name: String,
    val guild: String,
    val alliance: String,
    val faction: Int,
    var currentHealth: Float,
    var maxHealth: Float,
    var inCombat: Boolean,
    override var x: Float,
    override var y: Float,
    val equipmentIds: IntArray?,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.PLAYER
    
    fun isHostile(): Boolean = faction == 255
    fun isFaction(): Boolean = faction in 1..6
    fun isPassive(): Boolean = faction == 0
    fun isDead(): Boolean = currentHealth <= 0f
    fun hasKnownPosition(): Boolean = x != 999999f && y != 999999f
    
    fun getColor(): Int {
        return when {
            isHostile() -> 0xFFF44336.toInt() // Red
            isFaction() -> 0xFFFF9800.toInt() // Orange
            else -> 0xFF2196F3.toInt() // Blue
        }
    }
    
    fun getDisplayName(): String {
        val parts = mutableListOf<String>()
        if (guild.isNotEmpty()) parts.add("[$guild]")
        if (alliance.isNotEmpty()) parts.add("<$alliance>")
        parts.add(name)
        return parts.joinToString(" ")
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerEntity) return false
        return id == other.id
    }
    
    override fun hashCode(): Int = id
}

/**
 * Dungeon entrance entity
 */
data class DungeonEntity(
    override val id: Int,
    val dungeonType: Int,
    override val x: Float,
    override val y: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.DUNGEON
    
    fun getColor(): Int = 0xFF00BCD4.toInt()
}

/**
 * Chest entity
 */
data class ChestEntity(
    override val id: Int,
    val chestType: Int,
    override val x: Float,
    override val y: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.CHEST
    
    fun getColor(): Int = 0xFFFFD700.toInt()
}

/**
 * Fishing zone entity
 */
data class FishingEntity(
    override val id: Int,
    override val x: Float,
    override val y: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.FISHING
    
    fun getColor(): Int = 0xFF2196F3.toInt()
}

/**
 * Mist portal entity
 */
data class MistPortalEntity(
    override val id: Int,
    val rarity: Int,
    override val x: Float,
    override val y: Float,
    override val timestamp: Long = System.currentTimeMillis()
) : Entity {
    override val type = EntityType.MIST_PORTAL
    
    fun getColor(): Int = when (rarity) {
        in 1..3 -> 0xFF9C27B0.toInt() // Purple
        else -> 0xFF9E9E9E.toInt() // Gray
    }
}
