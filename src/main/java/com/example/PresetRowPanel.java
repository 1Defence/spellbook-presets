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

import net.runelite.client.ui.components.FlatTextField;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PresetRowPanel extends JPanel
{
    SpellbookPresetsPlugin plugin;

    String presetName;

    JLabel presetNameOutputField;
    FlatTextField presetNameInputField;

    List<String> presetListHandle;

    List<String> savedPresetList;

    public JLabel renameButtonLabel;
    public JLabel saveButtonLabel;
    public JLabel cancelButtonLabel;

    public PresetRowPanel(LayoutManager layout,String presetName,List<String> savedPresetList, List<String> presetListHandle, SpellbookPresetsPlugin plugin){
        super(layout,true);
        this.presetName = presetName;
        this.savedPresetList = savedPresetList;
        this.presetListHandle = presetListHandle;
        this.plugin = plugin;
    }

    public void SetNameFields(JLabel presetNameOutputField,FlatTextField presetNameInputField){
        this.presetNameOutputField = presetNameOutputField;
        this.presetNameInputField = presetNameInputField;
    }

    public void SetButtonFields(JLabel renameButtonLabel,JLabel saveButtonLabel,JLabel cancelButtonLabel){
        this.renameButtonLabel = renameButtonLabel;
        this.saveButtonLabel = saveButtonLabel;
        this.cancelButtonLabel = cancelButtonLabel;
    }

    public String GetPresetName(){
        return presetName;
    }

    /**Attempt to save a pending rename to config*/
    public void ChangePresetName(){
        String originalName = GetPresetName();
        String updatedName = presetNameInputField.getText().trim();
        //rename succeeded, update UI accordingly.
        if(!updatedName.isEmpty() && plugin.renameSpellbooks(originalName,updatedName)){
            presetName = updatedName;
            presetNameOutputField.setText(presetName);

            //modify the name in actives/inactives list
            int handleIndex = presetListHandle.indexOf(originalName);
            if (handleIndex != -1) {
                presetListHandle.set(handleIndex, presetName);
            }

            //modify the name in saves list if present, add it otherwise
            int saveIndex = savedPresetList.indexOf(originalName);
            if (saveIndex != -1) {
                savedPresetList.set(saveIndex, presetName);
            }else{
                savedPresetList.add(presetName);
            }

            //attempt to change to the new preset.
            if(plugin.currentPreset.equals(originalName)){
                plugin.changePreset(presetName);
            }
        }

        //update regardless of success, as we need to revert to origin
        ToggleRenameUI();
    }

    /**toggle and update the renaming UI of the row
     * essentially rename mode on/off */
    public void ToggleRenameUI(){

        boolean editEnabled = presetNameInputField.isBlocked();

        cancelButtonLabel.setVisible(editEnabled);
        saveButtonLabel.setVisible(editEnabled);
        renameButtonLabel.setVisible(!editEnabled);

        presetNameInputField.setEditable(editEnabled);
        presetNameInputField.setText(presetNameOutputField.getText());

        if(editEnabled){
            presetNameOutputField.setVisible(false);

            presetNameInputField.setFocusable(true);
            presetNameInputField.setVisible(true);
            presetNameInputField.requestFocusInWindow();
            presetNameInputField.getTextField().selectAll();
        }else{
            presetNameInputField.setVisible(false);
            presetNameInputField.setFocusable(false);

            presetNameOutputField.setVisible(true);
            presetNameOutputField.requestFocusInWindow();
        }
    }

    /**Moves preset string from current container to other container
     * i.e current container is active, other is inactive
     * removes preset from active and adds it to inactive.*/
    public void MoveTo(List<String> otherPresetContainer){
        presetListHandle.remove(presetName);
        otherPresetContainer.add(presetName);
    }

    /**Export Row's preset and saves to clipboard*/
    public void Export(){
        plugin.importExportHandler.exportPreset(GetPresetName());
    }
}
