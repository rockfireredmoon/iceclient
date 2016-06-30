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
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.controls.FancyButton;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;
import org.iceui.controls.ZMenu;
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.NaiveUserAgent;

import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;

import icetone.controls.extras.SplitPanel;
import icetone.controls.lists.Table;
import icetone.controls.lists.Table.ColumnResizeMode;
import icetone.controls.lists.Table.TableColumn;
import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.Element.Orientation;
import icetone.core.layout.FillLayout;
import icetone.core.layout.LUtil;
import icetone.core.layout.mig.MigLayout;
import icetone.listeners.MouseButtonListener;
import icetone.xhtml.TGGXHTMLRenderer;

/**
 * IGF, or In-Game Forum
 */
public class ForumAppState extends IcemoonAppState<HUDAppState> {

	public final class CategoryTable extends Table implements MouseButtonListener {
		private boolean doubleClick;
		private TableRow lastSel;

		public CategoryTable(ElementManager screen) {
			super(screen);
		}

		@Override
		public void onMouseLeftReleased(MouseButtonEvent evt) {
			TableRow selectedRow = getSelectedRow();
			doubleClick = LUtil.isDoubleClick(evt) && Objects.equals(selectedRow, lastSel);
			lastSel = selectedRow;
		}

		@Override
		public void onMouseRightReleased(MouseButtonEvent evt) {
			ZMenu m = new ZMenu(screen) {
				@Override
				protected void onItemSelected(ZMenuItem item) {
				}
			};
			if (category == null && isAnythingSelected() && (ForumItem) getSelectedRow().getValue() != null) {
				m.addMenuItem("New Topic", ForumActions.NEW_THREAD);
			} else if (category != null && isAnythingSelected() && (ForumItem) getSelectedRow().getValue() != null) {
				m.addMenuItem("Reply To Topic", ForumActions.REPLY_TO_THREAD);
			}
			screen.addElement(m);
			m.showMenu(null, evt.getX() + 10, evt.getY() + 10);
		}

		@Override
		public void onChange() {
			if (doubleClick) {
				TableRow selectedRow = getSelectedRow();
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
			}
		}

		@Override
		public void onMouseLeftPressed(MouseButtonEvent evt) {
		}

		@Override
		public void onMouseRightPressed(MouseButtonEvent evt) {
		}
	}

	public enum ForumActions {
		NEW_THREAD, REPLY_TO_THREAD, UP
	}

	private final static Logger LOG = Logger.getLogger(ForumAppState.class.getName());
	private NetworkAppState network;

	private FancyPersistentWindow forum;
	private Table categories;
	private ForumItem category;
	private ForumTopic topic;
	private TGGXHTMLRenderer viewer;
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
		forum = new FancyPersistentWindow(screen, Config.FORUM, screen.getStyle("Common").getInt("defaultWindowOffset"),
				VPosition.MIDDLE, HPosition.CENTER, new Vector2f(500, 400), FancyWindow.Size.SMALL, true,
				SaveType.POSITION_AND_SIZE, Config.get()) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(ForumAppState.this);
			}
		};
		forum.setWindowTitle("In-Game Forum");
		forum.setIsMovable(true);
		forum.setIsResizable(true);
		forum.setDestroyOnHide(true);

		// Category actions
		Container categoryActions = new Container(screen);
		categoryActions.setLayoutManager(new MigLayout(screen, "ins 0", "push[][]push", "[]"));
		FancyButton newCategoryOrThread = new FancyButton(screen) {

			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}

		};
		newCategoryOrThread.setText("New");
		categoryActions.addChild(newCategoryOrThread);
		FancyButton backToIndex = new FancyButton(screen) {

			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}

		};
		backToIndex.setText("Reply");
		categoryActions.addChild(backToIndex);

		// Categories
		categories = new CategoryTable(screen);
		categories.setTextPadding(new Vector4f(0, 2, 0, 0));
		categories.setColumnResizeMode(ColumnResizeMode.AUTO_FIRST);
		categories.addColumn("Category").setWidth(140);
		TableColumn lastCol = categories.addColumn("Last");
		lastCol.setMinDimensions(new Vector2f(20, 10));
		lastCol.setWidth(50);

		// Categories area
		Container categoriesArea = new Container(screen);
		categoriesArea.setMinDimensions(Vector2f.ZERO);
		categoriesArea.setLayoutManager(new MigLayout(screen, "ins 0, wrap 1", "[fill, grow]", "[][fill, grow]"));
		categoriesArea.addChild(categoryActions);
		categoriesArea.addChild(categories);

		// Viewer nav
		Container viewerNav = new Container(screen);
		viewerNav.setLayoutManager(new MigLayout(screen, "ins 0, fill", "push[][][][][][]push", "[]"));

		// First
		FancyButton first = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		Vector2f arrowSize = screen.getStyle("Common").getVector2f("arrowSize");
		first.setToolTipText("First Page");
		first.setButtonIcon(arrowSize.x, arrowSize.y, screen.getStyle("Common").getString("arrowLeftStart"));
		viewerNav.addChild(first);

		// Previous
		FancyButton previous = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		previous.setToolTipText("Previous Page");
		previous.setButtonIcon(arrowSize.x, arrowSize.y, screen.getStyle("Common").getString("arrowLeft"));
		viewerNav.addChild(previous);

		// Next
		FancyButton next = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		next.setToolTipText("Next Page");
		next.setButtonIcon(arrowSize.x, arrowSize.y, screen.getStyle("Common").getString("arrowRight"));
		viewerNav.addChild(next);

		// Last
		FancyButton last = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		last.setToolTipText("Last Page");
		last.setButtonIcon(arrowSize.x, arrowSize.y, screen.getStyle("Common").getString("arrowRightStart"));
		viewerNav.addChild(last);

		// Home
		FancyButton home = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				viewer.scrollToTop();
			}
		};
		home.setToolTipText("Top Of Page");
		home.setButtonIcon(arrowSize.x, arrowSize.y, screen.getStyle("Common").getString("arrowUp"));
		viewerNav.addChild(home);
		// Home
		FancyButton end = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				viewer.scrollToBottom();
			}
		};
		end.setToolTipText("Bottom Of Page");
		end.setButtonIcon(arrowSize.x, arrowSize.y, screen.getStyle("Common").getString("arrowDown"));
		viewerNav.addChild(end);

		// Viewer
		viewer = new TGGXHTMLRenderer(screen, new NaiveUserAgent());
		// viewer.setContentIndents(Vector4f.ZERO);

		// Viewer Info
		Container viewerInfo = new Container(screen);
		viewerInfo.setLayoutManager(new MigLayout(screen, "ins 0", "push[]push", "[]"));
		viewerInfoSummary = new Label(screen);
		viewerInfoSummary.setText("");
		viewerInfo.addChild(viewerInfoSummary);

		// Viewer area
		Container viewerArea = new Container(screen);
		viewerArea.setMinDimensions(Vector2f.ZERO);
		viewerArea
				.setLayoutManager(new MigLayout(screen, "ins 0, wrap 1, fill", "[grow, fill]", "[shrink 0][grow, fill][shrink 0]"));
		viewerArea.addChild(viewerInfo);
		viewerArea.addChild(viewer);
		viewerArea.addChild(viewerNav);

		// Split
		SplitPanel split = new SplitPanel(screen, Vector2f.ZERO, LUtil.LAYOUT_SIZE, Vector4f.ZERO, null, Orientation.HORIZONTAL);
		split.setDefaultDividerLocationRatio(0.25f);
		split.setLeftOrTop(categoriesArea);
		split.setRightOrBottom(viewerArea);

		// Window content
		final Element contentArea = forum.getContentArea();
		contentArea.setLayoutManager(new FillLayout());
		contentArea.addChild(split);

		// Show with an effect and sound
		screen.addElement(forum);
		forum.hide();
		forum.showWithEffect();

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
		if (forum.getIsVisible()) {
			forum.hideWithEffect();
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
				bui.append(String.format("%s [Post #%d] (%s) %s ago", post.getAuthor(), post.getNumber(),
						DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(post.getDate())),
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

					viewerInfoSummary.setText(
							String.format("%s (ID#%d) with %d post. Viewing %d to %d", topic.getTitle(), topic.getId(), 1, 1, 1));
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
							Table.TableRow row = new Table.TableRow(screen, categories, cat);
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

						Table.TableRow row = new Table.TableRow(screen, categories, ForumActions.UP);
						row.addCell("..", null);
						row.addCell("", null);
						categories.addRow(row);

						for (ForumTopic topic : forumTopics) {
							row = new Table.TableRow(screen, categories, topic);
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
