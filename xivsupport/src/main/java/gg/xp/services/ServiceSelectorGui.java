package gg.xp.services;

import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ServiceSelectorGui {

	private static final Logger log = LoggerFactory.getLogger(ServiceSelectorGui.class);

	private final JComboBox<HasFriendlyName> comboBox;
	private final JLabel label;
	private final ServiceSelector setting;
//	private volatile boolean ignoreChange;

	public ServiceSelectorGui(ServiceSelector setting, String label, Supplier<Boolean> enabled, boolean listen) {
		this.setting = setting;

		comboBox = new JComboBox<>() {
			@Override
			public boolean isEnabled() {
				return enabled.get();
			}
		};
		comboBox.setModel(new ComboBoxModel<>() {

			private final HasFriendlyName defaultItem = () -> "Default: %s".formatted(setting.defaultOption().getFriendlyName());

			private List<HasFriendlyName> makeList() {
				List<HasFriendlyName> items = new ArrayList<>();
				items.add(defaultItem);
				items.addAll(setting.getOptions());
				return items;
			}

			@Override
			public void setSelectedItem(Object item) {
				if (item == defaultItem) {
					setting.setCurrent(null);
				}
				else if (item instanceof ServiceHandle handle) {
					handle.setEnabled();
				}
				else {
					log.warn("Unknown item: {}", item);
				}
			}

			@Override
			public Object getSelectedItem() {
				ServiceHandle selection = setting.getExplicitlySelectedOption();
				if (selection == null) {
					return defaultItem;
				}
				else {
					return selection;
				}
			}

			@Override
			public int getSize() {
				return makeList().size();
			}

			@Override
			public HasFriendlyName getElementAt(int index) {
				return makeList().get(index);
			}

			@Override
			public void addListDataListener(ListDataListener l) {
				// TODO: is this needed?
			}

			@Override
			public void removeListDataListener(ListDataListener l) {
				// TODO: is this needed?
			}
		});
		comboBox.setRenderer(new FriendlyNameListCellRenderer());
//		comboBox.setSelectedItem(setting.get());
//		comboBox.addItemListener(event -> {
//			log.info("{} {}", event.getStateChange(), event.getItem());
//			if (event.getStateChange() == ItemEvent.SELECTED) {
//				ignoreChange = true;
//				setting.set((X) event.getItem());
//				ignoreChange = false;
//			}
//		});
		if (listen) {
			setting.addListener(() -> {
				comboBox.repaint();
//				log.info("{} {}", ignoreChange, setting.get());
//				if (!ignoreChange) {
//					comboBox.setSelectedItem(setting.get());
//				}
			});
		}
		comboBox.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, this::reset));
		this.label = new JLabel(label);
		this.label.setLabelFor(comboBox);
	}

	private void reset() {
		setting.delete();
		comboBox.repaint();
	}

	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel.add(label);
		panel.add(comboBox);
		return panel;
	}

	public JComboBox<HasFriendlyName> getComboBoxOnly() {
		return comboBox;
	}

}
