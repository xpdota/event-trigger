package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public final class EventEntityFilter<I, X> implements VisualFilter<I> {

	private static final Logger log = LoggerFactory.getLogger(EventEntityFilter.class);

	private final JComboBox<String> comboBox;
	private final Class<X> expectedClass;
	private final Function<X, XivCombatant> entityGetter;
	private final String labelText;
	// TODO: really shouldn't be a string
	private String selectedItem;

	private static final String ALL = "All (Including None)";
	private static final String ANY = "Any (Excluding None)";
	private static final String PLAYERS = "Players";
	private static final String NPCS = "NPCs";
	private static final String SELF = "Self";
	private static final String ENVIRONMENT = "Environment";
	private static final String NONE = "None (Non-Targeted Event)";

	public static EventEntityFilter<Event, HasSourceEntity> eventSourceFilter(Runnable filterUpdatedCallback) {
		return new EventEntityFilter<>(HasSourceEntity.class, HasSourceEntity::getSource, filterUpdatedCallback, "Source Entity");
	}

	public static EventEntityFilter<Event, HasTargetEntity> eventTargetFilter(Runnable filterUpdatedCallback) {
		return new EventEntityFilter<>(HasTargetEntity.class, HasTargetEntity::getTarget, filterUpdatedCallback, "Target Entity");
	}

	public static EventEntityFilter<BuffApplied, HasSourceEntity> buffSourceFilter(Runnable filterUpdatedCallback) {
		return new EventEntityFilter<>(HasSourceEntity.class, HasSourceEntity::getSource, filterUpdatedCallback, "Source Entity");
	}

	public static EventEntityFilter<BuffApplied, HasTargetEntity> buffTargetFilter(Runnable filterUpdatedCallback) {
		return new EventEntityFilter<>(HasTargetEntity.class, HasTargetEntity::getTarget, filterUpdatedCallback, "Target Entity");
	}

	public static EventEntityFilter<XivCombatant, XivCombatant> selfFilter(Runnable filterUpdatedCallback) {
		return new EventEntityFilter<>(XivCombatant.class, Function.identity(), filterUpdatedCallback, "Entity");
	}

	private EventEntityFilter(Class<X> expectedClass, Function<X, XivCombatant> entityGetter, Runnable filterUpdatedCallback, String label) {
		this.expectedClass = expectedClass;
		this.entityGetter = entityGetter;
		this.labelText = label;
		comboBox = new JComboBox<>();
		comboBox.setEditable(true);
		comboBox.addItem(ALL);
		comboBox.addItem(ANY);
		comboBox.addItem(SELF);
		comboBox.addItem(PLAYERS);
		comboBox.addItem(NPCS);
		comboBox.addItem(ENVIRONMENT);
		comboBox.addItem(NONE);
//		comboBox.addActionListener(event -> {
//			log.info("Combo Box Event: {}", comboBox);
//
//		});
		comboBox.addItemListener(event -> {
			log.info("Combo Box Event: {}", event);
			selectedItem = (String) comboBox.getSelectedItem();
			filterUpdatedCallback.run();
		});
		selectedItem = (String) comboBox.getSelectedItem();

	}

	@Override
	public boolean passesFilter(I item) {
		// TODO: computing a single lambda once when we change filters is probably faster?
		switch (selectedItem) {
			case ALL:
				return true;
			case ANY:
				return (expectedClass.isInstance(item));
			case NONE:
				return !(expectedClass.isInstance(item));
			case PLAYERS:
				if (expectedClass.isInstance(item)) {
					return entityGetter.apply(expectedClass.cast(item)).isPc();
				}
				return false;
			case NPCS:
				if (expectedClass.isInstance(item)) {
					return !entityGetter.apply(expectedClass.cast(item)).isPc();
				}
				return false;
			case ENVIRONMENT:
				if (expectedClass.isInstance(item)) {
					return entityGetter.apply(expectedClass.cast(item)).isEnvironment();
				}
				return false;
			case SELF:
				if (expectedClass.isInstance(item)) {
					return entityGetter.apply(expectedClass.cast(item)).isThePlayer();
				}
				return false;
			default:
				if (expectedClass.isInstance(item)) {
					XivCombatant source = entityGetter.apply(expectedClass.cast(item));
					// TODO: regex
					// Treat as hex
					return source.matchesFilter(selectedItem);
//					if (selectedItem.startsWith("0x")) {
//						String wantedId = selectedItem.substring(2);
//						String actualId = Long.toString(source.getId(), 16);
//						return wantedId.equalsIgnoreCase(actualId);
//					}
//					// Treat as partial match
//					return source.getName().toUpperCase(Locale.ROOT).contains(selectedItem.toUpperCase());
				}
				return false;
		}
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel(labelText + ": ");
		label.setLabelFor(comboBox);
		panel.add(label);
		panel.add(comboBox);
		return panel;
	}
}
