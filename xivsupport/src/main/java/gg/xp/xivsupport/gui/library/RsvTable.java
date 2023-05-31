package gg.xp.xivsupport.gui.library;

import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.TextBasedFilter;
import gg.xp.xivsupport.rsv.PersistentRsvLibrary;
import gg.xp.xivsupport.rsv.RsvEntry;

import java.util.Collections;

public final class RsvTable {

	private RsvTable() {
	}

	public static TableWithFilterAndDetails<RsvEntry, Object> table() {
		return TableWithFilterAndDetails.builder("RSV Entries", PersistentRsvLibrary.INSTANCE::dumpAll, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("Language", rsv -> rsv.language().getShortCode(), c -> {
					c.setMinWidth(50);
					c.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Key", RsvEntry::key))
				.addMainColumn(new CustomColumn<>("Value", RsvEntry::value))
				.addFilter(t -> new TextBasedFilter<>(t, "Language", rsv -> rsv.language().getShortCode() + ' ' + rsv.language().name()))
				.addFilter(t -> new TextBasedFilter<>(t, "Key", RsvEntry::key))
				.addFilter(t -> new TextBasedFilter<>(t, "Value", RsvEntry::value))
				.setFixedData(false)
				.build();
	}
}
