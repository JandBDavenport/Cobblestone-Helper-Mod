# Cobblestone Helper Mod

A Fabric client-side mod for play.cobblestone.gg that enhances the player experience.

## Features

###  Player Hiding
Toggle visibility of other players with a single command or keybind. Useful for reducing lag and visual clutter in crowded areas.
Note: hidden players are still able to be interacted with.

###  Shady Summoner HUD
Tracks the Shady Summoner respawn timer with a persistent 30-minute countdown. Displays bell alerts at 60 seconds and 30 seconds remaining. Timer persists across game restarts.

###  Bazaar Overlay
Displays current Bazaar crop prices directly in the container GUI. Automatically scrapes multi-page price data and caches results for 2 minutes. Includes best-crop highlighting in the Farm Warps menu.

###  Farm Warps
Quick-access grid of farm warp commands organized by category. Click any farm to teleport instantly. Highlights the best Bazaar crop for easy reference.

## Installation

1. Download the latest JAR from [Releases](https://github.com/JandBDavenport/Cobblestone-Helper-Mod/releases)
2. Place it in your `.minecraft/mods` folder
3. Requires Fabric Loader and Fabric API

## Commands

| Command | Description |
|---|---|
| `/wf` | Open Farm Warps screen |
| `/hideplayers` | Toggle player hiding |
| `/shadypos` | Reposition Shady Summoner HUD |
| `/clearbazaarcache` | Clear Bazaar price cache |

## Keybindings

| Key | Action |
|---|---|
| <kbd>H</kbd> | Toggle hide players |
| <kbd>R</kbd> | Open Farm Warps screen |

## License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0). Any modifications or derivatives must also be released under GPL-3.0. See [LICENSE](LICENSE) for details.
