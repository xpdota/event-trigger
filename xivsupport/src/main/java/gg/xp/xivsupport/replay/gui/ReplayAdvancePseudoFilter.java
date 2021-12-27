package gg.xp.xivsupport.replay.gui;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.VisualFilter;
import gg.xp.xivsupport.replay.ReplayController;

import javax.swing.*;
import java.awt.*;

public class ReplayAdvancePseudoFilter<X extends Event> implements VisualFilter<X> {
	private final ReplayController replay;
	private final Class<X> clazz;
	private TableWithFilterAndDetails<X, ?> table;

	public ReplayAdvancePseudoFilter(Class<X> clazz, EventMaster master, ReplayController replay) {
		this.replay = replay;
		this.clazz = clazz;
		EventDistributor dist = master.getDistributor();
		dist.registerHandler(clazz, (c, e) -> {
			if (eventPassesTableFilter(e)) {
				isPlaying = false;
			}
		});
	}

	public void setTable(TableWithFilterAndDetails<X, ?> table) {
		this.table = table;
	}

	private volatile boolean isPlaying;

	@Override
	public boolean passesFilter(X item) {
		return true;
	}

	private boolean eventPassesTableFilter(X event) {
		if (table == null) {
			return false;
		}
		if (clazz.isInstance(event)) {
			return table.passesFilters(event);
		}
		return false;
	}

	@Override
	public Component getComponent() {
		JButton theButton = new JButton("Play to Next Matching");
		theButton.addActionListener(l -> {
			isPlaying = true;
			replay.advanceByAsyncWhile(() -> isPlaying);
		});
		return theButton;
	}
}
