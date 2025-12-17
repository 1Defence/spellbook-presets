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

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.DragAndDropReorderPane;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.SpellbookPresetsConfig.ACTIVE_PRESETS_KEY;
import static com.example.SpellbookPresetsConfig.GROUP;

@Slf4j
public class SaveEditPanel extends PluginPanel
{

    SpellbookPresetsPlugin plugin;
    ConfigManager configManager;
    Gson gson;

    //arbitrary cap on presets, even 10 is high may reduce in future.
    private static final int MAX_ACTIVE_PRESETS_COUNT = 10;
    //"name (garbage) (number)"
    Pattern importCopyPattern = Pattern.compile("^(.+?)\\s*\\((\\d+)\\)$");
    //"Preset #"
    Pattern newPresetPattern = Pattern.compile("^Preset\\s*(\\d+)$");
    private final IconTextField searchBar;
    public String queryText = "";
    public Timer configUpdateTimer;

    /**Row Containers*/
    private final JPanel inactivePresetsContainer;
    private final DragAndDropReorderPane activePresetsContainer;
    public static List<String> inactivePresetList = new ArrayList<>();
    public static List<String> activePresetList = new ArrayList<>();

    //generated on start, parses what we have in the config.
    public static List<String> savedPresetList = new ArrayList<>();
    /***/

    public static final ImageIcon EXPORT_ICON;
    public static final ImageIcon DELETE_ICON;
    public static final ImageIcon REMOVE_ICON, REMOVE_ICON_FADED;
    public static final ImageIcon ADD_ICON, ADD_ICON_FADED;
    public static final ImageIcon IMPORT_ICON, IMPORT_ICON_FADED;
    static
    {
        final BufferedImage exportImg = ImageUtil.loadImageResource(SpellbookPresetsPlugin.class, "export_icon.png");
        EXPORT_ICON = new ImageIcon(exportImg);
        final BufferedImage deleteImg = ImageUtil.loadImageResource(SpellbookPresetsPlugin.class, "delete_icon.png");
        DELETE_ICON = new ImageIcon(deleteImg);

        final BufferedImage removeImg = ImageUtil.loadImageResource(SpellbookPresetsPlugin.class, "remove_icon.png");
        REMOVE_ICON = new ImageIcon(removeImg);
        REMOVE_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(removeImg, -180));

        final BufferedImage addImg = ImageUtil.loadImageResource(SpellbookPresetsPlugin.class, "add_icon.png");
        ADD_ICON = new ImageIcon(addImg);
        ADD_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(addImg, -180));

        final BufferedImage importImg = ImageUtil.loadImageResource(SpellbookPresetsPlugin.class, "import_icon.png");
        IMPORT_ICON = new ImageIcon(importImg);
        IMPORT_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(importImg, -180));
    }


    SaveEditPanel(SpellbookPresetsPlugin plugin, ConfigManager configManager, Gson gson)
    {
        super();
        this.plugin = plugin;
        this.configManager = configManager;
        this.gson = gson;

        setLayout(new BorderLayout());

        final JPanel layoutPanel = new JPanel();
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
        layoutPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(layoutPanel, BorderLayout.CENTER);

        final JPanel headerPanel;
        headerPanel = buildHeaderPanel();
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height));
        layoutPanel.add(headerPanel);

        final JPanel searchPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        searchPanel.setBorder(new EmptyBorder(2, 0, 0, 0)); //initial gap before presets

        //Search bar to filter Inactive presets by query.
        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.setMinimumSize(new Dimension(0, 30));
        searchBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                SetSearchQuery();
            }
        });
        searchBar.addClearListener(this::SetSearchQuery);
        searchPanel.add(searchBar);


        //Holder of Active-Preset rows (Presets that are currently active)
        activePresetsContainer = new DragAndDropReorderPane ();
        activePresetsContainer.setBorder(new EmptyBorder(10, 0, 0, 0)); //initial gap before presets
        activePresetsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        activePresetsContainer.setAlignmentX(LEFT_ALIGNMENT);

        //Detect preset swapping in active list
        activePresetsContainer.addDragListener(
        new DragAndDropReorderPane.DragListener()
            {
               @Override
               public void onDrag(Component component)
               {
                    activePresetList = activePresetsFromComponents();
                    requestConfigUpdate();
                    rebuildActiveList();
               }
            });

        //Holder of Inactive-Preset rows (Presets that are not currently active, but still stored.)
        inactivePresetsContainer = new JPanel ();
        inactivePresetsContainer.setBorder(new EmptyBorder(10, 0, 0, 0)); //initial gap before presets
        inactivePresetsContainer.setLayout(new BoxLayout(inactivePresetsContainer, BoxLayout.Y_AXIS));
        inactivePresetsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        inactivePresetsContainer.setAlignmentX(LEFT_ALIGNMENT);

        addComponents(layoutPanel,
                searchPanel,
                activePresetsContainer,
                inactivePresetsContainer);
        initPresetLists();
        rebuildActiveList();
        rebuildInactiveList(true);
    }

    /**Build panel containing :
     * header title
     * import preset button
     * create new preset button*/
    private JPanel buildHeaderPanel()
    {
        final JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerContainer.setPreferredSize(new Dimension(0, 30));
        headerContainer.setBorder(new EmptyBorder(5, 5, 5, 0));
        headerContainer.setVisible(true);

        final JLabel headerTitle = new JLabel();
        headerTitle.setFont(FontManager.getRunescapeFont());
        headerTitle.setForeground(Color.WHITE);
        headerTitle.setText("Spellbook Presets");

        JButton addButton;
        addButton = CreateButton("Create new preset",ADD_ICON_FADED);
        addButton.setRolloverIcon(ADD_ICON);
        addButton.addActionListener(e -> {
            String nextPresetName = getNextAvailablePreset();
            if(plugin.addSpellBooksKey(nextPresetName)){
                AddNewPresetToCollection(nextPresetName,true);
            }
        });

        JButton importButton;
        importButton = CreateButton("Import preset",IMPORT_ICON_FADED);
        importButton.setRolloverIcon(IMPORT_ICON);


        importButton.addActionListener(e -> {
            SpellbookPresetJsonObject stashedImport = plugin.importExportHandler.StashImportObject();
            if(stashedImport != null)
            {
                String presetName = stashedImport.preset;
                boolean exists = plugin.spellbooksKeyExists(presetName);
                if(exists){
                    presetName =  getNextAvailableImport(presetName);
                }
                stashedImport.preset = presetName;
                String messagePrompt = "Import preset (" + presetName + ") ?";

                int confirm = JOptionPane.showConfirmDialog(headerContainer, messagePrompt, "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION)
                {
                    log.debug("Importing " + stashedImport.preset + " : " + stashedImport.data);
                    plugin.importExportHandler.confirmImport(stashedImport);
                    AddNewPresetToCollection(presetName,exists);
                }

            }
        });

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(importButton);
        buttonsPanel.add(addButton);


        headerContainer.add(buttonsPanel, BorderLayout.EAST);
        headerContainer.add(headerTitle, BorderLayout.WEST);
        return headerContainer;
    }

    /**Used when a new preset is either created or imported
     *  determines the correct collection to add the row to
     *  additionally prompt the user to edit the name*/
    public void AddNewPresetToCollection(String preset, boolean startEdit){
        PresetRowPanel editRow;
        savedPresetList.add(preset);

        if(activePresetsContainer.getComponentCount() < MAX_ACTIVE_PRESETS_COUNT){
            //user hasn't hit the active preset limit, the new row is the last element of our active list
            activePresetList.add(preset);
            rebuildInactiveList(true);
            rebuildActiveList();
            editRow = (PresetRowPanel) activePresetsContainer.getComponent(activePresetsContainer.getComponentCount()-1);
        }else{
            //user has hit the active preset limit, the new row is the first element of our inactive list (prevents confusion as user will see it right away)
            inactivePresetList.add(0,preset);
            rebuildActiveList();
            rebuildInactiveList(false);
            editRow = (PresetRowPanel) inactivePresetsContainer.getComponent(0);
        }

        requestConfigUpdate();

        //attempt to change to the new preset.
        plugin.changePreset(preset);

        if(startEdit)
        {
            editRow.ToggleRenameUI();
        }
    }

    /**update search query to the text of the search bar
     * rebuilds the list of inactive saves that contain the query text*/
    public void SetSearchQuery(){
        if(searchBar != null){
            queryText = searchBar.getText().toLowerCase();
        }
        rebuildActiveList();
        rebuildInactiveList(true);
    }

    /**initial list generation, mostly from config*/
    void initPresetLists(){
        //generate saved list from config
        savedPresetList = savedPresetsFromConfig();

        //generate active list from config
        activePresetList = plugin.activePresetsFromConfig();

        //generate inactive list from whats in saved but not in active
        inactivePresetList = generateInactivePresetList();
        inactivePresetList.sort(ALPHANUMERICAL_COMPARATOR);
    }

    /**returns list of active preset names, specifically from the components in the "activePresetsContainer" panel*
     * used to update the active list to the state of the drag panel.
     */
    public List<String> activePresetsFromComponents(){
        List<String> presetList = new ArrayList<>();
        for (Component presetComponent : activePresetsContainer.getComponents()) {
            if (presetComponent instanceof PresetRowPanel) {
                PresetRowPanel presetRowPanel = (PresetRowPanel) presetComponent;
                presetList.add(presetRowPanel.GetPresetName());
            }
        }
        return presetList;
    }


    /**generates list of inactive preset names
     * this is effectively the list of saved presets with the list of active presets omitted.
     */
    public List<String> generateInactivePresetList(){
        if(savedPresetList.isEmpty())
            return new ArrayList<>();
        //parse the saves

        List<String> presetList = new ArrayList<>();

        //any preset not part of active, is considered to be an inactive.
        for (String preset : savedPresetList)
        {
            if(activePresetList.contains(preset))
                continue;
            presetList.add(preset);
        }
        return presetList;
    }

    /**returns a list of saved presets
     * generated by collecting all "data" keys from the config and filtering them into their preset names.
     */
    public List<String> savedPresetsFromConfig(){
        List<String> presetList = new ArrayList<>();
        for (var key : configManager.getConfigurationKeys(GROUP + ".spellbookData_"))
        {
            String[] splitKey = key.split("\\.", 2);
            if (splitKey.length == 2)
            {
                String[] splitSave = splitKey[1].split("_",2);
                if(splitSave.length == 2){
                    String foundSave = splitSave[1];
                    presetList.add(foundSave);
                }
            }
        }
        return presetList;
    }

    /**rebuilds the active presets container
     * creates a new row of PresetRowPanel for every preset in the active list.
     */
    private void rebuildActiveList(){
        activePresetsContainer.removeAll();

        for(String presetName : activePresetList)
        {
            activePresetsContainer.add(createPresetRow(presetName,true));
        }
        revalidate();
    }

    /**rebuilds the inactive presets container
     * creates a new row of PresetRowPanel for every preset in the inactive list
     * if a query is present, the preset name must contain the query
     * in most cases the list is sorted alphanumerically.
     */
    private void rebuildInactiveList(boolean sort)
    {
        if(sort)
            inactivePresetList.sort(ALPHANUMERICAL_COMPARATOR);

        inactivePresetsContainer.removeAll();

        for(String presetName : inactivePresetList)
        {
            if(!queryText.isEmpty() && !presetName.toLowerCase().contains(queryText)){
                //filters presets to only show if name contains search text
                continue;
            }
            inactivePresetsContainer.add(createPresetRow(presetName,false));
        }
        revalidate();
    }

    /**Creates a managed row for use of the following
     *
     * |-/+|   |Preset-Name|          |Edit| ^| X|
     *
     * Renaming the associated preset
     * Deleting(unsetting from config, removing from containers)
     * Exporting(copying preset data to clipboard for other users to import)
     *
     * Swapping between active container(presets that user can switch between) and inactive container(stored presets that user can later activate to switch between)
     *
     * the passed state "active" determines which container and lists the row in question is moved to.
     * Actives are draggable this is to translate in-game to change the order of your active presets, as such inactives don't support dragging.
     */
    private JPanel createPresetRow(String presetName, boolean active)
    {
        PresetRowPanel row = new PresetRowPanel(new BorderLayout(),presetName,savedPresetList,active ? activePresetList : inactivePresetList,plugin);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 8));
        JPanel nameContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 8));

        JLabel save = CreateButton("Save",ColorScheme.PROGRESS_COMPLETE_COLOR,false);
        JLabel cancel = CreateButton("Cancel",ColorScheme.PROGRESS_ERROR_COLOR,false);
        JLabel rename = CreateButton("Edit",ColorScheme.LIGHT_GRAY_COLOR.darker(),true);
        JLabel nameOutput = new JLabel();
        FlatTextField nameInput = new FlatTextField();


        row.SetNameFields(nameOutput,nameInput);
        row.SetButtonFields(rename,save,cancel);
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(BorderFactory.createMatteBorder(0, 3, 1, 0, active ? ColorScheme.PROGRESS_INPROGRESS_COLOR : ColorScheme.MEDIUM_GRAY_COLOR));


        //The editable name field, which is only visible during renaming
        nameInput.setBorder(new EmptyBorder(0, 2, 0, 0));
        nameInput.setText(presetName);
        nameInput.setFont(FontManager.getRunescapeFont());
        nameInput.getTextField().setForeground(Color.WHITE);
        nameInput.setOpaque(false);
        nameInput.setEditable(false);
        nameInput.setFocusable(false);
        nameInput.setVisible(false);
        nameInput.setPreferredSize(new Dimension(82, nameInput.getPreferredSize().height));

        nameInput.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    row.ChangePresetName();
                    requestConfigUpdate();
                }
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                {
                    row.ToggleRenameUI();
                }
            }
        });


        //The non-editable name field, which is visible when the editable field is hidden.
        nameOutput.setBorder(new EmptyBorder(0, 2, 0, 0));
        nameOutput.setText(presetName);
        nameOutput.setFont(FontManager.getRunescapeFont());
        nameOutput.setForeground(Color.WHITE);
        nameOutput.setOpaque(false);
        nameOutput.setFocusable(false);
        nameOutput.setPreferredSize(new Dimension(130, nameOutput.getPreferredSize().height));


        //conditional button that's setup differently depending on which container the row is in(active or inactive)
        JButton swapActiveButton;

        if(active){
            //row is currently active, the associated button will be a removal button that moves the row from active->inactive
            swapActiveButton = CreateButton("Remove",REMOVE_ICON_FADED);
            swapActiveButton.setRolloverIcon(REMOVE_ICON);

            //when removing a preset, your mouse is hovered over another remove post-refresh but rollover doesn't occur so we manually set it here if valid.
            //why is this needed for the minus button but rows work fine?....
            SwingUtilities.invokeLater(() ->
            {
                Point mousePoint = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mousePoint, swapActiveButton);

                if (swapActiveButton.contains(mousePoint))
                {
                    swapActiveButton.getModel().setRollover(true);
                }
            });

            swapActiveButton.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent mouseEvent)
                {
                    row.MoveTo(inactivePresetList);
                    requestConfigUpdate();
                    rebuildActiveList();
                    rebuildInactiveList(true);
                }
            });

        }else{
            //row is currently inactive, the associated button will be an add button that moves the row from inactive->active
            swapActiveButton = CreateButton("Add",ADD_ICON_FADED);
            swapActiveButton.setRolloverIcon(ADD_ICON);

            if(activePresetsContainer.getComponentCount() < MAX_ACTIVE_PRESETS_COUNT)
            {
                //store the event so both the row and the button can perform it.
                MouseAdapter addEvent = new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent mouseEvent)
                    {
                        row.MoveTo(activePresetList);
                        requestConfigUpdate();
                        rebuildActiveList();
                        rebuildInactiveList(true);
                    }

                    @Override
                    public void mouseEntered(MouseEvent mouseEvent)
                    {
                        swapActiveButton.setIcon(ADD_ICON);
                    }

                    @Override
                    public void mouseExited(MouseEvent mouseEvent)
                    {
                        swapActiveButton.setIcon(ADD_ICON_FADED);
                    }
                };

                //add the event to both the row and the button, similar to the entire row being a click box for the button for easier adding.
                row.addMouseListener(addEvent);
                swapActiveButton.addMouseListener(addEvent);

            }else{

                swapActiveButton.setEnabled(false);

            }

        }

        //two elements in the exact same position to hotswap whats active between the edit and the display text
        //inputfields have drag listeners that steal the events of DragAndDropReorderPane's and forwarding the events leads to inconsistencies
        nameContainer.setOpaque(false);
        addComponents(nameContainer,
                swapActiveButton,
                nameOutput,
                nameInput);


        //Save a preset name change
        save.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                row.ChangePresetName();
                requestConfigUpdate();
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR.darker());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                save.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
            }
        });

        //Cancel a preset name change
        cancel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                row.ToggleRenameUI();
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR.darker());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                cancel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
            }
        });


        //Initiate a preset name change
        rename.setToolTipText("Rename preset");
        rename.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                row.ToggleRenameUI();
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker().darker());
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                rename.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
            }
        });

        //Export button
        JButton exportBtn = CreateButton("Export", EXPORT_ICON);
        exportBtn.addActionListener(e -> {
            row.Export();
            log.debug("Exporting " + row.GetPresetName());
        });

        //Delete button
        JButton deleteBtn = CreateButton("Delete",DELETE_ICON);
        deleteBtn.addActionListener(e -> {
            forcePendingConfig();
            String rowPresetName = row.GetPresetName();
            int confirm = JOptionPane.showConfirmDialog(this, "Delete \"" + rowPresetName + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                log.debug("Deleting " + rowPresetName);
                if(active){
                    activePresetList.remove(rowPresetName);
                    requestConfigUpdate();
                }else
                {
                    inactivePresetList.remove(rowPresetName);
                }
                savedPresetList.remove(rowPresetName);
                plugin.removeSpellbooksKey(rowPresetName);
                rebuildActiveList();
                rebuildInactiveList(true);
            }
        });

        buttonsPanel.setOpaque(false);
        addComponents(buttonsPanel,
                rename,
                save,
                cancel,
                exportBtn,
                deleteBtn);


        //+Name --------- Rename [ ] [ ]
        row.add(nameContainer,BorderLayout.WEST);
        row.add(buttonsPanel, BorderLayout.EAST);

        return row;
    }

    /**convenience method to create row button in the form of a label with predefined values*/
    JLabel CreateButton(String label,Color color,boolean initialVisibility){
        JLabel buttonLabel = new JLabel(label);
        buttonLabel.setVisible(initialVisibility);
        buttonLabel.setFont(FontManager.getRunescapeSmallFont());
        buttonLabel.setForeground(color);
        buttonLabel.setBorder(new EmptyBorder(3, 0, 0, 3));
        return buttonLabel;
    }

    /**convenience method to create row button with predefined values*/
    JButton CreateButton(String tooltip, ImageIcon icon){
        JButton button = new JButton();
        SwingUtil.removeButtonDecorations(button);
        button.setOpaque(false);
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(16,16));
        return button;
    }

    /**convenience method to add a collection of components to a parent component*/
    public void addComponents(JComponent parent, JComponent... components) {
        for (JComponent component : components) {
            parent.add(component);
        }
    }

    /**comparer used to sort a list alphanumerically abc1, abc2, abc19...*/
    Comparator<String> ALPHANUMERICAL_COMPARATOR = new Comparator<>()
    {
        @Override
        public int compare(String a, String b)
        {
            int ia = 0, ib = 0;

            while (ia < a.length() && ib < b.length())
            {
                char ca = a.charAt(ia);
                char cb = b.charAt(ib);

                if (Character.isDigit(ca) && Character.isDigit(cb))
                {
                    int sa = ia, sb = ib;

                    while (ia < a.length() && Character.isDigit(a.charAt(ia))) ia++;
                    while (ib < b.length() && Character.isDigit(b.charAt(ib))) ib++;

                    int na = Integer.parseInt(a.substring(sa, ia));
                    int nb = Integer.parseInt(b.substring(sb, ib));

                    if (na != nb)
                    {
                        return Integer.compare(na, nb);
                    }
                }
                else
                {
                    ca = Character.toLowerCase(ca);
                    cb = Character.toLowerCase(cb);

                    if (ca != cb)
                    {
                        return ca - cb;
                    }

                    ia++;
                    ib++;
                }
            }
            return a.length() - b.length();
        }
    };

    /**config functions to prevent stressing the configmanager, there is no need to update 10 times a second if user is spam dragging/swapping actives
     * for now it's only for actives, because saves are modifed at a much less frequent rate(Addition,rename,deletion) and need to be immediate for stability.
     * request an update to config, if theres a pending update refresh the timer, otherwise set and start it.*/
    private void requestConfigUpdate(){
        if(configUpdateTimer != null){
            configUpdateTimer.restart();
            return;
        }

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                updateConfig();
            }
        };

        configUpdateTimer = new Timer(2000, actionListener);
        configUpdateTimer.setRepeats(false);
        configUpdateTimer.start();
    }

    /**immediate update called when delay timer(configUpdateTimer) has finished.*/
    public void updateConfig(){
        String json = gson.toJson(activePresetList);
        configManager.setConfiguration(GROUP, ACTIVE_PRESETS_KEY, json);
        log.debug("updating configs... "+json);
        configUpdateTimer = null;
    }

    /**If theres a pending config change instantly update the config and thus the options.*/
    public void forcePendingConfig(){
        if(configUpdateTimer == null)
            return;
        configUpdateTimer.stop();
        updateConfig();
    }


    /**find the next valid name of the given import
     * used if the name of the import already exists in the users saves.
     * appends (1),(2)..... to the preset name
     * incremented number if the name we're checking already has a number or if the resulting new string exists
     * ex: import is "pvm"
     * import 1, "pvm"
     * import 2, "pvm (1)"
     * import 3, "pvm (2)"
     * if you now import "pvm (1)", "pvm (3)"*/
    public String getNextAvailableImport(String preset) {

        preset = preset.trim();

        Matcher matcher = importCopyPattern.matcher(preset);
        String baseName;
        int count = 1;

        if (matcher.matches()) {
            baseName = matcher.group(1).trim();
            try {
                count = Integer.parseInt(matcher.group(2))+1;
            } catch (NumberFormatException e) {
                //NaN
                baseName = preset;
            }
        } else {
            baseName = preset;
        }

        String importName = baseName;
        while (savedPresetList.contains(importName)) {
            importName = baseName + " (" + count + ")";
            count++;
        }

        return importName;
    }

    /**find the next valid preset name
     * used when creating a new preset to generate the default name
     * "Preset 1", "Preset 2" .....*/
    public String getNextAvailablePreset() {

        int maxNumber = 0;
        for (String name : savedPresetList) {
            if (name != null) {
                Matcher matcher = newPresetPattern.matcher(name.trim());
                if (matcher.matches()) {
                    int num = Integer.parseInt(matcher.group(1));
                    if (num > maxNumber) {
                        maxNumber = num;
                    }
                }
            }
        }

        return "Preset " + (maxNumber + 1);
    }

    /**When a drastic change has occurred and an entire reset on the panel is required
     * currently this is used on resetting the configuration keys of the plugin*/
    public void Refresh(){
        initPresetLists();
        rebuildActiveList();
        rebuildInactiveList(true);
    }

}