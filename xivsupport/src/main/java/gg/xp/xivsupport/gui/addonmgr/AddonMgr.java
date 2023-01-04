package gg.xp.xivsupport.gui.addonmgr;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.AutoHeightScalingIcon;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.MultiLineVerticalCenteredTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;
import gg.xp.xivsupport.gui.tabs.AddonDef;
import gg.xp.xivsupport.gui.tabs.UpdaterConfig;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ScanMe
public class AddonMgr implements PluginTab {

	private static final Logger log = LoggerFactory.getLogger(AddonMgr.class);
	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private final UpdaterConfig updaterConfig;
	private final Set<Object> pending = new HashSet<>();
	private List<AddonDef> multiSelections = Collections.emptyList();
	private CustomTableModel<AddonDef> model;
	private TitleBorderFullsizePanel outer;

	@Override
	public String getTabName() {
		return "Manage Addons";
	}

	@Override
	public int getSortOrder() {
		return -50_000;
	}

	public AddonMgr(UpdaterConfig updaterConfig) {
		this.updaterConfig = updaterConfig;
	}

	@Override
	public Component getTabContents() {
		outer = new TitleBorderFullsizePanel("Manage Addons");
		CustomJsonListSetting<AddonDef> addonSetting = updaterConfig.getAddonSetting();

		model = CustomTableModel.builder(addonSetting::getItems)
				.addColumn(new CustomColumn<>("Icon", item -> item.iconUrl, c -> {
					c.setMaxWidth(100);
					c.setMinWidth(100);
					c.setCellRenderer(new DefaultTableCellRenderer() {
						@Override
						public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
							if (value == null || value.toString().isBlank()) {
								return super.getTableCellRendererComponent(table, "No Image", isSelected, hasFocus, row, column);
							}
							ScaledImageComponent element = IconTextRenderer.getIconOnly(() -> {
								URL url;
								try {
									url = new URL(value.toString());
								}
								catch (MalformedURLException e) {
									return null;
								}
								return url;
							});
							if (element == null) {
								return super.getTableCellRendererComponent(table, "No Image", isSelected, hasFocus, row, column);
							}
							Component defaultRenderer = super.getTableCellRendererComponent(table, "Loading...", isSelected, hasFocus, row, column);
							if (!element.isLoaded()) {
								boolean added = pending.add(value);
								if (added) {
									exs.submit(() -> {
										log.info("Downloading image: {}", value);
										element.forceLoadNow();
										log.info("Done downloading image: {}", value);
										SwingUtilities.invokeLater(table::repaint);
									});
								}
								return defaultRenderer;

							}
							AutoHeightScalingIcon out = new AutoHeightScalingIcon(element) {
								@Override
								public void paint(Graphics g) {
									((Graphics2D) g).setBackground(getBackground());
									g.clearRect(0, 0, getWidth(), getHeight());
									super.paint(g);
								}
							};
							out.setOpaque(true);
							out.setBackground(defaultRenderer.getBackground());
							return out;
						}
					});
				}))
				.addColumn(new CustomColumn<>("Name", item -> item.name, c -> c.setPreferredWidth(300)))
				.addColumn(new CustomColumn<>("Description", item -> item.description, c -> {
					c.setPreferredWidth(1000);
					c.setCellRenderer(new MultiLineVerticalCenteredTextRenderer("No Description"));
				}))
				.addColumn(new CustomColumn<>("Subdirectory", item -> item.dirName, c -> c.setPreferredWidth(200)))
				.build();
		addonSetting.addListener(model::signalNewData);
		JTable table = model.makeTable();
		table.setRowHeight(100);

		outer.setLayout(new BorderLayout());
		outer.add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout());

		JButton addButton = new JButton("Add From URL");
		addButton.addActionListener(l -> addFromUrl());

		JButton deleteButton = new JButton("Delete Selected") {
			@Override
			public boolean isEnabled() {
				return !multiSelections.isEmpty();
			}
		};
		deleteButton.addActionListener(l -> deleteSelected());

		JButton applyButton = new JButton("Apply and Restart");
		applyButton.addActionListener(l -> updaterConfig.runUpdaterNow());

		controlPanel.add(addButton);
		controlPanel.add(deleteButton);
		controlPanel.add(applyButton);
		outer.add(controlPanel, BorderLayout.SOUTH);

		table.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		return outer;
	}

	private void refreshSelection() {
		this.multiSelections = model.getSelectedValues();
//		this.selection = multiSelections.size() == 1 ? multiSelections.get(0) : null;
		SwingUtilities.invokeLater(() -> {
//			detailsInner.removeAll();
//			if (selection != null) {
//				detailsInner.add(new TriggerConfigPanel(selection));
//			}
//			detailsInner.revalidate();
//			detailsInner.repaint();
			outer.repaint();
		});
	}


	private void deleteSelected() {
		multiSelections.forEach(updaterConfig::removeAddon);
		SwingUtilities.invokeLater(outer::repaint);
	}

	private void addFromUrl() {
		String url = JOptionPane.showInputDialog(outer, "Enter the addon URL");
		if (url == null || url.isBlank()) {
			return;
		}
		try {
			updaterConfig.addNewAddon(url);
		} catch (Throwable t) {
			log.error("Error adding addon", t);
			JOptionPane.showMessageDialog(outer, "There was an error adding that URL: " + t + ".\nCheck the log for more details.", "Error adding addon", JOptionPane.ERROR_MESSAGE);
		}
		SwingUtilities.invokeLater(outer::repaint);
	}


}
