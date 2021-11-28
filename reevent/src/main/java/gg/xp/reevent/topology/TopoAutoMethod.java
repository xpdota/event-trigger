package gg.xp.reevent.topology;

import gg.xp.reevent.scan.AutoHandler;

import java.util.Collections;

public class TopoAutoMethod extends BaseToggleableTopo implements TopoMethod {

	public TopoAutoMethod(AutoHandler handler, TopologyInfo topo) {
		// TODO: this is getting out of hand...super can't call stuff that would be dependent on subclass
		// field inits. So we either have to stuff *everything* into the super() call, or need to rethink
		// this whole setup.
		super(handler.getTopoLabel(), Collections.emptyList(), topo, handler.getTopoKey(), handler::setEnabled);
	}
}
