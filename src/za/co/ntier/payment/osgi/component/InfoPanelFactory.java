package za.co.ntier.payment.osgi.component;

import org.adempiere.webui.factory.IInfoFactory;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.InfoPanel;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MInfoWindow;
import org.osgi.service.component.annotations.Component;

import za.co.ntier.payment.info.InfoPaymentSelectionWindow;

@Component(
		 property= {"service.ranking:Integer=2"},
		 service = IInfoFactory.class 
)
/**
 * care only open from the menu
 */
public class InfoPanelFactory implements IInfoFactory{
	/**
	 * call when open from menu
	 */
	@Override
	public InfoWindow create (int windowNo, int AD_InfoWindow_ID, String predefinedContextVariables) {
		MInfoWindow infoWindow = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
		String tableName = infoWindow.getAD_Table().getTableName();

		if ("C_Invoice_v".equalsIgnoreCase(tableName)) {
			InfoWindow info = new InfoPaymentSelectionWindow(windowNo, tableName, "C_Invoice_ID", null, true, null, AD_InfoWindow_ID, false, null, predefinedContextVariables);
			if (info.loadedOK()) {
				return info;
			}else
				info.dispose(false);
		}
		
		return null;
	}

	@Override
	public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		return null;
	}

	@Override
	public InfoPanel create(Lookup lookup, GridField field, String tableName, String keyColumn, String value,
			boolean multiSelection, String whereClause, int AD_InfoWindow_ID) {
		return null;
	}

	@Override
	public InfoWindow create(int AD_InfoWindow_ID) {
		return null;
	}

	@Override
	public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		return null;
	}
}
