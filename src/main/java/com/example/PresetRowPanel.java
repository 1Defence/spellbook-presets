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

    public void ChangePresetName(){
        String originalName = GetPresetName();
        String updatedName = presetNameInputField.getText();
        //rename succeeded, update UI accordingly.
        if(plugin.renameSpellbooks(originalName,updatedName)){
            presetName = updatedName;
            presetNameOutputField.setText(presetName);
            presetListHandle.remove(originalName);
            presetListHandle.add(presetName);

            savedPresetList.remove(originalName);
            savedPresetList.add(presetName);
        }

        //update regardless of success, as we need to revert back to origin
        ToggleRenameUI();
    }

    //toggle and update the renaming UI of the row.
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

    public void MoveTo(List<String> otherPresetContainer){
        presetListHandle.remove(presetName);
        otherPresetContainer.add(presetName);
    }

    public void Export(){
        plugin.selectionHandler.exportPreset(GetPresetName());
    }
}
