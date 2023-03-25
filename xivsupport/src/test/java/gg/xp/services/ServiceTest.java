package gg.xp.services;

import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class ServiceTest {

	private static final class TestService extends ServiceSelector {

		protected TestService(StringSetting setting) {
			super(setting);
		}

		@Override
		protected String name() {
			return "Test Service";
		}

		@Override
		protected int defaultOptionPriority() {
			return 5;
		}
	}

	@Test
	void doTheTest() {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		StringSetting setting = new StringSetting(pers, "my-setting", null);
		TestService serv = new TestService(setting);
		{
			List<ServiceHandle> options = serv.getOptions();
			MatcherAssert.assertThat(options, Matchers.hasSize(1));
			ServiceHandle defaultOpt = options.get(0);
			Assert.assertTrue(defaultOpt.enabled());
		}
		ServiceHandle hand0 = serv.getOptions().get(0);
		// Register something with higher priority than the default, should switch over automatically
		ServiceDescriptor desc1 = new ServiceDescriptor() {
			@Override
			public String name() {
				return "Service 1";
			}

			@Override
			public String id() {
				return "serv1";
			}

			@Override
			public int priority() {
				return 9;
			}
		};
		ServiceHandle hand1 = serv.register(desc1);
		Assert.assertFalse(hand0.enabled());
		Assert.assertTrue(hand1.enabled());

		// Register something with even higher priority than the previous option, should switch
		ServiceDescriptor desc2 = new ServiceDescriptor() {
			@Override
			public String name() {
				return "Service 2";
			}

			@Override
			public String id() {
				return "serv2";
			}

			@Override
			public int priority() {
				return 11;
			}
		};
		ServiceHandle hand2 = serv.register(desc2);
		Assert.assertFalse(hand0.enabled());
		Assert.assertFalse(hand1.enabled());
		Assert.assertTrue(hand2.enabled());

		hand1.setEnabled();
		Assert.assertFalse(hand0.enabled());
		Assert.assertTrue(hand1.enabled());
		Assert.assertFalse(hand2.enabled());

		hand0.setEnabled();
		Assert.assertTrue(hand0.enabled());
		Assert.assertFalse(hand1.enabled());
		Assert.assertFalse(hand2.enabled());

		serv.delete();
		Assert.assertFalse(hand0.enabled());
		Assert.assertFalse(hand1.enabled());
		Assert.assertTrue(hand2.enabled());
	}

}
