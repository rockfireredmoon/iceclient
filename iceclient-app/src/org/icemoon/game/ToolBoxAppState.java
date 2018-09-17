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
import org.iceui.controls.ElementStyle;

import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.layout.mig.MigLayout;
import icetone.extras.windows.PersistentWindow;
import icetone.extras.windows.SaveType;

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
	private PersistentWindow toolBoxWindow;
	private ToolManager toolManager;
	private BaseElement contentArea;

	public ToolBoxAppState() {
		super(Config.get());
	}

	@Override
	protected HUDAppState onInitialize(final AppStateManager stateManager, IcesceneApp app) {
		return stateManager.getState(HUDAppState.class);
	}

	@Override
	protected void postInitialize() {
		toolBoxWindow = (PersistentWindow) screen.getElementByStyleId(TOOLBOX_TOOLS);
		if (toolBoxWindow != null) {
			// In case the special effects have not finished yet
			toolBoxWindow.hide();
		}
		toolBoxWindow = new PersistentWindow(screen, TOOLBOX_TOOLS, VAlign.Center, Align.Center, new Size(400, 400),
				true, SaveType.POSITION, Config.get()) {
			@Override
			protected void onCloseWindow() {
				super.onCloseWindow();
				stateManager.detach(ToolBoxAppState.this);
			}
		};
		toolBoxWindow.setWindowTitle("Tool Box");
		toolBoxWindow.setMovable(true);
		toolBoxWindow.setResizable(false);

		contentArea = toolBoxWindow.getContentArea();
		contentArea.setLayoutManager(new MigLayout(screen, "wrap 1"));
		rebuildTools();

		// Show with an effect and sound
		toolBoxWindow.setDestroyOnHide(true);
		toolBoxWindow.sizeToContent();
		screen.addElement(toolBoxWindow);
		toolBoxWindow.show();

	}

	@Override
	public void update(float tpf) {
	}

	@Override
	protected void onCleanup() {
		toolBoxWindow.hide();
	}

	private ToolDraggable createDraggable(final DragContext dragContext, final Tool tool) {
		return new ToolDraggable(dragContext, screen, tool) {
			protected boolean rebuildThisToolBox;

			{
				onMouseReleased(evt -> {
					if (rebuildThisToolBox) {
						rebuildTools();
						rebuildThisToolBox = false;
					}
				});
			}

			@Override
			protected boolean doOnClick(MouseButtonEvent evt) {
				return false;
			}

			@Override
			protected boolean doOnDragEnd(MouseButtonEvent mbe, BaseElement elmnt) {
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
			ElementStyle.medium(l, true, false);
			contentArea.addElement(l);

			BaseElement tools = new BaseElement(screen);
			tools.setLayoutManager(new MigLayout(screen, "wrap 10"));
			for (final Tool tool : categoryTools.get(cat)) {
				// Only allow trashable tools to be added (or they wont be able
				// to be removed again)
				if (tool.isTrashable()) {
					tools.addElement(createDraggable(dragContext, tool));
				}
			}

			contentArea.addElement(tools);
		}

		Label l = new Label("Drag tools from here to the toolbars", screen);
		l.setTextAlign(BitmapFont.Align.Center);
		contentArea.addElement(l, "growx");
	}

	abstract class ToolDraggable extends AbstractDraggable {

		public ToolDraggable(DragContext dragContext, BaseScreen screen, Tool tool) {
			super(dragContext, screen, tool.getIcon(), null);
			addStyleClass("option-tool");
			setToolTipText(tool.getHelp());
		}
	}
}
