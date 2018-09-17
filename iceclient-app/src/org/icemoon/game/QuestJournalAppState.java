package org.icemoon.game;

import org.icemoon.Config;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;

import icetone.controls.buttons.PushButton;
import icetone.controls.extras.Separator;
import icetone.controls.table.Table;
import icetone.controls.text.Label;
import icetone.controls.text.XHTMLLabel;
import icetone.core.Orientation;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

/**
 * Displays the players quest journal.
 */
public class QuestJournalAppState extends IcemoonAppState<HUDAppState> {

	private PersistentWindow questWindow;
	private Table questsTable;
	private Label questsText;
	private PushButton abandon;
	private PushButton share;
	private XHTMLLabel questBody;
	private StyledContainer questObjectives;
	private StyledContainer questReward;
	private StyledContainer questRequirements;
	private StyledContainer questCurrency;

	public QuestJournalAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {

		/// Minmap window
		questWindow = new PersistentWindow(screen, Config.ABILITIES, VAlign.Top, Align.Left, new Size(500, 400), true,
				SaveType.POSITION_AND_SIZE, Config.get()) {

			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(QuestJournalAppState.this);
			}
		};
		questWindow.setWindowTitle("Quests");
		questWindow.setMovable(true);
		questWindow.setResizable(true);
		questWindow.setDestroyOnHide(true);

		// Quests Tables
		questsTable = new Table(screen);
		questsTable.addColumn("Quest Title").addStyleClass("quest-title");
		questsTable.addColumn("*").addStyleClass("quest-indicator");
		questsTable.addColumn("P").addStyleClass("party-size");

		// Quests Actions / Text
		questsText = new Label("Quests: 0/0", screen);
		abandon = new PushButton(screen, "Abandon") {
			{
				setStyleClass("fancy cancel");
			}
		};
		share = new PushButton(screen, "Share") {
			{
				setStyleClass("fancy");
			}
		};

		// Quest Info
		questBody = new XHTMLLabel(screen) {
			{
				setStyleClass("quest-body");
			}
		};
		questObjectives = new StyledContainer(screen) {
			{
				setStyleClass("objectives");
			}
		};
		questReward = new StyledContainer(screen) {
			{
				setStyleClass("reward");
			}
		};
		questRequirements = new StyledContainer(screen) {
			{
				setStyleClass("requirements");
			}
		};
		questCurrency = new StyledContainer(screen) {
			{
				setStyleClass("currency");
			}
		};

		// Left
		StyledContainer left = new StyledContainer(screen,
				new MigLayout(screen, "wrap 3, fill", "[grow][][]", "[:100:,grow][]")) {
			{
				setStyleClass("quests-area");
			}
		};
		left.addElement(questsTable, "span 3, growx, growy");
		left.addElement(questsText);
		left.addElement(abandon);
		left.addElement(share);

		// Right
		StyledContainer right = new StyledContainer(screen,
				new MigLayout(screen, "wrap 2, fill", "[][grow]", "[grow][grow][][]")) {
			{
				setStyleClass("quest-area");
			}
		};
		right.addElement(questBody, "span 2, growx");
		right.addElement(questObjectives, "span 2, growx");
		right.addElement(questReward, "spany 2");
		right.addElement(questRequirements, "growx");
		right.addElement(questCurrency, "growx");

		// Content
		final Element contentArea = questWindow.getContentArea();
		contentArea.setLayoutManager(new MigLayout(screen, "fill", "[:400:][][grow]", "[grow]"));
		contentArea.addElement(left, "growy");
		contentArea.addElement(new Separator(Orientation.VERTICAL));
		contentArea.addElement(right, "growy, growx");

		// Show with an effect and sound
		screen.addElement(questWindow);

	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		questWindow.hide();
	}
}
