package org.icemoon.game.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.icelib.Icelib;
import org.icelib.Persona;
import org.icelib.Slot;
import org.icemoon.Config;
import org.icemoon.Constants;
import org.icemoon.game.GameAppState;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.InventoryAndEquipment;
import org.icenet.NetworkException;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.tools.DragContext;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.XTabPanelContent;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyAlertBox;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyDialogBox;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.FancyWindow.Size;
import org.iceui.controls.IconTabControl;
import org.iceui.controls.SaveType;
import org.iceui.controls.UIUtil;
import org.iceui.controls.XSeparator;
import org.iceui.controls.ZMenu;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;

import icetone.controls.buttons.Button;
import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.layout.FlowLayout;
import icetone.core.layout.LUtil;
import icetone.core.layout.XYLayoutManager;
import icetone.core.layout.mig.MigLayout;

/**
 * Displays character sheet
 */
public class CharacterSheetAppState extends IcemoonAppState<HUDAppState> implements InventoryAndEquipment.Listener {

    private final static Logger LOG = Logger.getLogger(CharacterSheetAppState.class.getName());
    private Persona player;
    private FancyPersistentWindow character;
    private InventoryAndEquipment inventoryAndEquipment;
    private DragContext dragContext;
    private Element equipPanel;
    private Label damageResistMystic;
    private Label damageResistMelee;
    private Label damageResistFrost;
    private Label damageResistFire;
    private Label damageResistDeath;
    private Label spirit;
    private Label psyche;
    private Label consitution;
    private Label dexterity;
    private Label strength;
    private Label attackSpeedMod;
    private Label blockMod;
    private Label castSpeedMod;
    private Label healingMod;
    private Label magicCritMod;
    private Label magicHitMod;
    private Label meleeCritMod;
    private Label meleeHitMod;
    private Label parryMod;
    private Label regenerationMod;
    private Label runSpeedMod;
    private Label health;
    private Label armor;
    private ButtonAdapter deequipAll;
    private Map<Integer, Button> loadEquipButtons = new HashMap<Integer, Button>();
    private Preferences preferences;
    private Element charmPanel;
    private Container left;
    private NetworkAppState network;
    
    public CharacterSheetAppState() {
        super(Config.get());
    }

    @Override
    protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
        player = stateManager.getState(GameAppState.class).getPlayer();
        return stateManager.getState(HUDAppState.class);
    }

    @Override
    protected void postInitialize() {
        network = stateManager.getState(NetworkAppState.class);
        final GameAppState game = stateManager.getState(GameAppState.class);

        // Preferences are per-character for equipment
        preferences = Config.get().node("savedEquipment").node(String.valueOf(game.getPlayer().getEntityId()));

        inventoryAndEquipment = game.getInventory();
        dragContext = game.getDragContext();


        character = (FancyPersistentWindow) screen.getElementById(Config.CHARACTER);
        if (character != null) {
            // In case the special effects have not finished yet
            character.hide();
        }
        character = new FancyPersistentWindow(screen, Config.CHARACTER,
                screen.getStyle("Common").getInt("defaultWindowOffset"), VPosition.TOP, HPosition.LEFT, new Vector2f(300, 390), FancyWindow.Size.LARGE, true,
                SaveType.POSITION, Config.get()) {
            @Override
            protected void onCloseWindow() {
                super.onCloseWindow();
                stateManager.detach(CharacterSheetAppState.this);
            }
        };
        character.setWindowTitle(player.getDisplayName());
        character.setIsMovable(true);
        character.setIsResizable(false);

        final Element contentArea = character.getContentArea();
        contentArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2", "[fill, grow]8[]", "[fill, grow][shrink 0]"));

        // Left hand side (stats etc)
        left = new Container(screen);
        left.setLayoutManager(new MigLayout(screen, "ins 0, wrap 2, gap 1", "[grow, :120:, align right][grow, :70:, align left]", "[][][][][][][][][][][][][][]"));
        contentArea.addChild(left);

        ColorRGBA highlightTitlesColor = screen.getStyle("Common").getColorRGBA("highlightTitlesColor");
        Label characterDescription = new Label(screen);
        characterDescription.setTextAlign(BitmapFont.Align.Center);
        characterDescription.setText(String.format("Level %d %s", player.getLevel(), Icelib.toEnglish(player.getProfession())));
        ElementStyle.medium(screen, characterDescription);
        characterDescription.setFontColor(highlightTitlesColor);
        left.addChild(characterDescription, "span 2, ax center, growx");
        left.addChild(new XSeparator(screen, Element.Orientation.HORIZONTAL), "span 2, ax center, growx");

        // TODO show base and buffed

        // Health                
        health = addStatLabel(left, "Health : ", player.getHealth(), 0);
        left.addChild(new XSeparator(screen, Element.Orientation.HORIZONTAL), "span 2, ax center, growx");
        createTitle(left, "Statistics");

        strength = addStatLabel(left, "Strength :", player.getStrength(), inventoryAndEquipment.getStrengthBonus());
        dexterity = addStatLabel(left, "Dexterity :", player.getDexterity(), inventoryAndEquipment.getDexterityBonus());
        consitution = addStatLabel(left, "Constitution :", player.getConstitution(), inventoryAndEquipment.getConstitutionBonus());
        psyche = addStatLabel(left, "Psyche :", player.getPsyche(), inventoryAndEquipment.getPsycheBonus());
        spirit = addStatLabel(left, "Spirit :", player.getSpirit(), inventoryAndEquipment.getSpiritBonus());

        // Stats        
        createTitle(left, "Defenses");
        armor = addStatLabel(left, "Armor :", 0, 0);
        damageResistDeath = addStatLabel(left, "Death :", player.getDamageResistDeath(), inventoryAndEquipment.getDeathDefense());
        damageResistFire = addStatLabel(left, "Fire :", player.getDamageResistFire(), inventoryAndEquipment.getFireDefense());
        damageResistFrost = addStatLabel(left, "Frost :", player.getDamageResistFrost(), inventoryAndEquipment.getFrostDefense());
        damageResistMelee = addStatLabel(left, "Melee :", player.getDamageResistMelee(), inventoryAndEquipment.getMeleeDefense());
        damageResistMystic = addStatLabel(left, "Mystic :", player.getDamageResistMystic(), inventoryAndEquipment.getMysticDefense());

        // Right hand side (equip and charms)
        IconTabControl right = new IconTabControl(screen);
        contentArea.addChild(right);
        equipTab(right);
        charmTab(right);
        modsTab(right);

        // Buttons
        Element buttons = new Element(screen);
        buttons.setLayoutManager(new FlowLayout(4, BitmapFont.Align.Center));

        FancyButton save = new FancyButton(screen) {
            @Override
            public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
                showSaveMenu(evt.getX(), evt.getY());
            }
        };
        save.setText("Save");
        save.setToolTipText("Save this equipment set");
        buttons.addChild(save);

        for (int i = 1; i <= Constants.ALLOWED_SAVEABLE_EQUIPMENT_SETS; i++) {
            final int fi = i;
            FancyButton loadEquipButton = new FancyButton(screen) {
                @Override
                public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
                    super.onButtonMouseLeftUp(evt, toggled);
                    loadSavedEquipment(fi);
                }
            };
            loadEquipButton.setText(String.valueOf(i));
            loadEquipButton.setToolTipText("Load Equipment Set " + i);
            buttons.addChild(loadEquipButton);
            loadEquipButtons.put(i, loadEquipButton);
        }
        deequipAll = new FancyButton(screen) {
            @Override
            public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
                deequipAll();
            }
        };
        deequipAll.setText("De-equip All");
        deequipAll.setToolTipText("Remove all equipment and place it back in your inventory (space allowing)");
        buttons.addChild(deequipAll);

        contentArea.addChild(buttons, "span 2");

        // Initial build
        doRebuild();

        // Show with an effect and sound    
        character.setDestroyOnHide(true);
        screen.addElement(character);
        character.sizeToContent();
        character.hide();
        character.showWindow();

        // Listen for changes to inventory and equipment
        inventoryAndEquipment.addListener(this);

    }

    @Override
    public void update(float tpf) {
    }

    @Override
    public void rebuild(Persona persona, InventoryAndEquipment inv) {
        // Equipment has changed
        // TODO find a better of stopping exceptions when rebuilding during a drag
        if (dragContext.isDragging()) {
            dragContext.cancel();
        }
        app.enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                doRebuild();
                return null;
            }
        });
    }

    @Override
    public void slotChanged(InventoryAndEquipment.InventoryItem oldItem, InventoryAndEquipment.InventoryItem newItem) {
    }

    @Override
    protected void onCleanup() {
        character.hideWindow();
    }

    private void loadSavedEquipment(int saveSlot) {
        Preferences n = preferences.node(String.valueOf(saveSlot));
        try {
            // Work out if any additional slots are needed. If they are, do we have enough
            // space in inventory
            int spacesRequiredForNewSet = n.keys().length;
            int spacesTakenByCurrentSet = inventoryAndEquipment.getTotalBasicEquipment();
            int diff = spacesRequiredForNewSet - spacesTakenByCurrentSet;
            if (diff > inventoryAndEquipment.getFreeSlots()) {
                FancyAlertBox.alert(screen, "Insufficient Space",
                        "There is not enough space in your inventory to de-equip the current set. Please clear some slots and try again",
                        FancyAlertBox.AlertType.ERROR);
            } else {
                // Iterate the stored set and make sure the item is already in place, or is inventory
                // If it is inventory, swap it.
                List<String> items = new ArrayList<String>(Arrays.asList(n.keys()));

                try {
                    for (Iterator<String> itemsIt = items.iterator(); itemsIt.hasNext();) {
                        String key = itemsIt.next();
                        Slot slot = Slot.valueOf(key);
                        long entityId = n.getLong(key, -1);
                        InventoryAndEquipment.InventoryItem invItem = inventoryAndEquipment.getInventoryItemById(entityId);

                        // Is the item already there?
                        InventoryAndEquipment.EquipmentItem eq = inventoryAndEquipment.getEquipmentInSlot(slot);
                        if (eq.getItem() == null) {
                            // Nothing in the slot
                            if (invItem == null) {
                                // Item is not in inventory, nor equipped, must have been trashed
                                LOG.warning(String.format("Saved inventory item %d for slot %s no longer exists, skipping", entityId, slot));
                            } else {
                                // move in the new item
                                inventoryAndEquipment.equip(slot, invItem);
                            }
                        } else {
                            // Is it the same item anyway?
                            if (eq.getItem().getEntityId() != entityId) {

                                if (invItem == null) {
                                    // Item is not in inventory, nor equipped, must have been trashed
                                    LOG.warning(String.format("Saved inventory item %d for slot %s no longer exists, skipping", entityId, slot));
                                } else {
                                    // It's not, swap the items
                                    inventoryAndEquipment.equip(slot, invItem);
                                }
                            }
                        }
                    }

                    // Go through any items still in equipment that are not in the saved set
                    for (InventoryAndEquipment.EquipmentItem it : new ArrayList<InventoryAndEquipment.EquipmentItem>(inventoryAndEquipment.getEquipment())) {
                        if (it.getItem() != null && it.getSlot().isBasic() && !items.contains(it.getSlot().name())) {
                            inventoryAndEquipment.deequip(-1, it.getSlot());
                        }
                    }
                } catch (NetworkException ex) {
                    LOG.log(Level.SEVERE, "Failed to equip item.", ex);
                }

            }
        } catch (BackingStoreException bse) {
        }
    }

    private void deequipAll() {
        final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), Size.LARGE, true) {
            @Override
            public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
                hideWindow();
            }

            @Override
            public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
                try {
                    LOG.info("De-equiping all");
                    for (InventoryAndEquipment.EquipmentItem ei : new ArrayList<InventoryAndEquipment.EquipmentItem>(inventoryAndEquipment.getEquipment())) {
                        if (ei.getItem() != null && !ei.getSlot().isContainer() && !ei.getSlot().isCharm()) {
                            LOG.info(String.format("De-equiping %s", ei.getItem().getDisplayName()));
                            network.getClient().deequip(-1, ei.getSlot());
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to de-equip all items.", e);
                }
                hideWindow();
            }
        };
        dialog.setDestroyOnHide(true);
        dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
        dialog.setWindowTitle("De-equip All");
        dialog.setButtonOkText("De-equip");
        dialog.setMsg("Are you sure? All equipment will be removed, you will be naked and harmless!");
        dialog.sizeToContent();
        dialog.setIsResizable(false);
        dialog.setIsMovable(false);
        UIUtil.center(screen, dialog);
		screen.addElement(dialog, null, true);
        dialog.showAsModal(true);
    }

    private void setAvailable() {
        deequipAll.setIsEnabled(inventoryAndEquipment.getTotalBasicEquipment() > 0 && inventoryAndEquipment.getFreeSlots() >= inventoryAndEquipment.getTotalBasicEquipment());
        for (Map.Entry<Integer, Button> i : loadEquipButtons.entrySet()) {
            try {
                final List<String> getSavedSlots = Arrays.asList(preferences.childrenNames());
                i.getValue().setIsEnabled(getSavedSlots.contains(String.valueOf(i.getKey())));
            } catch (BackingStoreException bse) {
                throw new RuntimeException(bse);
            }
        }
    }

    private void doRebuild() {
        System.err.println("---- BEFORE REMOVING ----");
        UIUtil.dump(character, 0);
        charmPanel.removeAllChildren();
        equipPanel.removeAllChildren();

        System.err.println("---- AFTER REMOVING ----");
        UIUtil.dump(character, 0);

        for (final InventoryAndEquipment.EquipmentItem eq : inventoryAndEquipment.getEquipment()) {
            if (eq.getSlot().isBasic()) {
                addEquipmentToSheet("Equipment", equipPanel, eq);
            } else if (eq.getSlot().isCharm()) {
                addEquipmentToSheet("Charms", charmPanel, eq);
            }
        }

        // Update stats
        updateStatLabel(health, player.getHealth(), 0);
        updateStatLabel(strength, player.getStrength(), inventoryAndEquipment.getStrengthBonus());
        updateStatLabel(dexterity, player.getDexterity(), inventoryAndEquipment.getDexterityBonus());
        updateStatLabel(consitution, player.getConstitution(), inventoryAndEquipment.getConstitutionBonus());
        updateStatLabel(psyche, player.getPsyche(), inventoryAndEquipment.getPsycheBonus());
        updateStatLabel(spirit, player.getSpirit(), inventoryAndEquipment.getSpiritBonus());
        updateStatLabel(damageResistDeath, player.getDamageResistDeath(), inventoryAndEquipment.getDeathDefense());
        updateStatLabel(damageResistFire, player.getDamageResistFire(), inventoryAndEquipment.getFireDefense());
        updateStatLabel(damageResistFrost, player.getDamageResistFrost(), inventoryAndEquipment.getFrostDefense());
        updateStatLabel(damageResistMelee, player.getDamageResistMelee(), inventoryAndEquipment.getMeleeDefense());
        updateStatLabel(damageResistMystic, player.getDamageResistMystic(), inventoryAndEquipment.getMysticDefense());
        updateModLabel(attackSpeedMod, inventoryAndEquipment.getAttackSpeedMod());
        updateModLabel(blockMod, inventoryAndEquipment.getBlockMod());
        updateModLabel(castSpeedMod, inventoryAndEquipment.getCastSpeedMod());
        updateModLabel(healingMod, inventoryAndEquipment.getHealingMod());
        updateModLabel(magicCritMod, inventoryAndEquipment.getMagicCritMod());
        updateModLabel(magicHitMod, inventoryAndEquipment.getMagicHitMod());
        updateModLabel(meleeCritMod, inventoryAndEquipment.getMeleeCritMod());
        updateModLabel(meleeHitMod, inventoryAndEquipment.getMeleeHitMod());
        updateModLabel(parryMod, inventoryAndEquipment.getParryMod());
        updateModLabel(regenerationMod, inventoryAndEquipment.getRegenHealthMod());
        updateModLabel(runSpeedMod, inventoryAndEquipment.getRunSpeedMod());

        setAvailable();

        left.layoutChildren();
    }

    private void updateModLabel(Label label, int base) {
        label.setText(String.format("%d", base));
    }

    private void updateStatLabel(Label label, int base, int buffs) {
        if (buffs > 0) {
            label.setFontColor(screen.getStyle("Common").getColorRGBA("positiveColor"));
        } else if (buffs == 0) {
            label.setFontColor(screen.getStyle("Common").getColorRGBA("altColor"));
        } else {
            label.setFontColor(screen.getStyle("Common").getColorRGBA("negativeColor"));
        }
        label.setText(String.format("%d (%d)", base, base + buffs));
    }

    private void equipTab(IconTabControl tabs) {
	XTabPanelContent el = new XTabPanelContent(screen, "EquipmentContainer");
        el.setLayoutManager(new MigLayout(screen, "gap 0, ins 0, fill", "[align center]", "[align center]"));
        el.setIsMovable(false);
        el.setIsResizable(false);
        el.addChild(equipPanel = new Element(screen, "EquipmentPanel", Vector2f.ZERO,
                screen.getStyle("Equipment").getVector2f("defaultSize"),
                screen.getStyle("Equipment").getVector4f("resizeBorders"),
                screen.getStyle("Equipment").getString("defaultImg")));
        equipPanel.setLayoutManager(new XYLayoutManager());
        tabs.addTabWithIcon("Equipment", screen.getStyle("IconTab").getString("equipImg"));
        tabs.addTabChild(0, el);
    }

    private void charmTab(IconTabControl tabs) {
	XTabPanelContent el = new XTabPanelContent(screen, "CharmContainer", LUtil.LAYOUT_SIZE);
        el.setIsMovable(false);
        el.setIsResizable(false);
        el.setLayoutManager(new MigLayout(screen, "gap 0, ins 0, fill", "[align center]", "[align center]"));
        el.addChild(charmPanel = new Element(screen, "CharmPanel", Vector2f.ZERO,
                screen.getStyle("Charms").getVector2f("defaultSize"),
                screen.getStyle("Charms").getVector4f("resizeBorders"),
                screen.getStyle("Charms").getString("defaultImg")));
        charmPanel.setLayoutManager(new XYLayoutManager());
        tabs.addTabWithIcon("Charms", screen.getStyle("IconTab").getString("charmImg"));
        tabs.addTabChild(1, el);
    }

    private void modsTab(IconTabControl tabs) {
	XTabPanelContent el = new XTabPanelContent(screen, "ModsContainer", LUtil.LAYOUT_SIZE);
        el.setIsMovable(false);
        el.setIsResizable(false);
        el.setLayoutManager(new MigLayout(screen, "gap 0, ins 0", "push[]push", "push[]push"));
        Container modsPanel = new Container(screen, "ModsPanel", Vector2f.ZERO,
                screen.getStyle("Mods").getVector2f("defaultSize"), Vector4f.ZERO, null);
        modsPanel.setLayoutManager(new MigLayout(screen, "wrap 2, gap 1, fill", "[grow, align left][grow, align left]", "push[][][][][][][][][][][][][][]push"));
        el.addChild(modsPanel);
        attackSpeedMod = addModLabel(modsPanel, "Attack:", inventoryAndEquipment.getAttackSpeedMod());
        blockMod = addModLabel(modsPanel, "Block :", inventoryAndEquipment.getBlockMod());
        castSpeedMod = addModLabel(modsPanel, "Cast :", inventoryAndEquipment.getCastSpeedMod());
        healingMod = addModLabel(modsPanel, "Healing :", inventoryAndEquipment.getHealingMod());
        magicCritMod = addModLabel(modsPanel, "Magic Crit:", inventoryAndEquipment.getMagicCritMod());
        magicHitMod = addModLabel(modsPanel, "Magic Hit:", inventoryAndEquipment.getMagicHitMod());
        meleeCritMod = addModLabel(modsPanel, "Melee Crit:", inventoryAndEquipment.getMeleeCritMod());
        meleeHitMod = addModLabel(modsPanel, "Melee Hit:", inventoryAndEquipment.getMeleeHitMod());
        parryMod = addModLabel(modsPanel, "Parry:", inventoryAndEquipment.getParryMod());
        regenerationMod = addModLabel(modsPanel, "Regeneration:", inventoryAndEquipment.getRegenHealthMod());
        runSpeedMod = addModLabel(modsPanel, "Run:", inventoryAndEquipment.getRunSpeedMod());
        tabs.addTabWithIcon("Stat modifiers", screen.getStyle("IconTab").getString("modImg"));
        tabs.addTabChild(2, el);
    }

    private Label addModLabel(Element left, String title, int base) {
        Label statLabel = new Label(screen);
        ElementStyle.small(screen, statLabel);
        statLabel.setText(title);
        left.addChild(statLabel);
        Label statValue = new Label(screen);
        updateModLabel(statValue, base);
        ElementStyle.small(screen, statValue);
        statValue.setText(String.format("%d", base));
        statValue.setLabel(statLabel);
        left.addChild(statValue);
        return statValue;
    }

    private Label addStatLabel(Element left, String title, int base, int buffs) {
        Label statLabel = new Label(screen);
        statLabel.setText(title);
        left.addChild(statLabel);
        Label statValue = new Label(screen);
        updateStatLabel(statValue, base, buffs);
        statValue.setLabel(statLabel);
        left.addChild(statValue);
        return statValue;
    }

    private void createTitle(Container left, String title) {
        // Stats
        ColorRGBA highlightTitlesColor = screen.getStyle("Common").getColorRGBA("highlightTitlesColor");
        Label titleLabel = new Label(screen);
        titleLabel.setText(title);
        titleLabel.setTextAlign(BitmapFont.Align.Center);
        ElementStyle.medium(screen, titleLabel);
        titleLabel.setFontColor(highlightTitlesColor);
        left.addChild(titleLabel, "span 2, ax center, growx");
    }

    private void showSaveMenu(float x, float y) {
        ZMenu subMenu = new ZMenu(screen) {
            
            
            @Override
	    protected void onItemSelected(final ZMenuItem item) {
                try {
                    if (Arrays.asList(preferences.childrenNames()).contains(String.valueOf(item.getValue()))) {
                        final FancyDialogBox dialog = new FancyDialogBox(screen, new Vector2f(15, 15), Size.LARGE, true) {
                            @Override
                            public void onButtonCancelPressed(MouseButtonEvent evt, boolean toggled) {
                                hideWindow();
                            }

                            @Override
                            public void onButtonOkPressed(MouseButtonEvent evt, boolean toggled) {
                                saveCurrentToSlot((Integer) item.getValue());
                                hideWindow();
                            }
                        };
                        dialog.setDestroyOnHide(true);
                        dialog.getDragBar().setFontColor(screen.getStyle("Common").getColorRGBA("warningColor"));
                        dialog.setWindowTitle("Overwrite Saved Set");
                        dialog.setButtonOkText("Overwrite");
                        dialog.setMsg("Are you sure? This equipment save slot is already in use");
                        dialog.sizeToContent();
                        dialog.setIsResizable(false);
                        dialog.setIsMovable(false);
                        UIUtil.center(screen, dialog);
                		screen.addElement(dialog, null, true);
                        dialog.showAsModal(true);
                    } else {
                        saveCurrentToSlot((Integer) item.getValue());
                    }
                } catch (BackingStoreException bse) {
                    throw new RuntimeException(bse);
                }
            }
        };
        for (int i = 1; i <= Constants.ALLOWED_SAVEABLE_EQUIPMENT_SETS; i++) {
            subMenu.addMenuItem("Save to set " + i, i);
        }
        screen.addElement(subMenu);
        subMenu.showMenu(null, x, y);
    }

    private void saveCurrentToSlot(int integer) {
        Preferences n = preferences.node(String.valueOf(integer));
        if (inventoryAndEquipment.getTotalBasicEquipment() > 0) {
            for (InventoryAndEquipment.EquipmentItem i : inventoryAndEquipment.getEquipment()) {
                if (i.getItem() == null) {
                    n.remove(i.getSlot().name());
                } else {
                    n.putLong(i.getSlot().name(), i.getItem().getEntityId());
                }
            }
        } else {
            try {
                n.removeNode();
            } catch (BackingStoreException ex) {
                throw new RuntimeException(ex);
            }
        }
        setAvailable();
    }

    private void addEquipmentToSheet(String styleName, Element container, final InventoryAndEquipment.EquipmentItem eq) {
        LOG.info(String.format("Adding equipment to character sheet. %s at %s", eq.getItem(), eq.getSlot()));
        EquipmentDroppable eqd = new EquipmentDroppable(dragContext, inventoryAndEquipment, screen, eq) {
            @Override
            protected boolean doEndDraggableDrag(MouseButtonEvent mbe, Element elmnt) {
                if (elmnt instanceof InventoryItemDroppable) {
                    InventoryItemDroppable drop = (InventoryItemDroppable) elmnt;
                    if (drop.getInventoryItem().getItem() == null) {
                        // Empty slot, can drop straight in
                        try {
                            inventoryAndEquipment.deequip(drop.getInventoryItem().getSlot(), eq.getSlot());
                            return true;
                        } catch (NetworkException ex) {
                            LOG.log(Level.SEVERE, String.format("Failed to de-equip %s.", drop.getInventoryItem().getItem().getDisplayName()), ex);
                        }
                    } else {
                        // In one step, equip the item from the inventory, and stash
                        // the item equipped (without needing a bag slot to store)
                        try {
                            inventoryAndEquipment.swapWithInventoryItem(drop.getInventoryItem().getSlot(), eq.getSlot());
                            removeAllChildren();
                            return true;
                        } catch (NetworkException ex) {
                            LOG.log(Level.SEVERE, String.format("Failed to swap %s.", drop.getInventoryItem().getItem().getDisplayName()), ex);
                        }
                    }
                }
                return false;
            }
        };
        LOG.info(String.format("Adding %s to %s", eq.getItem(), eq.getSlot()));
        Vector2f pos = screen.getStyle(styleName).getVector2f("position" + eq.getSlot().name());
        if (pos != null) {
            eqd.setPosition(pos);
            if (screen.getElementById(eqd.getUID()) != null) {
                LOG.info("CONFLICT!!!!");
                for (Element el : equipPanel.getElements()) {
                    LOG.info(">>>>> " + el.getUID() + ": " + el.getName());
                }
            } else {
                container.addChild(eqd, pos);
            }
        } else {
            LOG.log(Level.SEVERE, String.format("Could not find position for slot %s", eq.getSlot()));
        }
    }
}
