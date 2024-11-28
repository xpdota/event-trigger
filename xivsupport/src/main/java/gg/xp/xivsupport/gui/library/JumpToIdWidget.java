package gg.xp.xivsupport.gui.library;

import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.sys.Threading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class JumpToIdWidget<X> {
	private static final Logger log = LoggerFactory.getLogger(JumpToIdWidget.class);
	private static final String fieldLabel = "Jump to ID";
	private final TextFieldWithValidation<@Nullable Long> textBox;
	private final TableWithFilterAndDetails<X, ?> tbl;
	private final Function<X, Long> idExtractor;

	public JumpToIdWidget(TableWithFilterAndDetails<X, ?> tbl, Function<X, Long> idExtractor) {
		this.tbl = tbl;
		this.idExtractor = idExtractor;
		textBox = new TextFieldWithValidation<>(JumpToIdWidget::parse, this::processInput, "");
		textBox.setToolTipText("Enter an ID in base 10 (1234) or 16 (0x12AB)");
	}

	public static <X> @NotNull Component create(TableWithFilterAndDetails<X, ?> tbl, Function<X, Long> idExtractor) {
		return new JumpToIdWidget<>(tbl, idExtractor).getComponent();
	}

	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JLabel label = new JLabel(fieldLabel + ": ");
		label.setLabelFor(textBox);
		panel.add(label);
		panel.add(textBox);
		return panel;
	}

	private static @Nullable Long parse(String input) {
		if (input.isEmpty()) {
			return null;
		}
		else {
			if (input.startsWith("0x")) {
				return Long.parseLong(input.substring(2).trim(), 16);
			}
			return Long.parseLong(input, 10);
		}
	}

	private final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("JumpToIdWidgetDebounce"));
	private final AtomicInteger debounceCounter = new AtomicInteger();

	private void processInput(@Nullable Long valueMaybe) {
		if (valueMaybe == null) {
			return;
		}
		// Unbox once
		long value = valueMaybe;
		int expected = debounceCounter.incrementAndGet();
		exs.submit(() -> {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException ignored) {
				//
			}
			// debounce
			if (expected != debounceCounter.get()) {
				return;
			}
			SwingUtilities.invokeLater(() -> {
				List<X> data = tbl.getMainModel().getData();
				for (X item : data) {
					if (idExtractor.apply(item) == value) {
						tbl.setAndScrollToSelection(item);
						return;
					}
				}
			});
		});
	}
}
