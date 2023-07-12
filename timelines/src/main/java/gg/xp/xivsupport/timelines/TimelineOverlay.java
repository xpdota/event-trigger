package gg.xp.xivsupport.timelines;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.OverlayMain;
import gg.xp.xivsupport.gui.overlay.RefreshType;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.timelines.gui.TimelineBarColorProvider;
import gg.xp.xivsupport.timelines.gui.TimelineBarRenderer;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class TimelineOverlay extends XivOverlay {
	private static final Logger log = LoggerFactory.getLogger(TimelineOverlay.class);
	private final TimelineManager timeline;
	private volatile List<VisualTimelineEntry> current = Collections.emptyList();
	private final IntSetting numberOfRows;
	private final IntSetting barWidth;
	private final CustomTableModel<VisualTimelineEntry> tableModel;
	private static final int BAR_WIDTH = 150;
	private final JTable table;

	public TimelineOverlay(PersistenceProvider persistence, TimelineManager timeline, OverlayConfig oc, TimelineBarColorProvider tbcp) {
		super("Timeline", "timeline-overlay", oc, persistence);
		log.info("Start");
		// TODO: fix the timer getting truncated. Just left-justify text and right-justify timer like on cactbot
		this.timeline = timeline;
		this.numberOfRows = timeline.getRowsToDisplay();
		numberOfRows.addListener(this::repackSize);
		tableModel = CustomTableModel.builder(() -> current)
				.addColumn(new CustomColumn<>("Bar", Function.identity(),
						c -> {
							c.setCellRenderer(new TimelineBarRenderer(tbcp));
//							c.setMaxWidth(BAR_WIDTH);
//							c.setMinWidth(BAR_WIDTH);
						}))
				.build();
		table = new JTable(tableModel);
		table.setOpaque(false);
		tableModel.configureColumns(table);
		table.setCellSelectionEnabled(false);
		barWidth = timeline.getBarWidth();
		barWidth.addListener(this::repackSize);
		getPanel().add(table);
	}

	@Override
	public void finishInit() {
		super.finishInit();
		repackSize();
		RefreshLoop<TimelineOverlay> refresher = new RefreshLoop<>("TimelineOverlay", this, TimelineOverlay::refresh, dt -> dt.calculateScaledFrameTime(200));
		refresher.start();
	}

	@Override
	protected void repackSize() {
		table.setPreferredSize(new Dimension(barWidth.get(), table.getRowHeight() * numberOfRows.get()));
		super.repackSize();
	}

	private RefreshType getAndSort() {
		if (!getEnabled().get()) {
			current = Collections.emptyList();
			return RefreshType.NONE;
		}
		current = timeline.getCurrentDisplayEntries().stream().limit(numberOfRows.get()).collect(Collectors.toList());
		return RefreshType.FULL;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			refresh();
		}
	}

	private void refresh() {
		if (isVisible()) {
			RefreshType refreshTypeNeeded = getAndSort();
			switch (refreshTypeNeeded) {
				case FULL -> tableModel.fullRefresh();
				case REPAINT -> table.repaint();
			}
		}
	}
}
