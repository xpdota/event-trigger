package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
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

public class BuffsWithTimersComponent extends BasePartyListComponent {
	private final StatusEffectRepository buffRepo;
	private final Component renderingComponent;
	private List<Tracker> buffs = Collections.emptyList();

	private static final Color myBuffColor = new Color(120, 220, 255);
	private static final Color removableBuffColor = new Color(255, 120, 120);
	private static final Color defaultTextColor = new Color(255, 255, 255);
	private static final Color shadow1 = new Color(0, 0, 0, 192);
	private static final Color shadow2 = new Color(0, 0, 0, 64);
	private static final Stroke stroke1 = new BasicStroke(2);
	private static final Stroke stroke2 = new BasicStroke(4);
	private static final Map<FontShapeKey, Shape> shapeCache = new HashMap<>();

	private record FontShapeKey(String string, Font font) {

	}

	public BuffsWithTimersComponent(StatusEffectRepository buffRepo) {
		this.buffRepo = buffRepo;
		renderingComponent = new Component() {

			// TODO: this FontMetric stuff could also be used to fix label alignment for number stuff
			private Font fontSmall;
			private Font fontBig;
			private FontMetrics fontSmallMetrics;
			private FontMetrics fontBigMetrics;

			@Override
			public void validate() {
				super.validate();
				Graphics g = getGraphics();
				Rectangle bounds = getBounds();
				int cellHeight = bounds.height;
				float textHeight = (float) Math.max(5.0f, Math.floor(cellHeight * 0.30f));
				Font fontOrig = g.getFont();
				fontBig = fontOrig.deriveFont(textHeight);
				float textHeightSmall = textHeight * 0.66f;
				fontSmall = fontBig.deriveFont(textHeightSmall);
				fontBigMetrics = g.getFontMetrics(fontBig);
				fontSmallMetrics = g.getFontMetrics(fontSmall);
			}

			@Override
			public void paint(Graphics gg) {
				Graphics2D g = (Graphics2D) gg;
				int xPadding = 0;
				Rectangle bounds = getBounds();
				int cellHeight = bounds.height;
				int cellWidth = bounds.width;
				AffineTransform transform = g.getTransform();
				int curX = 5;
				transform.translate(curX, 0);
				int buffHeight = iconSize();
				int buffWidth = (int) Math.ceil(buffHeight * 0.75);
				g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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
					String text = tracker.formattedText;
					component.paint(g, buffHeight);
					if (text != null) {
//						g.setTransform(transform);
						StatusEffectInfo info = tracker.info;
						int textWidth = fontBigMetrics.stringWidth(text);
						int yPad = 2;
						Font font;
						if (textWidth > (buffWidth + xPadding - 1)) {
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
						// TODO: unfortunately, I think the only fix for bad perf here will be to use a BufferedImage
						// in a non-AWT thread.
						// Or, alternatively, ditch the font shadow idea, and just draw a generic gradient
						// blob.
						AffineTransform shapeTrans = new AffineTransform(transform);
						shapeTrans.translate(buffWidth / 2 - (textWidth / 2), cellHeight - yPad);
						g.setTransform(shapeTrans);
						g.setColor(shadow1);
						g.setStroke(stroke1);
						g.draw(outline);
						g.setStroke(stroke2);
						g.setColor(shadow2);
						g.draw(outline);
//						g.drawString(text, -0.5f, -0.5f);
//						g.drawString(text, 0.5f, 0.5f);
						if (info != null && info.canDispel()) {
							g.setColor(removableBuffColor);
						}
						else if (tracker.buff.getSource().walkParentChain().isThePlayer()) {
							g.setColor(myBuffColor);
						}
						else {
							g.setColor(defaultTextColor);
						}
						g.fill(outline);
//						g.drawString(text, 0, 0);
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

	private Tracker makeTracker(BuffApplied buff) {
		HasIconURL icon = StatusEffectLibrary.iconForId(buff.getBuff().getId(), buff.getStacks());
		ScaledImageComponent scaled = IconTextRenderer.getIconOnly(icon);
		StatusEffectInfo info = StatusEffectLibrary.forId(buff.getBuff().getId());
		return new Tracker(buff,
				scaled,
				info,
				buff.getSource().walkParentChain().isThePlayer(),
				info != null && info.canDispel(),
				formatText(buff));
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
			String formattedText
	) {
	}


	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		List<BuffApplied> buffs = buffRepo.sortedStatusesOnTarget(xpc);
		List<Tracker> components = new ArrayList<>();
		for (BuffApplied buff : buffs) {
			components.add(makeTracker(buff));
		}
		this.buffs = components;

	}
}
