package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.triggers.jobs.DotRefreshReminders;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class DotTrackerOverlay extends XivOverlay {

	private static final Logger log = LoggerFactory.getLogger(DotTrackerOverlay.class);

	private final IntSetting numberOfRows;

	private final DotRefreshReminders dots;
	private final CustomTableModel<VisualDotInfo> tableModel;
	private final JTable table;
	private volatile List<BuffApplied> currentDots = Collections.emptyList();
	private volatile List<VisualDotInfo> croppedDots = Collections.emptyList();

	private static final int BAR_WIDTH = 150;


	public DotTrackerOverlay(PersistenceProvider persistence, DotRefreshReminders dots) {
		super("Dot Tracker", "dot-tracker.overlay", persistence);
		this.numberOfRows = dots.getNumberToDisplay();
		numberOfRows.addListener(this::repackSize);
		this.dots = dots;
		tableModel = CustomTableModel.builder(() -> croppedDots)
				.addColumn(new CustomColumn<>("Icon", c -> c.getEvent().getBuff(), c -> {
					c.setCellRenderer(new ActionAndStatusRenderer(true, false, false));
					c.setMaxWidth(20);
					c.setMinWidth(20);
				}))
				.addColumn(new CustomColumn<>("Bar", Function.identity(),
						c -> {
							c.setCellRenderer(new DotBarRenderer());
							c.setMaxWidth(BAR_WIDTH);
							c.setMinWidth(BAR_WIDTH);
						}))
				.build();
//		getPanel().setPreferredSize();
		table = new JTable(tableModel);
//		table.getPreferredSize();
		table.setOpaque(false);
		tableModel.configureColumns(table);
		getPanel().add(table);
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					refresh();
					// 200ms is the optimal update interval at 100% scale - typical dot is 30 seconds, and the bar is
					// 150 pixels wide, so we'd expect 5 updates per second. But then we also need to adjust by the
					// scale factor.
					//noinspection BusyWait
					Thread.sleep((long) (200.0 / getScale()));
				}
				catch (Throwable e) {
					log.error("Error refreshing dots", e);
				}
			}
		});
		thread.setName("DotRefreshOverlayThread");
		repackSize();
		//noinspection CallToThreadStartDuringObjectConstruction
		thread.start();
	}

	private void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows.get()));
		getFrame().revalidate();
		redoScale();
	}


	private void getAndSort() {
		if (!getEnabled().get()) {
			croppedDots = Collections.emptyList();
			return;
		}
		List<BuffApplied> newCurrentDots = dots.getCurrentDots();
		if (!newCurrentDots.equals(currentDots)) {
			if (newCurrentDots.isEmpty()) {
				currentDots = Collections.emptyList();
			}
			currentDots = newCurrentDots;
			// TODO: make limit configurable
			Map<Long, List<BuffApplied>> dotsForBuffId = newCurrentDots
					.stream()
					.collect(Collectors.groupingBy(dot -> dot.getBuff().getId()));
			List<VisualDotInfo> out = new ArrayList<>();
			dotsForBuffId.forEach((unused, buffs) -> {
				MutableLong ml = new MutableLong(buffs.get(0).getEstimatedRemainingDuration().toMillis());
				buffs.stream()
						.sorted(Comparator.comparing(b -> b.getEstimatedRemainingDuration().toMillis()))
						.collect(Collectors.groupingBy(b -> {
							long est = b.getEstimatedRemainingDuration().toMillis();
							long lastValue = ml.longValue();
							if (Math.abs(est - lastValue) < 500) {
								return lastValue;
							}
							else {
								return est;
							}
						}))
						.forEach((k, v) -> {
							if (v.size() == 1) {
								BuffApplied thisBuff = v.get(0);
								if (thisBuff.getTarget().isThePlayer()) {
									out.add(new VisualDotInfo(thisBuff, thisBuff.getBuff().getName()));
								}
								else {
									out.add(new VisualDotInfo(thisBuff));
								}
							}
							else {
								out.add(new VisualDotInfo(v.get(0), String.format("%s Targets", v.size())));
							}
						});
				buffs.sort(Comparator.comparing(BuffApplied::getEstimatedRemainingDuration));
			});

			croppedDots = out.stream()
					.sorted(Comparator.<VisualDotInfo, Long>comparing(event -> event.getEvent().getTarget().getHp() != null ? event.getEvent().getTarget().getHp().getMax() : 0)
							.reversed()
							.thenComparing(event -> event.getEvent().getTarget().getId()))
					.limit(numberOfRows.get())
					.collect(Collectors.toList());
		}
	}


	private void refresh() {
		getAndSort();
		tableModel.fullRefresh();
//		tableModel.overlayHackRefresh();
//		getFrame().paint(getFrame().getGraphics());
//		RepaintManager repaintManager = RepaintManager.currentManager(table);
//		repaintManager.addDirtyRegion(table, 0, 0, table.getSize().width, table.getSize().height);
//		repaintManager.paintDirtyRegions();
//		table.paintImmediately(table.getBounds());
//		table.repaint();
	}
}
