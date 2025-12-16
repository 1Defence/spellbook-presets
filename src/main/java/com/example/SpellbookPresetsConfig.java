/*
 * Copyright (c) 2025, Jamal <http://github.com/1Defence>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

	String CURRENT_PRESET_KEY = "currentPreset";

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
