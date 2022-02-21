package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoHandlerInstanceProvider;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.state.PlayerChangedJobEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static gg.xp.xivdata.data.Job.SCH;

public class JobOverlay extends XivOverlay implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(JobOverlay.class);

	private final AutoHandlerInstanceProvider instanceProvider;
	private final XivState state;
	private Container current;
	private volatile Job previous;
	private static final Map<Job, Class<? extends Container>> jobMapping
			= Map.of(
			SCH, SchOverlay.class
	);

	public JobOverlay(PersistenceProvider persistence, AutoHandlerInstanceProvider instanceProvider, XivState state) {
		super("Job-Specific", "job-specific-overlay", persistence);
		this.instanceProvider = instanceProvider;
		this.state = state;
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
		@Nullable Container newCurrent;
		if (newJob == null) {
			newCurrent = null;
		}
		else {
			Class<? extends Container> newOverlayClass = jobMapping.get(newJob);
			if (newOverlayClass == null) {
				newCurrent = null;
			}
			else {
				newCurrent = instanceProvider.getInstance(newOverlayClass);
			}
		}
		SwingUtilities.invokeLater(() -> {
			if (current != null) {
				current.setVisible(false);
				getPanel().remove(current);
			}
			current = newCurrent;
			if (current != null) {
				getPanel().add(current);
				current.setVisible(true);
				repackSize();
			}
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
