package gg.xp.xivsupport.gui.tabs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddonDef {

	@JsonProperty("name")
	public String name;
	@JsonProperty("dir_name")
	public String dirName;
	@JsonProperty("info_url")
	public String url;
	@JsonProperty("url_pattern")
	public String urlPattern;
	@JsonProperty("icon_url")
	public String iconUrl;
	@JsonProperty("description")
	public String description;

	public void readFrom(AddonDef that) {
		this.name = that.name;
		this.dirName = that.dirName;
		this.url = that.url;
		this.urlPattern = that.urlPattern;
		this.iconUrl = that.iconUrl;
		this.description = that.description;
	}

}
