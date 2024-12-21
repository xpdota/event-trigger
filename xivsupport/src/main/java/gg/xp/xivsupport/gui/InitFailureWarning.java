package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.AutoScan;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.InitException;
import gg.xp.reevent.scan.JarLoadException;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.Platform;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

@ScanMe
public class InitFailureWarning {

	private final AutoScan scan;

	public InitFailureWarning(AutoScan scan) {
		this.scan = scan;
	}

	@HandleEvents(order = 99999)
	public void init(InitEvent e) {
		List<InitException> failures = scan.getFailures();
		if (!failures.isEmpty()) {
			SwingUtilities.invokeLater(() -> {
				JFrame frame = new JFrame("Startup Issues!");
				frame.setLocationByPlatform(true);
				JPanel panel = new JPanel();
				panel.setBorder(new LineBorder(Color.RED));
				panel.setPreferredSize(new Dimension(800, 600));
				panel.setLayout(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.CENTER;
				c.gridx = 0;
				c.gridy = 0;
				c.weightx = 1;
				c.weighty = 0;
				panel.setAlignmentX(0.5f);
				panel.add(new JLabel("One or more components failed to initialize."), c);
				c.gridy++;
				c.weighty = 1;
				c.fill = GridBagConstraints.BOTH;
				JTextArea textArea = new JTextArea();
				StringBuilder text = new StringBuilder("One or more components failed to initialize. You may continue, but some functionality may be missing or broken.\n\n");
				text.append("Failed components:\n");
				failures.forEach(f -> {
					text.append(" - '");
					text.append(f.describeFailedComponent());
					text.append("': ");
					if (f instanceof JarLoadException) {
						text.append("JAR failed to load");
					}
					else {
						text.append("Component failed to initialize");
					}
					text.append('\n');
				});
				text.append("\nDetails: \n\n");
				failures.forEach(f -> {
					if (text.length() > 20_000) {
						return;
					}
					text.append(f.describeFailedComponent()).append(":\n");
					text.append(ExceptionUtils.getStackTrace(f.getCause()));
					text.append('\n');
				});
				String textFinal = text.toString();
				textArea.setText(textFinal);

				textArea.setEditable(false);
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(true);
				textArea.setCaretPosition(0);
				JScrollPane scroll = new JScrollPane(textArea);
				scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				panel.add(scroll, c);
				c.weighty = 0;
				c.gridy++;
				c.fill = GridBagConstraints.NONE;
				JButton exit = new JButton("Exit");
				exit.addActionListener(l -> System.exit(1));
				JButton tryContinue = new JButton("Continue");
				tryContinue.addActionListener(l -> {
					frame.setVisible(false);
					frame.dispose();
				});
				JPanel buttons = new JPanel(new WrapLayout(WrapLayout.CENTER));
				buttons.add(exit);
				buttons.add(tryContinue);
				JButton copy = new JButton("Copy Error Message");
				copy.addActionListener(l -> {
					GuiUtil.copyTextToClipboard(textFinal);
				});
				buttons.add(copy);
//			buttons.add(tryContinue);
				c.fill = GridBagConstraints.HORIZONTAL;
				panel.add(buttons, c);
				frame.add(panel);
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.validate();
				frame.pack();
				frame.setVisible(true);
			});
		}
	}

}
