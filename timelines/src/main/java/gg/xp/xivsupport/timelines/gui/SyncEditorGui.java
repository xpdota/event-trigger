package gg.xp.xivsupport.timelines.gui;

import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.util.EasyAction;
import gg.xp.xivsupport.timelines.CustomEventSyncController;
import gg.xp.xivsupport.timelines.EventSyncController;
import gg.xp.xivsupport.timelines.NullableEnumComboBox;
import gg.xp.xivsupport.timelines.cbevents.CbEventType;
import gg.xp.xivsupport.timelines.cbevents.CbfMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SyncEditorGui extends JDialog {

	private static final Logger log = LoggerFactory.getLogger(SyncEditorGui.class);

	private final CustomTableModel<Map.Entry<CbfMap<?>, List<String>>> model;
	private @Nullable CbEventType type;
	private Map<CbfMap<?>, List<String>> conditions = Collections.emptyMap();
	private Result result;

	public SyncEditorGui(@NotNull Component parent, @Nullable EventSyncController initialValue) {
		super(SwingUtilities.getWindowAncestor(parent), "Edit Sync");
		if (initialValue == null) {
			setEventType(null);
		}
		else {
			setEventType(initialValue.getType());
			Map<String, List<String>> rawConds = initialValue.getRawConditions();
			conditions.replaceAll((k, v) -> {
				List<String> strings = rawConds.get(k.cbField());
				if (strings != null && !strings.isEmpty()) {
					return new ArrayList<>(strings);
				}
				else {
					return v;
				}
			});
		}
		JPanel panel = new JPanel(new BorderLayout());
		JPanel top = new JPanel();
		JPanel buttons = new JPanel();

		JButton submitButton = new EasyAction("OK", this::submit).asButton();
		JButton cancelButton = new EasyAction("Cancel", this::cancel).asButton();
		buttons.add(submitButton);
		buttons.add(cancelButton);

		model = CustomTableModel.builder(
						() -> {
							List<Map.Entry<CbfMap<?>, List<String>>> entries = new ArrayList<>(conditions.entrySet());
							entries.sort(Comparator.comparing(e -> e.getKey().ourLabel()));
							return entries;
						})
				.addColumn(new CustomColumn<>(
						"Field",
						f -> f.getKey().ourLabel()
				))
				.addColumn(new CustomColumn<>(
						"Cactbot Equivalent",
						f -> f.getKey().cbField()
				))
				.addColumn(new CustomColumn<>(
						"Values (Separate with |)",
						Map.Entry::getValue,
						c -> {
							c.setCellRenderer(new DefaultTableCellRenderer() {
								@Override
								public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
									if (value == null) {
										value = "(None/Any)";
									}
									else if (value instanceof List<?> listVal) {
										if (listVal.isEmpty()) {
											value = "(None/Any)";
										}
										else {
											value = String.join("|", (Iterable<? extends CharSequence>) listVal);
										}
									}
									return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
								}
							});
							c.setCellEditor(StandardColumns.<Map.Entry<CbfMap<?>, List<String>>, List<String>>customStringEditor(
									(entry, value) -> entry.setValue(new ArrayList<>(value)),
									val -> {
										if (val == null || val.isEmpty()) {
											return "";
										}
										else {
											return String.join("|", val);
										}
									},
									value ->  Arrays.stream(value.split("\\|")).filter(s -> s != null && !s.isBlank()).toList()
									));
						}
				))
				.build();
		JTable table = new JTable(model) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 2;
			}
		};
		model.configureColumns(table);

		JComboBox<CbEventType> cb = new NullableEnumComboBox<>(CbEventType.class, "No Sync", this::setEventType, type);
		top.add(cb);

		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.add(top, BorderLayout.NORTH);
		panel.add(buttons, BorderLayout.SOUTH);
		panel.validate();
		panel.setPreferredSize(new Dimension(710, 400));
		setContentPane(panel);
		pack();
		setModal(true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(parent);

	}

	private void setEventType(@Nullable CbEventType type) {
		this.type = type;
		if (type == null) {
			conditions = Collections.emptyMap();
		}
		else {
			conditions = type.getFieldMappings()
					.stream()
					.collect(Collectors.toMap(
							Function.identity(),
							cbf -> new ArrayList<>()
					));
		}
		refresh();
	}

	private void cancel() {
		dispose();
	}

	private void submit() {
		if (type == null) {
			finish(new Result(true, null));
		}
		else {
			// Don't allow a condition-less event without user confirmation, as this is probably unintended
			Map<String, List<String>> finalValues = this.conditions.entrySet().stream()
					.filter(e -> !e.getValue().isEmpty())
					.collect(Collectors.toMap(
							e -> e.getKey().cbField(),
							Map.Entry::getValue
					));
			if (finalValues.isEmpty()) {
				int confirmEmpty = JOptionPane.showConfirmDialog(this, "You didn't choose any conditions. Are you sure?", "No Conditions", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (confirmEmpty != JOptionPane.YES_OPTION) {
					return;
				}
			}
			finish(new Result(true, new CustomEventSyncController(type, finalValues)));
		}
	}

	private void finish(Result value) {
		this.result = value;
		setVisible(false);
		dispose();
	}

	public static Result edit(@NotNull Component parent, @Nullable EventSyncController initialValue) {
		SyncEditorGui dialog = new SyncEditorGui(parent, initialValue);
		dialog.setVisible(true);
		Result res = dialog.result;
		if (res == null) {
			log.error("null result!");
			return new Result(false, null);
		}
		return res;
	}

	private void refresh() {
		// refresh table
		if (this.model != null) {
			this.model.fullRefresh();
		}
	}

	public static class Result {
		public final boolean submitted;
		public final @Nullable CustomEventSyncController value;

		public Result(boolean submitted, @Nullable CustomEventSyncController value) {
			this.submitted = submitted;
			this.value = value;
		}
	}


}
