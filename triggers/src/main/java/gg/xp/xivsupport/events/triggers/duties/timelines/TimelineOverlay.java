package gg.xp.xivsupport.events.triggers.duties.timelines;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.jobs.gui.TimelineBarRenderer;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.IntSetting;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class TimelineOverlay extends XivOverlay {
	private final TimelineManager timeline;
	private volatile List<VisualTimelineEntry> current = Collections.emptyList();
	private final IntSetting numberOfRows;
	private final CustomTableModel<VisualTimelineEntry> tableModel;
	private static final int BAR_WIDTH = 150;
	private final JTable table;

	public TimelineOverlay(PersistenceProvider persistence, TimelineManager timeline) {
		super("Timeline", "timeline-overlay", persistence);
		this.timeline = timeline;
		this.numberOfRows = timeline.getRowsToDisplay();
		numberOfRows.addListener(this::repackSize);
		tableModel = CustomTableModel.builder(() -> current)
				.addColumn(new CustomColumn<>("Bar", Function.identity(),
						c -> {
							c.setCellRenderer(new TimelineBarRenderer());
							c.setMaxWidth(BAR_WIDTH);
							c.setMinWidth(BAR_WIDTH);
						}))
				.build();
		table = new JTable(tableModel);
		table.setOpaque(false);
		tableModel.configureColumns(table);
		table.setCellSelectionEnabled(false);
		getPanel().add(table);
		RefreshLoop<TimelineOverlay> refresher = new RefreshLoop<>("TimelineOverlay", this, TimelineOverlay::refresh, dt -> (long) (60.0 / dt.getScale()));
		repackSize();
		refresher.start();

	}

	@Override
	protected void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows.get()));
		super.repackSize();
	}

	private void getAndSort() {
		if (!getEnabled().get()) {
			current = Collections.emptyList();
			return;
		}
		current = timeline.getCurrentDisplayEntries().stream().limit(numberOfRows.get()).collect(Collectors.toList());
	}

	private void refresh() {
		getAndSort();
		tableModel.fullRefresh();
	}
}
