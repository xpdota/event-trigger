package gg.xp.xivsupport.gui.cooldowns;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivsupport.cdsupport.CustomCooldown;
import gg.xp.xivsupport.cdsupport.CustomCooldownManager;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.lists.ItemList;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.renderers.AbilityListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivAbility;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@ScanMe
public class CustomCooldownTab implements PluginTab {

	private final CustomCooldownManager backend;
	private final ActionTableFactory actionTableFactory;
	private JPanel detailsInner;
	private CustomTableModel<CustomCooldown> model;
	private TitleBorderFullsizePanel outer;
	private CustomCooldown selection;
	private List<CustomCooldown> multiSelections = Collections.emptyList();
//	private boolean saveAnyway;

	public CustomCooldownTab(CustomCooldownManager backend, ActionTableFactory actionTableFactory) {
		this.backend = backend;
		this.actionTableFactory = actionTableFactory;
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
				.build();
		JTable cdChooserTable = new JTable(model) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 0;
			}
		};
		model.configureColumns(cdChooserTable);

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
		cdChooserTable.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		cdChooserTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
				JButton helpButton = new JButton("Help Button TODO Program Me");
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

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(cdChooserTable), bottomPanel);
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

	private void addnew() {
		ActionInfo action = actionTableFactory.pickItem(SwingUtilities.getWindowAncestor(outer));
		if (action == null) {
			return;
		}
		CustomCooldown newCc = new CustomCooldown();
		newCc.primaryAbilityId = action.actionid();
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

	private final AbilityListRenderer abilityRenderer = new AbilityListRenderer();

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
				generalPanel.setBorder(new TitledBorder("General"));
				generalPanel.setLayout(new WrapLayout());
				TextFieldWithValidation<String> nameField = new TextFieldWithValidation<>(
						emptyToNull(Function.identity()),
						modifyCd(selection, s -> selection.nameOverride = s),
						() -> selection.nameOverride);
				JLabel nameLabel = GuiUtil.labelFor("Name (Blank for Auto)", nameField);
				generalPanel.add(nameLabel);
				generalPanel.add(nameField);
				{
					JPanel primaryAbilityPanel = new JPanel();
					ActionInfo ai = ActionLibrary.forId(selection.primaryAbilityId);
					primaryAbilityPanel.add(new JLabel("Primary Ability (cannot be changed): "));
					if (ai != null) {
						primaryAbilityPanel.add(IconTextRenderer.getComponent(ai.getIcon(), new JLabel(' ' + ai.name()), false, false, true));
					}
					else {
						primaryAbilityPanel.add(new JLabel("None"));
					}
					generalPanel.add(primaryAbilityPanel);
				}
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
				TitleBorderPanel cdChargePanel = new TitleBorderPanel("Cooldown/Charges");
				cdChargePanel.setLayout(new WrapLayout());
				c.gridx = 0;
				c.gridy++;

				{
					TextFieldWithValidation<Double> durationField = new TextFieldWithValidation<>(emptyToNull(Double::parseDouble), modifyCd(selection, value -> selection.cooldown = value), () -> String.valueOf(selection.cooldown));
					JLabel durationLabel = GuiUtil.labelFor("Duration", durationField);
					cdChargePanel.add(durationField);
					cdChargePanel.add(durationLabel);
				}

				{
					TextFieldWithValidation<Integer> chargesField = new TextFieldWithValidation<>(emptyToNull(Integer::parseInt), modifyCd(selection, value -> selection.maxCharges = value), () -> String.valueOf(selection.maxCharges));
					JLabel chargesLabel = GuiUtil.labelFor("Charges", chargesField);
					cdChargePanel.add(chargesField);
					cdChargePanel.add(chargesLabel);
				}

				inner.add(cdChargePanel, c);
			}
			{
				TitleBorderPanel statusPanel = new TitleBorderPanel("Status Effect");
				c.gridx++;
				inner.add(statusPanel, c);
			}
			add(inner, BorderLayout.CENTER);
		}
	}


}
