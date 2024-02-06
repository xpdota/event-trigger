package gg.xp.xivsupport.groovy;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.debug.DebugEvent;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkSlotRequest;
import gg.xp.xivsupport.gui.groovy.GroovyScriptHolder;
import gg.xp.xivsupport.gui.groovy.ScriptSettingsControl;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.intellij.lang.annotations.Language;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GroovyTriggersTest {
	// Tests:
	// global var (like 'state')
	// global var qualified with 'this' (like 'this.state') for legacy compat
	// additional script var (like 'scriptSettings')
	// additional script var qualified with 'this' (like 'scriptSettings') for legacy compat
	// script-level var without 'def'
	// script-level var with 'def'
	// var within closure without 'def'
	// var within closure with 'def'
	// test all of these both in the sq body, a simple 'then' closure, the initial condition, the name, and a callout
	// test diff concurrency modes
	// amhelper
	// test one overriding another level

	private static final String TEST_VAL = "bar";

	private static void doTest(@Language("groovy") String script) {
		doTest(script, p -> {
		});
	}

	private static void doTest(@Language("groovy") String script, Consumer<MutablePicoContainer> prep) {
		doTest(script, prep, p -> {
		});
	}

	private static void doTest(@Language("groovy") String script, Consumer<MutablePicoContainer> prep, Consumer<MutablePicoContainer> post) {
		String expected = TEST_VAL;
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		GroovyManager mgr = pico.getComponent(GroovyManager.class);
		AtomicReference<Object> ar = new AtomicReference<>();
		mgr.getGlobalBinding().setVariable("output", ar);
		prep.accept(pico);
		GroovyScriptManager gsm = pico.getComponent(GroovyScriptManager.class);
		GroovyScriptHolder scriptHolder = new GroovyScriptHolder();
		scriptHolder.setScriptMgr(gsm);
		scriptHolder.setScriptContent(script);
		GroovyScriptResult result = scriptHolder.run(dummySSC);
		if (!result.success()) {
			throw new RuntimeException("Script failed!", result.failure());
		}
		EventMaster em = pico.getComponent(EventMaster.class);
		em.pushEventAndWait(new DebugEvent(expected));
		Assert.assertEquals(ar.get(), expected);
		List<TtsRequest> tts = pico.getComponent(RawEventStorage.class).getEventsOfType(TtsRequest.class);
		if (tts.size() != 1) {
			throw new AssertionError(String.format("%s TTS found when one was expected", tts.size()));
		}
		else {
			MatcherAssert.assertThat(tts.get(0).getTtsString(), Matchers.equalTo(TEST_VAL));
		}
		post.accept(pico);
	}


	@Test
	void testGlobalVar() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == foo }
					sequence { e1, s ->
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", TEST_VAL);
		});
	}

	@Test
	void testGlobalVarQualified() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					named this.foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == this.foo }
					sequence { e1, s ->
						this.log.info this.foo
						this.output.set this.foo
						callout { tts this.foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", TEST_VAL);
		});
	}

	@Test
	void testSpecialVar() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					named scriptSettings.toString()
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == scriptSettings.toString() }
					sequence { e1, s ->
						log.info scriptSettings.toString()
						output.set scriptSettings.toString()
						callout { tts scriptSettings.toString() }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", TEST_VAL);
		});
	}

	@Test
	void testSpecialVarQualified() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					named this.scriptSettings.toString()
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == this.scriptSettings.toString() }
					sequence { e1, s ->
						this.log.info this.scriptSettings.toString()
						this.output.set this.scriptSettings.toString()
						callout { tts this.scriptSettings.toString() }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", TEST_VAL);
		});
	}

	@Test
	void testScriptVar() {
		@Language("groovy") String script = "foo = '" + TEST_VAL + "'\n" + """
				groovyTriggers.add {
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == foo }
					sequence { e1, s ->
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script);
	}

	@Test
	void testScriptVarQualified() {
		@Language("groovy") String script = "foo = '" + TEST_VAL + "'\n" + """
				groovyTriggers.add {
					named this.foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == this.foo }
					sequence { e1, s ->
						this.log.info this.foo
						this.output.set this.foo
						callout { tts this.foo }
					}
				}
				""";
		doTest(script);
	}

	@Test
	void testScriptVarOvr() {
		@Language("groovy") String script = "foo = '" + TEST_VAL + "'\n" + """
				groovyTriggers.add {
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == foo }
					sequence { e1, s ->
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", "baz");
		});
	}

	@Test
	void testScriptVarQualifiedOvr() {
		@Language("groovy") String script = "foo = '" + TEST_VAL + "'\n" + """
				groovyTriggers.add {
					named this.foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == this.foo }
					sequence { e1, s ->
						this.log.info this.foo
						this.output.set this.foo
						callout { tts this.foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", "baz");
		});
	}

	@Test
	void testScriptDefVar() {
		@Language("groovy") String script = "def foo = '" + TEST_VAL + "'\n" + """
				groovyTriggers.add {
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == foo }
					sequence { e1, s ->
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script);
	}

	// I don't think this should work
//	@Test
//	void testScriptDefVarQualified() {
//		@Language("groovy") String script = "def foo = '" + TEST_VAL + "'\n" + """
//				groovyTriggers.add {
//					named this.foo
//					concurrency concurrent
//					when { DebugEvent dbg -> dbg.value == this.foo }
//					sequence { e1, s ->
//						this.log.info this.foo
//						this.output.set this.foo
//						callout { tts this.foo }
//					}
//				}
//				""";
//		doTest(script, pico -> {
//		});
//	}


	@Test
	void testDefInGroovyTrigger() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					def foo = "bar"
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == foo }
					sequence { e1, s ->
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", TEST_VAL);
		});
	}

	@Test
	void testVarInSequence() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					named "test"
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == "bar" }
					sequence { e1, s ->
						foo = "bar"
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script);
	}

	@Test
	void testDefInSequence() {
		@Language("groovy") String script = """
				groovyTriggers.add {
					named "test"
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == "bar" }
					sequence { e1, s ->
						def foo = "bar"
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script);
	}

	@Test
	void testSqtVarOvr() {
		@Language("groovy") String script = "foo = 'BAD'\n" + """
				groovyTriggers.add {
//					def foo = "waz"
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == "bar" }
					sequence { e1, s ->
						foo = "bar"
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", "baz");
		});
	}

	@Test
	void testSqtDefOvr() {
		@Language("groovy") String script = "foo = 'BAD'\n" + """
				groovyTriggers.add {
					def foo = "waz"
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == "bar" }
					sequence { e1, s ->
						foo = "bar"
						log.info foo
						output.set foo
						callout { tts foo }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", "baz");
		});
	}

	@Test
	void testAmHelper() {
		@Language("groovy") String script = """
				mk2 = MarkerSign.ATTACK2           
				def mk3 = MarkerSign.ATTACK3           
				groovyTriggers.add {
					named "test"
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == "bar" }
					sequence { e1, s ->
						def foo = "bar"
						log.info foo
						output.set foo
						callout { tts foo }
						amHelper.mark { slot 1 with mk1 }
						amHelper.mark { slot 2 with this.mk1 }
						amHelper.mark { slot 3 with mk2 }
						amHelper.mark { slot 4 with this.mk2 }
						amHelper.mark { slot 5 with mk3 }
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("mk1", MarkerSign.ATTACK1);
		}, pico -> {
			List<SpecificAutoMarkSlotRequest> ams = pico.getComponent(RawEventStorage.class).getEventsOfType(SpecificAutoMarkSlotRequest.class);
			if (ams.size() != 5) {
				throw new AssertionError(String.format("%s AMS found when one was expected", ams.size()));
			}
			else {
				Assert.assertEquals(ams.get(0).getSlotToMark(), 1);
				Assert.assertEquals(ams.get(0).getMarker(), MarkerSign.ATTACK1);
				Assert.assertEquals(ams.get(1).getSlotToMark(), 2);
				Assert.assertEquals(ams.get(1).getMarker(), MarkerSign.ATTACK1);
				Assert.assertEquals(ams.get(2).getSlotToMark(), 3);
				Assert.assertEquals(ams.get(2).getMarker(), MarkerSign.ATTACK2);
				Assert.assertEquals(ams.get(3).getSlotToMark(), 4);
				Assert.assertEquals(ams.get(3).getMarker(), MarkerSign.ATTACK2);
				Assert.assertEquals(ams.get(4).getSlotToMark(), 5);
				Assert.assertEquals(ams.get(4).getMarker(), MarkerSign.ATTACK3);
			}

		});
	}

	@Test
	void testNonSequential() {
		@Language("groovy") String script = "foo = 'BAD'\n" + """
				groovyTriggers.add {
//					def foo = "waz"
					named foo
					concurrency concurrent
					when { DebugEvent dbg -> dbg.value == "bar" }
					then { e, ctx ->
						def foo = "bar"
						log.info foo
						output.set foo
						ctx.accept(new TtsRequest(foo))
					}
				}
				""";
		doTest(script, pico -> {
			GroovyManager mgr = pico.getComponent(GroovyManager.class);
			mgr.getGlobalBinding().setVariable("foo", "baz");
		});
	}

	private static final ScriptSettingsControl dummySSC = new ScriptSettingsControl() {
		@Override
		public void requestRunOnStartup() {
		}

		@Override
		public String toString() {
			return TEST_VAL;
		}
	};
}
