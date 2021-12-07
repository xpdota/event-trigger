package gg.xp.xivsupport.gui;


import org.swingexplorer.Launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GuiLaunchSwexp {
	private GuiLaunchSwexp() {
	}

	public static void main(String[] args) {
		String mainClass = GuiLaunch.class.getCanonicalName();
		List<String> swexpArgs = new ArrayList<>();
		swexpArgs.add(mainClass);
		swexpArgs.addAll(Arrays.asList(args));
		Launcher.main(swexpArgs.toArray(String[]::new));
	}

}