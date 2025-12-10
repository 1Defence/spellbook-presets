/*
 * Copyright (c) 2024, Adam <Adam@sigterm.info>
 * Copyright (c) 2025, 1Defence
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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ItemComposition;
import net.runelite.api.ParamID;
import net.runelite.api.ScriptID;
import net.runelite.api.events.DraggingWidgetChanged;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import static net.runelite.api.widgets.WidgetConfig.DRAG;
import static net.runelite.api.widgets.WidgetConfig.DRAG_ON;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import static com.example.SpellbookPresetsConfig.GROUP;
import static com.example.SpellbookPresetsConfig.FIRST_RUN_ACTIVEPRESETS_KEY;
import static com.example.SpellbookPresetsConfig.FIRST_RUN_ACTIVEPRESETS_STRING;
import static com.example.SpellbookPresetsConfig.SWAP_MODE;

@PluginDescriptor(
		name = "Spellbook Presets",
		description = "Filtered spellbook presets, select spells you want to show for each given preset",
		conflicts = "Spellbook"
)
@Slf4j
public class SpellbookPresetsPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MenuManager menuManager;

	@Inject
	private Gson gson;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SpellbookPresetsConfig config;

	private static final String LOCK = "Save-spells";
	private static final String UNLOCK = "Edit-spells";
	// [proc,magic_spellbook_modifyops] shifts around ops, but seems to only use
	// 1 2 3 4 5 10. So use 6 for Hide/Unhide.
	private static final int HIDE_UNHIDE_OP = 6;

	private static final String DISABLE = "Hide preset";
	private static final String ENABLE = "Show preset";

	//menu ops for the 3 client sizes for editing/saving a preset & hiding/showing a preset
	private static final WidgetMenuOption FIXED_MAGIC_TAB_LOCK = new WidgetMenuOption(LOCK,
			"", InterfaceID.Toplevel.STONE6);
	private static final WidgetMenuOption FIXED_MAGIC_TAB_UNLOCK = new WidgetMenuOption(UNLOCK,
			"", InterfaceID.Toplevel.STONE6);

	private static final WidgetMenuOption FIXED_MAGIC_TAB_DISABLE = new WidgetMenuOption(DISABLE,
			"", InterfaceID.Toplevel.STONE6);
	private static final WidgetMenuOption FIXED_MAGIC_TAB_ENABLE = new WidgetMenuOption(ENABLE,
			"", InterfaceID.Toplevel.STONE6);

	private static final WidgetMenuOption RESIZABLE_MAGIC_TAB_LOCK = new WidgetMenuOption(LOCK,
			"", InterfaceID.ToplevelOsrsStretch.STONE6);
	private static final WidgetMenuOption RESIZABLE_MAGIC_TAB_UNLOCK = new WidgetMenuOption(UNLOCK,
			"", InterfaceID.ToplevelOsrsStretch.STONE6);

	private static final WidgetMenuOption RESIZABLE_MAGIC_TAB_DISABLE = new WidgetMenuOption(DISABLE,
			"", InterfaceID.ToplevelOsrsStretch.STONE6);
	private static final WidgetMenuOption RESIZABLE_MAGIC_TAB_ENABLE = new WidgetMenuOption(ENABLE,
			"", InterfaceID.ToplevelOsrsStretch.STONE6);

	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_MAGIC_TAB_LOCK = new WidgetMenuOption(LOCK,
			"", InterfaceID.ToplevelPreEoc.STONE6);
	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_MAGIC_TAB_UNLOCK = new WidgetMenuOption(UNLOCK,
			"", InterfaceID.ToplevelPreEoc.STONE6);

	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_MAGIC_TAB_DISABLE = new WidgetMenuOption(DISABLE,
			"", InterfaceID.ToplevelPreEoc.STONE6);
	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_MAGIC_TAB_ENABLE = new WidgetMenuOption(ENABLE,
			"", InterfaceID.ToplevelPreEoc.STONE6);


	private final Pattern NEWLINESPLITTER = Pattern.compile("\n");
	private final String DEFAULT_PRESET = "Default";
	private final int visibleOpacity = 0;
	private final int hiddenOpacity = 200;

	private boolean reordering;
	private boolean ignoreNextConfigEvent;
	private boolean showAllIfEmpty;
	private boolean filteringEnabled;

	private String currentPreset = "";

	private SWAP_MODE swapMode;

	@Getter(AccessLevel.PACKAGE)
	private List<String> presets = new ArrayList<>();
	private List<WidgetMenuOption> managedMenus = new ArrayList<>();

	private Map<Integer, Map<Integer, SpellData>> spellbooks = new HashMap<>();

	@Provides
	SpellbookPresetsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpellbookPresetsConfig.class);
	}

	@Override
	protected void startUp()
	{
		firstRunConfig();
		cacheConfigs();
		updatePreset();
		filteringEnabled = true;
		reordering = false;
		refreshReorderMenus();
		clientThread.invokeLater(this::reinitializeSpellbook);
	}

	@Override
	protected void shutDown()
	{
		clearReoderMenus();
		clientThread.invokeLater(this::cleanupSpellbook);
		clientThread.invokeLater(this::reinitializeSpellbook);
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		clientThread.invokeLater(this::redrawSpellbook);
	}

	//allows us to set the config on first-run, but not reset it to this value if the user sets the active presets to be empty.
	//only important in this scenario to indicate how it should look(new lines as opposed to traditional commas)
	public void firstRunConfig(){
		if(configManager.getConfiguration(GROUP,FIRST_RUN_ACTIVEPRESETS_KEY) == null){
			configManager.setConfiguration(GROUP,FIRST_RUN_ACTIVEPRESETS_KEY,true);
			configManager.setConfiguration(GROUP,"activePresets",FIRST_RUN_ACTIVEPRESETS_STRING);
		}
	}

	//convert active presets config to list
	public List<String> parsePresets(){
		return listFromString(config.getActivePresets());
	}

	//convert the string to a list of at most 10 presets
	public List<String> listFromString(String string) {
		if (string == null || string.isBlank()) {
			return Collections.emptyList();
		}
		return NEWLINESPLITTER.splitAsStream(string)
				.map(String::trim)
				.filter(Predicate.not(String::isEmpty))
				.limit(10)
				.collect(Collectors.toList());
	}

	//cache config values on start
	public void cacheConfigs(){
		showAllIfEmpty = config.showAllIfEmpty();
		presets = parsePresets();
		swapMode = config.spellMoveMode();
	}

	//updates active presets when changed
	//sanitise our saved presets config when changed, the intention is the user can remove individual,multiple or all saves here without need for a panel
	//unfortunately user can input data here, so we validate the correct saves in the field and overwrite the config.
	//....doing so triggers a second config event which we need to ignore to prevent infinite loop.
	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(GROUP))
		{
			return;
		}

		switch (configChanged.getKey()){
			case "spellMoveMode":
				swapMode = config.spellMoveMode();
				break;
			case "showAllIfEmpty":
				showAllIfEmpty = config.showAllIfEmpty();
				clientThread.invokeLater(this::reinitializeSpellbook);
				break;
			case "activePresets":
				presets = parsePresets();
				if(!presets.contains(currentPreset)){
					//current preset should no longer be active, update it.
					updatePreset();
				}else{
					//list changed refresh menu ops
					refreshReorderMenus();
				}
				break;
			case "savedPresets":
				//allows same-config override without an infinite loop.
				//event 1 starts, flag is set, Event 1 calls a change
				//event 2 starts, cancels cause flag is set
				//event 1 finishes, flag is unset.
				//(I really don't want to add more panel spam to the hub, so this will do)
				if (!ignoreNextConfigEvent) {
					ignoreNextConfigEvent = true;
					//convert saved config to list, search existing configs, if existing config not in the new saved list unset it
					boolean removeCurrentPreset = false;
					String uSaveStr = configChanged.getNewValue();
					if(uSaveStr != null){
						List<String> uSaveList = listFromString(uSaveStr);
						//these are what we expect to be valid, user can add garbage but for now it won't matter due to searching through the existing list only

						StringBuilder uSaveConfig = new StringBuilder();
						for (var key : configManager.getConfigurationKeys(GROUP + ".spellbookData_"))
						{
							//. in config name ignored with limit
							String[] splitKey = key.split("\\.", 2);
							if (splitKey.length == 2)
							{
								//_ in config name ignored with limit
								String[] splitSave = splitKey[1].split("_",2);
								if(splitSave.length == 2){
									String foundSave = splitSave[1];
									if(uSaveList.contains(foundSave)){
										uSaveConfig.append(foundSave).append("\n");
									}else{
										configManager.unsetConfiguration(splitKey[0], splitKey[1]);
										if(currentPreset.equals(foundSave)){
											removeCurrentPreset = true;
										}
									}
								}

							}
						}

						//if user removed the current preset, change to default or the first in the active list.
						if(removeCurrentPreset){
							updatePreset();
						}

						//this updates the given config to reflect whats actually saved, removing any extra text the user added incorrectly.
						configManager.setConfiguration(GROUP, "savedPresets", uSaveConfig.toString().trim());
					}

					//event has finished processing, new config events for this key are now valid again.
					ignoreNextConfigEvent = false;
				}
				break;
		}

	}

	//resets all stored config data of our presets.
	@Override
	public void resetConfiguration()
	{
		for (var key : configManager.getConfigurationKeys(GROUP + ".spellbookData_"))
		{
			String[] str = key.split("\\.", 2);
			if (str.length == 2)
			{
				configManager.unsetConfiguration(str[0], str[1]);
			}
		}

		configManager.setConfiguration(GROUP,"activePresets",FIRST_RUN_ACTIVEPRESETS_STRING);

		clientThread.invokeLater(this::redrawSpellbook);

		log.debug("Reset spellbooks");
	}

	//slight changes to SpellbookPlugin, removes the extra static menus and the list of dynamic menus created in refreshReorderMenus
	private void clearReoderMenus()
	{
		menuManager.removeManagedCustomMenu(FIXED_MAGIC_TAB_LOCK);
		menuManager.removeManagedCustomMenu(RESIZABLE_MAGIC_TAB_LOCK);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_LOCK);
		menuManager.removeManagedCustomMenu(FIXED_MAGIC_TAB_UNLOCK);
		menuManager.removeManagedCustomMenu(RESIZABLE_MAGIC_TAB_UNLOCK);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_UNLOCK);

		menuManager.removeManagedCustomMenu(FIXED_MAGIC_TAB_DISABLE);
		menuManager.removeManagedCustomMenu(RESIZABLE_MAGIC_TAB_DISABLE);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_DISABLE);
		menuManager.removeManagedCustomMenu(FIXED_MAGIC_TAB_ENABLE);
		menuManager.removeManagedCustomMenu(RESIZABLE_MAGIC_TAB_ENABLE);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_ENABLE);

		for (WidgetMenuOption widgetMenuOption : managedMenus)
		{
			menuManager.removeManagedCustomMenu(widgetMenuOption);
		}
		managedMenus.clear();
	}

	//rather than disabling the plugin for a one-off spell(i.e a teleport), user can temporarily disable filtering
	private void toggleFiltering(boolean state){
		filteringEnabled = state;
		refreshReorderMenus();
		redrawSpellbook();
	}

	//many changes to SpellbookPlugin, added menu for every active preset and menu for enabling and disabling filters to the 3 client sizes.
	//this might be excessive due to the 3 client sizes, possibly add checks and conditionally add the menus instead.
	private void refreshReorderMenus()
	{
		clearReoderMenus();
		if (reordering)
		{
			menuManager.addManagedCustomMenu(FIXED_MAGIC_TAB_LOCK, e -> reordering(false));
			menuManager.addManagedCustomMenu(RESIZABLE_MAGIC_TAB_LOCK, e -> reordering(false));
			menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_LOCK, e -> reordering(false));
		}
		else
		{
			menuManager.addManagedCustomMenu(FIXED_MAGIC_TAB_UNLOCK, e -> reordering(true));
			menuManager.addManagedCustomMenu(RESIZABLE_MAGIC_TAB_UNLOCK, e -> reordering(true));
			menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_UNLOCK, e -> reordering(true));

			if(filteringEnabled){
				menuManager.addManagedCustomMenu(FIXED_MAGIC_TAB_DISABLE, e -> toggleFiltering(false));
				menuManager.addManagedCustomMenu(RESIZABLE_MAGIC_TAB_DISABLE, e -> toggleFiltering(false));
				menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_DISABLE, e -> toggleFiltering(false));
			}else{
				menuManager.addManagedCustomMenu(FIXED_MAGIC_TAB_ENABLE, e -> toggleFiltering(true));
				menuManager.addManagedCustomMenu(RESIZABLE_MAGIC_TAB_ENABLE, e -> toggleFiltering(true));
				menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_MAGIC_TAB_ENABLE, e -> toggleFiltering(true));
			}

			for (String preset : presets)
			{
				WidgetMenuOption unlockFixed = new WidgetMenuOption("Load preset ("+preset+")",
						"", InterfaceID.Toplevel.STONE6);
				menuManager.addManagedCustomMenu(unlockFixed, e -> changePreset(preset));

				WidgetMenuOption unlockResizebleA = new WidgetMenuOption("Load preset ("+preset+")",
						"", InterfaceID.ToplevelOsrsStretch.STONE6);
				menuManager.addManagedCustomMenu(unlockResizebleA, e -> changePreset(preset));

				WidgetMenuOption unlockResizebleB = new WidgetMenuOption("Load preset ("+preset+")",
						"", InterfaceID.ToplevelPreEoc.STONE6);
				menuManager.addManagedCustomMenu(unlockResizebleB, e -> changePreset(preset));

				managedMenus.add(unlockFixed);
				managedMenus.add(unlockResizebleA);
				managedMenus.add(unlockResizebleB);
			}

		}
	}

	//change to a specifed preset, additionally re-enabling filtering if disabled.
	private void changePreset(String preset){
		filteringEnabled = true;
		currentPreset = preset;
		reordering = false;
		spellbooks = getSpellbooks(preset);
		refreshReorderMenus();
		clientThread.invokeLater(this::reinitializeSpellbook);
	}

	//update the current preset after changes have occured
	private void updatePreset(){
		if(presets.isEmpty()){
			//if no presets, the "Default" preset is used.
			//this allows the default preset to be deleted and means the plugin has the same functionality without any active presets.
			changePreset(DEFAULT_PRESET);
		}else if(currentPreset.isEmpty() || (currentPreset.equals(DEFAULT_PRESET) && !presets.contains(DEFAULT_PRESET) || !presets.contains(currentPreset))){
			//no current preset, but active presets exist, default to the first active preset
			changePreset(presets.get(0));
		}else{
			//current preset active and exists, it's likely changed in some other manner refresh it.
			changePreset(currentPreset);
		}
	}

	//Slight changes to SpellbookPlugin, saves collection of spells to config on reorder-end, modified game message.
	private void reordering(boolean state)
	{
		reordering = state;

		var message = reordering ?
				"[Spellbook Presets] Editing spellbook ("+currentPreset+")." :
				"[Spellbook Presets] Saved spellbook ("+currentPreset+")." ;

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(message)
				.build());

		if(!state){
			//spellbook has just been edited, more than likely the user wants the spells to be filtered again.
			filteringEnabled = true;
			//save spells all at once on reorder-end
			saveSpellbooks(currentPreset);
		}

		refreshReorderMenus();

		redrawSpellbook();
	}

	//Unchanged, matches SpellbookPlugin
	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.MAGIC_SPELLBOOK_INITIALISESPELLS)
		{
			int[] stack = client.getIntStack();
			int sz = client.getIntStackSize();
			int spellBookEnum = stack[sz - 12]; // eg 1982, 5285, 1983, 1984, 1985
			clientThread.invokeLater(() -> initializeSpells(spellBookEnum));
		}
	}

	//Slight changes to SpellbookPlugin, option to swap spell instead of inserting.
	@Subscribe
	public void onDraggingWidgetChanged(DraggingWidgetChanged event)
	{
		if (event.isDraggingWidget() && client.getMouseCurrentButton() == 0)
		{
			Widget draggedWidget = client.getDraggedWidget();
			Widget draggedOnWidget = client.getDraggedOnWidget();
			if (draggedWidget == null || draggedOnWidget == null)
			{
				return;
			}

			int draggedGroupId = WidgetUtil.componentToInterface(draggedWidget.getId());
			int draggedOnGroupId = WidgetUtil.componentToInterface(draggedOnWidget.getId());
			if (draggedGroupId != InterfaceID.MAGIC_SPELLBOOK || draggedOnGroupId != InterfaceID.MAGIC_SPELLBOOK)
			{
				return;
			}

			// from ~magic_spellbook_redraw
			int subSpellbookId = client.getEnum(EnumID.SPELLBOOKS_SUB).getIntValue(client.getVarbitValue(VarbitID.SPELLBOOK));
			int spellbookId = client.getEnum(subSpellbookId).getIntValue(client.getVarbitValue(VarbitID.SPELLBOOK_SUBLIST));

			EnumComposition spellbook = client.getEnum(spellbookId);
			int[] order = calculateSpellbookOrder(spellbookId, spellbook); // in enum indices

			int fromIdx = findSpellIdxForComponent(spellbook, order, draggedWidget);
			int toIdx = findSpellIdxForComponent(spellbook, order, draggedOnWidget);

			ItemComposition fromSpell = client.getItemDefinition(spellbook.getIntValue(order[fromIdx]));
			ItemComposition toSpell = client.getItemDefinition(spellbook.getIntValue(order[toIdx]));

			log.debug("Insert {} ({}) at {} ({}) spellbook {}",
					fromSpell.getStringValue(ParamID.SPELL_NAME), fromIdx,
					toSpell.getStringValue(ParamID.SPELL_NAME), toIdx,
					spellbookId);

			log.debug("Set {} to {}", client.getItemDefinition(spellbook.getIntValue(order[fromIdx])).getStringValue(ParamID.SPELL_NAME), toIdx);
			setPosition(spellbookId, spellbook.getIntValue(order[fromIdx]), toIdx);

			if(swapMode == SWAP_MODE.SWAP)
			{
				log.debug("Set {} to {}", client.getItemDefinition(spellbook.getIntValue(order[toIdx])).getStringValue(ParamID.SPELL_NAME), fromIdx);
				setPosition(spellbookId, spellbook.getIntValue(order[toIdx]), fromIdx);
			}else if(swapMode == SWAP_MODE.INSERT){
				if (fromIdx < toIdx)
				{
					for (int i = fromIdx + 1; i <= toIdx; ++i)
					{
						log.debug("Set {} to {}", client.getItemDefinition(spellbook.getIntValue(order[i])).getStringValue(ParamID.SPELL_NAME), i - 1);
						setPosition(spellbookId, spellbook.getIntValue(order[i]), i - 1);
					}
				}
				else
				{
					for (int i = toIdx; i < fromIdx; ++i)
					{
						log.debug("Set {} to {}", client.getItemDefinition(spellbook.getIntValue(order[i])).getStringValue(ParamID.SPELL_NAME), i + 1);
						setPosition(spellbookId, spellbook.getIntValue(order[i]), i + 1);
					}
				}
			}
			redrawSpellbook();
		}
	}

	//Unchanged, matches SpellbookPlugin
	private int findSpellIdxForComponent(EnumComposition spellbook, int[] spells, Widget c)
	{
		for (int i = 0; i < spells.length; ++i)
		{
			ItemComposition spellObj = client.getItemDefinition(spellbook.getIntValue(spells[i]));
			Widget w = client.getWidget(spellObj.getIntValue(ParamID.SPELL_BUTTON));
			if (w == c)
			{
				return i;
			}
		}
		return -1;
	}

	//Many changes to SpellbookPlugin,
	//invoke later on set opacity recolors CoolDown spells(H group,veng magic imbue etc) -- not proper fix for core plugin cause it can cause a slight flicker.
	//cleans up spells if no filters selected or filtering is disabled
	//additionally isHidden->isShown and different opacity defaults.
	//Occurs on load, reorder start, reorder end
	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"spellbookSort".equals(event.getEventName()))
		{
			return;
		}

		createWarning(reordering);

		// this is called after ~magic_spellbook_redraw has built and sorted the array of visible spells
		// based on the vanilla filtering

		int[] stack = client.getIntStack();
		int size = client.getIntStackSize();

		int spellbookEnumId = stack[size - 3];
		int spellArrayId = stack[size - 2];
		int numSpells = stack[size - 1];

		EnumComposition spellbookEnum = client.getEnum(spellbookEnumId);
		int[] spells = client.getArray(spellArrayId); // enum indices
		int[] newSpells = new int[numSpells];
		int numNewSpells = 0;


		// the specified book has no modifications and the user wants all spells to be displayed in said situation.
		// this likely makes more sense than displaying an empty book.
		// additionally, if the user has temporarily disabled filtering (e.x to use a one off teleport) display the entire spellbook.
		Map<Integer, SpellData> book = spellbooks.get(spellbookEnumId);
		boolean bookIsEmpty = book == null || book.isEmpty();
		if(!reordering
				&& ((bookIsEmpty && showAllIfEmpty) || !filteringEnabled)){
			cleanupSpells(spellbookEnumId);
			return;
		}

		for (int i = 0; i < numSpells; ++i)
		{
			ItemComposition spellObj = client.getItemDefinition(spellbookEnum.getIntValue(spells[i]));
			Widget w = client.getWidget(spellObj.getIntValue(ParamID.SPELL_BUTTON));
			boolean shown = isShown(spellbookEnumId, spellObj.getId());

			int widgetConfig = w.getClickMask();
			if (reordering)
			{
				if (shown)
				{
					clientThread.invokeLater(() ->
					{
						w.setOpacity(visibleOpacity);
						w.setAction(HIDE_UNHIDE_OP, "Hide");
					});
				}
				else
				{
					clientThread.invokeLater(() ->
					{
						w.setOpacity(hiddenOpacity);
						w.setAction(HIDE_UNHIDE_OP, "Show");
					});
				}

				newSpells[numNewSpells++] = spells[i];
				widgetConfig |= DRAG | DRAG_ON;
			}
			else
			{
				if (shown)
				{
					newSpells[numNewSpells++] = spells[i];
					w.setOpacity(visibleOpacity);
					w.setAction(HIDE_UNHIDE_OP, null);
				}
				else
				{
					w.setHidden(true);
				}

				widgetConfig &= ~(DRAG | DRAG_ON);
			}
			w.setClickMask(widgetConfig);
		}

		// Sort newSpells based on their configured order
		int[] order = calculateSpellbookOrder(spellbookEnumId, spellbookEnum);
		int[] indices = new int[order.length];
		for (int i = 0; i < order.length; ++i)
		{
			indices[order[i]] = i;
		}
		newSpells = Arrays.stream(newSpells, 0, numNewSpells)
				.boxed()
				.sorted(Comparator.comparingInt(i -> indices[i]))
				.mapToInt(i -> i)
				.toArray();

		System.arraycopy(newSpells, 0, spells, 0, numNewSpells);
		stack[size - 1] = numSpells = numNewSpells;
	}

	//Slight changes to SpellbookPlugin, added null check for widget so shutdown doesn't cause NPE
	private void createWarning(boolean unlocked)
	{
		Widget w = client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
		if(w != null)
		{
			w.deleteAllChildren();

			if (unlocked)
			{
				Widget c = w.createChild(WidgetType.RECTANGLE);
				c.setHeightMode(WidgetSizeMode.MINUS);
				c.setWidthMode(WidgetSizeMode.MINUS);
				c.setTextColor(0xff0000);
				c.setFilled(true);
				c.setOpacity(220);
				c.revalidate();
			}

		}
	}

	//Slight changes to SpellbookPlugin, mainly isHidden->isShown and different opacity defaults.
	//This is called when hiding/unhiding spells
	private void initializeSpells(int spellbookEnum)
	{
		EnumComposition spellbook = client.getEnum(spellbookEnum);
		for (int i = 0; i < spellbook.size(); ++i)
		{
			int spellObjId = spellbook.getIntValue(i);
			ItemComposition spellObj = client.getItemDefinition(spellObjId);
			int spellComponent = spellObj.getIntValue(ParamID.SPELL_BUTTON);
			Widget w = client.getWidget(spellComponent);

			// spells with no target mask have an existing op listener, capture it to
			// call it later
			Object[] opListener = w.getOnOpListener();
			w.setOnOpListener((JavaScriptCallback) e ->
			{
				if (e.getOp() == HIDE_UNHIDE_OP + 1)
				{
					Widget s = e.getSource();

					// Spells can be shared between spellbooks, so we can't assume spellbookEnum is the current spellbook.
					// from ~magic_spellbook_redraw
					int subSpellbookId = client.getEnum(EnumID.SPELLBOOKS_SUB).getIntValue(client.getVarbitValue(VarbitID.SPELLBOOK));
					int spellbookId = client.getEnum(subSpellbookId).getIntValue(client.getVarbitValue(VarbitID.SPELLBOOK_SUBLIST));

					boolean shown = isShown(spellbookId, spellObjId);
					shown = !shown;

					log.debug("Changing {} to hidden: {}", s.getName(), shown);
					setHidden(spellbookId, spellObjId, shown);

					s.setOpacity(shown ? visibleOpacity : hiddenOpacity);
					s.setAction(HIDE_UNHIDE_OP, shown ? "Hide" : "Show");
					return;
				}

				if (opListener != null)
				{
					client.runScript(opListener);
				}
			});
		}
	}

	//for the given spellbook, remove hide/unhide op and reset the opacity of the spells
	private void cleanupSpells(int spellbookEnum)
	{
		EnumComposition spellbook = client.getEnum(spellbookEnum);
		for (int i = 0; i < spellbook.size(); ++i)
		{
			int spellObjId = spellbook.getIntValue(i);
			ItemComposition spellObj = client.getItemDefinition(spellObjId);
			int spellComponent = spellObj.getIntValue(ParamID.SPELL_BUTTON);
			Widget w = client.getWidget(spellComponent);
			if(w != null)
			{
				w.setAction(HIDE_UNHIDE_OP, null);
				w.setOpacity(visibleOpacity);
			}
		}
	}

	//Unchanged, matches SpellbookPlugin
	private void reinitializeSpellbook()
	{
		Widget w = client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
		if (w != null && w.getOnLoadListener() != null)
		{
			client.createScriptEvent(w.getOnLoadListener())
					.setSource(w)
					.run();
		}
	}

	//Unchanged, matches SpellbookPlugin
	private void redrawSpellbook()
	{
		Widget w = client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
		if (w != null && w.getOnInvTransmitListener() != null)
		{
			client.createScriptEvent(w.getOnInvTransmitListener())
					.setSource(w)
					.run();
		}
	}

	//called on shutdown, removes hide/unhide ops and restores opacity to all spells & deletes red warning pane if present.
	private void cleanupSpellbook()
	{
		Widget w = client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
		if(w != null)
		{
			w.deleteAllChildren();
		}

		cleanupSpells(1982);
		cleanupSpells(5285);
		cleanupSpells(1983);
		cleanupSpells(1984);
		cleanupSpells(1985);
	}

	//Unchanged, matches SpellbookPlugin
	private int[] calculateSpellbookOrder(int spellbookId, EnumComposition spellbook)
	{
		int[] spells = defaultSpellbookOrder(spellbook);
		int[] indices = new int[spells.length]; // spell to desired index
		for (int i = 0; i < spells.length; ++i)
		{
			int pos = getPosition(spellbookId, spellbook.getIntValue(spells[i]));
			indices[spells[i]] = pos != -1 ? pos : i;
		}

		// sort by desired index
		return Arrays.stream(spells)
				.boxed()
				.sorted(Comparator.comparingInt(i -> indices[i]))
				.mapToInt(i -> i)
				.toArray();
	}

	//Unchanged, matches SpellbookPlugin
	private int[] defaultSpellbookOrder(EnumComposition spellbook)
	{
		return IntStream.range(0, spellbook.size())
				.boxed()
				.sorted((idx1, idx2) ->
				{
					var i1 = client.getItemDefinition(spellbook.getIntValue(idx1));
					var i2 = client.getItemDefinition(spellbook.getIntValue(idx2));
					int l1 = i1.getIntValue(ParamID.SPELL_LEVELREQ);
					int l2 = i2.getIntValue(ParamID.SPELL_LEVELREQ);
					return Integer.compare(l1, l2);
				})
				.mapToInt(i -> i)
				.toArray();
	}

	//Converts collection of spellbooks (spellbookid->(spellid,spelldata)) to json
	//Saves the json to config with the name of given preset
	//Only called on redorder-end for performance.
	public void saveSpellbooks(String preset){
		String json = gson.toJson(spellbooks);
		configManager.setConfiguration(GROUP, "spellbookData_"+preset, json);

		StringBuilder savedConfig = new StringBuilder();
		for (var key : configManager.getConfigurationKeys(GROUP + ".spellbookData_"))
		{
			String[] splitKey = key.split("\\.", 2);
			if (splitKey.length == 2)
			{
				String[] splitSave = splitKey[1].split("_",2);
				if(splitSave.length == 2){
					String foundSave = splitSave[1];
					savedConfig.append(foundSave).append("\n");
				}
			}
		}

		//this updates the given config to reflect whats actually saved, removing any extra text the user added incorrectly.
		configManager.setConfiguration(GROUP, "savedPresets", savedConfig.toString().trim());

	}

	//returns a collection of spellbook > spellData
	//ex lunarId, {spellid,spelldata}
	//.. normalId, {spell,spelldata}
	//instead of editing the config every spell edit, we modify this mapping. when edit mode is left we save the mapping to config as a whole.
	public Map<Integer, Map<Integer, SpellData>> getSpellbooks(String preset){
		String json = configManager.getConfiguration(GROUP, "spellbookData_"+preset);
		if (Strings.isNullOrEmpty(json))
		{
			return new HashMap<>();
		}
		return gson.fromJson(json, new TypeToken<Map<Integer, Map<Integer, SpellData>>>(){}.getType());
	}

	//sets spelldata to be hidden, adds book of spellbookId to spellbooks if missing
	private void setHidden(int spellbookId, int spellId, boolean hidden) {
		Map<Integer, SpellData> book = spellbooks.computeIfAbsent(spellbookId, k -> new HashMap<>());

		SpellData spell = book.computeIfAbsent(spellId, k -> new SpellData(spellId, -1, false));
		spell.h = hidden;

		if (spell.isUnused()) {
			book.remove(spellId);
		}
	}

	//sets spelldata position, adds book of spellbookId to spellbooks if missing
	private void setPosition(int spellbookId, int spellId, int position) {
		Map<Integer, SpellData> book = spellbooks.computeIfAbsent(spellbookId, k -> new HashMap<>());

		SpellData spell = book.computeIfAbsent(spellId, k -> new SpellData(spellId, -1, false));
		spell.p = position;

		if (spell.isUnused()) {
			book.remove(spellId);
		}
	}

	//indicates the spell in the current book should be visible (defaults to false if not present)
	private boolean isShown(int spellbookId, int spellId) {
		Map<Integer, SpellData> book = spellbooks.get(spellbookId);
		if (book == null) {
			return false;
		}
		SpellData spell = book.get(spellId);
		if (spell == null) {
			return false;
		}
		return spell.getHidden();
	}

	//the position of the current spell if tracked. (defaults to -1 if not present)
	private int getPosition(int spellbookId, int spellId) {
		Map<Integer, SpellData> book = spellbooks.get(spellbookId);
		if (book == null) {
			return -1;
		}
		SpellData spell = book.get(spellId);
		if (spell == null) {
			return -1;
		}
		return spell.getPosition();
	}
}