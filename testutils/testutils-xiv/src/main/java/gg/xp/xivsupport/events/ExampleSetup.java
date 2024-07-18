package gg.xp.xivsupport.events;

import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.testng.Assert;

public final class ExampleSetup {
	private ExampleSetup() {
	}

	public static MutablePicoContainer setup(PersistenceProvider pers) {
		// TODO: make this a template for integration testing
		MutablePicoContainer container = XivMain.testingMasterInit();
		container.removeComponent(InMemoryMapPersistenceProvider.class);
		container.addComponent(pers);
		EventDistributor dist = container.getComponent(EventDistributor.class);
		doEvents(dist);
		finishSetup(container);
		return container;
	}

	public static MutablePicoContainer setup() {
		// TODO: make this a template for integration testing
		MutablePicoContainer container = XivMain.testingMasterInit();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		doEvents(dist);
		finishSetup(container);
		return container;
	}

	private static void finishSetup(PicoContainer container) {
		BasicEventQueue queue = container.getComponent(BasicEventQueue.class);
		queue.waitDrain();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		XivState state = container.getComponent(XivStateImpl.class);
		// TODO: find actual solution to race conditions in tests
		try {
			Assert.assertEquals(state.getPartyList().size(), 8);
			Assert.assertEquals(state.getCombatantsListCopy().size(), 8);
		}
		catch (Throwable e) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			Assert.assertEquals(state.getPartyList().size(), 8);
			Assert.assertEquals(state.getCombatantsListCopy().size(), 8);
		}
		queue.waitDrain();
	}

	private static void doEvents(EventDistributor dist) {

		dist.acceptEvent(new InitEvent());
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":22,\"charName\":\"Foo Bar\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":777,\"zoneName\":\"the Weapon's Refrain (Ultimate)\"}"));
		// This player should be sorted first because they are the actual player
		dist.acceptEvent(new ActWsRawMsg("""
				{
				  "type": "PartyChanged",
				  "party": [
				    {
				      "id": "10",
				      "name": "Player One",
				      "worldId": 63,
				      "job": 22,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "13",
				      "name": "Random Person",
				      "worldId": 65,
				      "job": 21,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "12",
				      "name": "Who Dis",
				      "worldId": 73,
				      "job": 25,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "11",
				      "name": "Some Guy",
				      "worldId": 65,
				      "job": 27,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "14",
				      "name": "Other Alliance",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "16",
				      "name": "Foo Bar",
				      "worldId": 65,
				      "job": 24,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "15",
				      "name": "Pf Hero",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "17",
				      "name": "Last Guy",
				      "worldId": 65,
				      "job": 38,
				      "level": 0,
				      "inParty": true
				    }
				  ]
				}
				"""));
		dist.acceptEvent(new ActWsRawMsg(
				"""
						{
						  "combatants": [
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 16,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 22,
						      "Level": 80,
						      "Name": "Player One",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 17,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 27,
						      "Level": 80,
						      "Name": "Some Guy",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 18,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 25,
						      "Level": 80,
						      "Name": "Who Dis",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 19,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 21,
						      "Level": 80,
						      "Name": "Random Person",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 20,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 33,
						      "Level": 80,
						      "Name": "Other Alliance",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 21,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 33,
						      "Level": 80,
						      "Name": "Pf Hero",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 22,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 24,
						      "Level": 80,
						      "Name": "Foo Bar",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 23,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 38,
						      "Level": 80,
						      "Name": "Last Guy",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    }
						  ],
						  "rseq": 0
						}"""
		));

	}
}
