package gg.xp.xivsupport.gui.util;

import gg.xp.xivsupport.persistence.Platform;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class CatchFatalError {

	private static final Logger log = LoggerFactory.getLogger(CatchFatalError.class);

	private CatchFatalError() {
	}

	public static void run(Runnable run) {
		try {
			run.run();
		}
		catch (Throwable e) {
			log.error("Fatal Error!", e);
			JFrame frame = new JFrame("Startup Error!");
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
			panel.add(new JLabel("A Fatal Error Has Occurred"), c);
			c.gridy++;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			JTextArea textArea = new JTextArea();
			textArea.setText("You should report this as a bug and include log files in " +
					Platform.getTriggeventDir() +
					" as well as this error message." +
					"\n\n" +
					"You can also try moving/renaming the properties files in that directory to see if this error is being caused by a problem with your settings." +
					"\n\n" +
					ExceptionUtils.getStackTrace(e)
			);
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
			panel.add(exit, c);
			frame.add(panel);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.validate();
			frame.pack();
			frame.setVisible(true);

		}

	}

}
