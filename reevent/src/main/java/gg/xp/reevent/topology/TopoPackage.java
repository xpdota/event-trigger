package gg.xp.reevent.topology;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TopoPackage extends BaseToggleableTopo {

	private final String packageName;

	public TopoPackage(String packageName, List<TopoClass> classes) {
		super(packageName, classes);
		this.packageName = packageName;
	}

	@Override
	void applyEnabledStatus(boolean newEnabledStatus) {
		// No-op
	}

	@Override
	protected @Nullable String getPropertyKey() {
		return packageName;
	}

}
