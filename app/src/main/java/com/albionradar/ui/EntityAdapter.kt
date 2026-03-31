package com.albionradar.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.albionradar.R
import com.albionradar.data.*

/**
 * RecyclerView adapter for displaying entity lists
 */
class EntityAdapter : ListAdapter<Entity, EntityAdapter.EntityViewHolder>(EntityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return EntityViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        val entity = getItem(position)
        holder.bind(entity)
    }

    class EntityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(entity: Entity) {
            when (entity) {
                is ResourceEntity -> bindResource(entity)
                is MobEntity -> bindMob(entity)
                is PlayerEntity -> bindPlayer(entity)
                is DungeonEntity -> bindDungeon(entity)
                is ChestEntity -> bindChest(entity)
                is FishingEntity -> bindFishing(entity)
                is MistPortalEntity -> bindMistPortal(entity)
            }
        }

        private fun bindResource(resource: ResourceEntity) {
            val enchantStr = if (resource.enchantment > 0) ".${resource.enchantment}" else ""
            text1.text = "T${resource.tier}$enchantStr ${resource.typeName}"
            text1.setTextColor(resource.getColor())
            
            val posStr = if (resource.isLiving) "Living" else "Static"
            text2.text = "Size: ${resource.size} | $posStr | (${resource.x.toInt()}, ${resource.y.toInt()})"
            text2.setTextColor(Color.GRAY)
        }

        private fun bindMob(mob: MobEntity) {
            val categoryStr = when {
                mob.isBoss() -> "★ BOSS"
                mob.isVeteran() -> "♦ Veteran"
                else -> ""
            }
            text1.text = "${mob.name} $categoryStr"
            text1.setTextColor(mob.getColor())
            
            val healthStr = if (mob.healthPercent < 1f) "HP: ${(mob.healthPercent * 100).toInt()}%" else ""
            val enchantStr = if (mob.enchantment > 0) "E${mob.enchantment}" else ""
            text2.text = "$healthStr $enchantStr (${mob.x.toInt()}, ${mob.y.toInt()})"
            text2.setTextColor(Color.GRAY)
        }

        private fun bindPlayer(player: PlayerEntity) {
            val threatStr = when {
                player.isHostile() -> "⚠ HOSTILE"
                player.isFaction() -> "⚑ Faction"
                else -> ""
            }
            text1.text = "${player.getDisplayName()} $threatStr"
            text1.setTextColor(player.getColor())
            
            val healthStr = if (player.maxHealth > 0) {
                "HP: ${player.currentHealth.toInt()}/${player.maxHealth.toInt()}"
            } else ""
            val posStr = if (player.hasKnownPosition()) {
                "(${player.x.toInt()}, ${player.y.toInt()})"
            } else {
                "Position Unknown"
            }
            text2.text = "$healthStr $posStr"
            text2.setTextColor(Color.GRAY)
        }

        private fun bindDungeon(dungeon: DungeonEntity) {
            text1.text = "Dungeon Entrance"
            text1.setTextColor(dungeon.getColor())
            text2.text = "Type: ${dungeon.dungeonType} | (${dungeon.x.toInt()}, ${dungeon.y.toInt()})"
            text2.setTextColor(Color.GRAY)
        }

        private fun bindChest(chest: ChestEntity) {
            text1.text = "Treasure Chest"
            text1.setTextColor(chest.getColor())
            text2.text = "(${chest.x.toInt()}, ${chest.y.toInt()})"
            text2.setTextColor(Color.GRAY)
        }

        private fun bindFishing(fishing: FishingEntity) {
            text1.text = "Fishing Spot"
            text1.setTextColor(fishing.getColor())
            text2.text = "(${fishing.x.toInt()}, ${fishing.y.toInt()})"
            text2.setTextColor(Color.GRAY)
        }

        private fun bindMistPortal(portal: MistPortalEntity) {
            val rarityStr = when (portal.rarity) {
                1 -> "Common"
                2 -> "Uncommon"
                3 -> "Rare"
                else -> "Unknown"
            }
            text1.text = "Mist Portal ($rarityStr)"
            text1.setTextColor(portal.getColor())
            text2.text = "(${portal.x.toInt()}, ${portal.y.toInt()})"
            text2.setTextColor(Color.GRAY)
        }
    }

    class EntityDiffCallback : DiffUtil.ItemCallback<Entity>() {
        override fun areItemsTheSame(oldItem: Entity, newItem: Entity): Boolean {
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: Entity, newItem: Entity): Boolean {
            return oldItem == newItem
        }
    }
}
