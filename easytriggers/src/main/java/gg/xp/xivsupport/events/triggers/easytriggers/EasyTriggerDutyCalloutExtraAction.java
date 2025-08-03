package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.gui.ExtraCalloutAction;
import gg.xp.xivsupport.callouts.gui.ExtraCalloutActionInstance;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.DutyCalloutFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.EasyTriggersTab;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.lang.reflect.Field;

@ScanMe
public class EasyTriggerDutyCalloutExtraAction implements ExtraCalloutAction {

	public final EasyTriggers et;
	private final EasyTriggersTab ett;

	public EasyTriggerDutyCalloutExtraAction(EasyTriggers et, EasyTriggersTab ett) {
		this.et = et;
		this.ett = ett;
	}

	private void showError(String error) {
		JOptionPane.showMessageDialog(JFrame.getWindows()[0], "Error: " + error, error, JOptionPane.ERROR_MESSAGE);

	}

	@Nullable
	@Override
	public ExtraCalloutActionInstance getInstance(ModifiableCallout<?> callout, ModifiedCalloutHandle handle) {
		return new ExtraCalloutActionInstance() {
			@Override
			public String getLabel() {
				return "Make Easy Trigger";
			}

			@Override
			public boolean isVisible() {
				return true;
			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public void doAction() {

				Field field = handle.getField();
				if (field == null) {
					showError("Field was null!");
					return;
				}
				String className = field.getDeclaringClass().getSimpleName();
				String fieldName = field.getName();
				DutyCalloutFilter dcf = new DutyCalloutFilter();
				dcf.calloutClass = className;
				dcf.calloutField = fieldName;

				CalloutAction ca = new CalloutAction();
				ca.setText("{originalText}");
				ca.setTts("{originalTts}");

				EasyTrigger<ProcessedCalloutEvent> easyTrigger = new EasyTrigger();
				easyTrigger.setEventType(ProcessedCalloutEvent.class);
				easyTrigger.addCondition(dcf);
				easyTrigger.addAction(ca);
				et.addTrigger(null, easyTrigger);
				ett.bringToFront();
				ett.selectTrigger(easyTrigger);
			}
		};
	}
}
