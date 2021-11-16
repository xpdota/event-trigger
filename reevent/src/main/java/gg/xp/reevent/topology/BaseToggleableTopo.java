package gg.xp.reevent.topology;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseToggleableTopo implements TopoItem {
	private final String name;
	private final List<? extends TopoItem> children;
	protected boolean isEnabledDirectly = true;
	protected boolean isEnabledByParent = true;
	protected boolean isEffectivelyEnabled = true;

	protected BaseToggleableTopo(String name, List<? extends TopoItem> children) {
		this.name = name;
		this.children = new ArrayList<>(children);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<? extends TopoItem> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public boolean canBeDisabled() {
		return true;
	}

	@Override
	public boolean isEnabledDirectly() {
		return isEnabledDirectly;
	}

	@Override
	public void setEnabledDirectly(boolean newStatus) {
		this.isEnabledDirectly = newStatus;
		updateStatus();
	}

	@Override
	public boolean isEnabledByParent() {
		return isEnabledByParent;
	}

	@Override
	public void setEnabledByParent(boolean enabled) {
		this.isEnabledByParent = enabled;
		updateStatus();
	}

	private boolean isEffectivelyEnabled() {
		return isEnabledByParent && isEnabledDirectly;
	}

	private void updateStatus() {
		isEffectivelyEnabled = (isEffectivelyEnabled());
		applyEnabledStatus(isEffectivelyEnabled);
		children.forEach(child -> child.setEnabledByParent(isEffectivelyEnabled));
	}

	abstract void applyEnabledStatus(boolean newEnabledStatus);

	// TODO: make this abstract, and have this be where the property key goes for persistence
	protected abstract @Nullable String getPropertyKey();
}
