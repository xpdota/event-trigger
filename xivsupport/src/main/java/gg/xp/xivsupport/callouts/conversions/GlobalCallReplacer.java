package gg.xp.xivsupport.callouts.conversions;

import tools.jackson.core.type.TypeReference;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;

import java.util.Collections;
import java.util.List;

@ScanMe
public class GlobalCallReplacer {

	private final CustomJsonListSetting<GlobalReplacement> setting;
	private List<GlobalReplacement> items = Collections.emptyList();

	public GlobalCallReplacer(PersistenceProvider pers) {
		this.setting = CustomJsonListSetting.builder(
						pers,
						new TypeReference<GlobalReplacement>() {
						},
						"callout-processor.global-replacements.replacements",
						"callout-processor.global-replacements.replacements-errors")
				.build();
		setting.addAndRunListener(this::refresh);
	}

	private void refresh() {
		items = setting.getItems();
	}

	public String doReplacements(String input, boolean isTts) {
		for (GlobalReplacement item : items) {
			if (item.find == null) {
				continue;
			}
			if (isTts && !item.tts) {
				continue;
			}
			else if (!isTts && !item.text) {
				continue;
			}
			input = item.find.matcher(input).replaceAll(item.replaceWith);
		}
		return input;
	}

	public CustomJsonListSetting<GlobalReplacement> getReplacements() {
		return setting;
	}
}
