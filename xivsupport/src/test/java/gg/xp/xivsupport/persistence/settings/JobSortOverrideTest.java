package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

import java.util.List;

public class JobSortOverrideTest {

	private record TestData(JobSortSetting parent, JobSortOverrideSetting child) {

	}

	private TestData makeData() {
		MutablePicoContainer pico = XivMain.testingMinimalInit();
		PersistenceProvider pers = pico.getComponent(PersistenceProvider.class);
		XivState state = pico.getComponent(XivState.class);
		JobSortSetting parent = new JobSortSetting(pers, "sample-setting", state);
		JobSortOverrideSetting child = new JobSortOverrideSetting(pers, "sample-override", state, parent);
		return new TestData(parent, child);
	}

	@Test
	void testBasic() {
		TestData data = makeData();
		data.parent.setJobOrderPartial(List.of(Job.WHM, Job.DRK, Job.BRD));
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.WHM));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.DRK));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.BRD));
			MatcherAssert.assertThat(childOrder, Matchers.equalTo(data.parent.getJobOrder()));
		}
		data.parent.setJobOrderPartial(List.of(Job.SGE, Job.DRK, Job.BRD));
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SGE));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.DRK));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.BRD));
			MatcherAssert.assertThat(childOrder, Matchers.equalTo(data.parent.getJobOrder()));
		}
		data.child.getEnabled().set(true);
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SGE));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.DRK));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.BRD));
			MatcherAssert.assertThat(childOrder, Matchers.equalTo(data.parent.getJobOrder()));
		}
		data.child.setJobOrderPartial(List.of(Job.SCH, Job.PLD, Job.MNK));
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SCH));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.PLD));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.MNK));
		}
		data.child.getEnabled().set(false);
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SGE));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.DRK));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.BRD));
		}
		data.child.getEnabled().set(true);
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SCH));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.PLD));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.MNK));
		}
		data.child.resetJobOrder();
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SGE));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.DRK));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.BRD));
			MatcherAssert.assertThat(childOrder, Matchers.equalTo(data.parent.getJobOrder()));
		}
		data.child.setJobOrderPartial(List.of(Job.SCH, Job.PLD, Job.MNK));
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SCH));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.PLD));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.MNK));
		}
		data.parent.setJobOrderPartial(List.of(Job.AST, Job.WAR, Job.DRG));
		{
			List<Job> childOrder = data.child.getJobOrder();
			MatcherAssert.assertThat(childOrder.get(0), Matchers.equalTo(Job.SCH));
			MatcherAssert.assertThat(childOrder.get(1), Matchers.equalTo(Job.PLD));
			MatcherAssert.assertThat(childOrder.get(2), Matchers.equalTo(Job.MNK));
		}
	}

}
