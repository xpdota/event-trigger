package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuffsWithTimersComponent extends BasePartyListComponent {
	private final StatusEffectRepository buffRepo;
	private final ComponentListRenderer renderingComponent;
	private List<Tracker> buffs = Collections.emptyList();

	private static final Color myBuffColor = new Color(120, 220, 255);
	private static final Color removableBuffColor = new Color(255, 120, 120);

	public BuffsWithTimersComponent(StatusEffectRepository buffRepo) {
		this.buffRepo = buffRepo;
		renderingComponent = new ComponentListRenderer(0) {
			@Override
			public void paint(Graphics g) {
				int xPadding = 3;
				Rectangle bounds = getBounds();
				int cellHeight = bounds.height;
				int cellWidth = bounds.width;
				Graphics2D graphics = ((Graphics2D) g);
				AffineTransform transform = graphics.getTransform();
				int curX = 5;
				transform.translate(curX, 0);
				Font fontOrig = graphics.getFont();
				Font fontBig = fontOrig.deriveFont((float) (fontOrig.getSize() * 0.75));
				Font fontSmall = fontBig.deriveFont((float) (fontOrig.getSize() * 0.5));
				FontMetrics fontBigMetrics = g.getFontMetrics(fontBig);
				FontMetrics fontSmallMetrics = g.getFontMetrics(fontSmall);
				Color origColor = g.getColor();
				int buffWidth = 15;
				for (Tracker tracker : buffs) {
					graphics.setTransform(transform);
					graphics.setFont(fontBig);
					Component component = tracker.component;
					Dimension prefSize = component.getPreferredSize();
					int prefWidth = buffWidth + xPadding;
					int prefHeight = prefSize.height;
					int actualHeight = Math.min(prefHeight, cellHeight);
					int remainingX = cellWidth - curX;
					int actualWidth = Math.min(prefWidth, remainingX);
					if (actualWidth <= 0) {
						break;
					}
					String text = tracker.formatText();
					component.setBounds(curX, 0, actualWidth, actualHeight);
					component.paint(g);
					if (text != null) {
						graphics.setTransform(transform);
						StatusEffectInfo info = tracker.buff.getInfo();
						if (info != null && info.canDispel()) {
							g.setColor(removableBuffColor);
						}
						else if (tracker.buff.getSource().walkParentChain().isThePlayer()) {
							g.setColor(myBuffColor);
						}
						else {
							g.setColor(origColor);
						}
						int textWidth = fontBigMetrics.stringWidth(text);
						int yPad = 0;
						if (textWidth > (buffWidth + xPadding - 1)) {
							g.setFont(fontSmall);
							textWidth = fontSmallMetrics.stringWidth(text);
							yPad = 2;
						}
						((Graphics2D) g).drawString(text, buffWidth / 2 - (textWidth / 2), cellHeight - yPad);
					}
					int delta = actualWidth + xPadding;
					transform.translate(delta, 0);
//			g.translate(delta, 0);
					curX += delta;
				}

			}
		};
	}

	@Override
	protected Component makeComponent() {
		return renderingComponent;
	}

	private record Tracker(BuffApplied buff, Component component) {
		@Nullable String formatText() {
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
	}


	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		List<BuffApplied> buffs = buffRepo.sortedStatusesOnTarget(xpc);
		List<Tracker> components = new ArrayList<>();
		for (BuffApplied buff : buffs) {
			HasIconURL icon = StatusEffectLibrary.iconForId(buff.getBuff().getId(), buff.getStacks());
			Component component = IconTextRenderer.getIconOnly(icon);
			components.add(new Tracker(buff, component));
		}
		this.buffs = components;

	}
}
