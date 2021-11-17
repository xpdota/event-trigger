package gg.xp.reevent.topology;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BaseToggleableTopo implements TopoItem {
	private static final Logger log = LoggerFactory.getLogger(BaseToggleableTopo.class);

	private final String name;
	private final List<? extends TopoItem> children;
	private final TopologyInfo info;
	protected boolean isEnabledDirectly = true;
	protected boolean isEnabledByParent = true;
	protected boolean isEffectivelyEnabled = true;
	protected BaseToggleableTopo parent;
	private final String propKey;
	private final Consumer<Boolean> statusSetter;

	protected BaseToggleableTopo(String name, List<? extends TopoItem> children, TopologyInfo info, String propKey) {
		this(name, children, info, propKey, b -> {});
	}

	protected BaseToggleableTopo(String name, List<? extends TopoItem> children, TopologyInfo info, String propKey, Consumer<Boolean> statusSetter) {
		this.name = name;
		this.children = new ArrayList<>(children);
		this.info = info;
		this.propKey = propKey;
		this.statusSetter = statusSetter;
		children.stream()
				.filter(c -> c instanceof BaseToggleableTopo)
				.map(BaseToggleableTopo.class::cast)
				.forEach(c -> c.setParent(this));
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

	// Set enabled/disabled without wastefully writing back to properties
	private void setEnabledDirectly_noProp(boolean newStatus) {
		this.isEnabledDirectly = newStatus;
		updateStatus();
	}

	@Override
	public void setEnabledDirectly(boolean newStatus) {
		this.isEnabledDirectly = newStatus;
		info.setEnabled(this, newStatus);
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

	private void updateStatus() {
		isEffectivelyEnabled = (isEffectivelyEnabled());
		applyEnabledStatus(isEffectivelyEnabled);
		children.forEach(child -> child.setEnabledByParent(isEffectivelyEnabled));
	}

	private void applyEnabledStatus(boolean newEnabledStatus) {
		statusSetter.accept(newEnabledStatus);
	}

	// TODO: make this abstract, and have this be where the property key goes for persistence
	private @Nullable String getPropertyKey() {
		return propKey;
	}

	void setParent(BaseToggleableTopo parent) {
		this.parent = parent;
	}

	public String getFullPropKey() {
		List<String> rawKeys = new ArrayList<>();
		BaseToggleableTopo current = this;
		while (current != null) {
			rawKeys.add(current.getPropertyKey());
			current = current.parent;
		}
		Collections.reverse(rawKeys);
		return rawKeys.stream().filter(Objects::nonNull)
				.collect(Collectors.joining("."));
	}

	private void selfInit() {
		// TODO
	}

	protected void extraCustomInit() {

	}

	@Override
	public void init() {
		selfInit();
		extraCustomInit();
		this.setEnabledDirectly_noProp(info.isEnabled(this));
		children.forEach(TopoItem::init);
	}
}
