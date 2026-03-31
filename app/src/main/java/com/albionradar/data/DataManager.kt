package com.albionradar.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * Manages static game data (mobs, items, zones, etc.)
 */
object DataManager {
    
    private const val TAG = "DataManager"
    
    private lateinit var context: Context
    private val gson = Gson()
    
    // Data caches
    private var mobs: Map<Int, MobInfo> = emptyMap()
    private var zones: Map<Int, ZoneInfo> = emptyMap()
    private var items: Map<Int, ItemInfo> = emptyMap()
    
    private var isInitialized = false

    fun initialize(context: Context) {
        this.context = context.applicationContext
        
        loadMobs()
        loadZones()
        loadItems()
        
        isInitialized = true
        Log.d(TAG, "DataManager initialized: ${mobs.size} mobs, ${zones.size} zones, ${items.size} items")
    }
    
    // ==================== Mob Data ====================
    
    fun getMobInfo(typeNumber: Int): MobInfo? {
        return mobs[typeNumber]
    }
    
    private fun loadMobs() {
        try {
            val inputStream = context.assets.open("data/mobs.json")
            val reader = InputStreamReader(inputStream)
            val mobList: List<MobInfo> = gson.fromJson(reader, object : TypeToken<List<MobInfo>>() {}.type)
            
            mobs = mobList.associateBy { it.typeNumber }
            reader.close()
            inputStream.close()
            
            Log.d(TAG, "Loaded ${mobs.size} mob definitions")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading mobs", e)
            loadDefaultMobs()
        }
    }
    
    private fun loadDefaultMobs() {
        mobs = mapOf(
            // Heretic mobs
            1 to MobInfo(1, "Heretic Worker", 1, "normal", "HERETIC"),
            2 to MobInfo(2, "Heretic Mage", 2, "normal", "HERETIC"),
            3 to MobInfo(3, "Heretic Boss", 4, "boss", "HERETIC"),
            
            // Keeper mobs
            100 to MobInfo(100, "Keeper Giant", 5, "boss", "KEEPER"),
            101 to MobInfo(101, "Keeper Druid", 4, "veteran", "KEEPER"),
            
            // Undead mobs
            200 to MobInfo(200, "Undead Skeleton", 3, "normal", "UNDEAD"),
            201 to MobInfo(201, "Undead Mage", 5, "boss", "UNDEAD"),
            
            // Morgana mobs
            300 to MobInfo(300, "Morgana Mage", 4, "normal", "MORGANA"),
            301 to MobInfo(301, "Morgana Knight", 5, "boss", "MORGANA"),
            
            // Hell mobs
            400 to MobInfo(400, "Hell Demon", 5, "boss", "HELL"),
            
            // Avalon mobs
            500 to MobInfo(500, "Avalon Guardian", 7, "boss", "AVALON"),
            
            // Living resources
            1000 to MobInfo(1000, "Gatherer", 2, "living", "WOOD"),
            1001 to MobInfo(1001, "Treant", 4, "boss", "WOOD"),
            2000 to MobInfo(2000, "Rock Golem", 4, "boss", "ROCK"),
            3000 to MobInfo(3000, "Fiber Spirit", 3, "normal", "FIBER"),
            4000 to MobInfo(4000, "Hide Beast", 4, "boss", "HIDE"),
            5000 to MobInfo(5000, "Ore Elemental", 5, "boss", "ORE")
        )
    }
    
    // ==================== Zone Data ====================
    
    fun getZoneName(zoneId: Int): String {
        return zones[zoneId]?.name ?: "Unknown Zone"
    }
    
    fun isPvpZone(zoneId: Int): Boolean {
        return zones[zoneId]?.isPvp ?: false
    }
    
    private fun loadZones() {
        try {
            val inputStream = context.assets.open("data/zones.json")
            val reader = InputStreamReader(inputStream)
            val zoneList: List<ZoneInfo> = gson.fromJson(reader, object : TypeToken<List<ZoneInfo>>() {}.type)
            
            zones = zoneList.associateBy { it.id }
            reader.close()
            inputStream.close()
            
            Log.d(TAG, "Loaded ${zones.size} zone definitions")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading zones", e)
            loadDefaultZones()
        }
    }
    
    private fun loadDefaultZones() {
        zones = mapOf(
            1 to ZoneInfo(1, "Bridgewatch", false),
            2 to ZoneInfo(2, "Martlock", false),
            3 to ZoneInfo(3, "Thetford", false),
            4 to ZoneInfo(4, "Fort Sterling", false),
            5 to ZoneInfo(5, "Lymhurst", false),
            6 to ZoneInfo(6, "Caerleon", true),
            3000 to ZoneInfo(3000, "Black Zone", true),
            3001 to ZoneInfo(3001, "Red Zone", true),
            4000 to ZoneInfo(4000, "Yellow Zone", false)
        )
    }
    
    // ==================== Item Data ====================
    
    fun getItemName(itemId: Int): String {
        return items[itemId]?.name ?: "Unknown Item"
    }
    
    private fun loadItems() {
        try {
            val inputStream = context.assets.open("data/items.json")
            val reader = InputStreamReader(inputStream)
            val itemList: List<ItemInfo> = gson.fromJson(reader, object : TypeToken<List<ItemInfo>>() {}.type)
            
            items = itemList.associateBy { it.id }
            reader.close()
            inputStream.close()
            
            Log.d(TAG, "Loaded ${items.size} item definitions")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading items", e)
            loadDefaultItems()
        }
    }
    
    private fun loadDefaultItems() {
        items = emptyMap()
    }
    
    // ==================== Settings ====================
    
    fun getSettings(): RadarSettings {
        val prefs = context.getSharedPreferences("radar_settings", Context.MODE_PRIVATE)
        return RadarSettings(
            showResources = prefs.getBoolean("show_resources", true),
            showMobs = prefs.getBoolean("show_mobs", true),
            showPlayers = prefs.getBoolean("show_players", true),
            showDungeons = prefs.getBoolean("show_dungeons", true),
            showChests = prefs.getBoolean("show_chests", true),
            showFishing = prefs.getBoolean("show_fishing", true),
            showMist = prefs.getBoolean("show_mist", true),
            minTier = prefs.getInt("min_tier", 1),
            zoom = prefs.getFloat("zoom", 1f),
            showGrid = prefs.getBoolean("show_grid", true),
            showLabels = prefs.getBoolean("show_labels", true),
            alertSound = prefs.getBoolean("alert_sound", true),
            hostileAlert = prefs.getBoolean("hostile_alert", true),
            showOre = prefs.getBoolean("show_ore", true),
            showWood = prefs.getBoolean("show_wood", true),
            showRock = prefs.getBoolean("show_rock", true),
            showFiber = prefs.getBoolean("show_fiber", true),
            showHide = prefs.getBoolean("show_hide", true),
            showBosses = prefs.getBoolean("show_bosses", true),
            showVeteran = prefs.getBoolean("show_veteran", true),
            showNormalMobs = prefs.getBoolean("show_normal_mobs", false),
            hostileOnly = prefs.getBoolean("hostile_only", false)
        )
    }
    
    fun saveSettings(settings: RadarSettings) {
        val prefs = context.getSharedPreferences("radar_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("show_resources", settings.showResources)
            putBoolean("show_mobs", settings.showMobs)
            putBoolean("show_players", settings.showPlayers)
            putBoolean("show_dungeons", settings.showDungeons)
            putBoolean("show_chests", settings.showChests)
            putBoolean("show_fishing", settings.showFishing)
            putBoolean("show_mist", settings.showMist)
            putInt("min_tier", settings.minTier)
            putFloat("zoom", settings.zoom)
            putBoolean("show_grid", settings.showGrid)
            putBoolean("show_labels", settings.showLabels)
            putBoolean("alert_sound", settings.alertSound)
            putBoolean("hostile_alert", settings.hostileAlert)
            putBoolean("show_ore", settings.showOre)
            putBoolean("show_wood", settings.showWood)
            putBoolean("show_rock", settings.showRock)
            putBoolean("show_fiber", settings.showFiber)
            putBoolean("show_hide", settings.showHide)
            putBoolean("show_bosses", settings.showBosses)
            putBoolean("show_veteran", settings.showVeteran)
            putBoolean("show_normal_mobs", settings.showNormalMobs)
            putBoolean("hostile_only", settings.hostileOnly)
        }.apply()
    }
}

// ==================== Data Classes ====================

data class MobInfo(
    @SerializedName("t") val typeNumber: Int,
    @SerializedName("n") val name: String,
    @SerializedName("tier") val tier: Int,
    @SerializedName("c") val category: String, // normal, boss, veteran, living
    @SerializedName("f") val family: String    // HERETIC, KEEPER, UNDEAD, etc.
)

data class ZoneInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("n") val name: String,
    @SerializedName("pvp") val isPvp: Boolean
)

data class ItemInfo(
    @SerializedName("i") val id: Int,
    @SerializedName("n") val name: String,
    @SerializedName("t") val tier: Int,
    @SerializedName("p") val power: Int
)

data class RadarSettings(
    val showResources: Boolean = true,
    val showMobs: Boolean = true,
    val showPlayers: Boolean = true,
    val showDungeons: Boolean = true,
    val showChests: Boolean = true,
    val showFishing: Boolean = true,
    val showMist: Boolean = true,
    val minTier: Int = 1,
    val zoom: Float = 1f,
    val showGrid: Boolean = true,
    val showLabels: Boolean = true,
    val alertSound: Boolean = true,
    val hostileAlert: Boolean = true,
    val showOre: Boolean = true,
    val showWood: Boolean = true,
    val showRock: Boolean = true,
    val showFiber: Boolean = true,
    val showHide: Boolean = true,
    val showBosses: Boolean = true,
    val showVeteran: Boolean = true,
    val showNormalMobs: Boolean = false,
    val hostileOnly: Boolean = false
)
