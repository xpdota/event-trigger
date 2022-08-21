package gg.xp.xivsupport.events.actlines.events;

public class MapEvent {

	private final long instance;
	private final long data0;
	private final long data1;
	private final long data2;

	public MapEvent(long instance, long data0, long data1, long data2) {
		this.instance = instance;
		this.data0 = data0;
		this.data1 = data1;
		this.data2 = data2;
	}

	public long getInstance() {
		return instance;
	}

	public long getData0() {
		return data0;
	}

	public int getData1a() {
		return (int) (data1 >> 8);
	}

	public int getData1b() {
		return (int) data1 & 0xff;
	}

	public int getData2a() {
		return (int) (data2 >> 8);
	}

	public int getData2b() {
		return (int) data2 & 0xff;
	}

}
