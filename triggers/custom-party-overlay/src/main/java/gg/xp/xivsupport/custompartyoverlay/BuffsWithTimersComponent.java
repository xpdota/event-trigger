package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.java_websocket.util.NamedThreadFactory;
import org.jetbrains.annotations.NotNull;
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

public class BuffsWithTimersComponent extends BasePartyListComponent {
	private final StatusEffectRepository buffRepo;
	private final Component renderingComponent;
	private final boolean enableTimers;
	private List<BuffApplied> rawBuffs = Collections.emptyList();
	private List<Tracker> buffs = Collections.emptyList();

	private static final Color myBuffColor = new Color(120, 220, 255);
	private static final Color removableBuffColor = new Color(255, 120, 120);
	private static final Color defaultTextColor = new Color(255, 255, 255);
	private static final Color shadow1 = new Color(0, 0, 0, 192);
	private static final Color shadow2 = new Color(0, 0, 0, 64);
	private static final Stroke stroke1 = new BasicStroke(2);
	private static final Stroke stroke2 = new BasicStroke(3);
	private static final Map<FontShapeKey, Shape> shapeCache = new HashMap<>();
	private static final Map<TextRenderKey, Image> imageCache = new ConcurrentHashMap<>();
	private static final NamedThreadFactory threadFactory = new NamedThreadFactory("BuffsWithTimersRender");
	private final ExecutorService exs = Executors.newSingleThreadExecutor(threadFactory);
	private int xPadding = 0;

	private record FontShapeKey(String string, Font font) {

	}

	// TODO: this FontMetric stuff could also be used to fix label alignment for number stuff
	private Font fontSmall;
	private Font fontBig;
	private FontMetrics fontSmallMetrics;
	private FontMetrics fontBigMetrics;
	private int buffWidth;
	private double scale = 1.0f;
	private float prevTextHeight;
	private double prevScale;

	public BuffsWithTimersComponent(StatusEffectRepository buffRepo) {
		this.buffRepo = buffRepo;
		this.enableTimers = true;
		renderingComponent = new Component() {

			@Override
			public void validate() {
				super.validate();
				Graphics g = getGraphics();
				Rectangle bounds = getBounds();
				int cellHeight = bounds.height;
				float textHeight = (float) Math.max(5.0f, Math.floor(cellHeight * 0.30f));
				if (g instanceof Graphics2D gg) {
					scale = gg.getTransform().getScaleX();
				}
				//noinspection FloatingPointEquality
				if (scale != prevScale || prevTextHeight != textHeight) {
					Font fontOrig = g.getFont();
					fontBig = fontOrig.deriveFont(textHeight);
					float textHeightSmall = textHeight * 0.66f;
					fontSmall = fontBig.deriveFont(textHeightSmall);
					fontBigMetrics = g.getFontMetrics(fontBig);
					fontSmallMetrics = g.getFontMetrics(fontSmall);
					imageCache.clear();
					prevScale = scale;
					prevTextHeight = textHeight;

				}
				exs.submit(() -> recalc());
			}

			@Override
			public void paint(Graphics gg) {
//				if (true) return;
				Graphics2D g = (Graphics2D) gg;
				Rectangle bounds = getBounds();
				int cellHeight = bounds.height;
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
					int prefWidth = buffWidth + xPadding;
					int remainingX = cellWidth - curX;
					int actualWidth = Math.min(prefWidth, remainingX);
					if (actualWidth <= 0) {
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
					int delta = actualWidth + xPadding;
					transform.translate(delta, 0);
//			g.translate(delta, 0);
					curX += delta;
				}

			}
		};
	}

	private int iconSize() {
		int cellHeight = renderingComponent.getHeight();
		return Math.max(10, (int) Math.floor(cellHeight * 0.80));
	}

	@Override
	protected Component makeComponent() {
		return renderingComponent;
	}

	private record TextRenderKey(
			String text,
			Color color,
			int maxWidth,
			int height

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
					color = (removableBuffColor);
				}
				else if (isMine) {
					color = (myBuffColor);
				}
				else {
					color = (defaultTextColor);
				}
				int maxWidth = (buffWidth + xPadding - 1);
				int cellHeight = renderingComponent.getHeight();
				TextRenderKey key = new TextRenderKey(text, color, maxWidth, cellHeight);
				img = imageCache.computeIfAbsent(key, t -> {
					int textWidth;
					textWidth = fontBigMetrics.stringWidth(text);
					int yPad = 0;
					Image image = new BufferedImage((int) (scale * (buffWidth + (extraPadding * 2))), (int) (scale * cellHeight), BufferedImage.TYPE_INT_ARGB);
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
					shapeTrans.scale(scale, scale);
					shapeTrans.translate(extraPadding + (buffWidth / 2.0f) - (textWidth / 2.0f), cellHeight - yPad);
					g.setTransform(shapeTrans);
					g.setColor(shadow1);
					g.setStroke(stroke1);
					g.draw(outline);
					g.setStroke(stroke2);
					g.setColor(shadow2);
					g.draw(outline);
					g.setColor(t.color);
					g.fill(outline);
					return image;
				});
				int foo = 5+1;
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

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		rawBuffs = buffRepo.sortedStatusesOnTarget(xpc);
		exs.submit(this::recalc);
	}
}
