package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.lang.GameLanguageInfoEvent;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AutoMarkHandlerTest {

	@Test
	void testAutoLang() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		AutoMarkHandler amh = pico.getComponent(AutoMarkHandler.class);
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.EN);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.German));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.DE);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.French));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.EN);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.Japanese));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.JP);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.Korean));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.JP);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.Chinese));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.JP);
	}

	@Test
	void testManualLang() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		AutoMarkHandler amh = pico.getComponent(AutoMarkHandler.class);
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.EN);
		amh.getLanguageSetting().set(AutoMarkLanguage.DE);
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.DE);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.French));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.DE);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.Japanese));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.DE);

		amh.getLanguageSetting().set(AutoMarkLanguage.JP);
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.JP);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.English));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.JP);
		dist.acceptEvent(new GameLanguageInfoEvent(GameLanguage.Chinese));
		Assert.assertEquals(amh.getEffectiveLanguage(), AutoMarkLanguage.JP);
	}

}