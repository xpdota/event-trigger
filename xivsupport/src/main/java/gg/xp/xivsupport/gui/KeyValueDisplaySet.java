package gg.xp.xivsupport.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class KeyValueDisplaySet extends JPanel implements Refreshable {
	private static final Logger log = LoggerFactory.getLogger(KeyValueDisplaySet.class);

	private final List<KeyValuePairDisplay<?, ?>> displayed;

	KeyValueDisplaySet(List<KeyValuePairDisplay<?, ?>> keyValues) {
		super();
		// TODO: remove
//			setLayout(new GridLayout(keyValues.size(), 2, 2, 2));
		setLayout(new GridBagLayout());
//		setBorder(new LineBorder(Color.GREEN));
		displayed = new ArrayList<>(keyValues);
		int row = 0;
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 2;
		c.ipady = 2;
		for (KeyValuePairDisplay<?, ?> d : displayed) {
//				c.gridwidth = d.getLabel().getPreferredSize().width;
//				c.gridwidth = 200;
			c.gridx = 0;
			c.gridy = row++;
			add(d.getLabel(), c);
//				c.gridwidth = d.getComponent().getPreferredSize().width;
//				c.gridwidth = 400;
			c.gridx = 1;
			add(d.getComponent(), c);
		}
		refresh();
		this.setMaximumSize(getPreferredSize());
		this.setMinimumSize(getPreferredSize());
		this.setSize(getPreferredSize());
		this.repaint();
//			setPreferredSize(new Dimension(maxWidth, displayed.get(0).getHeight()));

	}

	public void refresh() {
		log.trace("Refreshing");
		displayed.forEach(KeyValuePairDisplay::refresh);
	}

}
