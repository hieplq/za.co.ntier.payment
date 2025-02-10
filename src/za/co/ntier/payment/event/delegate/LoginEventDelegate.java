package za.co.ntier.payment.event.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.event.LoginEventData;
import org.adempiere.base.event.annotations.AfterLoginEventDelegate;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

@EventTopicDelegate
public class LoginEventDelegate extends AfterLoginEventDelegate{

	public LoginEventDelegate(Event event) {
		super(event);
	}

	public static final String isAskInvokeGeneratePayment = "#" + MSysConfig.PAYMENT_SELECTION_MANUAL_ASK_INVOKE_GENERATE; 
	@Override
	protected void onAfterLogin(LoginEventData data) {
		// modify DisplayType.DEFAULT_TIMESTAMP_FORMAT for postgresql
		// https://www.postgresql.org/docs/current/functions-formatting.html#FUNCTIONS-FORMATTING-DATETIME-TABLE
		Env.setContext(Env.getCtx(), "#TimeStampFormat", "yyyy-MM-dd HH24:MI:ss");
		
		
		
		Env.setContext(Env.getCtx(), isAskInvokeGeneratePayment, MSysConfig.getBooleanValue(MSysConfig.PAYMENT_SELECTION_MANUAL_ASK_INVOKE_GENERATE, 
				true, Env.getAD_Client_ID(Env.getCtx()), Env.getAD_Org_ID(Env.getCtx())));
		
	}

}
