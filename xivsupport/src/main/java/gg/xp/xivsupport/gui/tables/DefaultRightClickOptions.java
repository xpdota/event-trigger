package gg.xp.xivsupport.gui.tables;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.fflogs.FflogsRawEvent;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.slf4j.LogEvent;

@ScanMe
public class DefaultRightClickOptions {
	public DefaultRightClickOptions(RightClickOptionRepo repo) {
		repo.addOption(CustomRightClickOption.forRowWithConverter(
				"Copy Net Line",
				Event.class,
				e -> e.getThisOrParentOfType(ACTLogLineEvent.class),
				line -> GuiUtil.copyTextToClipboard(line.getLogLine())));
		repo.addOption(CustomRightClickOption.forRowWithConverter(
				"Copy Emulated ACT Line",
				Event.class,
				e -> e.getThisOrParentOfType(ACTLogLineEvent.class),
				line -> GuiUtil.copyTextToClipboard(line.getEmulatedActLogLine())));
		repo.addOption(CustomRightClickOption.forRowWithConverter(
				"Copy WS JSON",
				Event.class,
				e -> e.getThisOrParentOfType(ActWsRawMsg.class),
				line -> GuiUtil.copyTextToClipboard(line.getRawMsgData())));
		repo.addOption(CustomRightClickOption.forRowWithConverter(
				"Copy FFLogs Fields",
				Event.class,
				e -> e.getThisOrParentOfType(FflogsRawEvent.class),
				line -> GuiUtil.copyTextToClipboard(line.getFields().toString())));

		repo.addOption(CustomRightClickOption.forRow(
				"Copy",
				LogEvent.class,
				tp -> GuiUtil.copyTextToClipboard(tp.getEncoded())));

	}
}
