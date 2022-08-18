package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class DataWatchingCustomPartyComponent<X> extends BasePartyListComponent {


	protected abstract X extractData(@NotNull XivPlayerCharacter xpc);

	protected abstract void applyData(X data);

	private X oldData;

	protected void forceApplyLastData() {
		if (oldData != null) {
			applyData(oldData);
		}
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		X newData = extractData(xpc);
		if (!Objects.equals(newData, oldData)) {
			oldData = newData;
			applyData(newData);
		}
	}
}
