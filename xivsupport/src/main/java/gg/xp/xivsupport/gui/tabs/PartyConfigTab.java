package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.JobType;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.state.PartySortOrder;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.RearrangeableEnumListSetting;
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

public class PartyConfigTab extends TitleBorderFullsizePanel {
	private static final Logger log = LoggerFactory.getLogger(PartyConfigTab.class);
	private final CustomTableModel<XivPlayerCharacter> partyTableModel;

	public PartyConfigTab(PicoContainer container) {
		super("Party Sort");
		XivState state = container.getComponent(XivState.class);
		partyTableModel = CustomTableModel.builder(state::getPartyList)
				.addColumn(new CustomColumn<>("Name", XivEntity::getName))
				.addColumn(new CustomColumn<>("Job", XivPlayerCharacter::getJob, col -> col.setCellRenderer(new JobRenderer())))
				.setItemEquivalence((a, b) -> a.getId() == b.getId() && a.getJob() == b.getJob())
				.build();
		SwingUtilities.invokeLater(() -> {

			setPreferredSize(getMaximumSize());
			PartySortOrder pso = container.getComponent(PartySortOrder.class);
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
			c.gridy++;
			c.weightx = 1;
			c.fill = GridBagConstraints.NONE;
			add(resetButton, c);
			c.fill = GridBagConstraints.BOTH;

			c.gridy++;
			c.gridwidth = 1;
			c.weighty = 1;

			EnumListSetting<JobType> catSort = pso.getCategorySortSetting();
			{
				RearrangeableList<JobType> categoryList = new RearrangeableEnumListSetting<>(catSort).getListGui();
				JScrollPane scroll = new JScrollPane(categoryList);
				scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				add(scroll, c);
				resetButton.addActionListener(l -> catSort.delete());
				catSort.addListener(partyTableModel::fullRefresh);
			}
			List<JobType> jobTypes = catSort.getDefault();
			for (JobType jobType : jobTypes) {
				c.gridx++;
				EnumListSetting<Job> setting = pso.getSortWithinCategories().get(jobType);
				RearrangeableList<Job> jobsList = new RearrangeableEnumListSetting<>(setting).getListGui();
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
//		resetButton.addActionListener(l -> JOptionPane.showMessageDialog(this, "Party Order Reset, Please Restart"));
		});
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
