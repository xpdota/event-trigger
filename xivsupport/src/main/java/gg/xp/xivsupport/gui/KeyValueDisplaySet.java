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
		int row = 0;
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 2;
		c.ipady = 2;
		for (KeyValuePairDisplay<?, ?> d : displayed) {
			c.gridx = 0;
			c.gridy = row++;
			add(d.getLabel(), c);
			c.gridx = 1;
			add(d.getComponent(), c);
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
