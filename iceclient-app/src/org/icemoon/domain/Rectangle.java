package org.icemoon.domain;

import java.io.Serializable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.icelib.Icelib;
import org.icelib.Point3D;
import org.icescene.props.Point;

@SuppressWarnings("serial")
public class Rectangle implements Serializable {
	public Rectangle(long topLeftX, long topLeftY, long bottomRightX, long bottomRightY) {
		topLeft.x = topLeftX;
		topLeft.y = topLeftY;
		bottomRight.x = bottomRightX;
		bottomRight.y = bottomRightY;
	}

	public Rectangle() {
	}

	public Rectangle(String value) {
		Iterator<Long> it = Icelib.toLongList(value).iterator();
		topLeft.x = it.next();
		topLeft.y = it.next();
		bottomRight.x = it.next();
		bottomRight.y = it.next();
	}

	public Rectangle(StringTokenizer t) {
		this(Long.parseLong(t.nextToken()), Long.parseLong(t.nextToken()), Long.parseLong(t.nextToken()), Long.parseLong(t.nextToken()));
	}

	public Point topLeft = new Point();
	public Point bottomRight = new Point();

	public boolean contains(Point location) {
		return location.x >= topLeft.x && location.x <= bottomRight.x && location.y >= topLeft.y && location.y <= bottomRight.y;
	}

	public String toValueString() {
		return topLeft.x + "," + topLeft.y + "," + bottomRight.x + "," + bottomRight.y;
	}

	public long getAreaSize() {
		return (bottomRight.x - topLeft.x) * (bottomRight.y - topLeft.y);
	}
 
	@Override
	public String toString() {
		return "Rectangle [topLeft=" + topLeft + ", bottomRight=" + bottomRight + "]";
	}

	public boolean contains(Point3D location) {
		return location.getX() >= topLeft.x && location.getX() <= bottomRight.x && location.getY() >= topLeft.y
			&& location.getY() <= bottomRight.y;
	}
}