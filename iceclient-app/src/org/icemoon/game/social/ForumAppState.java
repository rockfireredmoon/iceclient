package org.icemoon.game.social;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icemoon.Config;
import org.icemoon.game.HUDAppState;
import org.icemoon.network.NetworkAppState;
import org.icenet.NetworkException;
import org.icenet.client.ForumItem;
import org.icenet.client.ForumPost;
import org.icenet.client.ForumTopic;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.NaiveUserAgent;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.math.Vector4f;

import icetone.controls.buttons.PushButton;
import icetone.controls.containers.SplitPanel;
import icetone.controls.menuing.Menu;
import icetone.controls.table.Table;
import icetone.controls.table.Table.ColumnResizeMode;
import icetone.controls.table.TableColumn;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.Size;
import icetone.core.StyledContainer;
import icetone.core.event.MouseUIButtonEvent;
import icetone.core.layout.FillLayout;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;
import icetone.xhtml.XHTMLDisplay;

/**
 * IGF, or In-Game Forum
 */
public class ForumAppState extends IcemoonAppState<HUDAppState> {

	public final class CategoryTable extends Table {
		private boolean doubleClick;
		private TableRow lastSel;

		public CategoryTable(BaseScreen screen) {
			super(screen);
			onMousePressed(evt -> {
				TableRow selectedRow = getSelectedRow();
				doubleClick = evt.getClicks() == 2 && Objects.equals(selectedRow, lastSel);
				lastSel = selectedRow;
			}, MouseUIButtonEvent.LEFT);
			onMouseReleased(evt -> {

				TableRow selectedRow = getSelectedRow();
				doubleClick = evt.getClicks() == 2 && Objects.equals(selectedRow, lastSel);
				lastSel = selectedRow;

				if (category == null) {
					category = (ForumItem) selectedRow.getValue();
					loadCategoriesInBackground();
				} else {
					if (selectedRow.getValue().equals(ForumActions.UP)) {
						topic = null;
						category = null;
						loadCategoriesInBackground();
					} else {
						topic = (ForumTopic) selectedRow.getValue();
						loadTopicInBackground();
					}
				}
			}, MouseUIButtonEvent.LEFT);
			onMouseReleased(evt -> {
				Menu<ForumActions> m = new Menu<>(screen);
				if (category == null && isAnythingSelected() && (ForumItem) getSelectedRow().getValue() != null) {
					m.addMenuItem("New Topic", ForumActions.NEW_THREAD);
				} else if (category != null && isAnythingSelected()
						&& (ForumItem) getSelectedRow().getValue() != null) {
					m.addMenuItem("Reply To Topic", ForumActions.REPLY_TO_THREAD);
				}
				screen.addElement(m);
				m.showMenu(null, evt.getX() + 10, evt.getY() + 10);
			}, MouseUIButtonEvent.RIGHT);
		}

		public boolean isDoubleClick() {
			return doubleClick;
		}

	}

	public enum ForumActions {
		NEW_THREAD, REPLY_TO_THREAD, UP
	}

	private final static Logger LOG = Logger.getLogger(ForumAppState.class.getName());
	private NetworkAppState network;

	private PersistentWindow forum;
	private Table categories;
	private ForumItem category;
	private ForumTopic topic;
	private XHTMLDisplay viewer;
	private Label viewerInfoSummary;

	public ForumAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {

		network = stateManager.getState(NetworkAppState.class);

		// Forum window
		forum = new PersistentWindow(screen, Config.FORUM, VAlign.Center, Align.Center, new Size(500, 400), true,
				SaveType.POSITION_AND_SIZE, Config.get()) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(ForumAppState.this);
			}
		};
		forum.setWindowTitle("In-Game Forum");
		forum.setMovable(true);
		forum.setResizable(true);
		forum.setDestroyOnHide(true);

		// Category actions
		StyledContainer categoryActions = new StyledContainer(screen);
		categoryActions.setLayoutManager(new MigLayout(screen, "ins 0", "push[][]push", "[]"));
		PushButton newCategoryOrThread = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		newCategoryOrThread.setText("New");
		categoryActions.addElement(newCategoryOrThread);
		PushButton backToIndex = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		backToIndex.setText("Reply");
		categoryActions.addElement(backToIndex);

		// Categories
		categories = new CategoryTable(screen);
		categories.setTextPadding(new Vector4f(0, 2, 0, 0));
		categories.setColumnResizeMode(ColumnResizeMode.AUTO_FIRST);
		categories.addColumn("Category").setWidth(140);
		TableColumn lastCol = categories.addColumn("Last");
		lastCol.setMinDimensions(new Size(20, 10));
		lastCol.setWidth(50);

		// Categories area
		StyledContainer categoriesArea = new StyledContainer(screen);
		categoriesArea.setMinDimensions(Size.ZERO);
		categoriesArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[fill, grow]", "[][fill, grow]"));
		categoriesArea.addElement(categoryActions);
		categoriesArea.addElement(categories);

		// Viewer nav
		StyledContainer viewerNav = new StyledContainer(screen);
		viewerNav.setLayoutManager(new MigLayout(screen, "ins 0, fill", "push[][][][][][]push", "[]"));

		// First
		PushButton first = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		first.setToolTipText("First Page");
		first.getButtonIcon().setStyleClass("icon icon-start");
		viewerNav.addElement(first);

		// Previous
		PushButton previous = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		previous.setToolTipText("Previous Page");
		first.getButtonIcon().setStyleClass("icon icon-back");
		viewerNav.addElement(previous);

		// Next
		PushButton next = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		next.setToolTipText("Next Page");
		first.getButtonIcon().setStyleClass("icon icon-forward");
		viewerNav.addElement(next);

		// Last
		PushButton last = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		last.setToolTipText("Last Page");
		first.getButtonIcon().setStyleClass("icon icon-end");
		viewerNav.addElement(last);

		// Home
		PushButton home = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		home.onMouseReleased(evt -> viewer.scrollToTop());
		home.setToolTipText("Top Of Page");
		first.getButtonIcon().setStyleClass("icon icon-up");
		viewerNav.addElement(home);
		// Home
		PushButton end = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		end.onMouseReleased(evt -> viewer.scrollToBottom());
		end.setToolTipText("Bottom Of Page");
		first.getButtonIcon().setStyleClass("icon icon-down");
		viewerNav.addElement(end);

		// Viewer
		viewer = new XHTMLDisplay(screen, new NaiveUserAgent());
		// viewer.setContentIndents(Vector4f.ZERO);

		// Viewer Info
		StyledContainer viewerInfo = new StyledContainer(screen);
		viewerInfo.setLayoutManager(new MigLayout(screen, "ins 0", "push[]push", "[]"));
		viewerInfoSummary = new Label(screen);
		viewerInfoSummary.setText("");
		viewerInfo.addElement(viewerInfoSummary);

		// Viewer area
		StyledContainer viewerArea = new StyledContainer(screen);
		viewerArea.setMinDimensions(Size.ZERO);
		viewerArea.setLayoutManager(
				new MigLayout(screen, "ins 0, wrap 1, fill", "[grow, fill]", "[shrink 0][grow, fill][shrink 0]"));
		viewerArea.addElement(viewerInfo);
		viewerArea.addElement(viewer);
		viewerArea.addElement(viewerNav);

		// Split
		SplitPanel split = new SplitPanel(screen, Orientation.HORIZONTAL);
		split.setDefaultDividerLocationRatio(0.25f);
		split.setLeftOrTop(categoriesArea);
		split.setRightOrBottom(viewerArea);

		// Window content
		final BaseElement contentArea = forum.getContentArea();
		contentArea.setLayoutManager(new FillLayout());
		contentArea.addElement(split);

		// Show with an effect and sound
		screen.showElement(forum);

		// Load initial categories / posts
		loadCategoriesInBackground();

	}

	@Override
	public void update(float tpf) {
	}

	public void message(String text) {
	}

	@Override
	protected void onCleanup() {
		if (forum.isVisible()) {
			forum.hide();
		}
	}

	private void loadTopicInBackground() {
		new Thread("LoadTopic") {
			public void run() {
				reloadTopic();
			}
		}.start();
	}

	private void loadCategoriesInBackground() {
		new Thread("LoadCategories") {
			public void run() {
				reloadCategories();
			}
		}.start();
	}

	private void reloadTopic() {
		try {
			final StringBuilder bui = new StringBuilder();
			bui.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			bui.append("<!DOCTYPE html>\n");
			bui.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
			bui.append("<head>\n");
			bui.append(
					"<link rel=\"stylesheet\" type=\"text/css\" href=\"/META-INF/igf.css\" title=\"Style\" media=\"screen\"/>\n");
			bui.append("</head>\n");

			bui.append("<body>\n");

			int postInPage = 0;
			for (ForumPost post : network.getClient().getForumPosts(category, 0)) {
				if (postInPage > 0) {
					bui.append("<hr/>\n");
				}
				bui.append("<div class=\"authorLine\">");
				bui.append(
						String.format("%s [Post #%d] (%s) %s ago", post.getAuthor(), post.getNumber(),
								DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
										.format(new Date(post.getDate())),
								formatTime(System.currentTimeMillis() - post.getDate())));
				bui.append("</div>\n");
				if (post.getEdits() > 0) {
					bui.append("<div class=\"editLine\">");
					bui.append(String.format("Edited: %d times, Last update: %s ago", post.getEdits(),
							formatTime(System.currentTimeMillis() - post.getLastEdit())));
					bui.append("</div>\n");
				}
				bui.append("<p>\n");
				bui.append(convertPFMarkupToXHTML(post.getText()));
				bui.append("</p>\n");
				postInPage++;
			}

			bui.append("</body>\n");
			bui.append("</html>\n");

			app.enqueue(new Callable<Void>() {
				public Void call() throws Exception {

					viewerInfoSummary.setText(String.format("%s (ID#%d) with %d post. Viewing %d to %d",
							topic.getTitle(), topic.getId(), 1, 1, 1));
					viewer.setDocumentFromString(bui.toString(), null, new XhtmlNamespaceHandler());
					return null;
				}
			});

		} catch (NetworkException ne) {
			LOG.log(Level.SEVERE, "Failed to get ignored.", ne);
		}
	}

	private String convertPFMarkupToXHTML(String text) {
		text = text.replace("[b]", "<strong>");
		text = text.replace("[/b]", "</strong>");
		text = text.replace("[i]", "<em>");
		text = text.replace("[/i]", "</em>");
		text = text.replace("\n", "<br/>");
		return text;
	}

	private String formatTime(long t) {
		long hourMs = 1000l * 60l * 60l;
		long dayMs = hourMs * 24l;
		int days = (int) (t / dayMs);
		int hours = (int) Math.round(((t - ((long) days * dayMs)) / hourMs));
		String ts = "";
		if (days > 0) {
			ts += days + "d";
		}
		if (days < 7) {
			if (ts.length() > 0) {
				ts += ",";
			}
			ts += hours + "h";
		}
		return ts;
	}

	private void reloadCategories() {
		try {
			if (category == null) {
				final List<ForumItem> forumCategories = network.getClient().getForumCategories();

				app.enqueue(new Callable<Void>() {
					public Void call() throws Exception {
						categories.removeAllRows();
						for (ForumItem cat : forumCategories) {
							TableRow row = new TableRow(screen, categories, cat);
							row.addCell(cat.getTitle(), cat.getTitle());
							Date date = new Date(cat.getDate());
							row.addCell(formatTime(cat.getDate()), date);
							categories.addRow(row);
						}
						return null;
					}
				});
			} else {
				final List<ForumTopic> forumTopics = network.getClient().getForumThreads(category);
				app.enqueue(new Callable<Void>() {
					public Void call() throws Exception {
						categories.removeAllRows();

						TableRow row = new TableRow(screen, categories, ForumActions.UP);
						row.addCell("..", null);
						row.addCell("", null);
						categories.addRow(row);

						for (ForumTopic topic : forumTopics) {
							row = new TableRow(screen, categories, topic);
							row.addCell(topic.getTitle(), topic.getTitle());
							Date date = new Date(topic.getDate());
							row.addCell(formatTime(System.currentTimeMillis() - topic.getDate()), date);
							categories.addRow(row);
						}
						return null;
					}
				});
			}
		} catch (NetworkException ne) {
			LOG.log(Level.SEVERE, "Failed to get ignored.", ne);
		}
	}
}
