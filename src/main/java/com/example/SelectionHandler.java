/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
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
import com.google.common.util.concurrent.Runnables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextMenuInput;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map;
import java.util.List;

import static com.example.SpellbookPresetsConfig.GROUP;

@Slf4j
public class SelectionHandler
{
    SpellbookPresetsPlugin plugin;
    ChatboxPanelManager chatboxPanelManager;
    ConfigManager configManager;
    ClientThread clientThread;
    Gson gson;

    @Inject
    public SelectionHandler(SpellbookPresetsPlugin plugin, ChatboxPanelManager chatboxPanelManager, ConfigManager configManager, ClientThread clientThread, Gson gson){
        this.plugin = plugin;
        this.chatboxPanelManager = chatboxPanelManager;
        this.configManager = configManager;
        this.clientThread = clientThread;
        this.gson = gson;
    }

    //check if an export, import, or both are valid and prompt the user asking which option to perform.
    public void promptForImportExport(Map<Integer, Map<Integer, SpellData>> spellbooks, String currentPreset)
    {
        ChatboxTextMenuInput chatboxTextMenuInput = chatboxPanelManager.openTextMenuInput("");
        boolean hasImport = false;
        boolean hasExport = false;
        try
        {
            final String clipboardText = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor)
                    .toString();
            if (Strings.isNullOrEmpty(clipboardText))
            {
                log.warn("nothing to import");
            }else{
                    //clipboard is present
                try
                {
                    final SpellbookPresetJsonObject importPreset = gson.fromJson(clipboardText, SpellbookPresetJsonObject.class);
                    //import is valid, add the option
                    if(importPreset != null){
                        hasImport = true;
                        chatboxTextMenuInput.option("Import ("+importPreset.preset+")", () -> importPreset(clipboardText));
                    }
                }
                catch (JsonSyntaxException e)
                {
                    log.debug("Malformed JSON for clipboard import");
                }
            }
        }
        catch (IOException | UnsupportedFlavorException ex)
        {
            log.warn("error reading clipboard", ex);
        }

        //export is valid, add the option
        if(!spellbooks.isEmpty() && !currentPreset.isEmpty()){
            hasExport = true;
            chatboxTextMenuInput.option("Export ("+currentPreset+")", () -> exportPreset(spellbooks,currentPreset));
        }

        //export or import present, set title add cancel option and build chatbox
        if(hasImport || hasExport){
            String title =
                    !hasImport ? "Export spellbook preset?" :
                    !hasExport ? "Import spellbook preset?" :
                                 "Import or export spellbook preset?";
            chatboxTextMenuInput.title(title)
                .option("Cancel", Runnables.doNothing())
                    .build();
        }

    }

    //export the current loaded preset and copy it to clipboard.
    void exportPreset(Map<Integer, Map<Integer, SpellData>> spellbooks, String currentPreset){
        String json = gson.toJson(new SpellbookPresetJsonObject(currentPreset, spellbooks));
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(json), null);
        log.debug("Exported spellbook preset \"{}\" : {}", currentPreset,json);
    }

    public void exportPreset(String preset){
        Map<Integer, Map<Integer, SpellData>> spellbooks = plugin.getSpellbooks(preset);
        if(spellbooks == null)
            return;

        String json = gson.toJson(new SpellbookPresetJsonObject(preset, spellbooks));
        if(Strings.isNullOrEmpty(json))
            return;

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(json), null);
        log.debug("Exported spellbook preset \"{}\" : {}", preset,json);
    }

    SpellbookPresetJsonObject StashImportObject(){
        try
        {
            final String clipboardText = Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor)
                    .toString();
            if (Strings.isNullOrEmpty(clipboardText))
            {
                log.warn("nothing to import");
            }else{
                //clipboard is present
                try
                {
                    return gson.fromJson(clipboardText, SpellbookPresetJsonObject.class);
                }
                catch (JsonSyntaxException e)
                {
                    log.debug("Malformed JSON for clipboard import");
                }
            }
        }
        catch (IOException | UnsupportedFlavorException ex)
        {
            log.warn("error reading clipboard", ex);
        }
        return null;
    }


    //attempt to import a preset from a verified json(users clipboard after checks)
    void importPreset(String json){
        SpellbookPresetJsonObject jsonObject = gson.fromJson(json, SpellbookPresetJsonObject.class);
        log.debug("Attempting to import spellbook preset \"{}\" : {}", jsonObject.preset,json);

        boolean presetExists = configManager.getConfiguration(GROUP, "spellbookData_"+jsonObject.preset) != null;
        if(presetExists){
            log.debug("Preset exists!");
            askForOverwrite(jsonObject);
        }else{
            confirmImport(jsonObject);
        }
    }

    //confirm the import, saves the import to config.
    //inserts recent import to active presets on slot 0 and sets it to active.
    //slot 0 in specific is because we want there to be a hard cap of 10 active presets, more can be stored but will be ignored in list generation.
    public void confirmImport(SpellbookPresetJsonObject jsonObject){
        String presetName = jsonObject.preset;
        plugin.saveSpellbooks(presetName,jsonObject.data);
/*
        String activePresets = configManager.getConfiguration(GROUP, "activePresets");
        String activePresersUpdated = presetName+"\n"+activePresets;
        final String presetNameUpdated = presetName;

        configManager.setConfiguration(GROUP, "activePresets",activePresersUpdated);
        clientThread.invokeLater(() -> plugin.changePreset(presetNameUpdated));
*/
        log.debug("Imported spellbook preset \"{}\"", jsonObject.preset);
    }

    //inform user this will overwrite an existing preset and ask for confirmation
    void askForOverwrite(SpellbookPresetJsonObject jsonObject){
        chatboxPanelManager.openTextMenuInput("A preset with this name already exists! Do you want to overwrite it?")
                        .option("Yes (Overwrite \""+jsonObject.preset+"\")", () -> confirmImport(jsonObject))
                        .option("No", () -> Runnables.doNothing()).build();
    }

    public void promptForDeletion(List<String> removals, String removalString)
    {
        ChatboxTextMenuInput chatboxTextMenuInput = chatboxPanelManager.openTextMenuInput(removalString);
        chatboxTextMenuInput.option("yes",() -> confirmDeletion(removals,removalString))
                .option("no",() -> Runnables.doNothing()).build();
    }

    void confirmDeletion(List<String> removals, String removalString){
        log.debug("Deleting..");
    }

    public void promptForEdit(String oldPreset, String newPreset){
        ChatboxTextMenuInput chatboxTextMenuInput = chatboxPanelManager.openTextMenuInput("Rename preset ("+oldPreset+") to ("+newPreset+")?");
        chatboxTextMenuInput.option("yes",() -> confirmEdit(oldPreset,newPreset))
                .option("no",() -> Runnables.doNothing()).build();
    }

    void confirmEdit(String oldPreset, String newPreset){

    }

}
