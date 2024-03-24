package gg.xp.xivsupport.custompartyoverlay.selection;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class SelectionComponent extends BasePartyListComponent {

	private volatile boolean isSelected;
	private final Component comp;
	private final SelectionComponentConfig config;
	private final XivState state;

	public SelectionComponent(SelectionComponentConfig config, XivState state) {
		this.config = config;
		this.state = state;
		comp = new Component() {
			@Override
			public void paint(Graphics gg) {
				if (isSelected) {
					var g = ((Graphics2D) gg);
					Rectangle bounds = getBounds();
					int strokeWidth = 4;
					int radius = 8;
					g.setColor(Color.BLUE);
					g.fillRoundRect(strokeWidth,
							strokeWidth,
							bounds.width - 2 * strokeWidth,
							bounds.height - 2 * strokeWidth,
							radius,
							radius);
					g.setColor(Color.RED);
					g.setStroke(new BasicStroke(strokeWidth));
					g.drawRoundRect(
							strokeWidth,
							strokeWidth,
							bounds.width - 2 * strokeWidth,
							bounds.height - 2 * strokeWidth,
							radius,
							radius);
				}
			}
		};
		config.addAndRunListener(this::reformatFromSettings);
	}

	private void reformatFromSettings() {
	}

	@Override
	protected Component makeComponent() {
		return comp;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		XivPlayerCharacter player = state.getPlayer();
		if (player == null) {
			this.isSelected = false;
		}
		this.isSelected = (state.getPlayer().getTargetId() == xpc.getId());
	}

	@Override
	public int getZOrder() {
		return 200;
	}
}
