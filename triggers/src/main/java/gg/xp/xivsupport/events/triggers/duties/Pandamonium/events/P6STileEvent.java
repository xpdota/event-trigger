package gg.xp.xivsupport.events.triggers.duties.Pandamonium.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.models.Position;

public class P6STileEvent extends BaseEvent implements HasPrimaryValue {
	private final int x;
	private final int y;
	private final TileType tileType;
	private final int index;

	public P6STileEvent(int x, int y, TileType tileType, int index) {
		this.x = x;
		this.y = y;
		this.tileType = tileType;
		this.index = index;
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

	public int getIndex() {
		return index;
	}

	public Position tilePos() {
		return Position.of2d(85 + 10 * x, 85 + 10 * y);
	}
}
