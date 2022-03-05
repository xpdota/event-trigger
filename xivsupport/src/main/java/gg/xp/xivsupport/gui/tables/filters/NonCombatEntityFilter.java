package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class NonCombatEntityFilter implements VisualFilter<XivCombatant> {

	private static final Logger log = LoggerFactory.getLogger(NonCombatEntityFilter.class);

	private final JCheckBox checkBox;
	private final Runnable filterUpdatedCallback;
	private boolean selectedItem;

	public NonCombatEntityFilter(Runnable filterUpdatedCallback) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		checkBox = new JCheckBox();
		checkBox.addItemListener(i -> {
			update();
		});
	}

	private void update() {
		selectedItem = checkBox.isSelected();
		filterUpdatedCallback.run();
	}

	@Override
	public boolean passesFilter(XivCombatant item) {
		if (selectedItem) {
			return true;
		}
		return item.isCombative();
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		checkBox.setText("Show Non-Combat Entities");
		panel.add(checkBox);
		return panel;
	}
}
