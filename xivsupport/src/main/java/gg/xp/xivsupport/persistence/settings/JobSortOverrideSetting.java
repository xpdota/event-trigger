package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import java.util.Comparator;
import java.util.List;

public class JobSortOverrideSetting extends JobSortSetting {

	private final JobSortSetting parent;
	private final BooleanSetting enabled;


	public JobSortOverrideSetting(PersistenceProvider pers, String settingKey, XivState state, JobSortSetting parent) {
		super(pers, settingKey + ".sort-override", state);
		this.parent = parent;
		enabled = new BooleanSetting(pers, settingKey + ".enable-override", false);
		enabled.addAndRunListener(() -> {
			if (enabled.get() && !super.isSet()) {
				resetJobOrder();
			}
		});
	}

	@Override
	public Comparator<XivPlayerCharacter> getComparator() {
		if (enabled.get()) {
			return super.getComparator();
		}
		else {
			return parent.getComparator();
		}
	}

	@Override
	public List<Job> getJobOrder() {
		if (enabled.get()) {
			return super.getJobOrder();
		}
		else {
			return parent.getJobOrder();
		}
	}

	@Override
	protected Comparator<Job> getDefaultSort() {
		if (parent == null) {
			return super.getDefaultSort();
		}
		return Comparator.comparing(job -> parent.getJobOrder().indexOf(job));
	}

	@Override
	public boolean isSet() {
		return enabled.isSet() || super.isSet();
	}

	public BooleanSetting getEnabled() {
		return enabled;
	}
}
