package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SpellbookPresetsConfig.GROUP)
public interface SpellbookPresetsConfig extends Config
{
	String GROUP = "spellbookpresets";
	String ACTIVE_PRESETS_KEY = "activePresetsList";
	String SHOW_ALL_IF_EMPTY_KEY = "showAllIfEmpty";
	String SPELL_MOVE_MODE_KEY = "spellMoveMode";

	//in the situation that our data formatting/saving/loading changes. the version allows us to know how to handle migration.
	//for now it just gets set and exists.
	String LAST_VERSION_KEY = "version";
	String LIVE_VERSION_STRING = "1.0.0";

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
