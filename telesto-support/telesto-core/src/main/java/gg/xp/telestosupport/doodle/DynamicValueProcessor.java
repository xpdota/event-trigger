package gg.xp.telestosupport.doodle;

public interface DynamicValueProcessor {

	<X> X process(String input, Class<X> outputType);


}
