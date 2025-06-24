package za.co.ntier.payment.event.delegate;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.annotation.ModelEventTopic;
import org.adempiere.base.event.annotations.ModelEventDelegate;
import org.adempiere.base.event.annotations.po.BeforeNew;
import org.compiere.model.I_C_DepositBatch;
import org.compiere.model.X_C_DepositBatch;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

@EventTopicDelegate
@ModelEventTopic(modelClass = X_C_DepositBatch.class)
public class ModelDepositBatchHandle extends ModelEventDelegate<X_C_DepositBatch>{

	public ModelDepositBatchHandle(X_C_DepositBatch po, Event event) {
		super(po, event);
	}
	
	@BeforeNew
	public void beforeNewDepositBatch(){
		if (getModel().get_Value(I_C_DepositBatch.COLUMNNAME_C_Currency_ID) == null) {
			getModel().set_ValueOfColumn(I_C_DepositBatch.COLUMNNAME_C_Currency_ID, Env.getContext(Env.getCtx(), "$C_Currency_ID"));
		}
	}

}
