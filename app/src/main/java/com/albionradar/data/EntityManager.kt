package com.albionradar.data

import android.util.Log
import com.albionradar.photon.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Central manager for all game entities
 * Thread-safe, uses Kotlin Flows for reactive updates
 */
class EntityManager private constructor() {

    companion object {
        private const val TAG = "EntityManager"
        private const val STALE_TIMEOUT_MS = 120_000L // 2 minutes
        
        @Volatile
        private var instance: EntityManager? = null
        
        fun getInstance(): EntityManager {
            return instance ?: synchronized(this) {
                instance ?: EntityManager().also { instance = it }
            }
        }
    }

    // Entity storage
    private val resources = ConcurrentHashMap<Int, ResourceEntity>()
    private val mobs = ConcurrentHashMap<Int, MobEntity>()
    private val players = ConcurrentHashMap<Int, PlayerEntity>()
    private val dungeons = ConcurrentHashMap<Int, DungeonEntity>()
    private val chests = ConcurrentHashMap<Int, ChestEntity>()
    private val fishing = ConcurrentHashMap<Int, FishingEntity>()
    private val mistPortals = ConcurrentHashMap<Int, MistPortalEntity>()
    
    // Local player tracking
    private var localPlayerId: Int = 0
    private var localPlayerX: Float = 0f
    private var localPlayerY: Float = 0f
    
    // Zone tracking
    private var currentZoneId: Int = 0
    private var currentZoneName: String = ""
    
    // State flows for UI observation
    private val _entities = MutableStateFlow<List<Entity>>(emptyList())
    val entities: StateFlow<List<Entity>> = _entities.asStateFlow()
    
    private val _zoneName = MutableStateFlow("")
    val zoneName: StateFlow<String> = _zoneName.asStateFlow()
    
    private val _entityStats = MutableStateFlow(EntityStats())
    val entityStats: StateFlow<EntityStats> = _entityStats.asStateFlow()
    
    private val mutex = Mutex()
    private val photonParser = PhotonParser()

    /**
     * Process a game event
     */
    fun processEvent(event: GameEvent) {
        when (event.eventCode) {
            // Response operations (local player info)
            AlbionOperations.OP_JOIN, 
            AlbionOperations.OP_JOIN or 0x1000 -> handleJoinResponse(event)
            
            AlbionOperations.OP_CHANGE_CLUSTER,
            AlbionOperations.OP_CHANGE_CLUSTER or 0x1000 -> handleClusterChange(event)
            
            // Entity events
            AlbionEvents.NEW_SIMPLE_HARVESTABLE_LIST -> handleBatchResources(event)
            AlbionEvents.NEW_HARVESTABLE -> handleNewResource(event)
            AlbionEvents.HARVESTABLE_CHANGE_STATE -> handleResourceStateChange(event)
            AlbionEvents.NEW_MOB -> handleNewMob(event)
            AlbionEvents.MOB_CHANGE_STATE -> handleMobStateChange(event)
            AlbionEvents.NEW_CHARACTER -> handleNewCharacter(event)
            AlbionEvents.MOVE -> handleMove(event)
            AlbionEvents.LEAVE -> handleLeave(event)
            AlbionEvents.HEALTH_UPDATE -> handleHealthUpdate(event)
            AlbionEvents.HEALTH_UPDATES -> handleHealthUpdates(event)
            AlbionEvents.COMBAT_STATE_UPDATE -> handleCombatState(event)
            AlbionEvents.NEW_DUNGEON_EXIT -> handleNewDungeon(event)
            AlbionEvents.NEW_LOOT_CHEST, AlbionEvents.NEW_TREASURE_CHEST -> handleNewChest(event)
            AlbionEvents.NEW_FISHING_ZONE -> handleNewFishing(event)
            AlbionEvents.NEW_MIST_PORTAL -> handleMistPortal(event)
            
            else -> {
                // Log unhandled events for debugging
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Unhandled event: ${event.eventCode}")
                }
            }
        }
        
        updateState()
    }

    // ==================== Resource Handlers ====================
    
    private fun handleBatchResources(event: GameEvent) {
        try {
            val ids = event.getIntArray(0) ?: return
            val types = event.getIntArray(1) ?: return
            val tiers = event.getIntArray(2) ?: return
            val positions = event.getIntArray(3) ?: return
            val sizes = event.getIntArray(4) ?: return
            
            for (i in ids.indices) {
                val typeNumber = types.getOrNull(i) ?: continue
                val tier = tiers.getOrNull(i) ?: continue
                val size = sizes.getOrNull(i) ?: continue
                
                // Position is interleaved: [x0, y0, x1, y1, ...]
                val x = positions.getOrNull(i * 2)?.toFloat() ?: continue
                val y = positions.getOrNull(i * 2 + 1)?.toFloat() ?: continue
                
                if (!ResourceTypeNumbers.isResource(typeNumber)) continue
                
                val resource = ResourceEntity(
                    id = ids[i],
                    typeNumber = typeNumber,
                    typeName = ResourceTypeNumbers.getTypeName(typeNumber),
                    tier = tier,
                    enchantment = 0, // Batch spawn defaults to E0
                    size = size,
                    x = x,
                    y = y,
                    isLiving = false
                )
                
                resources[resource.id] = resource
            }
            
            Log.d(TAG, "Batch resources: ${ids.size} added")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling batch resources", e)
        }
    }
    
    private fun handleNewResource(event: GameEvent) {
        try {
            val id = event.entityId
            val typeNumber = event.getInt(5) ?: return
            val mobileTypeId = event.getInt(6)
            val tier = event.getInt(7) ?: return
            val location = event.getArray(8) ?: return
            val size = event.getInt(10) ?: 1
            val enchantment = event.getInt(11) ?: 0
            
            val x = location.getOrNull(0)?.asFloat() ?: return
            val y = location.getOrNull(1)?.asFloat() ?: return
            
            val isLiving = mobileTypeId != null && mobileTypeId != 65535 && mobileTypeId > 0
            
            if (!ResourceTypeNumbers.isResource(typeNumber)) return
            
            val resource = ResourceEntity(
                id = id,
                typeNumber = typeNumber,
                typeName = ResourceTypeNumbers.getTypeName(typeNumber),
                tier = tier,
                enchantment = enchantment,
                size = size,
                x = x,
                y = y,
                isLiving = isLiving
            )
            
            resources[resource.id] = resource
            Log.d(TAG, "Resource added: ${resource.getDisplayName()} at ($x, $y)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new resource", e)
        }
    }
    
    private fun handleResourceStateChange(event: GameEvent) {
        try {
            val id = event.entityId
            val newSize = event.getInt(1)
            val newEnchantment = event.getInt(2)
            
            val resource = resources[id]
            
            if (resource != null) {
                if (newSize == null || newSize <= 0) {
                    // Resource depleted
                    resources.remove(id)
                    Log.d(TAG, "Resource depleted: $id")
                } else {
                    resource.size = newSize
                    if (newEnchantment != null) {
                        // Update in map (need to recreate since data class is immutable)
                        resources[id] = resource.copy(
                            size = newSize,
                            enchantment = newEnchantment
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling resource state change", e)
        }
    }

    // ==================== Mob Handlers ====================
    
    private fun handleNewMob(event: GameEvent) {
        try {
            val id = event.entityId
            val typeNumber = event.getInt(1) ?: return
            val location = event.getArray(2) ?: return
            val healthPercent = event.getFloat(3) ?: 1f
            val enchantment = event.getInt(4) ?: 0
            
            val x = location.getOrNull(0)?.asFloat() ?: return
            val y = location.getOrNull(1)?.asFloat() ?: return
            
            // Look up mob info from database
            val mobInfo = DataManager.getMobInfo(typeNumber)
            
            val mob = MobEntity(
                id = id,
                typeNumber = typeNumber,
                name = mobInfo?.name ?: "Unknown",
                tier = mobInfo?.tier ?: 1,
                category = mobInfo?.category ?: "normal",
                enchantment = enchantment,
                healthPercent = healthPercent,
                x = x,
                y = y
            )
            
            mobs[id] = mob
            Log.d(TAG, "Mob added: ${mob.name} (${mob.category})")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new mob", e)
        }
    }
    
    private fun handleMobStateChange(event: GameEvent) {
        try {
            val id = event.entityId
            val newEnchantment = event.getInt(4)
            
            val mob = mobs[id]
            if (mob != null && newEnchantment != null) {
                mobs[id] = mob.copy(enchantment = newEnchantment)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling mob state change", e)
        }
    }

    // ==================== Player Handlers ====================
    
    private fun handleNewCharacter(event: GameEvent) {
        try {
            val id = event.entityId
            val name = event.getString(1) ?: return
            
            // Skip if name is empty or too short
            if (name.length < 2) return
            
            // Skip self
            if (id == localPlayerId) return
            
            val guild = event.getString(8) ?: ""
            val alliance = event.getString(51) ?: ""
            val faction = event.getInt(53) ?: 0
            val maxHealth = event.getFloat(22) ?: 0f
            val equipmentIds = event.getIntArray(40)
            
            val player = PlayerEntity(
                id = id,
                name = name,
                guild = guild,
                alliance = alliance,
                faction = faction,
                currentHealth = maxHealth,
                maxHealth = maxHealth,
                inCombat = false,
                x = 999999f, // Unknown position initially
                y = 999999f,
                equipmentIds = equipmentIds
            )
            
            players[id] = player
            
            val threatStr = when {
                player.isHostile() -> "HOSTILE"
                player.isFaction() -> "FACTION"
                else -> "PASSIVE"
            }
            Log.d(TAG, "Player: ${player.getDisplayName()} [$threatStr]")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new character", e)
        }
    }
    
    private fun handleMove(event: GameEvent) {
        try {
            val id = event.entityId
            
            // Update local player position if this is us
            if (id == localPlayerId) {
                localPlayerX = event.getFloat(4) ?: localPlayerX
                localPlayerY = event.getFloat(5) ?: localPlayerY
                return
            }
            
            val player = players[id] ?: return
            
            // Extract position using dual method
            val position = photonParser.extractPositionFromMove(event.parameters)
            
            if (position != null) {
                players[id] = player.copy(
                    x = position.first,
                    y = position.second
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling move", e)
        }
    }
    
    private fun handleLeave(event: GameEvent) {
        val id = event.entityId
        
        resources.remove(id)
        mobs.remove(id)
        players.remove(id)
        dungeons.remove(id)
        chests.remove(id)
        fishing.remove(id)
        mistPortals.remove(id)
        
        Log.d(TAG, "Entity removed: $id")
    }
    
    private fun handleHealthUpdate(event: GameEvent) {
        try {
            val id = event.entityId
            val currentHealth = event.getFloat(2) ?: return
            val maxHealth = event.getFloat(3)
            
            val player = players[id]
            if (player != null) {
                players[id] = player.copy(
                    currentHealth = currentHealth,
                    maxHealth = maxHealth ?: player.maxHealth
                )
            }
            
            val mob = mobs[id]
            if (mob != null) {
                mobs[id] = mob.copy(
                    healthPercent = currentHealth / (maxHealth ?: mob.healthPercent)
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling health update", e)
        }
    }
    
    private fun handleHealthUpdates(event: GameEvent) {
        // Batch health updates
        handleHealthUpdate(event)
    }
    
    private fun handleCombatState(event: GameEvent) {
        try {
            val id = event.entityId
            val inCombat = event.hasParameter(1)
            
            val player = players[id]
            if (player != null) {
                players[id] = player.copy(inCombat = inCombat)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling combat state", e)
        }
    }

    // ==================== Other Entity Handlers ====================
    
    private fun handleNewDungeon(event: GameEvent) {
        try {
            val id = event.entityId
            val dungeonType = event.getInt(3) ?: 0
            val location = event.getArray(1) ?: return
            
            val x = location.getOrNull(0)?.asFloat() ?: return
            val y = location.getOrNull(1)?.asFloat() ?: return
            
            dungeons[id] = DungeonEntity(id, dungeonType, x, y)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new dungeon", e)
        }
    }
    
    private fun handleNewChest(event: GameEvent) {
        try {
            val id = event.entityId
            val location = event.getArray(1) ?: return
            
            val x = location.getOrNull(0)?.asFloat() ?: return
            val y = location.getOrNull(1)?.asFloat() ?: return
            
            chests[id] = ChestEntity(id, event.eventCode, x, y)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new chest", e)
        }
    }
    
    private fun handleNewFishing(event: GameEvent) {
        try {
            val id = event.entityId
            val location = event.getArray(1) ?: return
            
            val x = location.getOrNull(0)?.asFloat() ?: return
            val y = location.getOrNull(1)?.asFloat() ?: return
            
            fishing[id] = FishingEntity(id, x, y)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new fishing", e)
        }
    }
    
    private fun handleMistPortal(event: GameEvent) {
        try {
            val id = event.entityId
            val location = event.getArray(1) ?: return
            val rarity = event.getInt(2) ?: 0
            
            val x = location.getOrNull(0)?.asFloat() ?: return
            val y = location.getOrNull(1)?.asFloat() ?: return
            
            mistPortals[id] = MistPortalEntity(id, rarity, x, y)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling mist portal", e)
        }
    }

    // ==================== Operation Handlers ====================
    
    private fun handleJoinResponse(event: GameEvent) {
        try {
            // Extract local player info
            val playerId = event.getInt(0) ?: return
            val playerName = event.getString(1) ?: return
            
            localPlayerId = playerId
            Log.d(TAG, "Local player: $playerName (ID: $playerId)")
            
            // Extract position from byte array if available
            val posBuffer = event.getByteArray(9)
            if (posBuffer != null && posBuffer.size >= 8) {
                val buffer = java.nio.ByteBuffer.wrap(posBuffer)
                buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                localPlayerX = buffer.getFloat(0)
                localPlayerY = buffer.getFloat(4)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling join response", e)
        }
    }
    
    private fun handleClusterChange(event: GameEvent) {
        // Zone changed - clear all entities
        clearAll()
        
        // Update zone info
        currentZoneId = event.getInt(0) ?: 0
        currentZoneName = DataManager.getZoneName(currentZoneId)
        _zoneName.value = currentZoneName
        
        // Re-extract local player info
        handleJoinResponse(event)
        
        Log.d(TAG, "Zone changed to: $currentZoneName ($currentZoneId)")
    }

    // ==================== State Management ====================
    
    private fun updateState() {
        val allEntities = mutableListOf<Entity>()
        allEntities.addAll(resources.values)
        allEntities.addAll(mobs.values)
        allEntities.addAll(players.values)
        allEntities.addAll(dungeons.values)
        allEntities.addAll(chests.values)
        allEntities.addAll(fishing.values)
        allEntities.addAll(mistPortals.values)
        
        _entities.value = allEntities
        
        _entityStats.value = EntityStats(
            resourceCount = resources.size,
            mobCount = mobs.size,
            playerCount = players.size,
            dungeonCount = dungeons.size,
            chestCount = chests.size,
            fishingCount = fishing.size,
            mistCount = mistPortals.size
        )
    }
    
    fun getEntitiesByType(type: EntityType): List<Entity> {
        return when (type) {
            EntityType.RESOURCE -> resources.values.toList()
            EntityType.MOB -> mobs.values.toList()
            EntityType.PLAYER -> players.values.toList()
            EntityType.DUNGEON -> dungeons.values.toList()
            EntityType.CHEST -> chests.values.toList()
            EntityType.FISHING -> fishing.values.toList()
            EntityType.MIST_PORTAL -> mistPortals.values.toList()
        }
    }
    
    fun getLocalPlayerPosition(): Pair<Float, Float> = Pair(localPlayerX, localPlayerY)
    
    fun cleanupStaleEntities() {
        val now = System.currentTimeMillis()
        
        resources.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        mobs.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        players.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        dungeons.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        chests.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        fishing.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        mistPortals.entries.removeIf { now - it.value.timestamp > STALE_TIMEOUT_MS }
        
        updateState()
    }
    
    fun clearAll() {
        resources.clear()
        mobs.clear()
        players.clear()
        dungeons.clear()
        chests.clear()
        fishing.clear()
        mistPortals.clear()
   
