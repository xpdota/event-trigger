package gg.xp.xivsupport.gui.util;

import gg.xp.xivsupport.gui.WrapperPanel;
import gg.xp.xivsupport.gui.tables.filters.InputValidationState;
import gg.xp.xivsupport.gui.tables.filters.MultiLineTextAreaWithValidation;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

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
			log.error("Error opening file '{}'", file, e);
			throw new RuntimeException(e);
		}
	}

	public static void openUrl(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		}
		catch (Throwable t) {
			log.error("Error opening url '{}'", url, t);
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

		simpleLayoutInternal(container, c, null, components);

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

			simpleLayoutInternal(container, c, width - 10, components);
		}

		{

			outer.setLayout(new GridBagLayout());
			outer.setMinimumSize(new Dimension(width, 1));
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

	private static void simpleLayoutInternal(JPanel container, GridBagConstraints c, @Nullable Integer strutWidth, Component[] components) {
		container.setLayout(new GridBagLayout());

		if (strutWidth != null) {
			container.add(Box.createHorizontalStrut(strutWidth), c);
//			container.add(Box.createHorizontalBox(), c);
			c.gridy++;
		}

		for (Component component : components) {
			container.add(component, c);
			c.gridy++;
		}

		c.weighty = 1;
		c.fill = GridBagConstraints.VERTICAL;
		container.add(Box.createVerticalGlue(), c);
	}

	public static <X> @Nullable X doImportDialog(String title, Function<String, X> converter) {
		Mutable<X> value = new MutableObject<>();
		JButton okButton = new JButton("Import");
		MultiLineTextAreaWithValidation<X> field = new MultiLineTextAreaWithValidation<>(converter, value::setValue, "", (vs -> okButton.setEnabled(vs == InputValidationState.VALID)));
		JButton cancelButton = new JButton("Cancel");
		field.setPreferredSize(null);
		field.setLineWrap(true);
		field.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(field);
		scrollPane.setPreferredSize(new Dimension(720, 480));
		JOptionPane opt = new JOptionPane(scrollPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new Object[]{okButton, cancelButton});
		okButton.addActionListener(l -> opt.setValue(JOptionPane.OK_OPTION));
		cancelButton.addActionListener(l -> opt.setValue(JOptionPane.CANCEL_OPTION));
		JDialog dialog = opt.createDialog(title);
		dialog.setVisible(true);
		Object dialogResult = opt.getValue();
		X theValue = value.getValue();
		if (dialogResult instanceof Integer dr && dr == JOptionPane.OK_OPTION && theValue != null) {
			return theValue;
		}
		else {
			return null;
		}
	}
}
