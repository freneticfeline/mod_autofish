# AutoFish Forge Mod by FreneticFeline

AutoFish Forge Mod is a Minecraft mod that provides the option to automatically reel
in and re-cast the fishing rod when a nibble is detected.  Primary ptions include
MultiRod, which automatically switches to the next available fishing rod in the hotbar
when the current rod breaks; and Break Protection, which will stop using a rod before
it breaks.  Additional options are available.

AutoFish bite detection is 100% accurate in Single Player games, where the mod has access to the
world server's state.  In MultiPlayer games, the mod must rely on the same hints given to players:
bobbing of the fish hook, water wake approaching the fish hook, the splash sounds, etc.  Therefore,
in MultiPlayer, bite detection is less accurate (around 85%).  See the "Aggressive Bite Detection"
option.

## Credits
This mod was originally a blatant copy of the functionality provided by the LiteMod
Autofish mod by troyboy50.  It is not affiliated with or endorsed by troyboy50.

## Installation Requirements
AutoFish Forge Mod versions 1.x-y.z require Minecraft 1.x with compatible version
of MinecraftForge installed.

## Installation Instructions
Copy the `mod_autofish_forge-1.x-y.z.jar` file to the `mods` directory in your Minecraft
data directory.

## Usage Instructions
By default, AutoFish is enabled, but all other options are disabled.  To change
these options, Click the **[Mods]** button from the opening Minecraft screen, locate the
AutoFish Forge Mod in the mods list, and click the **[Configure]** button.

The AutoFish Forge Mod options can also be reached in-game using the configured hotkey.
_Note that the hotkey will only function if the item currently in use by the player is a fishing rod._
By default, the hotkey is the letter `o`, but this binding can be changed or disabled in the
standard Minecraft **Controls** screen.

## Configuration Options

### Enable AutoFish
Automatically reel in and re-cast when a fish nibbles the hook.  If this is set to **false**, all mod
functionality is disabled.

### Enable MultiRod
Automatically switch to a new fishing rod when the current rod breaks, if one is available in the hotbar.

### Enable Break Protection
Stop fishing (or switch to a new rod if MultiRod is enabled and another rod is available) before the
current rod breaks.  Useful if you want to repair your enchanted fishing rods.

### Re-Cast Delay
Time (in seconds) to wait before automatically re-casting.  Increase this value if server lag causes
re-casting to fail.

### Enable Entity Clear Protection
When playing on a server, re-cast after the server clears entities.  Useful if you are playing on a server
that periodically deletes all entities (including fishing hooks) from the world, which causes you to
stop fishing.

### Aggressive Bite Detection
When playing on a server, be more aggressive about detecting fish bites.  Improves multiplayer bite
detection from ~85% to ~95%, but false positives will be more likely, especially if other players
are fishing very close by.

### Handle Problems
[HAS SIDE EFFECTS] Re-cast when problems detected (e.g. if not in water or if a MOB has been hooked).
If enabled, non-fishing use of the fishing rod (e.g. intentionally hooking MOBs) will be affected.

### Enable Fast Fishing
[SINGLE PLAYER ONLY] Fish will bite right after casting.


## Development Setup Instructions
Follow the standard Forge mod development setup instructions.  They can be found
in the file named `README-MinecraftForge.txt`
