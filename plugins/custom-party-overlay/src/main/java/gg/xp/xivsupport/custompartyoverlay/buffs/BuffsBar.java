package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.sys.Threading;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class BuffsBar extends Component {
	private List<BuffApplied> rawBuffs = Collections.emptyList();
	private List<Tracker> buffs = Collections.emptyList();

	public static final Color defaultMyBuffColor = new Color(120, 220, 255);
	public static final Color defaultRemovableBuffColor = new Color(255, 120, 120);
	public static final Color defaultTextColor = new Color(255, 255, 255);
	private Color myBuffColor = defaultMyBuffColor;
	private Color removableBuffColor = defaultRemovableBuffColor;
	private Color normalBuffColor = defaultTextColor;
	private static final Color shadow1 = new Color(0, 0, 0, 192);
	private static final Color shadow2 = new Color(0, 0, 0, 64);
	private static final Stroke stroke1 = new BasicStroke(2);
	private static final Stroke stroke2 = new BasicStroke(3);
	private static final Map<FontShapeKey, Shape> shapeCache = new HashMap<>();
	private static final Map<TextRenderKey, Image> imageCache = new ConcurrentHashMap<>();
	private static final ThreadFactory threadFactory = Threading.namedDaemonThreadFactory("BuffsWithTimersRender");
	private final ExecutorService exs = Executors.newSingleThreadExecutor(threadFactory);
	private int xPadding;
	private boolean enableTimers = true;
	private boolean enableShadows = true;

	private record FontShapeKey(String string, Font font) {

	}

	// TODO: this FontMetric stuff could also be used to fix label alignment for number stuff
	private Font fontSmall;
	private Font fontBig;
	private FontMetrics fontSmallMetrics;
	private FontMetrics fontBigMetrics;
	private int buffWidth;
	private double scale = 1.0f;
	private double scaleFromPaint;
	private float prevTextHeight;
	private double prevScale;

	private void resetScaling() {
		Rectangle bounds = getBounds();
		int cellHeight = bounds.height;
		Graphics g = getGraphics();
		float textHeight = (float) Math.max(5.0f, Math.floor(cellHeight * 0.30f));
		if (scaleFromPaint > 0) {
			scale = scaleFromPaint;
		}
		else {
			if (g instanceof Graphics2D gg) {
				scale = gg.getTransform().getScaleX();
			}
		}
		if (g == null) {
			return;
		}
		//noinspection FloatingPointEquality
		if (BuffsBar.this.scale != prevScale || prevTextHeight != textHeight) {
			Font fontOrig = g.getFont();
			fontBig = fontOrig.deriveFont(textHeight);
			float textHeightSmall = textHeight * 0.66f;
			fontSmall = fontBig.deriveFont(textHeightSmall);
			fontBigMetrics = g.getFontMetrics(fontBig);
			fontSmallMetrics = g.getFontMetrics(fontSmall);
			imageCache.clear();
			prevScale = BuffsBar.this.scale;
			prevTextHeight = textHeight;

		}
		exs.submit(this::recalc);

	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		resetScaling();
		imageCache.clear();
	}

	@Override
	public void validate() {
		super.validate();
		resetScaling();
	}

	@Override
	public void paint(Graphics gg) {
//				if (true) return;
		Graphics2D g = (Graphics2D) gg;
		scaleFromPaint = g.getTransform().getScaleX();
		Rectangle bounds = getBounds();
		int cellWidth = bounds.width;
		AffineTransform transform = g.getTransform();
		int curX = 5;
		transform.translate(curX, 0);
		int buffHeight = iconSize();
		buffWidth = (int) Math.ceil(buffHeight * 0.75);
		for (Tracker tracker : buffs) {
			g.setTransform(transform);
			ScaledImageComponent component = tracker.component;
			if (component == null) {
				continue;
			}
			int remainingX = cellWidth - curX;
			if (buffWidth > remainingX) {
				break;
			}
			Image textImage = tracker.image;
			component.paint(g, buffHeight);
			if (textImage != null) {
//						g.drawString(text, 0, 0);
				AffineTransform shapeTrans = new AffineTransform(transform);
				shapeTrans.translate(-5.0, 0);
				shapeTrans.scale(1.0f / shapeTrans.getScaleX(), 1.0f / shapeTrans.getScaleY());
//						shapeTrans.translate(buffWidth / 2 - (tracker.textWidth / 2), cellHeight - tracker.yPad);
				g.setTransform(shapeTrans);
				g.drawImage(textImage, 0, 0, null);
			}
			int delta = buffWidth + xPadding;
			transform.translate(delta, 0);
//			g.translate(delta, 0);
			curX += delta;
		}

	}

	private int iconSize() {
		int cellHeight = getHeight();
		return Math.max(10, (int) Math.floor(cellHeight * 0.80));
	}

	private record TextRenderKey(
			String text,
			Color color,
			int maxWidth,
			int height,
			double scale,
			boolean enableShadows
	) {
	}


	private Tracker makeTracker(BuffApplied buff) {
		int extraPadding = 5;
		HasIconURL icon = StatusEffectLibrary.iconForId(buff.getBuff().getId(), buff.getStacks());
		ScaledImageComponent scaled = IconTextRenderer.getIconOnly(icon);
		StatusEffectInfo info = StatusEffectLibrary.forId(buff.getBuff().getId());
		String text = enableTimers ? formatText(buff) : null;
		boolean canDispel = info != null && info.canDispel();
		boolean isMine = buff.getSource().walkParentChain().isThePlayer();
		Image img;
		if (text != null) {
			if (fontBigMetrics == null) {
				img = null;
			}
			else {
				Color color;
				if (canDispel) {
					color = removableBuffColor;
				}
				else if (isMine) {
					color = myBuffColor;
				}
				else {
					color = normalBuffColor;
				}
				int maxWidth = buffWidth + xPadding - 1;
				int cellHeight = getHeight();
				TextRenderKey key = new TextRenderKey(text, color, maxWidth, cellHeight, scale, enableShadows);
				img = imageCache.computeIfAbsent(key, t -> {
					int textWidth;
					textWidth = fontBigMetrics.stringWidth(text);
					int yPad = 0;
					Image image = new BufferedImage((int) (scale * (buffWidth + extraPadding * 2)), (int) (t.scale * cellHeight), BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = (Graphics2D) image.getGraphics();
					g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					yPad += 2;
					Font font;
					if (textWidth > maxWidth) {
						textWidth = fontSmallMetrics.stringWidth(text);
						yPad += 2;
						font = fontSmall;
					}
					else {
						font = fontBig;
					}
					g.setFont(font);
					FontRenderContext frc = g.getFontRenderContext();
					Shape outline = shapeCache.computeIfAbsent(new FontShapeKey(text, font), k -> {
						GlyphVector glyphVector = font.createGlyphVector(frc, text);
						return glyphVector.getOutline();
					});
//					g.setBackground(new Color(0, 0, 0, 0));
					AffineTransform shapeTrans = new AffineTransform();
					shapeTrans.scale(t.scale, t.scale);
					shapeTrans.translate(extraPadding + buffWidth / 2.0f - textWidth / 2.0f, cellHeight - yPad);
					g.setTransform(shapeTrans);
					if (t.enableShadows) {
						g.setColor(shadow1);
						g.setStroke(stroke1);
						g.draw(outline);
						g.setStroke(stroke2);
						g.setColor(shadow2);
						g.draw(outline);
					}
					g.setColor(t.color);
					g.fill(outline);
					return image;
				});
			}
		}
		else {
			img = null;
		}
		return new Tracker(buff,
				scaled,
				info,
				isMine,
				canDispel,
				text,
				img
		);
	}

	private @Nullable String formatText(BuffApplied buff) {
		if (!buff.shouldDisplayDuration()) {
			return null;
		}
		Duration estimatedRemainingDuration = buff.getEstimatedRemainingDuration();
		long seconds = estimatedRemainingDuration.toSeconds();
		if (seconds < 1) {
			int tenths = (int) (estimatedRemainingDuration.toMillis() / 100.0);
			return "0." + tenths;
		}
		if (seconds < 100) {
			return String.valueOf(seconds);
		}
		else {
			int minutes = (int) Math.floor(seconds / 60.0);
			if (minutes < 100) {
				return minutes + "m";
			}
			else {
				int hours = (int) Math.floor(seconds / 3600.0);
				return hours + "h";
			}
		}
	}

	private record Tracker(
			BuffApplied buff,
			ScaledImageComponent component,
			@Nullable StatusEffectInfo info,
			boolean isSelfApplied,
			boolean isDispellable,
			String formattedText,
			Image image) {
	}

	private void recalc() {
		List<Tracker> trackers = new ArrayList<>();
		for (BuffApplied buff : rawBuffs) {
			trackers.add(makeTracker(buff));
		}
		this.buffs = trackers;
	}

	public void setBuffs(List<BuffApplied> buffs) {
		rawBuffs = buffs;
		exs.submit(this::recalc);
	}

	public void reformat() {
		resetScaling();
		imageCache.clear();
	}


	public void setEnableShadows(boolean enableShadows) {
		this.enableShadows = enableShadows;
	}

	public void setEnableTimers(boolean enableTimers) {
		this.enableTimers = enableTimers;
	}

	public void setMyBuffColor(Color myBuffColor) {
		this.myBuffColor = myBuffColor;
	}

	public void setRemovableBuffColor(Color removableBuffColor) {
		this.removableBuffColor = removableBuffColor;
	}

	public void setNormalBuffColor(Color textColor) {
		this.normalBuffColor = textColor;
	}

	public void setXPadding(int xPadding) {
		this.xPadding = xPadding;
	}
}
