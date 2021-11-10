package gg.xp.scan;

public interface AutoHandlerInstanceProvider {

	<X> X getInstance(Class<X> clazz);

}
