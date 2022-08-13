package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.gui.overlay.TextAlignment;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class DropShadowLabel extends Component {

	private static final int xPad = 4;

	private static final Color shadow1 = new Color(0, 0, 0, 192);
	private static final Color shadow2 = new Color(0, 0, 0, 64);
	private static final Stroke stroke1 = new BasicStroke(2);
	private static final Stroke stroke2 = new BasicStroke(3);
	private String text = "";
	private Image image;
	private int lastWidth;
	private TextAlignment alignment = TextAlignment.LEFT;
	private FontRenderRequest lastReq;
	private boolean enableShadow = true;
	private double scaleFactor;

	public void setText(String text) {
		boolean changed = !text.equals(this.text) || (lastReq != null && lastReq.scale != scaleFactor);
		if (changed) {
			this.text = text;
			recalc();
		}
	}

	public void setAlignment(TextAlignment alignment) {
		this.alignment = alignment;
		invalidateImage();
	}

	public void setEnableShadow(boolean enableShadow) {
		this.enableShadow = enableShadow;
		invalidateImage();
	}

	private void invalidateImage() {
		lastReq = null;
		recalc();
	}

	@Override
	public void validate() {
		super.validate();
		invalidateImage();
	}

	@Override
	public void paint(Graphics gg) {
		if (image == null) {
			return;
		}
		Graphics2D g = (Graphics2D) gg;
		AffineTransform trans = g.getTransform();
		int textWidth = lastWidth;
		int xOffset = switch (alignment) {
			case LEFT -> xPad;
			case CENTER -> (int) ((getWidth() / 2.0) - (textWidth / 2.0));
			case RIGHT -> getWidth() - textWidth;
		};
		trans.translate(xOffset, 0);
		double scale = trans.getScaleX();
		this.scaleFactor = scale;
		trans.scale(1.0f / scale, 1.0f / trans.getScaleY());
		g.setTransform(trans);
		g.drawImage(image, 0, 0, null);
	}

	private record FontRenderRequest(String text, int height, Font font, double scale) {

	}

	private synchronized void recalc() {
		Font font = getFont();
		if (font == null) {
			return;
		}
		int height = getHeight();
		double scale;
		if (scaleFactor > 0) {
			scale = scaleFactor;
		}
		else {
			// Querying this to find the scale factor doesn't work on X11
			Graphics graphics = getGraphics();
			if (graphics == null) {
				return;
			}
			else {
				scale = ((Graphics2D) graphics).getTransform().getScaleX();
			}
		}
		FontRenderRequest req = new FontRenderRequest(text, height, font, scale);
		if (!req.equals(this.lastReq)) {
			format(req);
			this.lastReq = req;
		}
	}

	private void format(FontRenderRequest req) {
		String text = req.text;
		if (text == null || text.isBlank()) {
			image = null;
			return;
		}
		float fontSize = req.height * 0.65f;
		Font font = req.font.deriveFont(fontSize);

		Graphics graphics = getGraphics();
		FontMetrics fontMetrics = graphics.getFontMetrics(font);
		int width = getWidth();
		int targetWidth = width - (2 * xPad);
		int textWidth = fontMetrics.stringWidth(text);
		while (textWidth > targetWidth && fontSize > 6) {
			fontSize = Math.min(fontSize * (targetWidth / (float) textWidth), fontSize - 1.0f);
			font = req.font.deriveFont(fontSize);
			fontMetrics = graphics.getFontMetrics(font);
			textWidth = fontMetrics.stringWidth(text);
		}
		int fontVshift = fontMetrics.getAscent();
		int textWidthWithPadding = textWidth + 2 * xPad;

		BufferedImage bufferedImage = new BufferedImage((int) (textWidthWithPadding * req.scale), (int) (req.height * req.scale), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		FontRenderContext frc = g.getFontRenderContext();
		GlyphVector glyphVector = font.createGlyphVector(frc, text);
		Shape outline = glyphVector.getOutline();
		AffineTransform shapeTrans = new AffineTransform();
		shapeTrans.scale(req.scale, req.scale);
		shapeTrans.translate(xPad, fontVshift);
//		shapeTrans.translate(extraPadding + (buffWidth / 2.0f) - (textWidth / 2.0f), cellHeight - yPad);
		g.setTransform(shapeTrans);
		if (enableShadow) {
			g.setColor(shadow1);
			g.setStroke(stroke1);
			g.draw(outline);
			g.setStroke(stroke2);
			g.setColor(shadow2);
			g.draw(outline);
		}
		g.setColor(getForeground());
		g.fill(outline);
		image = bufferedImage;
		lastWidth = textWidthWithPadding;
		setPreferredSize(new Dimension(textWidthWithPadding, req.height));

	}

}
