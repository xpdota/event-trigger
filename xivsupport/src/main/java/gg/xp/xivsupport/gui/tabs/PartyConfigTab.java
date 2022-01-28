package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.JobType;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.PartySortOrder;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.EnumListSetting;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class PartyConfigTab extends TitleBorderFullsizePanel {
	private static final Logger log = LoggerFactory.getLogger(PartyConfigTab.class);
	private final CustomTableModel<XivPlayerCharacter> partyTableModel;

	public PartyConfigTab(PicoContainer container) {
		super("Party Sort");
		setPreferredSize(getMaximumSize());
		PartySortOrder pso = container.getComponent(PartySortOrder.class);
		XivState state = container.getComponent(XivState.class);
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 0;
		c.weighty = 0;
		c.ipadx = 20;
		c.ipady = 2;
		c.insets = new Insets(5, 5, 5, 5);

		JButton resetButton = new JButton("Reset to Default");
		c.fill = GridBagConstraints.BOTH;
		add(new JLabel("Instructions: Drag the party categories and jobs within each category to match your in-game sort."), c);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridy ++;
		c.weightx = 1;
		c.fill = GridBagConstraints.NONE;
		add(resetButton, c);
		c.fill = GridBagConstraints.BOTH;

		c.gridy ++;
		c.gridwidth = 1;
		c.weighty = 1;
		partyTableModel = CustomTableModel.builder(state::getPartyList)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, col -> col.setCellRenderer(new JobRenderer())))
				.setItemEquivalence((a, b) -> a.getId() == b.getId() && a.getJob() == b.getJob())
				.build();

		EnumListSetting<JobType> catSort = pso.getCategorySortSetting();
		List<JobType> jobTypes = catSort.get();
		{
			RearrangeableList<JobType> categoryList = new RearrangeableList<>(jobTypes, l -> {
				catSort.set(l);
				log.info("Changed category order: {}", l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			});
			JScrollPane scroll = new JScrollPane(categoryList);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//			add(new TitleBorderFullsizePanel("Foo"), c);
			add(scroll, c);
			resetButton.addActionListener(l -> catSort.delete());
			catSort.addListener(partyTableModel::fullRefresh);
		}
		for (JobType jobType : jobTypes) {
			c.gridx++;
			EnumListSetting<Job> setting = pso.getSortWithinCategories().get(jobType);
			RearrangeableList<Job> jobsList = new RearrangeableList<>(setting.get(), l -> {
				setting.set(l);
				log.info("Changed order for category {}: {}", jobType, l.stream().map(Enum::name).collect(Collectors.joining(", ")));
			});
			jobsList.setCellRenderer(new JobRenderer());
			JScrollPane scroll = new JScrollPane(jobsList);
			scroll.setMinimumSize(new Dimension(10, 10));
			scroll.setMaximumSize(new Dimension(32000, 32000));
			scroll.setPreferredSize(new Dimension(10, 10));
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			add(scroll, c);
			resetButton.addActionListener(l -> setting.delete());
			setting.addListener(partyTableModel::fullRefresh);
		}
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weighty = 0;

		// TODO: reset to default button?
		JTable partyMembersTable = new JTable(8, 3);
		partyMembersTable.setModel(partyTableModel);
		partyTableModel.configureColumns(partyMembersTable);
		JScrollPane scroller = new JScrollPane(partyMembersTable);
//		scroller.setPreferredSize(new Dimension(1, 215));
		scroller.setMinimumSize(new Dimension(1, 215));
		scroller.setMaximumSize(new Dimension(1, 215));
		add(scroller, c);

		// TODO: this really shouldn't require a restart, I'm just being lazy
		resetButton.addActionListener(l -> JOptionPane.showMessageDialog(this, "Party Order Reset, Please Restart"));
	}

	@HandleEvents(order = 20_000)
	public void updatePartyList(EventContext context, XivStateRecalculatedEvent event) {
		SwingUtilities.invokeLater(() -> {
			if (partyTableModel != null) {
				partyTableModel.fullRefresh();
			}
		});
	}
}
