package gg.xp.reevent.topology;

import gg.xp.reevent.scan.AutoHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class TopoAutoMethod extends BaseToggleableTopo implements TopoMethod {


	private final AutoHandler handler;

	public TopoAutoMethod(AutoHandler handler) {
		super("Method: " + handler.getTopoLabel(), Collections.emptyList());
		this.handler = handler;
		// TODO: this is where the lookup would go
		setEnabledDirectly(true);
	}

	@Override
	void applyEnabledStatus(boolean newEnabledStatus) {
		handler.setEnabled(newEnabledStatus);
	}

	@Override
	protected @Nullable String getPropertyKey() {
		return handler.getTopoKey();
	}
}
