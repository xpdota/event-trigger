package gg.xp.reevent.scan;

public interface AutoHandlerInstanceProvider {

	<X> X getInstance(Class<X> clazz);

	void preAdd(Class<?> clazz);

}
