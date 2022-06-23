package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ComponentListStretchyRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

public class CdBarRenderer extends ResourceBarRenderer<VisualCdInfo> {


	private final ComponentListStretchyRenderer componentListStretchyRenderer = new ComponentListStretchyRenderer(0);
	private final CdColorProvider colors;

	public CdBarRenderer(CdColorProvider colors) {
		super(VisualCdInfo.class);
		this.colors = colors;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof VisualCdInfo vci) {
			if (vci.useChargeDisplay()) {
				List<Supplier<Component>> components = vci.makeChargeInfo()
						.stream()
						.map(subVci -> (Supplier<Component>) () -> super.getTableCellRendererComponent(table, subVci, isSelected, hasFocus, row, column))
						.toList();
				componentListStretchyRenderer.setComponents(components);
				return componentListStretchyRenderer;
			}
		}
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	@Override
	protected void formatLabel(@NotNull VisualCdInfo item) {
		bar.setTextOptions(item.getLabel());
	}

	@Override
	protected Color getBarColor(double percent, @NotNull VisualCdInfo item) {
		if (item.getBuffApplied() != null) {
			return colors.getActiveColor();
		}
		if (percent > 0.999d) {
			return colors.getReadyColor();
		}
		else {
			return colors.getOnCdColor();
		}

	}
}
