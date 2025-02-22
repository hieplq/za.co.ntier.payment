package za.co.ntier.payment.util;

import java.util.function.Consumer;

import org.adempiere.util.Callback;
import org.adempiere.webui.ISupportMask;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.ProcessInfoDialog;
import org.adempiere.webui.event.DialogEvents;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoLog;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public final class WUtil {
	
	public static ProcessInfoLog createDocumentLink(String title, int tableId, int recordId) {
		return new ProcessInfoLog(0, null, null, title, tableId, recordId);
	}
	/**
	 * reuse dialog show process result to show info like document link  
	 */
	public static void showResultDialog(String tilte, String summary, Component compChild, Consumer<ProcessInfo> consumerLog, Callback<Event> callBack) {
		ProcessInfo pi = new ProcessInfo(tilte, 0);
		pi.setError(false);
		pi.setSummary(summary);
		
		if (consumerLog != null)
			consumerLog.accept(pi);
		
		ProcessInfoDialog dialog = new ProcessInfoDialog(pi, false);
		
		final ISupportMask supportMask = LayoutUtils.showWindowWithMask(dialog, compChild, LayoutUtils.OVERLAP_PARENT);
		dialog.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				supportMask.hideMask();
				if (callBack != null)
					callBack.onCallback(null);
			}
		});	
	}
}
