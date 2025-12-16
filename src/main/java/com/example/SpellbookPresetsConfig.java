package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SpellbookPresetsConfig.GROUP)
public interface SpellbookPresetsConfig extends Config
{
	String GROUP = "spellbookpresets";
	String ACTIVE_PRESETS_KEY = "activePresetsList";
	enum SWAP_MODE { SWAP, INSERT}

	@ConfigItem(
			position = 0,
			keyName = "showAllIfEmpty",
			name = "Show All If Empty",
			description = "Show all spells if none have been specified to show."
	)
	default boolean showAllIfEmpty()
	{
		return true;
	}

	@ConfigItem(
			position = 1,
			keyName = "spellMoveMode",
			name = "Spell Move Mode",
			description = "Insert spell (all spells shift forward), Swap spell (spell A swaps position with spell B)"
	)
	default SWAP_MODE spellMoveMode()
	{
		return SWAP_MODE.SWAP;
	}
}
