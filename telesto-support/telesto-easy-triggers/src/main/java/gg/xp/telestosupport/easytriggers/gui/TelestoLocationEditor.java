package gg.xp.telestosupport.easytriggers.gui;


import gg.xp.telestosupport.easytriggers.TelestoLocation;
import gg.xp.telestosupport.easytriggers.TelestoLocationType;
import gg.xp.telestosupport.easytriggers.TelestoPositionRefType;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.gui.GroovySubScriptEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui.GenericFieldEditor;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TelestoLocationEditor extends JPanel {

	private final GroovySubScriptEditor scriptEditor;
	private final TelestoLocation location;
	private final JComboBox<TelestoPositionRefType> position;
	private final JComboBox<TelestoLocationType> dropdown;

	public TelestoLocationEditor(String labelText, TelestoLocation location, PicoContainer container) {
		this.location = location;
		JLabel label = new JLabel(labelText);
		// TODO: restrict to valid values
		dropdown = new JComboBox<>(TelestoLocationType.values());
		dropdown.setRenderer(new FriendlyNameListCellRenderer());
		dropdown.setSelectedItem(location.type);
		dropdown.addItemListener(l -> reprocess());
		position = new JComboBox<>(TelestoPositionRefType.values());
		position.setRenderer(new FriendlyNameListCellRenderer());
		position.setSelectedItem(location.usePosition ? TelestoPositionRefType.SNAPSHOT_POSITION : TelestoPositionRefType.FOLLOW_ENTITY);
		position.addItemListener(l -> reprocess());
		scriptEditor = new GroovySubScriptEditor(location.customExpression);

		setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		add(label);
		add(dropdown);
		add(position);
		add(scriptEditor);
		add(new GenericFieldEditor(location.offsets, container));
		reprocess();
	}

	private void reprocess() {
		TelestoLocationType type = (TelestoLocationType) dropdown.getSelectedItem();
		location.type = type;
		switch (type) {
			case SOURCE, TARGET, PLAYER -> scriptEditor.setVisible(false);
			case CUSTOM -> scriptEditor.setVisible(true);
		}
		location.usePosition = Objects.equals(position.getSelectedItem(), TelestoPositionRefType.SNAPSHOT_POSITION);
	}


}
