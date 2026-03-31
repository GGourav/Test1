package com.albionradar.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.albionradar.R
import com.albionradar.data.DataManager
import com.albionradar.data.RadarSettings

class SettingsActivity : AppCompatActivity() {

    private var settings: RadarSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings)
        
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        settings = DataManager.getSettings()
        val s = settings ?: return
        
        // Radar settings
        findViewById<SeekBar>(R.id.zoomSeekBar).progress = (s.zoom * 50).toInt()
        findViewById<Switch>(R.id.showGridSwitch).isChecked = s.showGrid
        findViewById<Switch>(R.id.showLabelsSwitch).isChecked = s.showLabels
        
        // Alerts
        findViewById<Switch>(R.id.alertSoundSwitch).isChecked = s.alertSound
        findViewById<Switch>(R.id.hostileAlertSwitch).isChecked = s.hostileAlert
        
        // Resources
        findViewById<Switch>(R.id.showOreSwitch).isChecked = s.showOre
        findViewById<Switch>(R.id.showWoodSwitch).isChecked = s.showWood
        findViewById<Switch>(R.id.showRockSwitch).isChecked = s.showRock
        findViewById<Switch>(R.id.showFiberSwitch).isChecked = s.showFiber
        findViewById<Switch>(R.id.showHideSwitch).isChecked = s.showHide
        
        // Mobs
        findViewById<Switch>(R.id.showBossSwitch).isChecked = s.showBosses
        findViewById<Switch>(R.id.showVeteranSwitch).isChecked = s.showVeteran
        findViewById<Switch>(R.id.showNormalMobsSwitch).isChecked = s.showNormalMobs
        
        // Players
        findViewById<Switch>(R.id.showPlayersSwitch).isChecked = s.showPlayers
        findViewById<Switch>(R.id.hostileOnlySwitch).isChecked = s.hostileOnly
        
        // Other
        findViewById<Switch>(R.id.showDungeonsSwitch).isChecked = s.showDungeons
        findViewById<Switch>(R.id.showChestsSwitch).isChecked = s.showChests
        findViewById<Switch>(R.id.showFishingSwitch).isChecked = s.showFishing
        findViewById<Switch>(R.id.showMistSwitch).isChecked = s.showMist
        
        // Tier filter
        findViewById<SeekBar>(R.id.minTierSeekBar).progress = s.minTier - 1
    }

    private fun setupListeners() {
        // Zoom seek bar
        findViewById<SeekBar>(R.id.zoomSeekBar).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    saveSettings()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )
        
        // Tier seek bar
        findViewById<SeekBar>(R.id.minTierSeekBar).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    saveSettings()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )
        
        // All switches
        val switchIds = listOf(
            R.id.showGridSwitch, R.id.showLabelsSwitch,
            R.id.alertSoundSwitch, R.id.hostileAlertSwitch,
            R.id.showOreSwitch, R.id.showWoodSwitch, R.id.showRockSwitch,
            R.id.showFiberSwitch, R.id.showHideSwitch,
            R.id.showBossSwitch, R.id.showVeteranSwitch, R.id.showNormalMobsSwitch,
            R.id.showPlayersSwitch, R.id.hostileOnlySwitch,
            R.id.showDungeonsSwitch, R.id.showChestsSwitch, R.id.showFishingSwitch, R.id.showMistSwitch
        )
        
        switchIds.forEach { id ->
            findViewById<Switch>(id).setOnCheckedChangeListener { _, _ ->
                saveSettings()
            }
        }
    }

    private fun saveSettings() {
        val newSettings = RadarSettings(
            zoom = findViewById<SeekBar>(R.id.zoomSeekBar).progress / 50f,
            showGrid = findViewById<Switch>(R.id.showGridSwitch).isChecked,
            showLabels = findViewById<Switch>(R.id.showLabelsSwitch).isChecked,
            alertSound = findViewById<Switch>(R.id.alertSoundSwitch).isChecked,
            hostileAlert = findViewById<Switch>(R.id.hostileAlertSwitch).isChecked,
            showOre = findViewById<Switch>(R.id.showOreSwitch).isChecked,
            showWood = findViewById<Switch>(R.id.showWoodSwitch).isChecked,
            showRock = findViewById<Switch>(R.id.showRockSwitch).isChecked,
            showFiber = findViewById<Switch>(R.id.showFiberSwitch).isChecked,
            showHide = findViewById<Switch>(R.id.showHideSwitch).isChecked,
            showBosses = findViewById<Switch>(R.id.showBossSwitch).isChecked,
            showVeteran = findViewById<Switch>(R.id.showVeteranSwitch).isChecked,
            showNormalMobs = findViewById<Switch>(R.id.showNormalMobsSwitch).isChecked,
            showPlayers = findViewById<Switch>(R.id.showPlayersSwitch).isChecked,
            hostileOnly = findViewById<Switch>(R.id.hostileOnlySwitch).isChecked,
            showDungeons = findViewById<Switch>(R.id.showDungeonsSwitch).isChecked,
            showChests = findViewById<Switch>(R.id.showChestsSwitch).isChecked,
            showFishing = findViewById<Switch>(R.id.showFishingSwitch).isChecked,
            showMist = findViewById<Switch>(R.id.showMistSwitch).isChecked,
            minTier = findViewById<SeekBar>(R.id.minTierSeekBar).progress + 1
        )
        
        DataManager.saveSettings(newSettings)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
