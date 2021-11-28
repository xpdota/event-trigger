package gg.xp.xivsupport.persistence;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.topology.BaseToggleableTopo;
import gg.xp.reevent.topology.TopoItem;
import gg.xp.reevent.topology.Topology;
import gg.xp.xivsupport.sys.XivMain;
import org.junit.Assert;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TopoPersistence {

	private static final Logger log = LoggerFactory.getLogger(TopoPersistence.class);

	@Test
	void testTopoTogglePersistence() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		AutoEventDistributor auto = pico.getComponent(AutoEventDistributor.class);
		Topology topTopo = auto.getTopology();
		PersistenceProvider persistence = pico.getComponent(PersistenceProvider.class);
		// TODO: kinda flaky, assumes a lot about the actual topo
		TopoItem someRandomTopo = topTopo.getChildren().get(0).getChildren().get(0).getChildren().get(0);
		BaseToggleableTopo topo = (BaseToggleableTopo) someRandomTopo;
		// Key should not be found at first
		String propKey = topo.getFullPropKey();
		log.info("Testing prop key: {}", propKey);
		{
			Boolean value = persistence.get(propKey, Boolean.class, null);
			Assert.assertNull(value);
		}
		// Toggling parent shouldn't touch properties
		topo.setEnabledByParent(false);
		{
			Boolean value = persistence.get(propKey, Boolean.class, null);
			Assert.assertNull(value);
			Assert.assertFalse(topo.isEnabledByParent());
			Assert.assertTrue(topo.isEnabledDirectly());
			Assert.assertFalse(topo.isEffectivelyEnabled());
		}
		topo.setEnabledByParent(true);
		topo.setEnabledDirectly(false);
		{
			Boolean value = persistence.get(propKey, Boolean.class, null);
			Assert.assertFalse(value);
			Assert.assertTrue(topo.isEnabledByParent());
			Assert.assertFalse(topo.isEnabledDirectly());
			Assert.assertFalse(topo.isEffectivelyEnabled());
		}
		topo.setEnabledDirectly(true);
		{
			Boolean value = persistence.get(propKey, Boolean.class, null);
			Assert.assertTrue(value);
			Assert.assertTrue(topo.isEnabledByParent());
			Assert.assertTrue(topo.isEnabledDirectly());
			Assert.assertTrue(topo.isEffectivelyEnabled());
		}
		topo.setEnabledDirectly(false);
		{
			Boolean value = persistence.get(propKey, Boolean.class, null);
			Assert.assertFalse(value);
			Assert.assertTrue(topo.isEnabledByParent());
			Assert.assertFalse(topo.isEnabledDirectly());
			Assert.assertFalse(topo.isEffectivelyEnabled());
		}
		auto.reload();
		someRandomTopo = topTopo.getChildren().get(0).getChildren().get(0).getChildren().get(0);
		topo = (BaseToggleableTopo) someRandomTopo;
		{
			Boolean value = persistence.get(propKey, Boolean.class, null);
			Assert.assertFalse(value);
			Assert.assertTrue(topo.isEnabledByParent());
			Assert.assertFalse(topo.isEnabledDirectly());
			Assert.assertFalse(topo.isEffectivelyEnabled());
		}

	}

}
