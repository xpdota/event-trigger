package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.triggers.jobs.DotRefreshReminders;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.persistence.PersistenceProvider;
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
import java.util.stream.Collectors;

@ScanMe
public class DotTrackerOverlay extends XivOverlay {

	private static final Logger log = LoggerFactory.getLogger(DotTrackerOverlay.class);

	// TODO make this a setting
	private static final int MAX_DOTS = 8;

	private final DotRefreshReminders dots;
	private final CustomTableModel<VisualDotInfo> tableModel;
	private volatile List<BuffApplied> currentDots = Collections.emptyList();
	private volatile List<VisualDotInfo> croppedDots = Collections.emptyList();


	public DotTrackerOverlay(PersistenceProvider persistence, DotRefreshReminders dots) {
		super("Dot Tracker", "dot-tracker.overlay", persistence);
		this.dots = dots;
		tableModel = CustomTableModel.builder(() -> croppedDots)
				.addColumn(new CustomColumn<>("Icon", c -> c.getEvent().getBuff(), c -> {
					c.setCellRenderer(new ActionAndStatusRenderer(true));
					c.setMaxWidth(22);
					c.setMinWidth(22);
				}))
				.addColumn(new CustomColumn<>("Bar", c -> c,
						c -> {
							c.setCellRenderer(new DotBarRenderer());
							c.setMaxWidth(150);
							c.setMinWidth(150);
						}))
				.build();
		getPanel().setPreferredSize(new Dimension(200, 200));
		JTable table = new JTable(tableModel);
		table.setOpaque(false);
		tableModel.configureColumns(table);
		getPanel().add(table);
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					refresh();
					Thread.sleep(100);
				}
				catch (Throwable e) {
					log.error("Error refreshing dots", e);
				}
			}
		});
		thread.setName("DotRefreshOverlayThread");
		thread.start();
	}


	private void getAndSort() {
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
								out.add(new VisualDotInfo(v.get(0)));
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
					.limit(MAX_DOTS)
					.collect(Collectors.toList());

//			croppedDots = currentDots.stream()
//					.sorted(Comparator.<BuffApplied, Long>comparing(event -> event.getTarget().getHp() != null ? event.getTarget().getHp().getMax() : 0)
//							.reversed()
//							.thenComparing(event -> event.getTarget().getId())).map(VisualDotInfo::new)
//					.limit(MAX_DOTS)
//					.collect(Collectors.toList());
		}
	}


	private void refresh() {
		getAndSort();
		SwingUtilities.invokeLater(tableModel::fullRefresh);
	}
}
