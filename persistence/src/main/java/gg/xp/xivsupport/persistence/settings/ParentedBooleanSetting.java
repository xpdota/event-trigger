package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class ParentedBooleanSetting extends BooleanSetting {

	private final ObservableBoolean parent;

	public ParentedBooleanSetting(PersistenceProvider persistence, String settingKey, ObservableBoolean parent) {
		super(persistence, settingKey, false);
		this.parent = parent;
		parent.addListener(this::reprocess);
	}

	private void reprocess() {
		notifyListeners();
	}

	@Override
	public boolean get() {
		if (super.isSet()) {
			return super.get();
		}
		else {
			return parent.get();
		}
	}

	@Override
	public void set(boolean value) {
		super.set(value);
		notifyListeners();
	}

	@Override
	public void delete() {
		super.delete();
		notifyListeners();
	}

	@Override
	public boolean hasParent() {
		return true;
	}
}
