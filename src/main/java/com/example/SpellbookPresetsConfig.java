package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SpellbookPresetsConfig.GROUP)
public interface SpellbookPresetsConfig extends Config
{
	String GROUP = "spellbookpresets";
	//allows us to set the config on first-run, but not reset it to this value if the user sets the active presets to be empty.
	String FIRST_RUN_ACTIVEPRESETS_STRING = "Preset 1\nPreset 2";
	String FIRST_RUN_ACTIVEPRESETS_KEY = "setActivePresets";

	@ConfigItem(
			position = 0,
			keyName = "activePresets",
			name = "Active Presets (Max 10)",
			description = "Active Filter presets, separate by new line. Max of 10 -- you can have more than 10 saved but not active at once."
	)
	default String getActivePresets()
	{
		return "";
	}

	@ConfigItem(
			position = 1,
			keyName = "savedPresets",
			name = "Saved Presets (Remove/View Only)",
			description = "Saved Filter presets, remove any you wish to be unsaved"
	)
	default String getSavedPresets()
	{
		return "";
	}

	@ConfigItem(
			position = 2,
			keyName = "showAllIfEmpty",
			name = "Show All If Empty",
			description = "Show all spells if none have been specified to show."
	)
	default boolean showAllIfEmpty()
	{
		return true;
	}
}
