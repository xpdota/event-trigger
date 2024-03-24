package gg.xp.xivsupport.custompartyoverlay.selection;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.custompartyoverlay.gui.CustomPartyConfig;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class SelectionComponent extends BasePartyListComponent {

	private volatile boolean isSelected;
	private volatile @Nullable Paint bgPaint;
	private volatile @Nullable Paint borderPaint;
	private final Component comp;
	private final SelectionComponentConfig config;
	private final XivState state;

	public SelectionComponent(SelectionComponentConfig cfg, XivState state) {
		this.config = cfg;
		this.state = state;
		comp = new Component() {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				super.setBounds(x, y, width, height);
				resetPaints();
			}

			@Override
			public void paint(Graphics gg) {
				if (isSelected) {
					var g = ((Graphics2D) gg);
					Rectangle bounds = getBounds();
					var cfg = config;
					boolean enableBorder = cfg.getEnableBorder().get();
					int strokeWidth = enableBorder ? cfg.getBorderThickness().get() : 0;
					int radiusX = cfg.getBorderRadiusX().get();
					int radiusY = cfg.getBorderRadiusY().get();
					if (cfg.getEnableBg().get()) {
						g.setPaint(bgPaint);
						//noinspection SuspiciousNameCombination
						g.fillRoundRect(strokeWidth,
								strokeWidth,
								bounds.width - 2 * strokeWidth,
								bounds.height - 2 * strokeWidth,
								radiusX,
								radiusY);
					}
					if (enableBorder) {
						g.setComposite(AlphaComposite.Clear);
						g.setPaint(borderPaint);
						g.setStroke(new BasicStroke(strokeWidth));
						//noinspection SuspiciousNameCombination
						g.drawRoundRect(
								strokeWidth,
								strokeWidth,
								bounds.width - 2 * strokeWidth,
								bounds.height - 2 * strokeWidth,
								radiusX,
								radiusY);
						g.setComposite(AlphaComposite.SrcOver);
						g.setStroke(new BasicStroke(strokeWidth));
						//noinspection SuspiciousNameCombination
						g.drawRoundRect(
								strokeWidth,
								strokeWidth,
								bounds.width - 2 * strokeWidth,
								bounds.height - 2 * strokeWidth,
								radiusX,
								radiusY);
					}
				}
			}
		};
		config.addAndRunListener(this::reformatFromSettings);
	}

	private void reformatFromSettings() {
		this.resetPaints();
	}

	private void resetPaints() {
		// TODO: move this out
		if (config.getEnableGradient().get()) {
			double angle = Math.toRadians(config.getGradientAngle().get());
			int width = comp.getWidth();
			int height = comp.getHeight();
			int centerX = width / 2;
			int centerY = height / 2;
			int start = config.getGradientStart().get();
			double xBasis = Math.sin(angle);
			int deltaX = (int) ((start / 2) * xBasis);
			double yBasis = -Math.cos(angle);
			int deltaY = (int) ((start / 2) * yBasis);
			int startX = centerX + deltaX;
			int startY = centerY + deltaY;
			int length = config.getGradientLength().get();
			int endX = (int) (startX + length * xBasis);
			int endY = (int) (startY + length * yBasis);
			bgPaint = new GradientPaint(startX, startY, config.getBgColor().get(), endX, endY, RenderUtils.withAlpha(config.getBgColor().get(), 0));
			borderPaint = new GradientPaint(startX, startY, config.getBorderColor().get(), endX, endY, RenderUtils.withAlpha(config.getBorderColor().get(), 0));
		}
		else {
			bgPaint = config.getBgColor().get();
			borderPaint = config.getBorderColor().get();
		}

	}


	@Override
	protected Component makeComponent() {
		return comp;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		// Dummy value for testing
		if (xpc.getId() == CustomPartyConfig.dummyCharacter.getId()) {
			this.isSelected = true;
			return;
		}
		XivPlayerCharacter player = state.getPlayer();
		if (player == null) {
			this.isSelected = false;
			return;
		}
		this.isSelected = (state.getPlayer().getTargetId() == xpc.getId());
	}

	@Override
	public int getZOrder() {
		return 200;
	}
}
