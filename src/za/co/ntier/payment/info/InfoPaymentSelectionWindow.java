package za.co.ntier.payment.info;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.session.SessionManager;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridField;
import org.compiere.model.MBankAccount;
import org.compiere.model.X_AD_CtxHelp;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/**
 * convention name Info
 */
public class InfoPaymentSelectionWindow extends InfoWindow {

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
            	
				setInfoContext("C_Currency_ID", currencyId);
				setInfoContext("CurrentBalance", bankBalanceStr);
				setInfoContext("C_BankAccount_ID", bankAccountID);
				
				// main context
				// TODO: need to setto main context because quick info window is not know about info context
				setContext("CurrentBalance", bankBalanceStr);
				setContext("C_Currency_ID", currencyId);
				if (infoWindow != null)
					SessionManager.getAppDesktop().updateHelpContext(X_AD_CtxHelp.CTXTYPE_Info, infoWindow.getAD_InfoWindow_ID(), this);
				else
					SessionManager.getAppDesktop().updateHelpContext(X_AD_CtxHelp.CTXTYPE_Home, 0, this);
            }
        }
	}
	
	public void setInfoContext(String key, Object value) {
		if (value == null) {
			Env.setContext(infoContext, p_WindowNo, key, (String)null);
			return;
		}
		
		if(value instanceof KeyNamePair)
			value = ((KeyNamePair)value).getKey();
		else if(value instanceof IDColumn)
			value = ((IDColumn)value).getRecord_ID();
		
		paraCtxValues.put(key, value);
		
		if (value.getClass().equals(Integer.class)) {
			Integer intValue = (Integer)value;
            Env.setContext(infoContext, p_WindowNo, key, intValue);
            Env.setContext(infoContext, p_WindowNo, Env.TAB_INFO, key, intValue);
        } else if (value.getClass().equals(String.class)) {
        	String strValue = (String)value;
            Env.setContext(infoContext, p_WindowNo, key, strValue);
            Env.setContext(infoContext, p_WindowNo, Env.TAB_INFO, key, strValue);
        } else if (value.getClass().equals(Timestamp.class)) {
			Timestamp timeValue = (Timestamp) value;
			Env.setContext(infoContext, p_WindowNo, key, timeValue);
			Env.setContext(infoContext, p_WindowNo, Env.TAB_INFO+"|" + key, timeValue);
        } else if (value.getClass().equals(Boolean.class)) {
        	Boolean boolValue = (Boolean)value;
        	Env.setContext(infoContext, p_WindowNo, key, boolValue);
        	Env.setContext(infoContext, p_WindowNo, Env.TAB_INFO, key, boolValue);
        } else {
        	String objValue = value.toString();
        	Env.setContext(infoContext, p_WindowNo, key, objValue);
        	Env.setContext(infoContext, p_WindowNo, Env.TAB_INFO, key, objValue);
        	log.warning("Not a supported type. Use with care");
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
}
