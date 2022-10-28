package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class CsvParseHelper {

	private final String[] cells;

	private CsvParseHelper(String[] cells) {
		this.cells = cells;
	}

	public static CsvParseHelper ofRow(String[] row) {
		return new CsvParseHelper(row);
	}

	// STRING
	public String getRaw(int col) {
		return cells[col];
	}

	public @Nullable String getStringOrNull(int col) {
		String raw = cells[col];
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw;
	}


	// BOOLEAN

	public boolean getRequiredBool(int col) {
		String cellValue = cells[col];
		if ("true".equalsIgnoreCase(cellValue)) {
			return true;
		}
		else if ("false".equalsIgnoreCase(cellValue)) {
			return false;
		}
		else {
			throw new IllegalArgumentException("Not a boolean: " + cellValue);
		}
	}

	public boolean getBoolOrDefault(int col, boolean dflt) {
		String cellValue = cells[col];
		if ("true".equalsIgnoreCase(cellValue)) {
			return true;
		}
		else if ("false".equalsIgnoreCase(cellValue)) {
			return false;
		}
		else {
			return dflt;
		}
	}

	public @Nullable Boolean getOptionalBool(int col) {
		String cellValue = cells[col];
		if ("true".equalsIgnoreCase(cellValue)) {
			return true;
		}
		else if ("false".equalsIgnoreCase(cellValue)) {
			return false;
		}
		else {
			return null;
		}
	}

	// INT
	public int getRequiredInt(int col) {
		return Integer.parseInt(cells[col]);
	}

	public int getIntOrDefault(int col, int dflt) {
		try {
			return Integer.parseInt(cells[0]);
		}
		catch (NumberFormatException nfe) {
			// Ignore the bad value at the top
			return dflt;
		}
	}

	public @Nullable Integer getOptionalInt(int col) {
		String raw = cells[col];
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(raw);
		}
		catch (NumberFormatException nfe) {
			// Ignore the bad value at the top
			return null;
		}
	}

	// LONG

	public long getLongId() {
		try {
			return Long.parseLong(cells[0]);
		}
		catch (NumberFormatException nfe) {
			// Ignore the bad value at the top
			return -1;
		}
	}

	public int getIntId() {
		try {
			return Integer.parseInt(cells[0]);
		}
		catch (NumberFormatException nfe) {
			// Ignore the bad value at the top
			return -1;
		}
	}

	public boolean hasValidId() {
		try {
			Long.parseLong(cells[0]);
			return true;
		}
		catch (NumberFormatException nfe) {
			// Ignore the bad value at the top
			return false;
		}
	}

}
