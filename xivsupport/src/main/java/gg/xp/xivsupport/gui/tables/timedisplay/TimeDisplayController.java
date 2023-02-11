package gg.xp.xivsupport.gui.tables.timedisplay;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.AbilityResolutionFilter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class TimeDisplayController {

	private static final Logger log = LoggerFactory.getLogger(TimeDisplayController.class);

	private TimeDisplayOption displayMode = TimeDisplayOption.LOCAL_TIME;
	private Supplier<@Nullable Event> selectionSupplier = () -> null;
	private final DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private Runnable updateCallback = () -> {
	};
	private volatile @Nullable Event currentSelection;

	public Duration offsetForEvent(@NotNull Event other) {
		Event basis = currentSelection;
		if (basis == null) {
			return null;
		}
		else {
			return Duration.between(basis.getEffectiveHappenedAt(), other.getEffectiveHappenedAt());
		}
	}

	public void updateSelection() {
		currentSelection = selectionSupplier.get();
		updateCallback.run();
	}

	public void setDisplayMode(TimeDisplayOption currentSetting) {
		this.displayMode = currentSetting;
		updateCallback.run();
	}

	public String formatForEvent(Event e) {
		switch (displayMode) {
			case RELATIVE_TO_SELECTION:
				Duration offset = offsetForEvent(e);
				if (offset != null) {
					return formatOffset(offset);
				}
		}
		return formatLocalTime(e);
	}

	private String formatOffset(Duration d) {
		long delta = d.toMillis();
		boolean isPositive;
		if (delta < 0) {
			isPositive = false;
		}
		else if (delta > 0) {
			isPositive = true;
		}
		else {
			return " --";
		}
		Duration abs = d.abs();
		// space and minus is the same width as plus...for the specific font....this is janky
		return String.format("%s%d:%02d.%03d", isPositive ? "+" : " -", abs.toMinutes(), abs.toSecondsPart(), abs.toMillisPart());
	}

	private String formatLocalTime(Event e) {
		return e.getEffectiveHappenedAt()
				.atZone(ZoneId.systemDefault())
				.format(format);
	}

	public void setSelectionSupplier(Supplier<@Nullable Event> selectionSupplier) {
		this.selectionSupplier = selectionSupplier;
	}

	public void setUpdateCallback(Runnable updateCallback) {
		this.updateCallback = updateCallback;
	}

	public Component configureWidget(TableWithFilterAndDetails<Event, ?> table) {
		setUpdateCallback(() -> SwingUtilities.invokeLater(table::repaint));
		setSelectionSupplier(table::getCurrentSelection);
		table.getMainTable().getSelectionModel().addListSelectionListener(l -> SwingUtilities.invokeLater(() -> updateSelection()));
		JComboBox<TimeDisplayOption> comboBox = new JComboBox<>(TimeDisplayOption.values());
		comboBox.setRenderer(new FriendlyNameListCellRenderer());
		comboBox.addItemListener(event -> {
			setDisplayMode((TimeDisplayOption) event.getItem());
		});
		return comboBox;
	}

	public CustomColumn<? super Event> getColumnDef() {
		return new CustomColumn<>("Time", this::formatForEvent, col -> {
			col.setMaxWidth(100);
		});
	}
}
