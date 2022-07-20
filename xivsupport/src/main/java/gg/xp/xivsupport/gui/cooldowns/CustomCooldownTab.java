package gg.xp.xivsupport.gui.cooldowns;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivsupport.cdsupport.CustomCooldown;
import gg.xp.xivsupport.cdsupport.CustomCooldownManager;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.lists.ItemList;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.renderers.AbilityListCellRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.StatusEffectListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.StatusListCellRenderer;
import gg.xp.xivsupport.gui.util.GridBagHelper;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivStatusEffect;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@ScanMe
public class CustomCooldownTab implements PluginTab {

	private final CustomCooldownManager backend;
	private final ActionTableFactory actionTableFactory;
	private JPanel detailsInner;
	private CustomTableModel<CustomCooldown> model;
	private TitleBorderFullsizePanel outer;
	private CustomCooldown selection;
	private List<CustomCooldown> multiSelections = Collections.emptyList();
	private JTable table;
	//	private boolean saveAnyway;

	public CustomCooldownTab(CustomCooldownManager backend, ActionTableFactory actionTableFactory, RightClickOptionRepo rightClicks) {
		this.backend = backend;
		this.actionTableFactory = actionTableFactory;
		rightClicks.addOption(CustomRightClickOption.forRow(
				"Add as Custom Cooldown",
				HasAbility.class,
				ha -> {
					addnew(ha.getAbility().getId());
					GuiUtil.bringToFront(outer);
				}));
	}

	@Override
	public String getTabName() {
		return "Custom Cooldowns";
	}

	private static <X> Function<CustomCooldown, X> resultingValue(Function<ExtendedCooldownDescriptor, X> func) {
		return ccd -> func.apply(ccd.buildCd());
	}

	@Override
	public Component getTabContents() {
		GridBagConstraints c = GuiUtil.defaultGbc();
		c.weighty = 1;

		model = CustomTableModel.builder(backend::getCooldowns)
				.addColumn(new CustomColumn<>("Name/Skill", resultingValue(cd -> new XivAbility(cd.getPrimaryAbilityId(), cd.getLabel())),
						col -> col.setCellRenderer(new ActionAndStatusRenderer())))
				.addColumn(new CustomColumn<>("Cooldown", resultingValue(cd -> {
					int maxCharges = cd.getMaxCharges();
					if (maxCharges > 1) {
						return String.format("%s (%d charges)", cd.getCooldown(), maxCharges);
					}
					return cd.getCooldown();
				}), col -> col.setCellEditor(new NoCellEditor())))
				.addColumn(new CustomColumn<>("Duration", resultingValue(cd -> {
					Double dur = cd.getDurationOverride();
					if (dur == null) {
						return "Auto";
					}
					else {
						return String.valueOf(dur);
					}
				})))
				.addColumn(new CustomColumn<>("Status Effects", cd -> {
					if (cd.autoBuffs) {
						return "Auto";
					}
					else if (cd.buffIds.length == 0) {
						return "None";
					}
					else {
						return Arrays.stream(cd.buffIds).mapToObj(XivStatusEffect::new).toList();
					}
				}, col -> col.setCellRenderer(
						new DefaultTableCellRenderer() {
							final StatusEffectListRenderer selr = new StatusEffectListRenderer();

							@Override
							public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
								if (value instanceof List) {
									return selr.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
								}
								return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
							}
						})))

				.build();
		table = new JTable(model) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		model.configureColumns(table);

		// TODO: saving
		outer = new TitleBorderFullsizePanel("Custom Cooldowns") {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					model.signalNewData();
				}
				else {
					backend.commit();
				}
				super.setVisible(visible);
			}
		};

		outer.setLayout(new GridBagLayout());
		table.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		{
			// TODO: most of these could also be right click options
			JPanel controlsPanel = new JPanel(new FlowLayout());
			{
				JButton newCdButton = new JButton("New Cooldown");
				controlsPanel.add(newCdButton);
				newCdButton.addActionListener(l -> addnew());
			}
			{
				JButton deleteCdButton = new JButton("Delete Selected") {
					@Override
					public boolean isEnabled() {
						return !multiSelections.isEmpty();
					}
				};
				controlsPanel.add(deleteCdButton);
				deleteCdButton.addActionListener(l -> delete());
			}
			{
				JButton helpButton = new JButton("Help");
				helpButton.addActionListener(l -> {
					GuiUtil.openUrl("https://triggevent.io/pages/Cooldown-Tracker/#adding-custom-cooldowns");
				});
				controlsPanel.add(helpButton);
			}
			bottomPanel.add(controlsPanel, BorderLayout.NORTH);
		}

		{
			this.detailsInner = new JPanel(new BorderLayout());
//			JScrollPane detailsScroller = new JScrollPane(detailsInner);
//			detailsScroller.setBorder(null);
//			detailsScroller.setPreferredSize(detailsScroller.getMaximumSize());
//			TitleBorderFullsizePanel detailsOuter = new TitleBorderFullsizePanel("Cooldown Details");
//			detailsOuter.setLayout(new BorderLayout());
//			detailsOuter.add(detailsScroller, BorderLayout.CENTER);
			bottomPanel.add(detailsInner, BorderLayout.CENTER);
		}

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), bottomPanel);
		split.setOneTouchExpandable(true);
		outer.add(split, c);
		split.setResizeWeight(0.5);
		split.setDividerLocation(300);

//		RefreshLoop<CustomCooldownTab> refresher = new RefreshLoop<>("CustomCdAutoSave", this, cct -> {
//			if (outer.isShowing()) {
//				cct.backend.commit();
//				saveAnyway = true;
//			}
//			else if (saveAnyway) {
//				cct.backend.commit();
//				saveAnyway = false;
//			}
//		}, (unused) -> 15000L);

//		refresher.start();
		return outer;
	}

	private void refreshSelection() {
		this.multiSelections = model.getSelectedValues();
		this.selection = multiSelections.size() == 1 ? multiSelections.get(0) : null;
		SwingUtilities.invokeLater(() -> {
			detailsInner.removeAll();
			if (selection != null) {
				detailsInner.add(new CooldownConfigPanel(selection));
			}
			detailsInner.revalidate();
			detailsInner.repaint();
			outer.repaint();
		});
	}

	private <X> Consumer<X> modifyCd(CustomCooldown cd, Consumer<X> raw) {
		return value -> {
			raw.accept(value);
			cd.invalidate();
			backend.commit();
			table.repaint();
		};
	}

	private static <X> Function<String, @Nullable X> emptyToNull(Function<String, X> raw) {
		return value -> {
			if (value.isBlank()) {
				return null;
			}
			else {
				return raw.apply(value);
			}
		};
	}

	private static <X> Function<@Nullable X, String> nullToEmpty(Function<X, String> raw) {
		return value -> {
			if (value == null) {
				return "";
			}
			else {
				return raw.apply(value);
			}
		};
	}

	private static <X> Supplier<String> valueToStr(Supplier<X> extractor) {
		return () -> nullToEmpty(String::valueOf).apply(extractor.get());
	}

	private void addnew() {
		ActionInfo action = actionTableFactory.pickItem(SwingUtilities.getWindowAncestor(outer));
		if (action == null) {
			return;
		}
		addnew(action.actionid());
	}

	private void addnew(long id) {
		CustomCooldown newCc = new CustomCooldown();
		newCc.primaryAbilityId = id;
		backend.addCooldown(newCc);
		refresh();
		SwingUtilities.invokeLater(() -> {
			model.setSelectedValue(newCc);
			refreshSelection();
		});

	}

	private void delete() {
		multiSelections.forEach(backend::removeCooldown);
		refresh();
		model.setSelectedValue(null);
	}


	private void refresh() {
		model.signalNewData();
	}

	private final AbilityListCellRenderer abilityRenderer = new AbilityListCellRenderer();
	private final StatusListCellRenderer statusRenderer = new StatusListCellRenderer();

	private class CooldownConfigPanel extends JPanel {

		public CooldownConfigPanel(CustomCooldown selection) {
			setLayout(new BorderLayout());
			GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
			{
				ReadOnlyText instructions = new ReadOnlyText("""
						Note: For fields without an explicit manual/auto toggle, leaving them blank will cause them to be automatically derived from game data and/or log lines. This is usually correct, but you may need to override in specific cases (e.g. traits lowering CD or adding charges, or when you don't want specific buffs to be included).
						""");
				c.gridwidth = GridBagConstraints.REMAINDER;
				add(instructions, BorderLayout.NORTH);
				c.gridwidth = 1;
				c.gridy++;
				c.weighty = 1;
			}
			JPanel inner = new JPanel();
			inner.setLayout(new GridLayout(2, 2));
			{
				JPanel generalPanel = new JPanel();
				GridBagConstraints innerGbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 5, 0);
				GridBagHelper gbh = new GridBagHelper(generalPanel, innerGbc);
				generalPanel.setBorder(new TitledBorder("General"));
				TextFieldWithValidation<String> nameField = new TextFieldWithValidation<>(
						emptyToNull(Function.identity()),
						modifyCd(selection, s -> selection.nameOverride = s),
						() -> selection.nameOverride);
				JLabel nameLabel = GuiUtil.labelFor("Name (Blank for Auto)", nameField);
				gbh.addRow(nameLabel, nameField);
				{
					ActionInfo ai = ActionLibrary.forId(selection.primaryAbilityId);
					JLabel label = new JLabel("Primary Ability");
					Component icon;
					if (ai != null) {
						icon = IconTextRenderer.getComponent(ai.getIcon(), new JLabel(' ' + ai.name()), false, false, true);
					}
					else {
						icon = new JLabel("None");
					}
					gbh.addRow(label, icon);
				}
				gbh.addVerticalPadding();
				inner.add(generalPanel, c);
			}
			{
				TitleBorderPanel abilityPanel = new TitleBorderPanel("Secondary Abilities");
				abilityPanel.setLayout(new BorderLayout());
				c.gridx++;
				{
					JPanel secondaryAbilityPanel = new ItemList<>(
							() -> Arrays.stream(selection.secondaryAbilityIds).mapToObj(XivAbility::new).toList(),
							modifyCd(selection, abilities -> selection.secondaryAbilityIds = abilities.stream().mapToLong(XivAbility::getId).toArray()),
							abilityRenderer,
							() -> {
								ActionInfo ai = actionTableFactory.pickItem(SwingUtilities.getWindowAncestor(outer));
								if (ai == null) {
									return null;
								}
								return new XivAbility(ai.actionid());
							}
					);
					abilityPanel.add(secondaryAbilityPanel, BorderLayout.CENTER);
				}
				inner.add(abilityPanel, c);
			}
			{
				TitleBorderPanel cdChargePanel = new TitleBorderPanel("Cooldown/Charges/Duration (Leave Blank for Auto)");
//				cdChargePanel.setLayout(new BorderLayout());
				JPanel innerCdPanel = new JPanel();
				GridBagConstraints innerGbc = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 5, 0);
				GridBagHelper gbh = new GridBagHelper(innerCdPanel, innerGbc);
				c.gridx = 0;
				c.gridy++;

				{
					TextFieldWithValidation<Double> cdField = new TextFieldWithValidation<>(emptyToNull(Double::parseDouble), modifyCd(selection, value -> selection.cooldown = value), valueToStr(() -> selection.cooldown));
					JLabel cdLabel = GuiUtil.labelFor("Cooldown", cdField);
					gbh.addRow(cdLabel, cdField);
				}

				{
					TextFieldWithValidation<Integer> chargesField = new TextFieldWithValidation<>(emptyToNull(Integer::parseInt), modifyCd(selection, value -> selection.maxCharges = value), valueToStr(() -> selection.maxCharges));
					JLabel chargesLabel = GuiUtil.labelFor("Charges", chargesField);
					gbh.addRow(chargesLabel, chargesField);
				}

				{
					TextFieldWithValidation<Double> durationField = new TextFieldWithValidation<>(emptyToNull(Double::parseDouble), modifyCd(selection, value -> selection.duration = value), valueToStr(() -> selection.duration));
					JLabel durationLabel = GuiUtil.labelFor("Duration", durationField);
					gbh.addRow(durationLabel, durationField);
				}
				cdChargePanel.add(innerCdPanel, BorderLayout.WEST);
				inner.add(cdChargePanel, c);
			}
			{
				TitleBorderPanel statusPanel = new TitleBorderPanel("Status Effect");
				statusPanel.setLayout(new BorderLayout());
				c.gridx++;
				JCheckBox autoCb = new JCheckBox("Automatic");
				autoCb.setSelected(selection.autoBuffs);
				autoCb.addActionListener(l -> {
					CustomCooldownTab.this.<Boolean>modifyCd(selection, value -> selection.autoBuffs = value).accept(autoCb.isSelected());
					statusPanel.repaint();
				});
				statusPanel.add(autoCb, BorderLayout.NORTH);
				{
					JPanel statusList = new ItemList<>(
							() -> Arrays.stream(selection.buffIds).mapToObj(XivStatusEffect::new).toList(),
							modifyCd(selection, statuses -> selection.buffIds = statuses.stream().mapToLong(XivStatusEffect::getId).toArray()),
							statusRenderer,
							() -> {
								StatusEffectInfo sei = StatusTable.pickItem(SwingUtilities.getWindowAncestor(outer));
								if (sei == null) {
									return null;
								}
								return new XivStatusEffect(sei.statusEffectId());
							}
					) {
						@Override
						public boolean isEnabled() {
							return !selection.autoBuffs;
						}
					};
					statusPanel.add(statusList, BorderLayout.CENTER);
				}
				inner.add(statusPanel, c);
			}
			add(inner, BorderLayout.CENTER);
		}
	}


	@Override
	public int getSortOrder() {
		return 8;
	}

}
