package gg.xp.xivsupport.replay.gui;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.replay.ReplayController;

import javax.swing.*;
import java.awt.*;

public class ReplayAdvancePseudoFilter<X extends Event> {
	private final ReplayController replay;
	private final Class<X> clazz;
	private final TableWithFilterAndDetails<X, ?> table;
	private volatile boolean isPlaying;

	public ReplayAdvancePseudoFilter(Class<X> clazz, EventMaster master, ReplayController replay, TableWithFilterAndDetails<X, ?> table) {
		this.replay = replay;
		this.clazz = clazz;
		this.table = table;
		EventDistributor dist = master.getDistributor();
		dist.registerHandler(clazz, (c, e) -> {
			if (isPlaying && eventPassesTableFilter(e)) {
				isPlaying = false;
			}
		});
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

	public Component getComponent() {
		JButton theButton = new JButton("Play to Next Matching");
		theButton.addActionListener(l -> {
			boolean isCurrentlyPlaying = isPlaying;
			isPlaying = !isCurrentlyPlaying;
			if (isPlaying) {
				replay.advanceByAsyncWhile(() -> isPlaying);
			}
		});
		return theButton;
	}
}
