package org.icemoon.test;

import org.icemoon.Config;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.controls.FancyPositionableWindow;
import org.iceui.controls.FancyWindow;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.text.Label;
import icetone.core.Element;
import icetone.core.Screen;
import icetone.core.layout.BasicLayoutManager;
import icetone.core.layout.FillLayout;
import icetone.core.utils.UIDUtil;

public class TestBasicLayout extends SimpleApplication {

    public static void main(String[] args) {
        TestBasicLayout app = new TestBasicLayout();
        app.start();
    }
    private Element buttons;
    private FancyPositionableWindow minimapWindow;
    private Element minimap;
    private Element overlay;

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10);
        flyCam.setDragToRotate(true);

        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        rootNode.attachChild(geom);

        Screen screen = new Screen(this, "Interface/Styles/Gold/style_map.xml");

       
        /// Minmap window
        minimapWindow = new FancyPositionableWindow(screen, Config.MINIMAP,
                10, VPosition.TOP, HPosition.RIGHT,
                new Vector2f(179, 195), FancyWindow.Size.MINIMAP, false);
        minimapWindow.setWindowTitle("Sime text");
        minimapWindow.setIsMovable(true);
        minimapWindow.setIsResizable(false);
        final Element contentArea = minimapWindow.getContentArea();

        contentArea.setLayoutManager(new FillLayout());


        // Map itself
        minimap = new Element(screen, UIDUtil.getUID(), Vector4f.ZERO, screen.getStyle("Minimap").getString("blankImg"));
        contentArea.addChild(minimap);

        // Overlay
        overlay = new Element(screen, UIDUtil.getUID(), Vector4f.ZERO, screen.getStyle("Minimap").getString("overlayImg"));
        contentArea.addChild(overlay);
        //

        // Buttons
        buttons = new Element(screen);
        buttons.setLayoutManager(new BasicLayoutManager());
        ButtonAdapter openWorldMap = new ButtonAdapter(screen, screen.getStyle("Minimap").getVector2f("worldMapButtonPosition"),
                screen.getStyle("Minimap").getVector2f("worldMapButtonSize"), screen.getStyle("Minimap").getVector4f("worldMapButtonResizeBorders"), screen.getStyle("Minimap").getString("worldMapImg")) {
            @Override
            public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
            }
        };
        openWorldMap.setButtonHoverInfo(screen.getStyle("Minimap").getString("worldMapHoverImg"), null);
        openWorldMap.setButtonPressedInfo(screen.getStyle("Minimap").getString("worldMapPressedImg"), null);
        buttons.addChild(openWorldMap);
        
        Label north = new Label(screen, UIDUtil.getUID(), screen.getStyle("Minimap").getVector2f("northPosition"), 
                screen.getStyle("Minimap").getVector2f("directionButtonSize"),
                screen.getStyle("Minimap").getVector4f("directionButtonResizeBorders"),
                screen.getStyle("Minimap").getString("directionButtonImg"));
        north.setTextAlign(BitmapFont.Align.Center);
        north.setTextVAlign(BitmapFont.VAlign.Top);
        north.setText("N");
        north.setTextPadding(2);
        buttons.addChild(north);
        
         north = new Label(screen, UIDUtil.getUID(), screen.getStyle("Minimap").getVector2f("northPosition"), 
                screen.getStyle("Minimap").getVector2f("directionButtonSize"),
                screen.getStyle("Minimap").getVector4f("directionButtonResizeBorders"),
                screen.getStyle("Minimap").getString("directionButtonImg"));
        north.setTextAlign(BitmapFont.Align.Center);
        north.setTextVAlign(BitmapFont.VAlign.Top);
        north.setText("N");
        north.setTextPadding(2);
        buttons.addChild(north);
        
        Label east = new Label(screen, UIDUtil.getUID(), screen.getStyle("Minimap").getVector2f("eastPosition"), 
                screen.getStyle("Minimap").getVector2f("directionButtonSize"),
                screen.getStyle("Minimap").getVector4f("directionButtonResizeBorders"),
                screen.getStyle("Minimap").getString("directionButtonImg"));
        east.setText("E");
        east.setTextAlign(BitmapFont.Align.Center);
        east.setTextVAlign(BitmapFont.VAlign.Top);
        east.setTextPadding(2);
        buttons.addChild(east);
        
        
        
        
        Label west = new Label(screen, UIDUtil.getUID(), screen.getStyle("Minimap").getVector2f("westPosition"), 
                screen.getStyle("Minimap").getVector2f("directionButtonSize"),
                screen.getStyle("Minimap").getVector4f("directionButtonResizeBorders"),
                screen.getStyle("Minimap").getString("directionButtonImg"));
        west.setText("W");
        west.setTextAlign(BitmapFont.Align.Center);
        west.setTextVAlign(BitmapFont.VAlign.Top);
        west.setTextPadding(2);
        buttons.addChild(west);
        
        Label south = new Label(screen, UIDUtil.getUID(), screen.getStyle("Minimap").getVector2f("southPosition"), 
                screen.getStyle("Minimap").getVector2f("directionButtonSize"),
                screen.getStyle("Minimap").getVector4f("directionButtonResizeBorders"),
                screen.getStyle("Minimap").getString("directionButtonImg"));
        south.setText("S");
        south.setTextAlign(BitmapFont.Align.Center);
        south.setTextVAlign(BitmapFont.VAlign.Top);
        south.setTextPadding(2);
        buttons.addChild(south);
        
        contentArea.addChild(buttons);
        
        screen.addElement(minimapWindow);

        guiNode.addControl(screen);

    }

    @Override
    public void simpleUpdate(float tpf) {
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }
}
