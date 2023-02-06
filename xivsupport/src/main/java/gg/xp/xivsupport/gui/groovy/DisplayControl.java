package gg.xp.xivsupport.gui.groovy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DisplayControl {


	public enum ListDisplayMode {
		AUTO,
		TABLE
	}

	private ListDisplayMode listDisplayMode = ListDisplayMode.AUTO;
	private List<String> listTableColumns = Collections.emptyList();

	public void listAuto() {
		listDisplayMode = ListDisplayMode.AUTO;
	}

	public void listAsTable(String... columns) {
		listDisplayMode = ListDisplayMode.TABLE;
		listTableColumns = Arrays.asList(columns);
	}

	public ListDisplayMode getListDisplayMode() {
		return listDisplayMode;
	}
	public List<String> getListTableColumns() {
		return Collections.unmodifiableList(listTableColumns);
	}

}
