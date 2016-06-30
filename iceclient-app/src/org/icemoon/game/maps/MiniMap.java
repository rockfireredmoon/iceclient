package org.icemoon.game.maps;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.icescene.SceneConstants;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

public class MiniMap {

    private final static Logger LOG = Logger.getLogger(MiniMap.class.getName());
    private List<String> images;
    private final int gridSize;
    private BufferedImage bufferedImage;
    private int scaledWidth;
    private float deg;
    private final BufferedImage pointer;

    public MiniMap(AssetManager assetManager, int gridSize, int scaledWidth) {
        try {
            this.gridSize = gridSize;
            setScaledWidth(scaledWidth);
            AssetInfo img = assetManager.locateAsset(new AssetKey("Interface/maparrow.png"));
            InputStream in = img.openStream();
            try {
                pointer = ImageIO.read(in);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setOrientation(float rotDeg) {
        this.deg = rotDeg;
    }

    public final void setScaledWidth(int scaledWidth) {
        this.scaledWidth = scaledWidth;
        this.bufferedImage = new BufferedImage(gridSize * scaledWidth, gridSize * scaledWidth, BufferedImage.TYPE_INT_ARGB);
        if (this.images != null) {
            try {
                this.setImages(this.images);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public void setImages(List<String> images) throws IOException {
        int cells = gridSize * gridSize;
        if (images.size() != cells) {
            throw new IllegalArgumentException("Expected " + cells + " images");
        }
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        this.images = images;
        Iterator<String> it = images.iterator();
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                final String next = it.next();
                if (next != null) {
                    URL url = MiniMap.class.getResource("/" + next);
                    if (url == null) {
                        url = MiniMap.class.getResource("/" + SceneConstants.TERRAIN_PATH + "/Terrain-Common/Flat.png");
                    }
                    BufferedImage mapPageImage = ImageIO.read(url);

                    // Flip the image vertically
                    AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                    tx.translate(0, -mapPageImage.getHeight(null));
                    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    mapPageImage = op.filter(mapPageImage, null);

                    // Flip the image horizontally
                    tx = AffineTransform.getScaleInstance(-1, 1);
                    tx.translate(-mapPageImage.getWidth(null), 0);
                    op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                    mapPageImage = op.filter(mapPageImage, null);

                    g.drawImage(mapPageImage, x * scaledWidth, y * scaledWidth, ((x + 1) * scaledWidth), ((y + 1) * scaledWidth), 0, 0, mapPageImage.getWidth(), mapPageImage.getHeight(), null);
                }
            }
        }
        ImageIO.write(bufferedImage, "png", new File("/tmp/xx.png"));
    }

    public BufferedImage getImageForCenter(Vector2f location) {
        float radius = ((float) gridSize - 1f) / 2f;
        float offset = (radius * scaledWidth * 1.5f) - (float) scaledWidth;
        int totalSize = gridSize * scaledWidth;
        int sx = (int) (((1 - location.x) * (float) scaledWidth) + offset);
        int sy = (int) (((1 - location.y) * (float) scaledWidth) + offset);
        if (sx < 0 || sy < 0 || sx + scaledWidth > bufferedImage.getWidth() || sy + scaledWidth > bufferedImage.getHeight()) {
            throw new IllegalArgumentException("Cannot get map image for " + sx + "," + sy + " sxw = " + (sx + scaledWidth) + " syw = " + (sy + scaledWidth) + " bw = " + (bufferedImage.getWidth()) + " bh = " + (bufferedImage.getHeight()));
        }
        BufferedImage subImg = bufferedImage.getSubimage(sx, sy, scaledWidth, scaledWidth);
        BufferedImage finalImage = new BufferedImage(scaledWidth, scaledWidth, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = (Graphics2D) finalImage.getGraphics();
        g2.setClip(new Ellipse2D.Double(0, 0, scaledWidth, scaledWidth));
        g2.drawImage(subImg, 0, 0, null);
        g2.drawImage(rotate(pointer, deg, pointer.getWidth(), pointer.getHeight()), (scaledWidth - pointer.getWidth()) / 2, (scaledWidth - pointer.getHeight()) / 2, null);

//        AffineTransform xform = new AffineTransform();
//        xform.rotate(FastMath.DEG_TO_RAD * 180, scaledWidth, scaledWidth);
//        AffineTransformOp op = new AffineTransformOp(xform, AffineTransformOp.TYPE_BILINEAR);
//        return op.filter(finalImage, null);
        return finalImage;
    }

    public BufferedImage rotate(BufferedImage image, float deg, int w, int h) {
        AffineTransform xform = new AffineTransform();
        float rad = FastMath.DEG_TO_RAD * deg;
        xform.rotate(rad, w / 2, h / 2);
        AffineTransformOp op = new AffineTransformOp(xform, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(image, null);
    }
}
