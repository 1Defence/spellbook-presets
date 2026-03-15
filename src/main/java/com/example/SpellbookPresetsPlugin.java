/*
 * Copyright (c) 2024, Adam <Adam@sigterm.info>
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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;

import static com.example.SpellbookPresetsConfig.*;
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
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

import net.runelite.client.util.ColorUtil;

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
	public ConfigManager configManager;

	@Inject
	private SpellbookPresetsConfig config;

	@Inject
	public ImportExportHandler importExportHandler;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	private static final String LOCK = "Save-spells";
	private static final String UNLOCK = "Edit-spells";
	// [proc,magic_spellbook_modifyops] shifts around ops, but seems to only use
	// 1 2 3 4 5 10. So use 6 for Hide/Unhide.
	private static final int HIDE_UNHIDE_OP = 6;

	private static final String DISABLE = "Hide preset";
	private static final String ENABLE = "Show preset";

	private static final Color MENU_NAME_COLOR_STANDARD = new Color(255, 144, 64);
	private static final Color MENU_NAME_COLOR_GREYOUT = new Color(128,128,128);

	//menu ops for the 3 client sizes for editing/saving a preset & hiding/showing a preset
	private static final int FIXED_MAGIC_TAB_ID = InterfaceID.Toplevel.STONE6;
	private static final int RESIZABLE_A_MAGIC_TABID = InterfaceID.ToplevelOsrsStretch.STONE6;
	private static final int RESIZABLE_B_MAGIC_TABID = InterfaceID.ToplevelPreEoc.STONE6;

	/**Fixed**/
	private static final WidgetMenuOption LOCK_MENU_FIXED = new WidgetMenuOption(LOCK, "", FIXED_MAGIC_TAB_ID);
	private static final WidgetMenuOption UNLOCK_MENU_FIXED = new WidgetMenuOption(UNLOCK, "", FIXED_MAGIC_TAB_ID);
	private static final WidgetMenuOption DISABLE_MENU_FIXED = new WidgetMenuOption(DISABLE, "", FIXED_MAGIC_TAB_ID);
	private static final WidgetMenuOption ENABLE_MENU_FIXED = new WidgetMenuOption(ENABLE, "", FIXED_MAGIC_TAB_ID);

	/**Resizable Top**/
	private static final WidgetMenuOption LOCK_MENU_RESIZE_A = new WidgetMenuOption(LOCK, "", RESIZABLE_A_MAGIC_TABID);
	private static final WidgetMenuOption UNLOCK_MENU_RESIZE_A = new WidgetMenuOption(UNLOCK, "", RESIZABLE_A_MAGIC_TABID);
	private static final WidgetMenuOption DISABLE_MENU_RESIZE_A = new WidgetMenuOption(DISABLE, "", RESIZABLE_A_MAGIC_TABID);
	private static final WidgetMenuOption ENABLE_MENU_RESIZE_A = new WidgetMenuOption(ENABLE, "", RESIZABLE_A_MAGIC_TABID);

	/**Resizable Bottom**/
	private static final WidgetMenuOption LOCK_MENU_RESIZE_B = new WidgetMenuOption(LOCK, "", RESIZABLE_B_MAGIC_TABID);
	private static final WidgetMenuOption UNLOCK_MENU_RESIZE_B = new WidgetMenuOption(UNLOCK, "", RESIZABLE_B_MAGIC_TABID);
	private static final WidgetMenuOption DISABLE_MENU_RESIZE_B = new WidgetMenuOption(DISABLE, "", RESIZABLE_B_MAGIC_TABID);
	private static final WidgetMenuOption ENABLE_MENU_RESIZE_B = new WidgetMenuOption(ENABLE, "", RESIZABLE_B_MAGIC_TABID);


	private final int visibleOpacity = 0;
	private final int hiddenOpacity = 200;

	private boolean reordering;
	private boolean showAllIfEmpty;
	private boolean filteringEnabled;

	public String currentPreset = "";

	private SWAP_MODE configSwapMode;
	private CURRENT_OPTION_STYLE configCurrentOpRenderStyle;
	private Color configCurrentOpCustomColor;
	private MODIFY_OPTION_STYLE configModifyOpRenderStyle;
	private boolean configNoLoadOps = false;
	private FILTERING_OPTION_STYLE configFilteringOps;
	private OPEN_TAB_CONDITION configOpenTabCondition;
	private boolean configDisplayPresetsPanel = false;
	private boolean modifyOpHotkeyHeld = false;

	@Getter(AccessLevel.PACKAGE)
	private List<String> presets = new ArrayList<>();
	private final List<WidgetMenuOption> managedMenus = new ArrayList<>();

	private Map<Integer, Map<Integer, SpellData>> spellbooks = new HashMap<>();

	public SaveEditPanel sidePanel;

	private NavigationButton navButton_panel;

	private final HotkeyListener modifyOptionsKeyListener = new HotkeyListener(() -> config.modifyOptionsKey())
	{
		@Override
		public void hotkeyPressed()
		{
			modifyOpHotkeyHeld = true;
			refreshReorderMenus();
		}
		@Override
		public void hotkeyReleased()
		{
			modifyOpHotkeyHeld = false;
			refreshReorderMenus();
		}
	};

	@Provides
	SpellbookPresetsConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpellbookPresetsConfig.class);
	}

	@Override
	protected void startUp()
	{
		//currently just sets and exists, used for future migration if data handling changes.
		configManager.setConfiguration(GROUP,LAST_VERSION_KEY, LIVE_VERSION_STRING);
		cacheConfigs();

		modifyOpHotkeyHeld = false;
		if(configModifyOpRenderStyle == MODIFY_OPTION_STYLE.HOTKEY){
			keyManager.registerKeyListener(modifyOptionsKeyListener);
		}

		String cachedPreset = configManager.getConfiguration(GROUP,CURRENT_PRESET_KEY);
		if(!Strings.isNullOrEmpty(cachedPreset) && presets.contains(cachedPreset)){
			changePreset(cachedPreset);
		}else{
			updatePreset();
		}

		filteringEnabled = true;
		reordering = false;
		refreshReorderMenus();
		clientThread.invokeLater(this::reinitializeSpellbook);

		sidePanel = new SaveEditPanel(this, configManager, gson);

		final BufferedImage icon = ImageUtil.loadImageResource(SpellbookPresetsPlugin.class, "icon_panel.png");
		navButton_panel = NavigationButton.builder()
				.tooltip("Spellbook Presets")
				.icon(icon)
				.priority(1)
				.panel(sidePanel)
				.build();

		if(configDisplayPresetsPanel)
		{
			clientToolbar.addNavigation(navButton_panel);
		}
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(modifyOptionsKeyListener);
		clearReoderMenus();
		clientThread.invokeLater(this::cleanupSpellbook);
		clientThread.invokeLater(this::reinitializeSpellbook);
		clientToolbar.removeNavigation(navButton_panel);
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		clientThread.invokeLater(this::redrawSpellbook);
	}

	/**Generate preset list from config actives*/
	public List<String> activePresetsFromConfig(){
		String json = configManager.getConfiguration(GROUP, ACTIVE_PRESETS_KEY);
		if (Strings.isNullOrEmpty(json))
		{
			return new ArrayList<>();
		}
		return gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
	}

	/**cache config values on start*/
	public void cacheConfigs(){
		showAllIfEmpty = config.showAllIfEmpty();
		presets = activePresetsFromConfig();
		configSwapMode = config.spellMoveMode();
		configCurrentOpRenderStyle = config.currentOptionRendering();
		configCurrentOpCustomColor = config.currentOptionCustomColor();
		configModifyOpRenderStyle = config.modifyOptionRendering();
		configNoLoadOps = config.noLoadOps();
		configFilteringOps = config.filteringOps();
		configOpenTabCondition = config.openTabCondition();
		configDisplayPresetsPanel = config.displayPresetsPanel();
	}

	/**updates current preset if actives no longer contains it*/
	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(GROUP))
		{
			return;
		}

		switch (configChanged.getKey()){
			case SPELL_MOVE_MODE_KEY:
				configSwapMode = config.spellMoveMode();
				break;
			case SHOW_ALL_IF_EMPTY_KEY:
				showAllIfEmpty = config.showAllIfEmpty();
				clientThread.invokeLater(this::reinitializeSpellbook);
				break;
			case ACTIVE_PRESETS_KEY:
				presets = activePresetsFromConfig();
				if(presets.isEmpty()){
					currentPreset = "";
				}
				if(!presets.contains(currentPreset)){
					//current preset should no longer be active, update it.
					updatePreset();
				}else{
					//list changed refresh menu ops
					refreshReorderMenus();
				}
				break;
			case CURRENT_OPTION_RENDER_KEY:
			case CURRENT_OPTION_COLOR_KEY:
				configCurrentOpCustomColor = config.currentOptionCustomColor();
				configCurrentOpRenderStyle = config.currentOptionRendering();
				refreshReorderMenus();
				break;
			case MODIFY_OPTION_RENDER_KEY:
				configModifyOpRenderStyle = config.modifyOptionRendering();
				if(configModifyOpRenderStyle == MODIFY_OPTION_STYLE.ALWAYS_RENDER){
					keyManager.unregisterKeyListener(modifyOptionsKeyListener);
					modifyOpHotkeyHeld = false;
				}else{
					keyManager.registerKeyListener(modifyOptionsKeyListener);
				}
				refreshReorderMenus();
				break;
			case NO_LOAD_OPTIONS_KEY:
				configNoLoadOps = config.noLoadOps();
				break;
			case FILTERING_OPTIONS_KEY:
				configFilteringOps = config.filteringOps();
				break;
			case OPEN_TAB_CONDITION_KEY:
				configOpenTabCondition = config.openTabCondition();
				break;
			case DISPLAY_PRESETS_PANEL_KEY:
				configDisplayPresetsPanel = config.displayPresetsPanel();
				if(configDisplayPresetsPanel)
				{
					clientToolbar.addNavigation(navButton_panel);
				}else{
					clientToolbar.removeNavigation(navButton_panel);
				}
				break;

		}

	}

	/**resets all stored config data of our presets.*/
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

		configManager.unsetConfiguration(GROUP,ACTIVE_PRESETS_KEY);
		sidePanel.Refresh();

		clientThread.invokeLater(this::redrawSpellbook);

		log.debug("Reset spellbooks");
	}

	/**slight changes to SpellbookPlugin, removes the extra static menus and the list of dynamic menus created in refreshReorderMenus*/
	private void clearReoderMenus()
	{
		menuManager.removeManagedCustomMenu(LOCK_MENU_FIXED);
		menuManager.removeManagedCustomMenu(LOCK_MENU_RESIZE_A);
		menuManager.removeManagedCustomMenu(LOCK_MENU_RESIZE_B);

		menuManager.removeManagedCustomMenu(UNLOCK_MENU_FIXED);
		menuManager.removeManagedCustomMenu(UNLOCK_MENU_RESIZE_A);
		menuManager.removeManagedCustomMenu(UNLOCK_MENU_RESIZE_B);

		menuManager.removeManagedCustomMenu(DISABLE_MENU_FIXED);
		menuManager.removeManagedCustomMenu(DISABLE_MENU_RESIZE_A);
		menuManager.removeManagedCustomMenu(DISABLE_MENU_RESIZE_B);

		menuManager.removeManagedCustomMenu(ENABLE_MENU_FIXED);
		menuManager.removeManagedCustomMenu(ENABLE_MENU_RESIZE_A);
		menuManager.removeManagedCustomMenu(ENABLE_MENU_RESIZE_B);

		for (WidgetMenuOption widgetMenuOption : managedMenus)
		{
			menuManager.removeManagedCustomMenu(widgetMenuOption);
		}
		managedMenus.clear();
	}

	/**rather than disabling the plugin for a one-off spell(i.e a teleport), user can temporarily disable filtering*/
	private void toggleFiltering(boolean state){
		filteringEnabled = state;
		refreshReorderMenus();
		redrawSpellbook();
	}

	/**many changes to SpellbookPlugin, added menu for every active preset and menu for enabling and disabling filters to the 3 client sizes.
	 * this might be excessive due to the 3 client sizes, possibly add checks and conditionally add the menus instead.*/
	private void refreshReorderMenus()
	{
		clearReoderMenus();

		if(presets.isEmpty())
		{
			return;
		}

		if (reordering)
		{
			menuManager.addManagedCustomMenu(LOCK_MENU_FIXED, e -> reordering(false));
			menuManager.addManagedCustomMenu(LOCK_MENU_RESIZE_A, e -> reordering(false));
			menuManager.addManagedCustomMenu(LOCK_MENU_RESIZE_B, e -> reordering(false));
		}
		else
		{

			//for those who don't want the extra clutter of options they don't use often they will only render while holding their chosen key.
			if(configModifyOpRenderStyle == MODIFY_OPTION_STYLE.ALWAYS_RENDER || modifyOpHotkeyHeld)
			{
				String currentPresetColored = ColorUtil.wrapWithColorTag(currentPreset, MENU_NAME_COLOR_STANDARD);
				String currentOp = "Edit " + currentPresetColored;

				UNLOCK_MENU_FIXED.setMenuOption(currentOp);
				menuManager.addManagedCustomMenu(UNLOCK_MENU_FIXED, e -> reordering(true));
				UNLOCK_MENU_RESIZE_A.setMenuOption(currentOp);
				menuManager.addManagedCustomMenu(UNLOCK_MENU_RESIZE_A, e -> reordering(true));
				UNLOCK_MENU_RESIZE_B.setMenuOption(currentOp);
				menuManager.addManagedCustomMenu(UNLOCK_MENU_RESIZE_B, e -> reordering(true));

				if (filteringEnabled)
				{
					menuManager.addManagedCustomMenu(DISABLE_MENU_FIXED, e -> toggleFiltering(false));
					menuManager.addManagedCustomMenu(DISABLE_MENU_RESIZE_A, e -> toggleFiltering(false));
					menuManager.addManagedCustomMenu(DISABLE_MENU_RESIZE_B, e -> toggleFiltering(false));
				} else
				{
					menuManager.addManagedCustomMenu(ENABLE_MENU_FIXED, e -> toggleFiltering(true));
					menuManager.addManagedCustomMenu(ENABLE_MENU_RESIZE_A, e -> toggleFiltering(true));
					menuManager.addManagedCustomMenu(ENABLE_MENU_RESIZE_B, e -> toggleFiltering(true));
				}

				if(modifyOpHotkeyHeld && configNoLoadOps){
					return;
				}

			}

			for (String preset : presets)
			{
				Color col = MENU_NAME_COLOR_STANDARD;
				if(preset.equals(currentPreset)){
					switch (configCurrentOpRenderStyle){
						case NO_RENDER:
							continue;
						case GREY_OUT:
							col = MENU_NAME_COLOR_GREYOUT;
							break;
						case CUSTOM_COLOR:
							col = configCurrentOpCustomColor;
							break;
					}
				}

				String presetColored = ColorUtil.wrapWithColorTag(preset, col);
				String option = "Load "+presetColored;

				WidgetMenuOption unlockFixed = new WidgetMenuOption(option, "", FIXED_MAGIC_TAB_ID);
				menuManager.addManagedCustomMenu(unlockFixed, e -> changePreset(preset));
				managedMenus.add(unlockFixed);

				WidgetMenuOption unlockResizebleA = new WidgetMenuOption(option, "", RESIZABLE_A_MAGIC_TABID);
				menuManager.addManagedCustomMenu(unlockResizebleA, e -> changePreset(preset));
				managedMenus.add(unlockResizebleA);

				WidgetMenuOption unlockResizebleB = new WidgetMenuOption(option, "", RESIZABLE_B_MAGIC_TABID);
				menuManager.addManagedCustomMenu(unlockResizebleB, e -> changePreset(preset));
				managedMenus.add(unlockResizebleB);
			}

		}
	}

	/**change to a specifed preset, additionally re-enabling filtering if disabled.*/
	public void changePreset(String preset){
		filteringEnabled = true;
		currentPreset = preset;
		reordering = false;
		spellbooks = getSpellbooks(preset);
		refreshReorderMenus();
		configManager.setConfiguration(GROUP, CURRENT_PRESET_KEY, preset);
		clientThread.invokeLater(this::reinitializeSpellbook);
		requestMagicTabOpen(OPEN_TAB_CONDITION.LOAD_PRESET);
	}

	/**update the current preset after changes have occured*/
	private void updatePreset(){
		if(presets.isEmpty() ){
			//if no presets, cleanup spellbook and menu ops similar to a shutdown
			clientThread.invokeLater(() ->
			{
				clearReoderMenus();
				cleanupSpellbook();
				reinitializeSpellbook();
			});
		}else if(currentPreset.isEmpty() || !presets.contains(currentPreset)){
			//list of actives exists, current preset is blank or no longer part of the active presets list, default to the first active preset
			changePreset(presets.get(0));
		}else{
			//current preset active and exists, it's likely changed in some other manner refresh it.
			changePreset(currentPreset);
		}
	}

	/**Slight changes to SpellbookPlugin, saves collection of spells to config on reorder-end, modified game message.*/
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
			saveSpellbooks(currentPreset,spellbooks);
		}

		refreshReorderMenus();

		redrawSpellbook();

		if(reordering){
			requestMagicTabOpen(OPEN_TAB_CONDITION.EDIT_PRESET);
		}

	}

	/**Attempt to open the magic tab
	 * occurs on menu-op selection & if user starts the plugin or removes their current preset from active list
	 * which menu triggers this depends on user-preference
	 * Edit-spellbook | Load-preset | None*/
	public void requestMagicTabOpen(OPEN_TAB_CONDITION condition){
		if((configOpenTabCondition.value & condition.value) == 0)
			return;

		int tabScriptId = 915;
		int magicTabId = 6;
		clientThread.invokeLater(() ->
		{
			client.runScript(tabScriptId, magicTabId);
		});
	}

	/**Unchanged, matches SpellbookPlugin*/
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

	/**Slight changes to SpellbookPlugin, option to swap spell instead of inserting.*/
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

			if(configSwapMode == SWAP_MODE.SWAP)
			{
				log.debug("Set {} to {}", client.getItemDefinition(spellbook.getIntValue(order[toIdx])).getStringValue(ParamID.SPELL_NAME), fromIdx);
				setPosition(spellbookId, spellbook.getIntValue(order[toIdx]), fromIdx);
			}else if(configSwapMode == SWAP_MODE.INSERT){
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

	/**Unchanged, matches SpellbookPlugin*/
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

	/**Many changes to SpellbookPlugin,
	 * invoke later on set opacity recolors CoolDown spells(H group,veng magic imbue etc) -- not proper fix for core plugin cause it can cause a slight flicker.
	 * cleans up spells if no filters selected or filtering is disabled
	 * additionally isHidden->isShown and different opacity defaults.
	 * Occurs on load, reorder start, reorder end*/
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
		// if the user has no presets active, display spellbook as normal.
		Map<Integer, SpellData> book = spellbooks.get(spellbookEnumId);
		boolean bookIsEmpty = book == null || book.isEmpty();
		if(showAllIfEmpty && !bookIsEmpty){
			//Verify the contents aren't all hidden, this can happen when spells have a modified position but none of them are set visible by user.
			//while you could clear the map on save to prevent said situation, we want to retain the current positioning status incase the user wants to modify this set later.
			boolean hasAnyModifiedSpell = false;
			for (SpellData spellData : book.values())
			{
				if(spellData.getHidden()){
					hasAnyModifiedSpell = true;
					break;
				}
			}
			if(!hasAnyModifiedSpell){
				bookIsEmpty = true;
			}
		}
		if(!reordering
				&& ((bookIsEmpty && showAllIfEmpty) || !filteringEnabled || presets.isEmpty())){
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

	/**If theres a pending config change and user is about to rightclick the spellbook, instantly update the config and thus the options.
	 * move functionality if theres ever an event for cursor leaving the panel.
	 * situationally remove enable/disable spellbook filtering option, rarely ever used once set so it only serves confusion to remain as a visible op.*/
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e){
		String op = e.getOption();
		if(sidePanel.configUpdateTimer != null)
		{
			if (op.equals("Magic"))
			{
				sidePanel.forcePendingConfig();
			}
		}

		if(configFilteringOps == FILTERING_OPTION_STYLE.UNCHANGED)
			return;

		boolean hideBoth = configFilteringOps == FILTERING_OPTION_STYLE.HIDE_BOTH || (configFilteringOps == FILTERING_OPTION_STYLE.HOTKEY_SHOW && !modifyOpHotkeyHeld);

		if(op.equals("Enable spell filtering") && (hideBoth || configFilteringOps == FILTERING_OPTION_STYLE.HIDE_ENABLE)){
			removeOption();
		}

		if(op.equals("Disable spell filtering") && (hideBoth || configFilteringOps == FILTERING_OPTION_STYLE.HIDE_DISABLE)){
			removeOption();
		}
	}

	/**Removes most recent menu option*/
	public void removeOption(){
		client.setMenuEntries(Arrays.copyOf(client.getMenuEntries(), client.getMenuEntries().length - 1));
	}

	/**Slight changes to SpellbookPlugin, added null check for widget so shutdown doesn't cause NPE*/
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

	/**Slight changes to SpellbookPlugin, mainly isHidden->isShown and different opacity defaults.
	 * This is called when hiding/unhiding spells*/
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

	/**for the given spellbook, remove hide/unhide op and reset the opacity of the spells*/
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

	/**Unchanged, matches SpellbookPlugin*/
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

	/**Unchanged, matches SpellbookPlugin*/
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

	/**called on shutdown, removes hide/unhide ops and restores opacity to all spells & deletes red warning pane if present.*/
	private void cleanupSpellbook()
	{
		reordering = false;

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

	/**Unchanged, matches SpellbookPlugin*/
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

	/**Unchanged, matches SpellbookPlugin*/
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

	/**Rename preset, return the result of its success.*/
	public boolean renameSpellbooks(String originalPreset, String updatedPreset){
		if(updatedPreset.length() >= 50)
			return false;

		boolean configAlreadyExists = spellbooksKeyExists(updatedPreset);
		if(configAlreadyExists)
			return false;

		saveSpellbooks(
				updatedPreset,
				getSpellbooks(originalPreset));
		removeSpellbooksKey(originalPreset);
		return true;
	}

	/**Check if preset config key exists*/
	public boolean spellbooksKeyExists(String preset)
	{
		return configManager.getConfiguration(GROUP,"spellbookData_"+preset) != null;
	}

	/**Clear preset from config*/
	public void removeSpellbooksKey(String preset){
		configManager.unsetConfiguration(GROUP,"spellbookData_"+preset);
	}


	/**Add empty preset of name to config*/
	public boolean addSpellBooksKey(String preset){
		boolean configAlreadyExists = configManager.getConfiguration(GROUP,"spellbookData_"+preset) != null;
		if(configAlreadyExists)
		{
			return false;
		}

		saveSpellbooks(preset,new HashMap<>());
		return true;
	}


	/**Converts collection of spellbooks (spellbookid->(spellid,spelldata)) to json
	 * Saves the json to config with the name of given preset
	 * Only called on redorder-end for performance.*/
	public void saveSpellbooks(String preset, Map<Integer, Map<Integer, SpellData>> data){
		String json = gson.toJson(data);
		configManager.setConfiguration(GROUP, "spellbookData_"+preset, json);
		log.debug("setting "+preset +" > "+json);
	}

	/**returns a collection of spellbook > spellData
	 * ex lunarId, {spellid,spelldata}
	 * .. normalId, {spell,spelldata}
	 * instead of editing the config every spell edit, we modify this mapping. when edit mode is left we save the mapping to config as a whole.*/
	public Map<Integer, Map<Integer, SpellData>> getSpellbooks(String preset){
		String json = configManager.getConfiguration(GROUP, "spellbookData_"+preset);
		if (Strings.isNullOrEmpty(json))
		{
			return new HashMap<>();
		}
		return gson.fromJson(json, new TypeToken<Map<Integer, Map<Integer, SpellData>>>(){}.getType());
	}

	/**sets spelldata to be hidden, adds book of spellbookId to spellbooks if missing*/
	private void setHidden(int spellbookId, int spellId, boolean hidden) {
		Map<Integer, SpellData> book = spellbooks.computeIfAbsent(spellbookId, k -> new HashMap<>());

		SpellData spell = book.computeIfAbsent(spellId, k -> new SpellData(-1, false));
		spell.setHidden(hidden);

		if (spell.isUnused()) {
			book.remove(spellId);
		}
	}

	/**sets spelldata position, adds book of spellbookId to spellbooks if missing*/
	private void setPosition(int spellbookId, int spellId, int position) {
		Map<Integer, SpellData> book = spellbooks.computeIfAbsent(spellbookId, k -> new HashMap<>());

		SpellData spell = book.computeIfAbsent(spellId, k -> new SpellData(-1, false));
		spell.setPosition(position);

		if (spell.isUnused()) {
			book.remove(spellId);
		}
	}

	/**indicates the spell in the current book should be visible (defaults to false if not present)*/
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

	/**the position of the current spell if tracked. (defaults to -1 if not present)*/
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