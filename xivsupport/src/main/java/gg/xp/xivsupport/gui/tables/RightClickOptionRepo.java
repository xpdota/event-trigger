package gg.xp.xivsupport.gui.tables;

import gg.xp.reevent.scan.ScanMe;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ScanMe // Establishes a default base instance
public class RightClickOptionRepo {
	private @Nullable RightClickOptionRepo parent;
	private final List<CustomRightClickOption> options = new CopyOnWriteArrayList<>();

	public void addOption(CustomRightClickOption cro) {
		options.add(cro);
	}

	void setParent(@Nullable RightClickOptionRepo parent) {
		this.parent = parent;
	}

	public List<CustomRightClickOption> getOptions() {
		RightClickOptionRepo parent = this.parent;
		if (parent == null) {
			return Collections.unmodifiableList(options);
		}
		else {
			List<CustomRightClickOption> out = new ArrayList<>(parent.options.size() + options.size());
			out.addAll(parent.options);
			out.addAll(options);
			return Collections.unmodifiableList(out);
		}
	}

	public static final RightClickOptionRepo EMPTY = new RightClickOptionRepo() {
		@Override
		public void addOption(CustomRightClickOption cro) {
			throw new IllegalArgumentException("Cannot add to the empty repo");
		}
	};

	/**
	 * Create a standalone RightClickOptionRepo
	 *
	 * @param options The right-click options
	 * @return A standalone repo with the given options
	 */
	public static RightClickOptionRepo of(CustomRightClickOption... options) {
		RightClickOptionRepo out = new RightClickOptionRepo();
		for (CustomRightClickOption option : options) {
			out.addOption(option);
		}
		return out;
	}

	/**
	 * Create a standalone RightClickOptionRepo
	 *
	 * @param options The right-click options
	 * @return A standalone repo with the given options
	 */
	public static RightClickOptionRepo of(List<CustomRightClickOption> options) {
		RightClickOptionRepo out = new RightClickOptionRepo();
		for (CustomRightClickOption option : options) {
			out.addOption(option);
		}
		return out;
	}

	/**
	 * Create a RightClickOptionRepo which is linked with the current instance and inherits all options from the
	 * parent.
	 *
	 * @param newOptions The right-click options to add
	 * @return A standalone repo with the given options
	 */
	public RightClickOptionRepo withMore(CustomRightClickOption... newOptions) {
		RightClickOptionRepo out = of(newOptions);
		out.setParent(this);
		return out;
	}

	/**
	 * Configure a table using these options
	 *
	 * @param table The table
	 * @param model The table model
	 */
	public void configureTable(JTable table, CustomTableModel<?> model) {
		CustomRightClickOption.configureTable(table, model, this::getOptions);
	}
}
