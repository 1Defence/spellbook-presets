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
import net.runelite.client.config.Keybind;

import java.awt.*;
import java.awt.event.KeyEvent;

@ConfigGroup(SpellbookPresetsConfig.GROUP)
public interface SpellbookPresetsConfig extends Config
{
	String GROUP = "spellbookpresets";
	String ACTIVE_PRESETS_KEY = "activePresetsList";
	String SHOW_ALL_IF_EMPTY_KEY = "showAllIfEmpty";
	String SPELL_MOVE_MODE_KEY = "spellMoveMode";

	String CURRENT_PRESET_KEY = "currentPreset";

	String CURRENT_OPTION_RENDER_KEY = "currentOpRenderStyle";

	String CURRENT_OPTION_COLOR_KEY = "currentOpCustomColor";

	String MODIFY_OPTION_RENDER_KEY = "moidfyOpRenderStyle";
	String MODIFY_OPTION_HOTKEY_KEY = "modifyOpsHotkey";

	String NO_LOAD_OPTIONS_KEY = "noLoadOps";

	String OPEN_TAB_CONDITION_KEY = "openTabCondition";

	//in the situation that our data formatting/saving/loading changes. the version allows us to know how to handle migration.
	//for now it just gets set and exists.
	String LAST_VERSION_KEY = "version";
	String LIVE_VERSION_STRING = "1.0.0";

	enum SWAP_MODE { SWAP, INSERT}

	enum CURRENT_OPTION_STYLE {STANDARD, GREY_OUT, CUSTOM_COLOR, NO_RENDER}

	enum MODIFY_OPTION_STYLE {ALWAYS_RENDER,HOTKEY}

	enum OPEN_TAB_CONDITION {
		NONE(0),
		EDIT_PRESET(1),
		LOAD_PRESET(1 << 1),
		BOTH(EDIT_PRESET.value | LOAD_PRESET.value);
		public final int value;
		OPEN_TAB_CONDITION(int value) {
			this.value = value;
		}
	}

	@ConfigItem(
			position = 0,
			keyName = SHOW_ALL_IF_EMPTY_KEY,
			name = "Show All If Empty",
			description = "Show all spells if none have been specified to show."
	)
	default boolean showAllIfEmpty()
	{
		return true;
	}

	@ConfigItem(
			position = 1,
			keyName = SPELL_MOVE_MODE_KEY,
			name = "Spell Move Mode",
			description = "Insert spell (all spells shift forward), Swap spell (spell A swaps position with spell B)"
	)
	default SWAP_MODE spellMoveMode()
	{
		return SWAP_MODE.SWAP;
	}

	@ConfigItem(
			position = 2,
			keyName = CURRENT_OPTION_RENDER_KEY,
			name = "Current Render",
			description = "The rendering style of the load menu option for the currently enabled preset."
	)
	default CURRENT_OPTION_STYLE currentOptionRendering()
	{
		return CURRENT_OPTION_STYLE.GREY_OUT;
	}

	@ConfigItem(
			position = 3,
			keyName = CURRENT_OPTION_COLOR_KEY,
			name = "Current Custom Color",
			description = "The custom color used for the current load menu option, when Current Render is set to [Custom Coloring]"
	)
	default Color currentOptionCustomColor()
	{
		return new Color(37, 150, 190);
	}

	@ConfigItem(
			position = 4,
			keyName = MODIFY_OPTION_RENDER_KEY,
			name = "Modify Render",
			description = "The rendering style of the Edit-preset and Hide-preset options"
	)
	default MODIFY_OPTION_STYLE modifyOptionRendering()
	{
		return MODIFY_OPTION_STYLE.ALWAYS_RENDER;
	}

	@ConfigItem(
			position = 5,
			keyName = MODIFY_OPTION_HOTKEY_KEY,
			name = "Modify Key",
			description = "Holding this key renders the edit and hide preset options on spellbook right-click, when Modify Render is set to [Hotkey]"
	)
	default Keybind modifyOptionsKey()
	{
		return new Keybind(KeyEvent.VK_E, 0);
	}

	@ConfigItem(
			position = 6,
			keyName = NO_LOAD_OPTIONS_KEY,
			name = "No Load Ops",
			description = "When hotkey is held, load preset options won't be rendered"
	)
	default boolean noLoadOps()
	{
		return true;
	}

	@ConfigItem(
			position = 7,
			keyName = OPEN_TAB_CONDITION_KEY,
			name = "Open Tab",
			description = "The options that will additionally open the magic tab in game"
	)
	default OPEN_TAB_CONDITION openTabCondition()
	{
		return OPEN_TAB_CONDITION.EDIT_PRESET;
	}
}
