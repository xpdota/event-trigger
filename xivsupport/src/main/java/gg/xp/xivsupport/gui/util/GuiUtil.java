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

	/**
	 * Format a container, adding the given components, in a manner such that the components
	 * are centered in the container, as far up in the container as they can go, and left-justified.
	 *
	 * @param outer      The container to format
	 * @param components The components to add
	 */
	public static void simpleTopDownLayout(Container outer, Component... components) {
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 5);
		JPanel container = new JPanel();

		simpleLayoutInternal(container, c, components);

		outer.setLayout(new BorderLayout());
		outer.getInsets().set(10, 10, 10, 10);
		outer.add(container, BorderLayout.CENTER);
	}

	/**
	 * Format a container, adding the given components, in a manner such that the components
	 * are centered in the container, as far up in the container as they can go, and left-justified.
	 *
	 * @param outer      The container to format
	 * @param width      The max width of the inner container
	 * @param components The components to add
	 */
	public static void simpleTopDownLayout(Container outer, int width, Component... components) {
		JPanel container;
		{
			GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 5);
			container = new JPanel() {

				private Dimension getParentSpace() {
					Dimension outerSz = outer.getSize();
					Insets outerInsets = outer.getInsets();
					return new Dimension(outerSz.width - outerInsets.left - outerInsets.right, outerSz.height - outerInsets.top - outerInsets.bottom);
				}

				@Override
				public Dimension getPreferredSize() {
					Dimension sup = super.getPreferredSize();
					return new Dimension(Math.min(Math.min(width, getParentSpace().width), sup.width), sup.height);
				}

				//
				@Override
				public Dimension getMinimumSize() {
					Dimension sup = super.getMinimumSize();
					return new Dimension(getParentSpace().width, sup.height);
				}

				//
				@Override
				public Dimension getMaximumSize() {
					Dimension sup = super.getMaximumSize();
					return new Dimension(Math.min(width, sup.width), sup.height);
				}
			};

			simpleLayoutInternal(container, c, components);
		}

		{

			outer.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, outer.getInsets(), 0, 0);
			c.weightx = 1;
			outer.add(Box.createHorizontalGlue(), c);
			c.gridx++;
			c.weightx = 0.001;
			outer.add(container, c);
			c.gridx++;
			c.weightx = 1;
			outer.add(Box.createHorizontalGlue(), c);
		}

	}

	private static void simpleLayoutInternal(JPanel container, GridBagConstraints c, Component[] components) {
		container.setLayout(new GridBagLayout());

		for (Component component : components) {
			container.add(component, c);
			c.gridy++;
		}

		c.weighty = 1;
		c.fill = GridBagConstraints.VERTICAL;
		container.add(Box.createVerticalGlue(), c);
	}
}
