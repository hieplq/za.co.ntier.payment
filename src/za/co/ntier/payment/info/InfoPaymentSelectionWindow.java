package za.co.ntier.payment.info;

import java.math.BigDecimal;

import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.session.SessionManager;
import org.compiere.model.GridField;
import org.compiere.model.MBankAccount;
import org.compiere.model.X_AD_CtxHelp;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * convention name Info
 */
public class InfoPaymentSelectionWindow extends ImproveInfoWindow {

	private static final long serialVersionUID = -3587025842411121458L;

	public InfoPaymentSelectionWindow(int WindowNo, String tableName, String keyColumn,
			String queryValue, boolean multipleSelection, String whereClause,
			int AD_InfoWindow_ID, boolean lookup, GridField field, String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,
				whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);
	}

	@Override
	public void valueChange(ValueChangeEvent evt) {
		super.valueChange(evt);
		if (evt != null && evt.getSource() instanceof WEditor){
            WEditor editor = (WEditor)evt.getSource();
            if ("C_BankAccount_ID".equalsIgnoreCase(editor.getColumnName())){
            	Integer bankAccountID = (Integer)editor.getValue();
            	
            	Integer currencyId = null;
            	BigDecimal bankBalance = null;
            	String bankBalanceStr = null;
            	
				if (bankAccountID != null) {
					MBankAccount ba = MBankAccount.get(bankAccountID);
					currencyId = ba.getC_Currency_ID();
					bankBalance = ba.getCurrentBalance();
					bankBalanceStr = DisplayType.getNumberFormat(DisplayType.Amount).format(bankBalance);
				}
            	
				setInfoContext("C_Currency_ID", currencyId, true);
				setInfoContext("CurrentBalance", bankBalanceStr, true);
				setInfoContext("C_BankAccount_ID", bankAccountID, true);
				
				// main context
				if (infoWindow != null)
					SessionManager.getAppDesktop().updateHelpContext(X_AD_CtxHelp.CTXTYPE_Info, infoWindow.getAD_InfoWindow_ID(), this);
				else
					SessionManager.getAppDesktop().updateHelpContext(X_AD_CtxHelp.CTXTYPE_Home, 0, this);
            }
        }
	}
	
	@Override
	protected String getSQLWhere() {
		if (!isQueryByUser && prevWhereClause != null){
			return prevWhereClause;
		}
		
		String superDynamicWhere = super.getSQLWhere();
        StringBuilder dynmicWhere = new StringBuilder(superDynamicWhere);
        
        if ((p_whereClause != null && p_whereClause.trim().length() > 0) 
        		|| dynmicWhere.length() > 0) {
        	dynmicWhere.append(" AND ");
		}
        
        dynmicWhere.append("((i.isSOTrx = 'Y' AND '@PaymentRule@' = 'D' AND i.PaymentRule='D') OR (i.isSOTrx = 'N' AND '@PaymentRule@' <> 'D'))");
        dynmicWhere.append(" AND ((i.DueDate <= TO_TIMESTAMP('@PayDate:@', '@#TimeStampFormat@')) OR '@OnlyDue:N@' = 'N')");
        
        String sql = dynmicWhere.toString();
        if (sql.indexOf("@") >= 0) {
        	sql = Env.parseContext(infoContext, p_WindowNo, sql, true, true);
		}
        
        prevWhereClause = sql;
        
        return sql;
	}
	
	@Override
	protected void initExport() {
		// ntier don't need to display export button
		// super.initExport();
	}
}
