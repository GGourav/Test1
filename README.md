# Albion Radar Android

A real-time radar for Albion Online on Android devices.

## Features

- **Resource Detection**: Track ore, wood, rock, fiber, and hide nodes with tier and enchantment info
- **Mob Detection**: See mobs, veterans, and bosses with health tracking
- **Player Detection**: Detect nearby players with guild/alliance info and faction status
- **Dungeon Detection**: Find dungeon entrances
- **Chest Detection**: Locate treasure and loot chests
- **Fishing Spots**: Find fishing zones
- **Mist Portals**: Track mist portal locations

## Requirements

- Android device running Android 8.0 (API 26) or higher
- Albion Online game client

## Installation

1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from Unknown Sources" in your device settings
3. Install the APK
4. Grant VPN permission when prompted (required for packet capture)
5. Grant Overlay permission when prompted (required for radar overlay)

## Usage

1. Start Albion Online
2. Open Albion Radar
3. Tap "Start VPN" to begin packet capture
4. Tap "Show Overlay" to display the radar overlay
5. Use the tabs at the bottom to view entity lists
6. Configure filters in Settings

## How It Works

The app uses Android's VpnService API to capture network packets from the game client. It parses the Photon protocol used by Albion Online and extracts entity information from game events.

## Building

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/albion-radar-android.git
cd albion-radar-android

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
Architecture
app/src/main/java/com/albionradar/
├── AlbionRadarApp.kt          # Application class
├── MainActivity.kt            # Main activity
├── vpn/
│   └── AlbionVpnService.kt    # VPN packet capture
├── photon/
│   ├── PhotonParser.kt        # Protocol parser
│   ├── PhotonTypes.kt         # Type constants
│   └── PhotonValue.kt         # Value types
├── data/
│   ├── EntityManager.kt       # Entity tracking
│   ├── EntityType.kt          # Entity definitions
│   ├── GameEvent.kt           # Event model
│   └── DataManager.kt         # Static data
├── overlay/
│   └── RadarOverlayService.kt # Floating overlay
└── ui/
    ├── RadarView.kt           # Radar canvas
    ├── EntityAdapter.kt       # List adapter
    └── SettingsActivity.kt    # Settings
Known Limitations
Player positions may not always be accurate due to encryption
Resource charges/sizes are estimated
Game updates may require app updates
License
MIT License - See LICENSE file for details

Disclaimer
This tool is for educational purposes only. Use at your own risk. The developers are not responsible for any consequences of using this software, including but not limited to game account bans.

Albion Online is a trademark of Sandbox Interactive GmbH.

text


---
Let me fix the SettingsActivity and add the remaining files:

File: app/src/main/java/com/albionradar/ui/SettingsActivity.kt (Fixed)
text

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
}
File: gradle/wrapper/gradle-wrapper.properties
text

distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
File: gradlew (Make executable with chmod +x gradlew)
text

#!/bin/sh

#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
#
#   Gradle start up script for POSIX generated by Gradle.
#
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in                #(
  CYGWIN* )         cygwin=true  ;; #(
  Darwin* )         darwin=true  ;; #(
  MSYS* | MINGW* )  msys=true    ;; #(
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
exec "$JAVACMD" "$@"
📋 FILE STRUCTURE SUMMARY
Here's the complete file structure for the GitHub repository:

text

albion-radar-android/
├── .github/
│   └── workflows/
│       └── build.yml                    # GitHub Actions auto-build
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/albionradar/
│   │       │   ├── AlbionRadarApp.kt    # Application class
│   │       │   ├── MainActivity.kt      # Main activity
│   │       │   ├── vpn/
│   │       │   │   └── AlbionVpnService.kt
│   │       │   ├── photon/
│   │       │   │   ├── PhotonParser.kt
│   │       │   │   ├── PhotonTypes.kt
│   │       │   │   └── PhotonValue.kt
│   │       │   ├── data/
│   │       │   │   ├── DataManager.kt
│   │       │   │   ├── EntityManager.kt
│   │       │   │   ├── EntityType.kt
│   │       │   │   └── GameEvent.kt
│   │       │   ├── overlay/
│   │       │   │   └── RadarOverlayService.kt
│   │       │   └── ui/
│   │       │       ├── EntityAdapter.kt
│   │       │       ├── RadarView.kt
│   │       │       └── SettingsActivity.kt
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   ├── activity_main.xml
│   │       │   │   ├── activity_settings.xml
│   │       │   │   └── overlay_radar.xml
│   │       │   └── values/
│   │       │       ├── colors.xml
│   │       │       ├── strings.xml
│   │       │       └── themes.xml
│   │       ├── assets/
│   │       │   └── data/
│   │       │       ├── mobs.json
│   │       │       ├── zones.json
│   │       │       └── items.json
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── .gitignore
└── README.md
