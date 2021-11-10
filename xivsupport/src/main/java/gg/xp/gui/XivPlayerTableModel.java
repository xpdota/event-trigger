package gg.xp.gui;

import gg.xp.events.models.XivPlayerCharacter;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;

class XivPlayerTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 3653231965328482100L;
	private List<XivPlayerCharacter> data = Collections.emptyList();
	// TODO: IIRC, column models do this, but cleaner?
	private String[] columnNames = {"Name", "Job", "ID"};

	public void setData(List<XivPlayerCharacter> data) {
		this.data = data;
		fireTableDataChanged();
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 1:
				return data.get(rowIndex - 1).getName();
			case 2:
				return data.get(rowIndex - 1).getJob().name();
			case 3:
				return data.get(rowIndex - 1).getId();
			default:
				// TODO
				return null;
		}
	}
}
