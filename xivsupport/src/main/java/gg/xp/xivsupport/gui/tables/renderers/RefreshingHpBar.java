package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.gui.Refreshable;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.function.Supplier;

public class RefreshingHpBar extends HpBar implements Refreshable {

	private final Supplier<XivCombatant> dataSource;

	public RefreshingHpBar(Supplier<XivCombatant> dataSource) {
		this.dataSource = dataSource;
		refresh();
	}

	@Override
	public void refresh() {
		XivCombatant xc = dataSource.get();
		setData(xc, 0);
		revalidate();
	}
}
