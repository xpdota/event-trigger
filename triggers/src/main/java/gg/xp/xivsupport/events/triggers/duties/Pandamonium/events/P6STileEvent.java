package gg.xp.xivsupport.events.triggers.duties.Pandamonium.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

public class P6STileEvent extends BaseEvent implements HasPrimaryValue {
	private final int x;
	private final int y;
	private final TileType tileType;

	public P6STileEvent(int x, int y, TileType tileType) {
		this.x = x;
		this.y = y;
		this.tileType = tileType;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("Tile (%s, %s) %s", x, y, tileType);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public TileType getTileType() {
		return tileType;
	}
}
