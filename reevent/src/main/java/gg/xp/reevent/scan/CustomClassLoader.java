package gg.xp.reevent.scan;

public class CustomClassLoader extends ClassLoader{

	public CustomClassLoader() {
		super(null);
	}

	@Override
	public String getName() {
		return "Custom Loader";
	}
}
