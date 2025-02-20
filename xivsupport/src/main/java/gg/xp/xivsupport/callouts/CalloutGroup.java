package gg.xp.xivsupport.callouts;

import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of callouts, generally from a single class.
 */
public class CalloutGroup {
	private final Class<?> clazz;
	private final String name;
	private final BooleanSetting enabled;
	private final List<ModifiedCalloutHandle> callouts;

	public CalloutGroup(Class<?> clazz, String name, String topLevelPropStub, PersistenceProvider persistence, List<ModifiedCalloutHandle> callouts) {
		this.clazz = clazz;
		this.name = name;
		this.enabled = new BooleanSetting(persistence, topLevelPropStub + ".enabled", true);
		this.callouts = new ArrayList<>(callouts);
		updateChildren();
	}

	public CalloutGroup(Class<?> clazz, String name, String topLevelPropStub, BooleanSetting enabled, List<ModifiedCalloutHandle> callouts) {
		this.clazz = clazz;
		this.name = name;
		this.enabled = enabled;
		this.callouts = new ArrayList<>(callouts);
		updateChildren();
	}

	/**
	 * Should be called after enabling/disabling this group.
	 */
	public void updateChildren() {
		callouts.forEach(call -> call.setEnabledByParent(enabled.get()));
	}

	/**
	 * @return The name for this callout group
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The setting that controls the enablement of this group.
	 */
	public BooleanSetting getEnabled() {
		return enabled;
	}

	/**
	 * @return The callouts within this group
	 */
	public List<ModifiedCalloutHandle> getCallouts() {
		return Collections.unmodifiableList(callouts);
	}

	/**
	 * @return The class in which the callouts are defined
	 */
	public Class<?> getCallClass() {
		return clazz;
	}

	/**
	 * @return The duty for which the callouts are relevant to, or {@link KnownDuty#None} if these are not
	 * marked as being specific to any duty.
	 */
	public KnownDuty getDuty() {
		CalloutRepo ann = getCallClass().getAnnotation(CalloutRepo.class);
		if (ann == null) {
			return KnownDuty.None;
		}
		else {
			return ann.duty();
		}
	}

	/**
	 * Reset all enabled/disable status for this group.
	 *
	 * @see ModifiedCalloutHandle#resetAllBooleans()
	 */
	public void resetAllBooleans() {
		callouts.forEach(ModifiedCalloutHandle::resetAllBooleans);
	}
}
