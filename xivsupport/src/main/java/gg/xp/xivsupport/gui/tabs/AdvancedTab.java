package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.events.misc.Management;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.events.misc.Stats;
import gg.xp.xivsupport.events.ws.ActWsLogSource;
import gg.xp.xivsupport.gui.KeyValueDisplaySet;
import gg.xp.xivsupport.gui.KeyValuePairDisplay;
import gg.xp.xivsupport.gui.Refreshable;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapperPanel;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingGui;
import gg.xp.xivsupport.persistence.gui.WsURISettingGui;
import gg.xp.xivsupport.sys.Threading;
import org.picocontainer.PicoContainer;
import org.swingexplorer.Launcher;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedTab extends SmartTabbedPane implements Refreshable, TabAware {

	private static final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("AdvancedTab"));
	private final KeyValueDisplaySet displayed;
	private final KeyValueDisplaySet mem;

	public AdvancedTab(PicoContainer container) {
		super(LEFT);
		{
			JPanel statsAndMemory = new JPanel();
			statsAndMemory.setLayout(new GridBagLayout());


			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = GridBagConstraints.NORTH;
			c.weightx = 1;
			c.weighty = 0;
			c.fill = GridBagConstraints.BOTH;

			RawEventStorage storage = container.getComponent(RawEventStorage.class);
			Stats stats = container.getComponent(Stats.class);

			JPanel statsPanel = new TitleBorderFullsizePanel("Stats");
			statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.PAGE_AXIS));
			JButton refreshButton = new JButton("Refresh");
			refreshButton.addActionListener(e -> recheckTabs());
			List<KeyValuePairDisplay<?, ?>> leftItems = List.of(
					new KeyValuePairDisplay<>(
							"Duration",
							makeTextArea(),
							() -> stats.getDuration().toString(),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Total",
							makeTextArea(),
							() -> String.valueOf(stats.getTotal()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Primo",
							makeTextArea(),
							() -> String.valueOf(stats.getPrimogenitor()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Synthetic",
							makeTextArea(),
							() -> String.valueOf(stats.getSynthetic()),
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"In-Memory",
							makeTextArea(),
							() -> String.valueOf(storage.getEvents().size()),
							JTextArea::setText
					)
			);
			displayed = new KeyValueDisplaySet(leftItems);
			statsPanel.add(new WrapperPanel(refreshButton));
			statsPanel.add(displayed);
			statsPanel.add(new IntSettingGui(storage.getMaxEventsStoredSetting(), "Max In-Memory Events").getComponent());
			statsPanel.setPreferredSize(new Dimension(300, 300));
			statsAndMemory.add(statsPanel, c);
			TitleBorderFullsizePanel memoryPanel = new TitleBorderFullsizePanel("Memory Info");
			memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.PAGE_AXIS));

			MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
			List<KeyValuePairDisplay<?, ?>> memoryItems = List.of(
					new KeyValuePairDisplay<>(
							"Heap Used",
							makeTextArea(),
							() -> (memoryMXBean.getHeapMemoryUsage().getUsed() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Heap Committed",
							makeTextArea(),
							() -> (memoryMXBean.getHeapMemoryUsage().getCommitted() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Heap Max",
							makeTextArea(),
							() -> (memoryMXBean.getHeapMemoryUsage().getMax() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Other Used",
							makeTextArea(),
							() -> (memoryMXBean.getNonHeapMemoryUsage().getUsed() >> 20) + "M",
							JTextArea::setText
					),
					new KeyValuePairDisplay<>(
							"Other Committed",
							makeTextArea(),
							() -> (memoryMXBean.getNonHeapMemoryUsage().getCommitted() >> 20) + "M",
							JTextArea::setText
					)
			);
			// TODO: also add a button to force a full refresh on all tables
			{
				JButton forceGcButton = new JButton("Force GC");
				forceGcButton.addActionListener(l -> {
					exs.submit(System::gc);
					exs.submit(() -> SwingUtilities.invokeLater(this::recheckTabs));
				});
				memoryPanel.add(new WrapperPanel(forceGcButton));
			}
			{
				mem = new KeyValueDisplaySet(memoryItems);
				memoryPanel.add(mem);
			}
			{
				Management management = container.getComponent(Management.class);
				memoryPanel.add(new WrapperPanel(new BooleanSettingGui(management.getGcOnNewPullEnabled(), "GC on new pull").getComponent()));
				JButton heapDumpButton = new JButton("Dump Heap");
				memoryPanel.add(new WrapperPanel(heapDumpButton));
				heapDumpButton.addActionListener(l -> {
					String body = management.dumpHeap();
					JOptionPane.showMessageDialog(this, body, "Heap Dump", JOptionPane.INFORMATION_MESSAGE);
				});
			}
			memoryPanel.setPreferredSize(new Dimension(300, 300));
			c.gridx++;
			statsAndMemory.add(memoryPanel, c);
			{
				JPanel diskStoragePanel = new TitleBorderFullsizePanel("Disk Storage");
				BooleanSettingGui saveCheckbox = new BooleanSettingGui(storage.getSaveToDisk(), "Save to Disk");
				diskStoragePanel.setLayout(new BoxLayout(diskStoragePanel, BoxLayout.PAGE_AXIS));
				diskStoragePanel.add(new WrapperPanel(saveCheckbox.getComponent()));
				JButton flushButton = new JButton("Flush");
				flushButton.addActionListener(l -> storage.flushToDisk());
				diskStoragePanel.add(new WrapperPanel(flushButton));
				diskStoragePanel.setPreferredSize(null);
				c.gridx = 0;
				c.gridy++;
//				c.fill = GridBagConstraints.BOTH;
				statsAndMemory.add(diskStoragePanel, c);
			}

			{
				JPanel dirsPanel = new TitleBorderFullsizePanel("Directories");
				dirsPanel.setLayout(new BoxLayout(dirsPanel, BoxLayout.PAGE_AXIS));
				{
					JButton button = new JButton("Open Install Dir");
					button.addActionListener(l -> GuiUtil.openFile(Platform.getInstallDir()));
					dirsPanel.add(new WrapperPanel(button));
				}
				{
					JButton button = new JButton("Open Settings Dir");
					button.addActionListener(l -> GuiUtil.openFile(Platform.getTriggeventDir().toFile()));
					dirsPanel.add(new WrapperPanel(button));
				}
				{
					JButton button = new JButton("Open Sessions Dir");
					button.addActionListener(l -> GuiUtil.openFile(Platform.getSessionsDir().toFile()));
					dirsPanel.add(new WrapperPanel(button));
				}
				{
					JButton button = new JButton("Open ACT Log Dir");
					button.addActionListener(l -> GuiUtil.openFile(Platform.getActDir().toFile()));
					dirsPanel.add(new WrapperPanel(button));
				}

				dirsPanel.setPreferredSize(null);
				c.gridx++;
				statsAndMemory.add(dirsPanel, c);

			}
			c.gridx = 0;
			c.gridy++;
			{
				JPanel devToolsPanel = new TitleBorderFullsizePanel("Dev Tools");
				JButton swexpButton = new JButton("Launch Swing Explorer");
				swexpButton.addActionListener((l) -> exs.submit(Launcher::launch));
				devToolsPanel.add(swexpButton);
				devToolsPanel.setPreferredSize(null);
				statsAndMemory.add(devToolsPanel, c);
			}
			c.gridx = 0;
			c.gridy++;
			c.weighty = 1;
			statsAndMemory.add(Box.createGlue(), c);
			addTab("System", statsAndMemory);
		}
		{
			addTab("Party", container.getComponent(PartyConfigTab.class));
		}

		{
			TitleBorderFullsizePanel wsPanel = new TitleBorderFullsizePanel("Websocket (Restart Required)");
			wsPanel.setPreferredSize(new Dimension(300, 150));
			ActWsLogSource actWs = container.getComponent(ActWsLogSource.class);
			wsPanel.add(new WsURISettingGui(actWs.getUriSetting(), "ACT WS URI").getComponent());
			wsPanel.add(new BooleanSettingGui(actWs.getAllowBadCert(), "Allow Bad Certs").getComponent());
			addTab("Websocket", wsPanel);
		}
		{
			addTab("Topology", new PluginTopologyPanel(container));
		}
		{
			addTab("Updates", new UpdatesPanel());
		}
		{
			addTab("Java", new JavaPanel());
		}
		{
			addTab("FFLogs", new FflogsPanel(container));
		}
		recheckTabs();
		new Timer(5000, l -> this.recheckTabs()).start();
	}

	private JTextArea makeTextArea() {
		JTextArea textArea = new JTextArea(1, 15);
		textArea.setPreferredSize(textArea.getPreferredSize());
		return textArea;
	}

	@Override
	public void refresh() {
		displayed.refresh();
		mem.refresh();
	}
}
