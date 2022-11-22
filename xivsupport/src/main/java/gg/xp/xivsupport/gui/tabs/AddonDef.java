package gg.xp.xivsupport.gui.tabs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddonDef {

	@JsonProperty("name")
	public String name;
	@JsonProperty("info_url")
	public String url;
	@JsonProperty("manifest_url")
	public String manifestUrl;
	@JsonProperty("icon_url")
	public String iconUrl;

}
