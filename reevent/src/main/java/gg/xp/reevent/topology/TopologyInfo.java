package gg.xp.reevent.topology;

public interface TopologyInfo {

	boolean isEnabled(BaseToggleableTopo topo);

	void setEnabled(BaseToggleableTopo topo, boolean enabled);
}
