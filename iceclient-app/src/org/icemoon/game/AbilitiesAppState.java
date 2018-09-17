package org.icemoon.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.icelib.Coin;
import org.icemoon.Config;
import org.icemoon.domain.Abilities;
import org.icemoon.domain.Ability;
import org.icemoon.ui.controls.CoinPanel;
import org.icescene.HUDMessageAppState;
import org.icescene.HUDMessageAppState.Channel;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.iceui.controls.ElementStyle;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;

import icetone.controls.buttons.Button;
import icetone.controls.buttons.PushButton;
import icetone.controls.containers.SplitPanel;
import icetone.controls.containers.TabControl;
import icetone.controls.containers.TabControl.TabButton;
import icetone.controls.extras.Meter;
import icetone.controls.extras.Separator;
import icetone.controls.table.TableRow;
import icetone.controls.table.Table.SelectionMode;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Layout.LayoutType;
import icetone.core.Orientation;
import icetone.core.PseudoStyles;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.layout.Border;
import icetone.core.layout.FillLayout;
import icetone.core.layout.XYLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.css.CssProcessor.PseudoStyle;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

/**
 * Displays the abilities.
 */
public class AbilitiesAppState extends IcemoonAppState<HUDAppState> {

	public enum AbilityCategory {
		KNIGHT("CoreK", "CrossK"), ROGUE("CoreR", "CrossR"), MAGE("CoreM", "CrossM"), DRUID("CoreD",
				"CrossD"), WEAPON("Weapon"), PROTECTION("Protection"), TRAVEL("Travel"), RESTORATION("Restoration");

		private List<String> categories;

		private AbilityCategory(String... categories) {
			this.categories = Arrays.asList(categories);
		}

		public static AbilityCategory fromCategory(String category) {
			for (AbilityCategory c : values()) {
				if (c.categories.contains(category))
					return c;
			}
			throw new IllegalArgumentException("No such category " + category);
		}

		public String getStyleId() {
			return "ability-tab-" + name().toLowerCase();
		}

	}

	public final static int TIERS = 6;
	public final static int ROWS = 9;

	private PersistentWindow abilityFrame;
	private TabControl abilityTabs;
	private Abilities abilities;
	private Map<AbilityCategory, Element> categoryTabs = new HashMap<>();
	private Meter nextImprovement;
	private Label abilityName;
	private StyledContainer description;
	private CoinPanel copper;
	private Label apCost;
	private PushButton purchase;
	private Label level;
	private CoreElement coreKnight;
	private CoreElement coreRogue;
	private CoreElement coreMage;
	private CoreElement coreDruid;
	private Label prereqText;
	private Label abilityPoints;
	private PushButton respecialise;
	private Label pointsSpent;

	public AbilitiesAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	int startLevelForTier(int tier) {
		return tier == 1 ? 1 : (tier == 2 ? 6 : (tier - 1) * 10);
	}

	@Override
	protected void postInitialize() {

		/// Minmap window
		abilityFrame = new PersistentWindow(screen, Config.ABILITIES, VAlign.Bottom, Align.Left, new Size(500, 400),
				true, SaveType.POSITION_AND_SIZE, Config.get()) {

			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(AbilitiesAppState.this);
			}
		};
		abilityFrame.setStyleClass("fancy large");
		abilityFrame.setWindowTitle("Abilities");
		abilityFrame.setMovable(true);
		abilityFrame.setResizable(true);
		abilityFrame.setDestroyOnHide(true);

		abilityTabs = new TabControl(screen, Border.SOUTH);
		abilityTabs.setUseSlideEffect(true);
		for (AbilityCategory c : AbilityCategory.values()) {
			TabButton tc = new TabButton(screen);
			tc.setStyleId(c.getStyleId());
			Element catTab = createAbilityCategoryTabContent(c);
			categoryTabs.put(c, catTab);
			abilityTabs.addTab(tc, catTab);
		}

		/* Improvement */
		StyledContainer improvementArea = new StyledContainer(screen,
				new MigLayout(screen, "fill, wrap 1, ins 0, gap 0", "[grow, al 50%]", "[][][]")) {
			{
				setStyleId("improvement");
			}
		};
		improvementArea.addElement(new Label("Ability Improvement At:", screen));
		improvementArea.addElement((nextImprovement = new Meter(screen, Orientation.HORIZONTAL)).setMinValue(1)
				.setMaxValue(7).setShowValue(true).setCurrentValue(4).setReverseDirection(true));
		improvementArea.addElement(ElementStyle.medium(abilityName = new Label(" ", screen)), "growx");

		StyledContainer prereqArea = new StyledContainer(screen,
				new MigLayout(screen, "wrap 7, ins 0, gap 0", "[][:64:][][][][][]")) {
			{
				setStyleId("prereq");
			}
		};
		prereqArea.addElement(ElementStyle.altColor(new Label("Prerequisites", screen)), "span 7");
		prereqArea.addElement(new Label("Level:", screen));
		prereqArea.addElement(level = new Label("00", screen));
		prereqArea.addElement(new Label("Core Class:", screen));
		prereqArea.addElement(coreKnight = new CoreElement(screen, "knight"));
		prereqArea.addElement(coreRogue = new CoreElement(screen, "rogue"));
		prereqArea.addElement(coreMage = new CoreElement(screen, "mage"));
		prereqArea.addElement(coreDruid = new CoreElement(screen, "druid"));
		prereqArea.addElement(prereqText = new Label("Must Have:", screen), "growx, span 7, growy");

		/* Descript */
		description = new StyledContainer(screen) {
			{
				setStyleId("description");
			}
		};

		StyledContainer details = new StyledContainer(screen) {
			{
				setStyleId("details");
			}
		};

		StyledContainer cost = new StyledContainer(screen,
				new MigLayout(screen, "fill, wrap 2, ins 0, gap 0", "[grow][]", "[][]")) {
			{
				setStyleId("cost");
			}
		};
		cost.addElement(copper = new CoinPanel(screen));
		cost.addElement(purchase = new PushButton(screen, "Purchase") {
			{
				setStyleClass("fancy");
			}
		}, "spany 2");
		cost.addElement(apCost = new Label(screen), "growx");

		/* Main ability table */
		StyledContainer ability = new StyledContainer(screen, new MigLayout(screen, "wrap 1, fill", "[grow]",
				"[:81:,grow, shrink 0][shrink 0][:70:,grow, shrink 0][shrink 0][:115:,grow][shrink 0][:144:,grow][shrink 0][:56:, shrink 0]")) {
			{
				setStyleId("ability");
			}
		};

		ability.addElement(improvementArea, "growx");
		ability.addElement(new Separator(screen), "growx");
		ability.addElement(prereqArea, "growx");
		ability.addElement(new Separator(screen), "growx");
		ability.addElement(description, "growx");
		ability.addElement(new Separator(screen), "growx");
		ability.addElement(details, "growx");
		ability.addElement(new Separator(screen), "growx");
		ability.addElement(cost, "growx");

		/* Right */
		StyledContainer main = new StyledContainer(screen,
				new MigLayout(screen, "fill, wrap 2, ins 0, gap 0", "[grow][]", "[][shrink 0][shrink 0][shrink 0]")) {
			{
				setStyleId("main");
			}
		};
		main.addElement(ability, "span 2, growy, growx");
		main.addElement(abilityPoints = new Label("Ability Points: 0", screen));
		main.addElement(respecialise = new PushButton(screen, "Respecialise") {
			{
				setStyleClass("fancy");
			}
		}, "spany 2");
		main.addElement(pointsSpent = new Label("Points Spent: 0", screen));
		main.addElement(purchase = new PushButton(screen, "Respecialise") {
			{
				setStyleClass("purchase");
			}
		}, "span 2, growx");

		/* Content **/
		final BaseElement contentArea = abilityFrame.getContentArea();
		contentArea.setLayoutManager(new MigLayout(screen, "fill, ins 0, gap 0", "[:380:,grow][:270:]", "[grow]"));
		contentArea.addElement(abilityTabs, "growy");
		contentArea.addElement(main);

		/* Show */
		screen.addElement(abilityFrame);

		/* Queue a load of the abilities */
		app.getWorldLoaderExecutorService().execute(() -> loadAbilities());

	}

	public void setAbility(Ability ability) {
		abilityName.setText(ability.getName());
		description.setText(ability.getDescription());
		copper.setCoin(new Coin(ability.getGoldCost()));
		apCost.setText(String.format("Ability Point Cost: %d", ability.getClassCost()));
		coreKnight.setHighlight(ability.isKnight());
		coreRogue.setHighlight(ability.isRogue());
		coreMage.setHighlight(ability.isMage());
		coreDruid.setHighlight(ability.isDruid());

		/* Build prereq text */
		StringBuilder bui = new StringBuilder();
		ability.getRequiredAbilities().forEach(a -> {
			if (bui.length() > 0)
				bui.append(", ");
			Ability ab = abilities.getBackingObject().get(a);
			bui.append("Tier ");
			bui.append(ab.getTier());
			bui.append(" ");
			bui.append(ab.getName());
		});
		bui.insert(0, "Must Have: ");
		prereqText.setText(bui.toString());

	}

	protected void loadAbilities() {
		try {
			abilities = new Abilities(app.getAssetManager(), "Data/AbilityTable.txt");
			app.run(() -> {
				abilityTabs.invalidate();
				Map<String, AbilityBox> boxes = new HashMap<>();
				categoryTabs.values().forEach(t -> t.invalidate());
				List<Ability> values = new ArrayList<Ability>(abilities.getBackingObject().values());
				Collections.sort(values, new Comparator<Ability>() {

					@Override
					public int compare(Ability o1, Ability o2) {
						int i = Integer.valueOf(o1.getY()).compareTo(o2.getY());
						return i == 0 ? Integer.valueOf(o1.getX()).compareTo(o2.getX()) : i;
					}
				});
				for (Ability a : values) {
					try {
						String categoryName = a.getCategory();
						if (categoryName == null || categoryName.length() == 0)
							continue;

						AbilityCategory cat = AbilityCategory.fromCategory(categoryName);
						String k = categoryName + "-" + a.getX() + "-" + a.getY();

						if (!a.isAnyClass() || a.getX() < 0 || a.getY() < 0 || a.getX() >= TIERS || a.getY() >= ROWS)
							continue;

						AbilityBox box = boxes.get(k);
						if (box == null) {
							Element el = categoryTabs.get(cat);
							AbilityBox bbox = new AbilityBox(screen, a);
							bbox.onMouseReleased(evt -> setAbility(a));
							el.addElement(bbox, String.format("cell %d %d, growx", a.getX(), a.getY() + 3));
							boxes.put(k, bbox);
						} else {
							box.addAbility(a);
						}
					} catch (Exception e) {
						LOG.log(Level.SEVERE,
								String.format("Failed to add ability %d (%s).", a.getAbilityId(), a.getName()));
					}
				}
				categoryTabs.values().forEach(t -> t.validate());
				abilityTabs.validate();
			});
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to load abilities.", e);
			app.getStateManager().getState(HUDMessageAppState.class).message(Channel.ERROR, "Failed to load abilities.",
					e);
		}
	}

	protected Element createAbilityCategoryTabContent(AbilityCategory category) {
		StyledContainer el = new StyledContainer(screen, new MigLayout(screen, "fill, wrap " + TIERS)) {
			{
				setStyleId("ability-content-" + category.name().toLowerCase());
			}
		};
		for (int i = 0; i < TIERS; i++)
			el.addElement(new Label(String.format("TIER %d", i + 1, screen)), "growx");
		for (int i = 0; i < TIERS; i++)
			el.addElement(new Label(String.format("Level %d", startLevelForTier(i + 1)), screen), "growx");
		return el;
	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		if (abilityFrame.isVisible()) {
			abilityFrame.hide();
		}
	}

	public static class AbilityBox extends Element {

		private Meter meter;
		private List<Ability> abilities = new ArrayList<>();

		public AbilityBox(BaseScreen screen, Ability ability) {
			super(screen, new XYLayout());

			abilities.add(ability);

			meter = new Meter(Orientation.VERTICAL);
			meter.setMaxValue(1);

			setToolTipText(
					String.format("%d - %d - %s", ability.getAbilityId(), ability.getLevel(), ability.getName()));

			addElement(meter);
			addElement(new Button(screen).setButtonIcon("Icons/" + ability.getIcon1())
					.setTexture("Icons/" + ability.getIcon2()));

		}

		public void addAbility(Ability a) {
			abilities.add(a);
			if (meter.getMaxValue() < 5)
				meter.setMaxValue(meter.getMaxValue() + 1);
		}

	}

	class CoreElement extends Element {
		private boolean highlight;

		CoreElement(BaseScreen screen, String className) {
			super(screen);
			setStyleClass("core-" + className);
		}

		public boolean isHighlight() {
			return highlight;
		}

		public void setHighlight(boolean highlight) {
			this.highlight = highlight;
			dirtyLayout(false, LayoutType.styling);
			layoutChildren();
		}

		@Override
		public PseudoStyles getPseudoStyles() {
			PseudoStyles pseudoStyles = super.getPseudoStyles();
			if (highlight) {
				pseudoStyles = PseudoStyles.get(pseudoStyles).addStyle(PseudoStyle.active);
			}
			return pseudoStyles;
		}

	}
}
