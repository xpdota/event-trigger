package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoHandlerInstanceProvider;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.state.PlayerChangedJobEvent;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static gg.xp.xivdata.jobs.Job.SCH;

public class JobOverlay extends XivOverlay implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(JobOverlay.class);

	private final AutoHandlerInstanceProvider instanceProvider;
	private Container current;
	private static final Map<Job, Class<? extends Container>> jobMapping
			= Map.of(
			SCH, SchOverlay.class
	);

	public JobOverlay(PersistenceProvider persistence, AutoHandlerInstanceProvider instanceProvider) {
		super("Job-Specific", "job-specific-overlay", persistence);
		this.instanceProvider = instanceProvider;
	}

	@HandleEvents
	public void jobChange(EventContext context, PlayerChangedJobEvent event) {
		Job newJob = event.getNewJob();
		changeJob(newJob);
	}

	private void changeJob(Job newJob) {
		log.info("Job Overlay Change: {} -> {}", current, newJob);
		if (current != null) {
			current.setVisible(false);
			SwingUtilities.invokeLater(() -> getPanel().remove(current));
			current = null;
		}
		Class<? extends Container> newOverlayClass = jobMapping.get(newJob);
		if (newOverlayClass == null) {
			return;
		}
		current = instanceProvider.getInstance(newOverlayClass);
		SwingUtilities.invokeLater(() -> {
			getPanel().add(current);
			current.setVisible(true);
			repackSize();
		});
	}

	@Override
	public boolean enabled(EventContext context) {
		return getEnabled().get();
	}
}
