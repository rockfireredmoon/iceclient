package org.icemoon.game;

import java.util.List;
import java.util.Map;

import org.icemoon.Config;
import org.icemoon.Iceclient;
import org.icescene.IcemoonAppState;
import org.icescene.IcesceneApp;
import org.icescene.tools.AbstractDraggable;
import org.icescene.tools.DragContext;
import org.icescene.tools.Tool;
import org.icescene.tools.ToolCategory;
import org.icescene.tools.ToolDroppable;
import org.icescene.tools.ToolManager;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector2f;

import icetone.controls.text.Label;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

public class ToolBoxAppState extends IcemoonAppState<HUDAppState> {

	public static void toggle(AppStateManager stateManager) {
		ToolBoxAppState toolBox = stateManager.getState(ToolBoxAppState.class);
		if (toolBox == null) {
			stateManager.attach(new ToolBoxAppState());
		} else {
			stateManager.detach(toolBox);
		}
	}

	private static final String TOOLBOX_TOOLS = "ToolBoxTools";
	private FancyPersistentWindow toolBoxWindow;
	private ToolManager toolManager;
	private Element contentArea;

	public ToolBoxAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {
		toolBoxWindow = (FancyPersistentWindow) screen.getElementById(TOOLBOX_TOOLS);
		if (toolBoxWindow != null) {
			// In case the special effects have not finished yet
			toolBoxWindow.hide();
		}
		toolBoxWindow = new FancyPersistentWindow(screen, TOOLBOX_TOOLS, screen.getStyle("Common").getInt("defaultWindowOffset"),
				VPosition.MIDDLE, HPosition.CENTER, new Vector2f(400, 400), FancyWindow.Size.SMALL, true, SaveType.POSITION,
				Config.get()) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(ToolBoxAppState.this);
			}
		};
		toolBoxWindow.setWindowTitle("Tool Box");
		toolBoxWindow.setIsMovable(true);
		toolBoxWindow.setIsResizable(false);

		contentArea = toolBoxWindow.getContentArea();
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1"));
		rebuildTools();

		// Show with an effect and sound
		toolBoxWindow.setDestroyOnHide(true);
		toolBoxWindow.pack(false);
		screen.addElement(toolBoxWindow);
		toolBoxWindow.hide();
		toolBoxWindow.showWindow();

	}

	@Override
	public void update(float tpf) {
	}

	@Override
	protected void onCleanup() {
		toolBoxWindow.hideWindow();
	}

	private ToolDraggable createDraggable(final DragContext dragContext, final Tool tool) {
		return new ToolDraggable(dragContext, screen, tool) {
			protected boolean rebuildThisToolBox;

			@Override
			protected boolean doOnClick(MouseButtonEvent evt) {
				return false;
			}

			@Override
			protected boolean doOnDragEnd(MouseButtonEvent mbe, Element elmnt) {
				if (elmnt instanceof ToolDroppable) {
					ToolDroppable toolDroppable = (ToolDroppable) elmnt;
					final Tool existingTool = toolDroppable.getTool();
					if (existingTool == null) {
						toolDroppable.getToolBox().drop(tool, toolDroppable.getSlot());
						ActionBarsAppState actions = stateManager.getState(ActionBarsAppState.class);
						actions.rebuildTools(toolDroppable.getToolBox());
						rebuildThisToolBox = true;
						return true;
					}
				}
				return false;
			}

			@Override
			public void onMouseLeftReleased(MouseButtonEvent evt) {
				super.onMouseLeftReleased(evt);
				if (rebuildThisToolBox) {
					rebuildTools();
					rebuildThisToolBox = false;

				}
			}
		};
	}

	private void rebuildTools() {
		final DragContext dragContext = parent.getParent().getDragContext();
		contentArea.removeAllChildren();
		ActionBarsAppState actions = stateManager.getState(ActionBarsAppState.class);
		toolManager = ((Iceclient) app).getToolManager();
		Map<ToolCategory, List<Tool>> categoryTools = toolManager.getTools(actions.getHudType());
		for (ToolCategory cat : categoryTools.keySet()) {
			Label l = new Label(cat.getName(), screen);
			l.setToolTipText(cat.getHelp());
			ElementStyle.medium(screen, l, true, false);
			contentArea.addChild(l);

			Element tools = new Element(screen);
			tools.setLayoutManager(new MigLayout(screen, "wrap 10"));
			for (final Tool tool : categoryTools.get(cat)) {
				// Only allow trashable tools to be added (or they wont be able
				// to be removed again)
				if (tool.isTrashable()) {
					tools.addChild(createDraggable(dragContext, tool));
				}
			}

			contentArea.addChild(tools);
		}

		Label l = new Label("Drag tools from here to the toolbars", screen);
		l.setTextAlign(BitmapFont.Align.Center);
		contentArea.addChild(l, "growx");
	}

	abstract class ToolDraggable extends AbstractDraggable {

		public ToolDraggable(DragContext dragContext, ElementManager screen, Tool tool) {
			super(dragContext, screen, UIDUtil.getUID(), screen.getStyle("OptionButton").getVector2f("defaultSize"),
					screen.getStyle("OptionButton").getVector4f("resizeBorders"), tool.getIcon(), null);
			setToolTipText(tool.getHelp());
		}
	}
}
