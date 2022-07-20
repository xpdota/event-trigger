package gg.xp.xivsupport.cdsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ScanMe
public class CustomCooldownManager {

	private static final Logger log = LoggerFactory.getLogger(CustomCooldownManager.class);
	private static final String settingKey = "custom-cooldowns.my-cooldowns";
	private static final String failedTriggersSettingKey = "custom-cooldowns.failed-to-load";

	private List<CustomCooldown> cds;
	private final ObjectMapper mapper = new ObjectMapper();
	private final PersistenceProvider pers;
	private final EventMaster master;

	public CustomCooldownManager(PersistenceProvider pers, EventMaster master) {
		this.pers = pers;
		this.master = master;
		String strVal = pers.get(settingKey, String.class, null);
		// TODO: dedup with EasyTriggers
		List<CustomCooldown> cds = new ArrayList<>();
		if (strVal != null) {
			try {
				// First, convert to List<JsonNode> so that errors can be reported for individual cds
				List<JsonNode> jsonNodes = mapper.readValue(strVal, new TypeReference<>() {
				});
				List<JsonNode> failed = new ArrayList<>();
				for (JsonNode jsonNode : jsonNodes) {
					try {
						CustomCooldown cd = mapper.convertValue(jsonNode, CustomCooldown.class);
						cds.add(cd);
					}
					catch (Throwable jpe) {
						log.error("Trigger failed to load: \n{}\n", jsonNode, jpe);
						failed.add(jsonNode);
					}
				}
				if (!failed.isEmpty()) {
					String failedSetting = pers.get(failedTriggersSettingKey, String.class, "[]");
					List<String> otherFailues = mapper.readValue(failedSetting, new TypeReference<>() {
					});
					List<String> failures = new ArrayList<>(otherFailues);
					failures.addAll(jsonNodes.stream().map(Object::toString).toList());
					pers.save(failedTriggersSettingKey, mapper.writeValueAsString(failures));
					log.error("One or more custom cooldowns failed to load - they have been saved to the setting '{}'", failedTriggersSettingKey);
				}
			}
			catch (Throwable e) {
				log.error("Error loading Custom Cooldowns", e);
				log.error("Dump of CD data:\n{}", strVal);
				throw new RuntimeException("There was an error loading Custom Cooldowns. Check the log.", e);
			}
			log.info("Successfully loaded easy cds");
		}
		this.cds = cds;
	}

	private void makeListWritable() {
		if (!(cds instanceof ArrayList<?>)) {
			cds = new ArrayList<>(cds);
		}
	}


	public List<CustomCooldown> getCooldowns() {
		return Collections.unmodifiableList(cds);
	}

	public void addCooldown(CustomCooldown cooldown) {
		makeListWritable();
		cds.add(cooldown);
		commit();
	}

	public void removeCooldown(CustomCooldown cooldown) {
		makeListWritable();
		cds.remove(cooldown);
		commit();
	}


	public void commit() {
		try {
			String cdsSerialized = mapper.writeValueAsString(cds);
			pers.save(settingKey, cdsSerialized);
			master.pushEvent(new CustomCooldownsUpdated());
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}


}
