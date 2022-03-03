package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.misc.pulls.Pull;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.function.Predicate;

public class PullNumberFilter implements VisualFilter<Event> {
	private static final Predicate<Event> NO_FILTER = e -> true;
	private final PullTracker pt;
	private final Runnable filterUpdatedCallback;
	private final JTextField textBox;
	private Predicate<Event> filter = NO_FILTER;

	public PullNumberFilter(PullTracker pt, Runnable filterUpdatedCallback) {
		this.pt = pt;
		this.filterUpdatedCallback = filterUpdatedCallback;
		this.textBox = new TextFieldWithValidation<>(this::makeFilter, this::setFilter, "");
		textBox.setColumns(3);
	}

	private void setFilter(Predicate<Event> filter) {
		this.filter = filter;
		filterUpdatedCallback.run();
	}

	private Predicate<Event> makeFilter(String input) {
		if (input == null || input.isEmpty()) {
			return NO_FILTER;
		}
		// TODO: ranges and multiple selections
		int pullNum = Integer.parseInt(input);
		Pull pull = pt.getPull(pullNum);
		if (pull == null) {
			throw new IllegalArgumentException(String.format("Pull %s does not exist", pullNum));
		}
		Instant earliest = pull.getStart().getPumpedAt();
		Instant latest = pull.getEnd() == null ? Instant.MAX : pull.getEnd().getPumpFinishedAt();
		// Intentionally mismatching pumped-at vs pump-finished-at to allow for some leeway
		// Using ! and backwards comparisons also takes care of them being exactly equal.
		return e -> !e.getPumpFinishedAt().isBefore(earliest) && !e.getPumpedAt().isAfter(latest);
	}

	// TODO: is there a better way?
	public void setPullNumberExternally(int pullNum) {
		textBox.setText(Integer.toString(pullNum));
	}

	@Override
	public boolean passesFilter(Event item) {
		return filter.test(item);
	}

	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel("Pull: ");
		label.setLabelFor(textBox);
		panel.add(label);
		panel.add(textBox);
		return panel;
	}
}
