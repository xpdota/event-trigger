package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.StringSetting;

@ScanMe
public class FflogsController {

	private final StringSetting clientId;
	private final StringSetting secret;

	public FflogsController(PersistenceProvider pers) {
		clientId = new StringSetting(pers, "fflogs-support.client-id", "");
		secret = new StringSetting(pers, "fflogs-support.secret", "");
	}

	public StringSetting clientId() {
		return clientId;
	}

	public StringSetting clientSecret() {
		return secret;
	}

	public JsonNode downloadReport(FflogsReportLocator reportLocator) {
		FflogsClient client = new FflogsClient(this);
		client.init();
		String query = String.format("""
				{
				 reportData
				 {
				  report(code:"%s")
				  {
				   code
				   startTime
				   endTime
				   masterData {
				    actors {
				      gameID,
				      id,
				      name,
				      petOwner,
				      type,
				      subType
				   
				     }
				    }
				    events(startTime:0, fightIDs:[%s], endTime:9639365786158, useAbilityIDs:true, includeResources:true, limit:10000) {
				    data,
				    nextPageTimestamp
				   }
				  }
				 }
				}
					
				""", reportLocator.report(), reportLocator.fight());
		return client.queryV2(query);


	}
}
