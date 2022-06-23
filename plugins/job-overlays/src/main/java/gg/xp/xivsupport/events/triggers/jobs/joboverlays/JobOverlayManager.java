package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoHandlerInstanceProvider;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.state.PlayerChangedJobEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Map;

import static gg.xp.xivdata.data.Job.SCH;
import static gg.xp.xivdata.data.Job.SGE;

public class JobOverlayManager extends XivOverlay implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(JobOverlayManager.class);

	private final AutoHandlerInstanceProvider instanceProvider;
	private final XivState state;
	private final RefreshLoop<JobOverlayManager> refresher;
	private BaseJobOverlay current;
	private volatile Job previous;
	private static final Map<Job, Class<? extends BaseJobOverlay>> jobMapping
			= Map.of(
			SCH, SchOverlay.class,
			SGE, SgeOverlay.class
	);

	public JobOverlayManager(PersistenceProvider persistence, AutoHandlerInstanceProvider instanceProvider, OverlayConfig oc, XivState state) {
		super("Job-Specific", "job-specific-overlay", oc, persistence);
		this.instanceProvider = instanceProvider;
		this.state = state;
		refresher = new RefreshLoop<>("JobOverlay", this, JobOverlayManager::periodicRefresh, dt -> calculateScaledFrameTime(100));
		refresher.start();
	}

	private void periodicRefresh() {
		BaseJobOverlay cur = current;
		if (cur != null) {
			cur.periodicRefresh();
		}
	}



	@HandleEvents
	public void jobChange(EventContext context, PlayerChangedJobEvent event) {
		changeJob(state.getPlayerJob());
	}

	private void changeJob(Job newJob) {
		log.info("Job Overlay Change: {} -> {}", previous, newJob);
		if (previous == newJob) {
			return;
		}
		previous = newJob;
		@Nullable BaseJobOverlay newCurrent;
		if (newJob == null) {
			newCurrent = null;
		}
		else {
			Class<? extends BaseJobOverlay> newOverlayClass = jobMapping.get(newJob);
			if (newOverlayClass == null) {
				newCurrent = null;
			}
			else {
				newCurrent = instanceProvider.getInstance(newOverlayClass);
			}
		}
		SwingUtilities.invokeLater(() -> {
			log.info("Switching job panel (to {}). null before: {}, null after: {}", newJob, current == null, newCurrent == null);
			JPanel panel = getPanel();
			if (current != null) {
				log.info("Removing old panel");
				current.setVisible(false);
				panel.remove(current);
			}
			current = newCurrent;
			if (current != null) {
				log.info("Adding new panel");
				current.setVisible(true);
				panel.add(current);
				repackSize();
			}
			refresher.refreshNow();
//			getFrame().repaint();
			panel.repaint();
		});
	}

//	@Override
//	public void setVisible(boolean visible) {
//		if (visible) {
//			changeJob(state.getPlayerJob());
//		}
//		super.setVisible(visible);
//	}

	@Override
	public boolean enabled(EventContext context) {
		return getEnabled().get();
	}
}
