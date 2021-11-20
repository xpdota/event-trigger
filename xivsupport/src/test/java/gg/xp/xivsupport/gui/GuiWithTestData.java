package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.DummyEventToForceAutoScan;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class GuiWithTestData {
	public static void main(String[] args) {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.addComponent(GuiMain.class);
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		dist.acceptEvent(new DummyEventToForceAutoScan());
		pico.getComponent(GuiMain.class);

		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":22,\"charName\":\"Foo Bar\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":777,\"zoneName\":\"the Weapon's Refrain (Ultimate)\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\n" +
				"  \"type\": \"PartyChanged\",\n" +
				"  \"party\": [\n" +
				"    {\n" +
				"      \"id\": \"10\",\n" +
				"      \"name\": \"Player One\",\n" +
				"      \"worldId\": 63,\n" +
				"      \"job\": 22,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"13\",\n" +
				"      \"name\": \"Random Person\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 21,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"12\",\n" +
				"      \"name\": \"Who Dis\",\n" +
				"      \"worldId\": 73,\n" +
				"      \"job\": 25,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"11\",\n" +
				"      \"name\": \"Some Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 26,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"14\",\n" +
				"      \"name\": \"Other Alliance\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 33,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"16\",\n" +
				"      \"name\": \"Foo Bar\",\n" + // This player should be sorted first because they are the actual player
				"      \"worldId\": 65,\n" +
				"      \"job\": 24,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"15\",\n" +
				"      \"name\": \"Pf Hero\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 33,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"17\",\n" +
				"      \"name\": \"Last Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 38,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    }\n" +
				"  ]\n" +
				"}\n"));
		dist.acceptEvent(new ActWsRawMsg(
				"{\n" +
						"  \"combatants\": [\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 16,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 22,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Player One\",\n" +
						"      \"CurrentHP\": 122700,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 17,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 26,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Some Guy\",\n" +
						"      \"CurrentHP\": 90700,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 18,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 25,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Who Dis\",\n" +
						"      \"CurrentHP\": 40000,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 19,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 21,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Random Person\",\n" +
						"      \"CurrentHP\": 109820,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 20,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 33,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Pf Hero\",\n" +
						"      \"CurrentHP\": 5421,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 21,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 33,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Other Alliance\",\n" +
						"      \"CurrentHP\": 65345,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 22,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 24,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Foo Bar\",\n" +
						"      \"CurrentHP\": 12270,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 23,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 38,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Last Guy\",\n" +
						"      \"CurrentHP\": 122700,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    }\n" +
						"  ],\n" +
						"  \"rseq\": 0\n" +
						"}"
		));

	}

	@Ignore
	@Test
	private void guiWithTestData() {
	}

}
