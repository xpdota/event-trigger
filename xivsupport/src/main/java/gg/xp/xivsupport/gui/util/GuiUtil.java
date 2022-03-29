package gg.xp.xivsupport.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

public final class GuiUtil {
	private static final Logger log = LoggerFactory.getLogger(GuiUtil.class);
	private GuiUtil() {
	}

	public static void copyTextToClipboard(String text) {
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	public static void openFile(File file) {
		try {
			Desktop.getDesktop().open(file);
		}
		catch (IOException e) {
			log.error("Error opening install dir", e);
			throw new RuntimeException(e);
		}

	}

	public static GridBagConstraints defaultGbc() {
		return new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
	}

	public static JLabel labelFor(String text, Component component) {
		JLabel label = new JLabel(text);
		label.setLabelFor(component);
		return label;
	}

	public static void copyToClipboard(String contents) {
		StringSelection stringSelection = new StringSelection(contents);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	public static void bringToFront(Component component) {
		if (component.isShowing()) {
			return;
		}
		Component current = component;
		Component parent = component.getParent();
		while (parent != null) {
			if (parent instanceof JTabbedPane pane) {
				pane.setSelectedComponent(current);
			}
			current = parent;
			parent = current.getParent();
		}
	}
}
