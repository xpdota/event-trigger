package gg.xp.xivsupport.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KeyValueDisplaySet extends JPanel implements Refreshable {
	private static final Logger log = LoggerFactory.getLogger(KeyValueDisplaySet.class);

	private final List<KeyValuePairDisplay<?, ?>> displayed;

	public KeyValueDisplaySet(List<KeyValuePairDisplay<?, ?>> keyValues) {
		super();
		setLayout(new GridBagLayout());
		displayed = new ArrayList<>(keyValues);
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 2;
		c.ipady = 2;
		c.gridy = 0;
		c.gridx = 0;
		for (KeyValuePairDisplay<?, ?> d : displayed) {
			c.gridx = 0;
			c.weightx = 0.1;
			add(Box.createHorizontalGlue(), c);
			add(d.getLabel(), c);
			add(Box.createHorizontalGlue(), c);
			c.weightx = 1;
			c.gridx = 1;
			Component component = d.getComponent();
			add(component, c);
			c.gridy++;
			c.weightx = 0.1;
			add(Box.createHorizontalGlue(), c);
		}
		refresh();
		this.setMaximumSize(getPreferredSize());
		this.setMinimumSize(getPreferredSize());
		this.setSize(getPreferredSize());
		this.repaint();
	}

	@Override
	public void refresh() {
		log.trace("Refreshing");
		displayed.forEach(KeyValuePairDisplay::refresh);
	}

}
