package com.albionradar.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.albionradar.data.*
import kotlin.math.*

/**
 * Custom View for rendering the radar display
 */
class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#33FFFFFF")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val centerPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.FILL_AND_STROKE
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    
    private val resourcePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val mobPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val playerPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val healthBarPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    
    private val healthBarBgPaint = Paint().apply {
        color = Color.parseColor("#66000000")
        style = Paint.Style.FILL
    }

    // Settings
    private var zoom: Float = 1f
    private var showGrid: Boolean = true
    private var showLabels: Boolean = true
    private var minTier: Int = 1
    
    // Entity filters
    private var showResources: Boolean = true
    private var showMobs: Boolean = true
    private var showPlayers: Boolean = true
    private var showDungeons: Boolean = true
    private var showChests: Boolean = true
    private var showFishing: Boolean = true
    private var showMist: Boolean = true
    
    // Resource filters
    private var showOre: Boolean = true
    private var showWood: Boolean = true
    private var showRock: Boolean = true
    private var showFiber: Boolean = true
    private var showHide: Boolean = true
    
    // Mob filters
    private var showBosses: Boolean = true
    private var showVeteran: Boolean = true
    private var showNormalMobs: Boolean = false
    
    // Player filters
    private var hostileOnly: Boolean = false

    // Entity data
    private var entities: List<Entity> = emptyList()
    
    // Local player position (center of radar)
    private var localX: Float = 0f
    private var localY: Float = 0f

    fun updateSettings(settings: RadarSettings) {
        zoom = settings.zoom
        showGrid = settings.showGrid
        showLabels = settings.showLabels
        minTier = settings.minTier
        
        showResources = settings.showResources
        showMobs = settings.showMobs
        showPlayers = settings.showPlayers
        showDungeons = settings.showDungeons
        showChests = settings.showChests
        showFishing = settings.showFishing
        showMist = settings.showMist
        
        showOre = settings.showOre
        showWood = settings.showWood
        showRock = settings.showRock
        showFiber = settings.showFiber
        showHide = settings.showHide
        
        showBosses = settings.showBosses
        showVeteran = settings.showVeteran
        showNormalMobs = settings.showNormalMobs
        
        hostileOnly = settings.hostileOnly
        
        invalidate()
    }

    fun updateEntities(entities: List<Entity>) {
        this.entities = entities
        invalidate()
    }
    
    fun updateLocalPosition(x: Float, y: Float) {
        this.localX = x
        this.localY = y
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw background
        canvas.drawColor(Color.parseColor("#CC000000"))
        
        // Draw grid
        if (showGrid) {
            drawGrid(canvas, centerX, centerY)
        }
        
        // Draw center point (local player)
        canvas.drawCircle(centerX, centerY, 6f, centerPaint)
        
        // Draw entities
        entities.forEach { entity ->
            drawEntity(canvas, entity, centerX, centerY)
        }
    }

    private fun drawGrid(canvas: Canvas, centerX: Float, centerY: Float) {
        val gridSize = 100f * zoom
        val gridCount = (max(width, height) / gridSize / 2).toInt() + 1
        
        // Vertical lines
        for (i in -gridCount..gridCount) {
            val x = centerX + i * gridSize
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        
        // Horizontal lines
        for (i in -gridCount..gridCount) {
            val y = centerY + i * gridSize
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
        
        // Draw concentric circles
        for (i in 1..5) {
            canvas.drawCircle(centerX, centerY, i * gridSize, gridPaint)
        }
    }

    private fun drawEntity(canvas: Canvas, entity: Entity, centerX: Float, centerY: Float) {
        // Apply filters
        if (!shouldDisplayEntity(entity)) return
        
        // Calculate screen position
        val screenPos = worldToScreen(entity.x, entity.y, centerX, centerY)
        
        // Check if on screen
        if (screenPos.first < 0 || screenPos.first > width ||
            screenPos.second < 0 || screenPos.second > height) {
            return
        }
        
        val x = screenPos.first
        val y = screenPos.second
        
        when (entity) {
            is ResourceEntity -> drawResource(canvas, entity, x, y)
            is MobEntity -> drawMob(canvas, entity, x, y)
            is PlayerEntity -> drawPlayer(canvas, entity, x, y)
            is DungeonEntity -> drawDungeon(canvas, entity, x, y)
            is ChestEntity -> drawChest(canvas, entity, x, y)
            is FishingEntity -> drawFishing(canvas, entity, x, y)
            is MistPortalEntity -> drawMistPortal(canvas, entity, x, y)
        }
    }

    private fun shouldDisplayEntity(entity: Entity): Boolean {
        return when (entity) {
            is ResourceEntity -> {
                if (!showResources) return false
                if (entity.tier < minTier) return false
                when (entity.typeName) {
                    "ORE" -> showOre
                    "WOOD" -> showWood
                    "ROCK" -> showRock
                    "FIBER" -> showFiber
                    "HIDE" -> showHide
                    else -> true
                }
            }
            is MobEntity -> {
                if (!showMobs) return false
                when {
                    entity.isBoss() -> showBosses
                    entity.isVeteran() -> showVeteran
                    else -> showNormalMobs
                }
            }
            is PlayerEntity -> {
                if (!showPlayers) return false
                if (hostileOnly && !entity.isHostile()) return false
                true
            }
            is DungeonEntity -> showDungeons
            is ChestEntity -> showChests
            is FishingEntity -> showFishing
            is MistPortalEntity -> showMist
            else -> true
        }
    }

    private fun drawResource(canvas: Canvas, resource: ResourceEntity, x: Float, y: Float) {
        val size = (8 + resource.enchantment * 2 + (resource.tier - 1)).toFloat()
        
        resourcePaint.color = resource.getColor()
        canvas.drawCircle(x, y, size, resourcePaint)
        
        // Draw enchantment indicator
        if (resource.enchantment > 0) {
            textPaint.textSize = 16f
            canvas.drawText(".${resource.enchantment}", x + size, y, textPaint)
        }
        
        // Draw size indicator
        if (showLabels && resource.size > 0) {
            textPaint.textSize = 12f
            canvas.drawText(resource.size.toString(), x, y + size + 12, textPaint)
        }
    }

    private fun drawMob(canvas: Canvas, mob: MobEntity, x: Float, y: Float) {
        val size = if (mob.isBoss()) 16f else if (mob.isVeteran()) 12f else 8f
        
        mobPaint.color = mob.getColor()
        
        // Draw as diamond for bosses
        if (mob.isBoss()) {
            val path = Path()
            path.moveTo(x, y - size)
            path.lineTo(x + size, y)
            path.lineTo(x, y + size)
            path.lineTo(x - size, y)
            path.closePath()
            canvas.drawPath(path, mobPaint)
        } else {
            canvas.drawCircle(x, y, size, mobPaint)
        }
        
        // Draw health bar
        if (mob.healthPercent < 1f) {
            val barWidth = 30f
            val barHeight = 4f
            canvas.drawRect(x - barWidth/2, y + size + 2, x + barWidth/2, y + size + 2 + barHeight, healthBarBgPaint)
            healthBarPaint.color = Color.parseColor("#FF4CAF50")
            canvas.drawRect(x - barWidth/2, y + size + 2, x - barWidth/2 + barWidth * mob.healthPercent, y + size + 2 + barHeight, healthBarPaint)
        }
        
        if (showLabels) {
            textPaint.textSize = 12f
            textPaint.color = mob.getColor()
            canvas.drawText(mob.name, x + size + 4, y + 4, textPaint)
            textPaint.color = Color.WHITE
        }
    }

    private fun drawPlayer(canvas: Canvas, player: PlayerEntity, x: Float, y: Float) {
        // Unknown position - draw at edge with indicator
        if (!player.hasKnownPosition()) {
            // Draw at edge of radar with arrow pointing outward
            val angle = atan2(y - height/2f, x - width/2f)
            val edgeX = width/2f + cos(angle) * (width/2f - 20)
            val edgeY = height/2f + sin(angle) * (height/2f - 20)
            
            playerPaint.color = Color.parseColor("#66FFFFFF")
            canvas.drawCircle(edgeX, edgeY, 8f, playerPaint)
            
            textPaint.textSize = 10f
            canvas.drawText("?", edgeX - 3, edgeY + 3, textPaint)
            return
        }
        
        val size = 10f
        playerPaint.color = player.getColor()
        
        // Draw player as square
        canvas.drawRect(x - size, y - size, x + size, y + size, playerPaint)
        
        // Draw health bar
        if (player.maxHealth > 0 && player.currentHealth < player.maxHealth) {
            val barWidth = 40f
            val barHeight = 4f
            val healthPercent = player.currentHealth / player.maxHealth
            
            canvas.drawRect(x - barWidth/2, y + size + 2, x + barWidth/2, y + size + 2 + barHeight, healthBarBgPaint)
            healthBarPaint.color = if (player.isDead()) Color.RED else Color.parseColor("#FF4CAF50")
            canvas.drawRect(x - barWidth/2, y + size + 2, x - barWidth/2 + barWidth * healthPercent, y + size + 2 + barHeight, healthBarPaint)
        }
        
        // Draw name
        if (showLabels) {
            textPaint.textSize = 12f
            textPaint.color = player.getColor()
            canvas.drawText(player.getDisplayName(), x + size + 4, y + 4, textPaint)
            textPaint.color = Color.WHITE
            
            // Hostile indicator
            if (player.isHostile()) {
                textPaint.color = Color.RED
                canvas.drawText("⚠", x - 15, y + 4, textPaint)
                textPaint.color = Color.WHITE
            }
        }
    }

    private fun drawDungeon(canvas: Canvas, dungeon: DungeonEntity, x: Float, y: Float) {
        mobPaint.color = dungeon.getColor()
        
        // Draw as triangle
        val path = Path()
        path.moveTo(x, y - 10)
        path.lineTo(x + 10, y + 8)
        path.lineTo(x - 10, y + 8)
        path.closePath()
        canvas.drawPath(path, mobPaint)
        
        if (showLabels) {
            textPaint.textSize = 10f
            canvas.drawText("D", x - 3, y + 3, textPaint)
        }
    }

    private fun drawChest(canvas: Canvas, chest: ChestEntity, x: Float, y: Float) {
        resourcePaint.color = chest.getColor()
        
        // Draw as square
        canvas.drawRect(x - 8, y - 8, x + 8, y + 8, resourcePaint)
        
        if (showLabels) {
            textPaint.textSize = 10f
            canvas.drawText("$", x - 4, y + 4, textPaint)
        }
    }

    private fun drawFishing(canvas: Canvas, fishing: FishingEntity, x: Float, y: Float) {
        resourcePaint.color = fishing.getColor()
        
        // Draw as fish shape (simplified as oval)
        canvas.drawOval(x - 10, y - 5, x + 10, y + 5, resourcePaint)
    }

    private fun drawMistPortal(canvas: Canvas, portal: MistPortalEntity, x: Float, y: Float) {
        mobPaint.color = portal.getColor()
        
        // Draw as hexagon
        val path = Path()
        for (i in 0 until 6) {
            val angle = i * PI / 3 - PI / 6
            val px = x + 10 * cos(angle).toFloat()
            val py = y + 10 * sin(angle).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.closePath()
        canvas.drawPath(path, mobPaint)
    }

    /**
     * Convert world coordinates to screen coordinates
     */
    private fun worldToScreen(worldX: Float, worldY: Float, centerX: Float, centerY: Float): Pair<Float, Float> {
        val scale = zoom * 4 // Albion uses ~4 units per meter
        
        val screenX = centerX + (worldX - localX) * scale
        val screenY = centerY + (worldY - localY) * scale
        
        return Pair(screenX, screenY)
    }
}
