package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.GroovyFilter;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.rsv.PersistentRsvLibrary;
import gg.xp.xivsupport.rsv.RsvEntry;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@ScanMe
public class RsvTableFactory {

	private final PicoContainer container;

	public RsvTableFactory(PicoContainer container) {
		this.container = container;
	}

	public TableWithFilterAndDetails<RsvEntry, Object> table() {
		TableWithFilterAndDetails<RsvEntry, Object> table = TableWithFilterAndDetails.builder("RSV Entries", () -> {
					List<RsvEntry> rsvEntries = new ArrayList<>(PersistentRsvLibrary.INSTANCE.dumpAll());
					rsvEntries.sort(Comparator.comparing(RsvEntry::numericId));
					return rsvEntries;
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("Language", rsv -> rsv.language().getShortCode(), c -> {
					c.setMinWidth(50);
					c.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Key", RsvEntry::key))
				.addMainColumn(new CustomColumn<>("Value", RsvEntry::value))
				.addFilter(t -> new TextBasedFilter<>(t, "Language", rsv -> rsv.language().getShortCode() + ' ' + rsv.language().name()))
				.addFilter(t -> new TextBasedFilter<>(t, "Key", RsvEntry::key))
				.addFilter(t -> new TextBasedFilter<>(t, "Value", RsvEntry::value))
				.addFilter(GroovyFilter.forClass(RsvEntry.class, container.getComponent(GroovyManager.class), "it"))
				.setFixedData(false)
				.build();
		table.getMainTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		return table;
	}
}
