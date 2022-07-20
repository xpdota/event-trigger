package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;

import javax.swing.*;

public final class GuiWithTestData {
	private GuiWithTestData() {
	}

	public static void main(String[] args) {
		try {
//			UIManager.setLookAndFeel(new DarculaLaf());
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.addComponent(GuiMain.class);
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		dist.acceptEvent(new InitEvent());
		pico.getComponent(GuiMain.class);

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
				      "job": 26,
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
						      "PosX": 100,
						      "PosY": 120,
						      "PosZ": 1.5,
						      "Heading": 0
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
						      "Job": 26,
						      "Level": 80,
						      "Name": "Some Guy",
						      "CurrentHP": 90700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 120,
						      "PosY": 120,
						      "PosZ": 1.5,
						      "Heading": 1
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
						      "CurrentHP": 40000,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 120,
						      "PosY": 100,
						      "PosZ": 1.5,
						      "Heading": 2
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
						      "CurrentHP": 109820,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 120,
						      "PosY": 80,
						      "PosZ": 1.5,
						      "Heading": 3
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
						      "Name": "Pf Hero",
						      "CurrentHP": 5421,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 100,
						      "PosY": 80,
						      "PosZ": 1.5,
						      "Heading": 4
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
						      "Name": "Other Alliance",
						      "CurrentHP": 65345,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 80,
						      "PosY": 80,
						      "PosZ": 1.5,
						      "Heading": 5
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
						      "CurrentHP": 12270,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 80,
						      "PosY": 100,
						      "PosZ": 1.5,
						      "Heading": 6
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
						      "PosX": 80,
						      "PosY": 120,
						      "PosZ": 1.5,
						      "Heading": 7
						    }
						  ],
						  "rseq": 0
						}"""
		));

	}
}
