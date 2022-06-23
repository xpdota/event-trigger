package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SingleFlatFileProvider;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.nio.file.Paths;

public class JavaPanel extends TitleBorderFullsizePanel {
	public JavaPanel() {
		super("Java");
		SingleFlatFileProvider provider = new SingleFlatFileProvider(Paths.get(Platform.getInstallDir().toString(), "args.txt").toFile());
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		{
			JLabel label = new JLabel("Java Command Line Options:");
			add(label, c);
			if (provider.exists()) {
				JTextField textbox = new JTextField();
				label.setLabelFor(textbox);
				textbox.setPreferredSize(new Dimension(textbox.getMaximumSize().width, textbox.getPreferredSize().height));
				textbox.setText(provider.read());
				textbox.getDocument().addDocumentListener(new DocumentListener() {
					// TODO: make a thing for these
					@Override
					public void insertUpdate(DocumentEvent e) {
						provider.write(textbox.getText());
					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						provider.write(textbox.getText());
					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						provider.write(textbox.getText());
					}
				});
				c.gridy++;
				c.gridwidth = 1;
				add(textbox, c);
				c.gridy++;
				c.gridwidth = GridBagConstraints.REMAINDER;
				JLabel label2 = new JLabel("WARNING: This can break things if you set bad values. These are stored in args.txt in your install dir.");
				add(label2, c);
			}
			else {
				JLabel bad = new JLabel("Unfortunately, it appears you have an old launcher version. Please download the latest release manually.");
				c.gridy++;
				add(bad, c);
			}

		}
		c.gridy++;
		c.weighty = 1;
		add(new JPanel(), c);
	}
}
