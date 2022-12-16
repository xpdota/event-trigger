package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class ParentedBooleanSetting extends ObservableSetting implements ObservableMutableBoolean, Resettable {

	private final ObservableBoolean parent;
	private final BooleanSetting internalSetting;

	public ParentedBooleanSetting(PersistenceProvider persistence, String settingKey, ObservableBoolean parent) {
		this.internalSetting = new BooleanSetting(persistence, settingKey, false);
		this.parent = parent;
		parent.addListener(this::reprocess);
	}

	private void reprocess() {
		notifyListeners();
	}

	@Override
	public boolean get() {
		if (internalSetting.isSet()) {
			return internalSetting.get();
		}
		else {
			return parent.get();
		}
	}

	@Override
	public void set(boolean value) {
		internalSetting.set(value);
		notifyListeners();
	}

	@Override
	public boolean isSet() {
		return internalSetting.isSet();
	}

	@Override
	public void delete() {
		internalSetting.delete();
		notifyListeners();
	}
}
