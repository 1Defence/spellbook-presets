/*
 * Copyright (c) 2021, Adam <Adam@sigterm.info>
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
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class ImportExportHandler
{
    SpellbookPresetsPlugin plugin;
    Gson gson;

    @Inject
    public ImportExportHandler(SpellbookPresetsPlugin plugin, Gson gson){
        this.plugin = plugin;
        this.gson = gson;
    }

    /**Export saved preset of specified name and saves to clipboard*/
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

    /**Stash the pending import into an object
     * used to extract data like the name prior to accepting the import.
     * thrown to GC if the user declines.*/
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

                //quick check to prevent json parse in the common event that the clipboard text is obviously not a valid import.
                if (!clipboardText.trim().startsWith("{")) {
                    return null;
                }

                //clipboard text present and potentially a json.
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

    /**confirm the import, saves the import to config.*/
    public void confirmImport(SpellbookPresetJsonObject jsonObject){
        plugin.saveSpellbooks(jsonObject.preset,jsonObject.data);
        log.debug("Imported spellbook preset \"{}\"", jsonObject.preset);
    }



}
