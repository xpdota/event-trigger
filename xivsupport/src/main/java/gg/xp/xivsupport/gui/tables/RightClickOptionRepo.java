package gg.xp.xivsupport.gui.tables;

import gg.xp.reevent.scan.ScanMe;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ScanMe
public class RightClickOptionRepo {
	private final List<CustomRightClickOption> options = new ArrayList<>();

	public void addOption(CustomRightClickOption cro) {
		options.add(cro);
	}

	public List<CustomRightClickOption> getOptions() {
		return Collections.unmodifiableList(options);
	}

	public static final RightClickOptionRepo EMPTY = new RightClickOptionRepo() {
		@Override
		public void addOption(CustomRightClickOption cro) {
			throw new IllegalArgumentException("Cannot add to the empty repo");
		}
	};

	public static RightClickOptionRepo of(CustomRightClickOption... options) {
		RightClickOptionRepo out = new RightClickOptionRepo();
		for (CustomRightClickOption option : options) {
			out.addOption(option);
		}
		return out;
	}

	public static RightClickOptionRepo of(List<CustomRightClickOption> options) {
		RightClickOptionRepo out = new RightClickOptionRepo();
		for (CustomRightClickOption option : options) {
			out.addOption(option);
		}
		return out;
	}

	public RightClickOptionRepo withMore(CustomRightClickOption... newOptions) {
		List<CustomRightClickOption> allOptions = new ArrayList<>(options);
		allOptions.addAll(Arrays.asList(newOptions));
		return of(allOptions);
	}

	public void configureTable(JTable table, CustomTableModel<?> model) {
		CustomRightClickOption.configureTable(table, model, () -> options);
	}
}
