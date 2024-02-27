package gg.xp.xivsupport.gui.library;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.IdOrNameFilter;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ScanMe
public final class NpcYellTableFactory {

	private final RightClickOptionRepo rightClicks;

	public NpcYellTableFactory(RightClickOptionRepo rightClicks) {
		this.rightClicks = rightClicks;
	}

	public TableWithFilterAndDetails<NpcYellInfo, Object> table() {
		return TableWithFilterAndDetails.builder("NpcYell Entries", () -> {
					Map<Integer, NpcYellInfo> csvValues = NpcYellLibrary.INSTANCE.getAll();
					List<NpcYellInfo> values = new ArrayList<>(csvValues.values());
					values.sort(Comparator.comparing(NpcYellInfo::id));
					return values;
				}, unused -> Collections.emptyList())
				.addMainColumn(new CustomColumn<>("ID", v -> String.format("0x%X (%s)", v.id(), v.id()), col -> {
					col.setMinWidth(100);
					col.setMaxWidth(100);
				}))
				.addMainColumn(new CustomColumn<>("Text", NpcYellInfo::text, col -> {
					col.setPreferredWidth(200);
				}))
				.addFilter(t -> new IdOrNameFilter<>("Text/ID", item -> (long) item.id(), NpcYellInfo::text, t))
				// TODO: ability to create easy trigger from library
				.withRightClickRepo(rightClicks)
				.setFixedData(true)
				.build();
	}

	public @Nullable NpcYellInfo pickItem(Window owner) {
		return ChooserDialog.chooserReturnItem(owner, table());
	}

}
