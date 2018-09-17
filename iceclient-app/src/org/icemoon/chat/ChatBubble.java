package org.icemoon.chat;

import org.icelib.ChannelType;
import org.icelib.Icelib;
import org.icemoon.Config;
import org.icescene.entities.AbstractSpawnEntity;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.XYLayout;
import icetone.extras.util.ExtrasUtil;

public class ChatBubble extends Element {

	private AbstractSpawnEntity entity;
	private Camera camera;

	public ChatBubble(BaseScreen screen, ChannelType channel, Camera camera, AbstractSpawnEntity entity,
			String text) {
		super(screen);
		setLayoutManager(new XYLayout());

		this.camera = camera;
		this.entity = entity;

		setStyleClass("speech-bubble");
		setFontColor(ExtrasUtil.fromColorString(Config.get().node(Config.CHAT_CHANNELS).node(channel.name())
				.get("color", Icelib.toHexString(channel.getColor()))));
		setText(text);

		Element e = new Element(screen) {
			{
				setStyleClass("speech-arrow");
			}
		};
		addElement(e);
		addControl(new AbstractControl() {

			@Override
			protected void controlUpdate(float tpf) {
				position();
			}

			@Override
			protected void controlRender(RenderManager rm, ViewPort vp) {
			}
		});

		setDestroyOnHide(true);
		sizeToContent();

		// // Make sure chat bubble isnt too large
		// if (getTextElement().getLineCount() < 2) {
		// final float cw = getTextElement().getLineWidth() +
		// getTotalPadding().x;
		// if (getWidth() > cw) {
		// setWidth(cw);
		// }
		// }
		// final float ch = getTextElement().getLineHeight() *
		// getTextElement().getLineCount() + getTotalPadding().y;
		// if (getHeight() > ch) {
		// setHeight(ch);
		// }
	}

	public void position() {
		// Get world position above spawns head
		Vector3f loc = entity.getSpatial().getWorldTranslation().clone();
		BoundingBox boundingBox = entity.getBoundingBox();
		if (boundingBox != null) {
			loc.y += (boundingBox.getYExtent() * 2f);
			final Vector3f screenCoordinates = camera.getScreenCoordinates(loc);
			setPosition(new Vector2f((int) (screenCoordinates.x - getWidth() *0.9f),
					screen.getHeight() - (int) (screenCoordinates.y) - getHeight()));
		}
	}

}