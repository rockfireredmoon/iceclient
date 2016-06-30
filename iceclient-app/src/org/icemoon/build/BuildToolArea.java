package org.icemoon.build;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.icelib.Icelib;
import org.icemoon.game.GameHudType;
import org.icescene.IcesceneApp;
import org.icescene.SceneConstants;
import org.icescene.tools.AbstractToolArea;
import org.icescene.tools.ToolManager;
import org.iceui.controls.AutocompleteTextField;
import org.iceui.controls.AutocompleteTextField.AutocompleteItem;
import org.iceui.controls.AutocompleteTextField.AutocompleteSource;
import org.iceui.controls.FancyButton;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.Vector4f;

import icemoon.iceloader.ServerAssetManager;
import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;
import icetone.core.utils.UIDUtil;

/**
 * Component for the area in game mode that contains all the main tool actions,
 * XP bar, Gold and credits hover elements and quick bar toggles.
 */
public class BuildToolArea extends AbstractToolArea implements AutocompleteSource<String> {

	private AutocompleteTextField<String> propSearch;

	public BuildToolArea(ToolManager toolMgr, final ElementManager screen) {
		super(GameHudType.BUILD, toolMgr, screen, "BuildToolBar", "Buildbar", 7);
		//
		updateBarText();
		Container el = new Container(screen, UIDUtil.getUID(), mainToolBarStyle.getVector2f("propSearchPosition"),
				mainToolBarStyle.getVector2f("propSearchSize"), Vector4f.ZERO, null);
		el.setLayoutManager(new MigLayout(screen, "", "[][fill, grow][]", "[]"));
		propSearch = new AutocompleteTextField<String>(screen, this);
		propSearch.setToolTipText("Type in a partial or full prop name and press Ctrl+Space to list all props matching that name");
		el.addChild(new Label("Prop: ", screen));
		el.addChild(propSearch, "ay 50%");
		FancyButton add = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
				BuildAppState bas = screen.getApplication().getStateManager().getState(BuildAppState.class);
				bas.add(propSearch.getText());
			}
		};
		add.setText("Add");
		el.addChild(add);
		container.addChild(el);
	}

	@Override
	protected void build() {
	}

	@Override
	protected void updateBarText() {
		// TODO Use for prop count
	}

	@Override
	protected void onDestroy() {
	}

	@Override
	public List<AutocompleteItem<String>> getItems(String text) {
		String pattern = "Prop/.*" + text + ".*\\.csm.xml";
		List<AutocompleteItem<String>> items = new ArrayList<>();
		for (String s : ((ServerAssetManager) ((IcesceneApp) screen.getApplication()).getAssetManager())
				.getAssetNamesMatching(pattern, Pattern.CASE_INSENSITIVE)) {
			String n = s.substring(0, s.length() - 8).substring(6).replace("/", "#");
			String e = n;
			int idx = e.indexOf("#");
			if (idx != -1) {
				e = e.substring(idx + 1);
			}
			if (e.startsWith("Prop-")) {
				e = e.substring(5);
			}
			if (e.startsWith("Clutter-")) {
				e = e.substring(8);
			}
			e = Icelib.camelToEnglish(e);
			items.add(new AutocompleteItem<String>(e, n));
			if (items.size() >= SceneConstants.MAX_SEARCH_RESULTS) {
				break;
			}
		}
		return items;
	}

}
