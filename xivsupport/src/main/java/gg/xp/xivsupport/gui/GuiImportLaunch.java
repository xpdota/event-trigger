package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.fflogs.FflogsController;
import gg.xp.xivsupport.events.fflogs.FflogsFight;
import gg.xp.xivsupport.events.fflogs.FflogsReportLocator;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.imprt.ACTLogImportSpec;
import gg.xp.xivsupport.gui.imprt.EventIterator;
import gg.xp.xivsupport.gui.imprt.FflogsImportSpec;
import gg.xp.xivsupport.gui.imprt.ImportSpec;
import gg.xp.xivsupport.gui.imprt.MainImportController;
import gg.xp.xivsupport.gui.imprt.SessionImportSpec;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.CatchFatalError;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.sys.XivMain;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GuiImportLaunch {
	private static final Logger log = LoggerFactory.getLogger(GuiImportLaunch.class);
	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private static JCheckBox decompressCheckbox;
	private static JFrame frame;
	private static JLabel statusLabel;
	private static MainImportController importController;

	private GuiImportLaunch() {
	}

	public static void main(String[] args) {
		log.info("GUI Import Init");
		CatchFatalError.run(CommonGuiSetup::setup);
		SwingUtilities.invokeLater(() -> CatchFatalError.run(() -> {
			importController = new MainImportController(XivMain.importPersProvider());
			frame = new JFrame("Triggevent Import");
			frame.setLocationByPlatform(true);
			JPanel panel = new TitleBorderFullsizePanel("Import");

			statusLabel = new JLabel("Please select something to import");

			decompressCheckbox = new JCheckBox("Decompress events (uses more memory)");
			Path sessionsDir = Paths.get(Platform.getTriggeventDir().toString(), "sessions");
			JFileChooser sessionChooser = new JFileChooser(sessionsDir.toString());
			sessionChooser.setPreferredSize(new Dimension(800, 600));
			JButton importSessionButton = new JButton("Import Session");
			importSessionButton.addActionListener(e -> {
				int result = sessionChooser.showOpenDialog(panel);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = sessionChooser.getSelectedFile();
					startImport(new SessionImportSpec(file, decompress()), true);
				}
			});


			Path actLogDir = Platform.getActDir();
			JFileChooser actLogChooser = new JFileChooser(actLogDir.toString());
			actLogChooser.setPreferredSize(new Dimension(800, 600));
			JButton importActLogButton = new JButton("Import ACT Log");
			importActLogButton.addActionListener(e -> {
				int result = actLogChooser.showOpenDialog(panel);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = actLogChooser.getSelectedFile();
					startImport(new ACTLogImportSpec(file), true);
				}
			});

			FflogsController fflogsController = new FflogsController(XivMain.persistenceProvider());
			boolean hasApiKey = !(fflogsController.clientSecret().get().isEmpty()
					|| fflogsController.clientSecret().get().isEmpty());

			JButton importFflogsButton = new JButton("Import from FFLogs");
			Mutable<FflogsReportLocator> locator = new MutableObject<>();
			TextFieldWithValidation<FflogsReportLocator> fflogsUrlField = new TextFieldWithValidation<>(url -> {
				FflogsReportLocator result;
				try {
					result = FflogsReportLocator.fromURL(url);
				}
				catch (Throwable t) {
					importFflogsButton.setEnabled(false);
					throw t;
				}
				importFflogsButton.setEnabled(true);
				return result;
			}, locator::setValue, "");
			fflogsUrlField.setPlaceholderText("Paste your FFLogs URL here");
			ActionListener fflogsAction = l -> {
				FflogsReportLocator fight = locator.getValue();
				if (!fight.fightSpecified()) {
					FflogsReportLocator finalReport = fight;
					DecimalFormat percentFormat = new DecimalFormat("0.#%");
					TableWithFilterAndDetails<FflogsFight, Object> table = TableWithFilterAndDetails.builder("Choose a Fight",
									() -> fflogsController.getFights(finalReport.report()))
							.addMainColumn(new CustomColumn<>("Fight #", FflogsFight::id, 50))
							.addMainColumn(new CustomColumn<>("Zone", f -> f.zone().getName()))
							.addMainColumn(new CustomColumn<>("Kill/Wipe", f -> f.kill() ? "Kill" : "Wipe", 100))
							.addMainColumn(new CustomColumn<>("Percent", f -> f.kill() ? "" : percentFormat.format(f.fightPercentage()), 100))
							.addMainColumn(new CustomColumn<>("Duration", f -> DurationFormatUtils.formatDuration(f.duration().toMillis(), "m:ss"), 100))
							.build();

					FflogsFight item = ChooserDialog.chooserReturnItem(null, table);
					if (item == null) {
						System.exit(0);
					}
					fight = fight.withFight(item.id());
				}
				startImport(new FflogsImportSpec(fight.report(), fight.fight()), true);
			};
			importFflogsButton.addActionListener(fflogsAction);
			fflogsUrlField.addActionListener(fflogsAction);
			fflogsUrlField.setEnabled(hasApiKey);
			importFflogsButton.setEnabled(hasApiKey);
			ReadOnlyText fflogsImportText = new ReadOnlyText("In order to use FFLogs importing, launch the client normally, and then enter your API keys in Advanced > FFLogs.");

			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			Insets fullInsets = new Insets(10, 10, 10, 10);
			c.insets = fullInsets;
			c.fill = GridBagConstraints.HORIZONTAL;
			{
				c.weightx = 1;
				c.gridwidth = GridBagConstraints.REMAINDER;
				JLabel importLabel = new JLabel("Please select a file to import, or drag and drop a file onto this window.");
				panel.add(importLabel, c);
			}
			{
				c.anchor = GridBagConstraints.LINE_START;
				c.gridy++;
				c.gridwidth = 1;
				c.weightx = 0;
				panel.add(importSessionButton, c);
				c.gridx++;
				c.weightx = 1;
				panel.add(new JLabel("Import a Triggevent Session"), c);
			}
			{
				c.gridy++;
				c.gridx = 0;
				c.gridwidth = 1;
				c.weightx = 0;
				panel.add(importActLogButton, c);
				c.gridx++;
				c.weightx = 1;
				panel.add(new JLabel("Import an ACT Log file"), c);
			}
			{
				c.gridx = 0;
				c.gridy++;
				c.gridwidth = GridBagConstraints.REMAINDER;
				panel.add(fflogsUrlField, c);
				c.insets = new Insets(0, 10, 10, 10);
				c.gridy++;
				c.gridx = 0;
				c.gridwidth = 1;
				c.weightx = 0;
				panel.add(importFflogsButton, c);
				c.gridx++;
				c.weightx = 1;
				panel.add(new JLabel("Import an FFLogs URL"), c);
				if (!hasApiKey) {
					c.gridx = 0;
					c.gridy++;
					c.gridwidth = GridBagConstraints.REMAINDER;
					panel.add(fflogsImportText, c);
				}
			}
			{
				c.insets = fullInsets;
				c.gridy++;
				c.weighty = 0;
				c.gridx = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;

				panel.add(decompressCheckbox, c);
			}
//			{
//				c.gridy++;
//				c.weighty = 0;
//				c.gridx = 0;
//				c.gridwidth = GridBagConstraints.REMAINDER;
//				panel.add(new ReadOnlyText("In replay mode, the program will use your existing settings, but any changes you make will not be saved."), c);
//			}
			{
				c.gridy++;
				c.weighty = 1;
				c.fill = GridBagConstraints.BOTH;
				// Filler
				CustomTableModel<ImportSpec<?>> tableModel = CustomTableModel.builder(importController::getRecentImports)
						.addColumn(new CustomColumn<>("Type", ImportSpec::typeLabel, 150))
						.addColumn(new CustomColumn<>("Item", ImportSpec::extendedLabel))
						.build();
				JTable table = tableModel.makeTable();
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				JScrollPane sp = new JScrollPane(table);
				TitleBorderPanel recentPanel = new TitleBorderPanel("Recent");
				recentPanel.setLayout(new BorderLayout());
				recentPanel.add(sp, BorderLayout.CENTER);
				JButton importRecentButton = new JButton("Import") {
					@Override
					public boolean isEnabled() {
						return tableModel.getSelectedValue() != null;
					}
				};
				table.getSelectionModel().addListSelectionListener(l -> importRecentButton.repaint());
				importRecentButton.addActionListener(l -> {
					ImportSpec<?> selected = tableModel.getSelectedValue();
					if (selected != null) {
						startImport(selected, false);
					}
				});
				recentPanel.add(importRecentButton, BorderLayout.SOUTH);
				panel.add(recentPanel, c);
			}

			{
				c.gridy++;
				c.weighty = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				panel.add(statusLabel, c);
			}


			frame.add(panel);
			frame.setSize(new Dimension(600, 600));
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setVisible(true);

			// adapted from https://stackoverflow.com/a/39415436
			frame.setTransferHandler(new TransferHandler() {
				@Override
				public boolean canImport(TransferSupport support) {
					try {
						return Arrays.stream(support.getDataFlavors()).anyMatch(DataFlavor::isFlavorJavaFileListType)
								&& verifyAndGetFile(support) != null;
					}
					// https://coderanch.com/t/664525/java/Invalid-Drag-Drop-Exception
					catch (InvalidDnDOperationException ignored) {
						return true;
					}
				}

				@SuppressWarnings("unchecked")
				private static @Nullable ImportSpec<?> verifyAndGetFile(TransferSupport support) {
					List<File> files;
					try {
						files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					}
					catch (UnsupportedFlavorException | IOException e) {
						return null;
					}
					// Not supporting multi-file import yet
					if (files.size() == 1) {
						File theFile = files.get(0);
						if (theFile.getName().toLowerCase(Locale.ROOT).endsWith(".log")) {
							return new ACTLogImportSpec(theFile);
						}
						else if (theFile.getName().toLowerCase(Locale.ROOT).endsWith(".oos.gz")) {
							return new SessionImportSpec(theFile, decompress());
						}
						else {
							return null;
						}
					}
					else {
						return null;
					}
				}

				@Override
				public boolean importData(TransferSupport support) {
					ImportSpec<?> importSpec = verifyAndGetFile(support);
					if (importSpec == null) {
						return false;
					}
					startImport(importSpec, true);
					return true;
				}
			});
		}));

	}

	private static boolean decompress() {
		return decompressCheckbox.isSelected();
	}

	private static <X extends Event> void startImport(ImportSpec<X> importSpec, boolean saveToRecents) {
		statusLabel.setText("Importing %s, Please Wait...".formatted(importSpec.typeLabel()));
		JPanel gp = (JPanel) frame.getGlassPane();
		gp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				e.consume();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				e.consume();
			}
		});
		gp.setVisible(true);
		exs.submit(() -> CatchFatalError.run(() -> {
			EventIterator<X> events = importController.readEvents(importSpec, saveToRecents);
			SwingUtilities.invokeLater(() -> statusLabel.setText("Read Events, Launching GUI."));
			importSpec.launch(events);
			hideFrame();
		}));
	}

	private static void hideFrame() {
		SwingUtilities.invokeLater(() -> frame.setVisible(false));
	}
}
