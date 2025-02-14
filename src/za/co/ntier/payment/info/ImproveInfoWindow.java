package za.co.ntier.payment.info;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.adempiere.webui.info.InfoWindow;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.UUIDColumn;
import org.compiere.model.GridField;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.KeyNamePair;
import org.compiere.util.NamePair;
import org.compiere.util.ValueNamePair;

public class ImproveInfoWindow extends InfoWindow {

	private static final long serialVersionUID = 7851362282537774596L;

	public ImproveInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public ImproveInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public ImproveInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public ImproveInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}
	
	/**
	 * override to use for MQA before done IDEMPIERE-6427
	 */
	@Override
	public void createT_Selection_InfoWindow(int AD_PInstance_ID){
		MTable table = MTable.get(infoWindow.getAD_Table_ID());
		StringBuilder insert = new StringBuilder();
		insert.append("INSERT INTO T_Selection_InfoWindow (AD_PINSTANCE_ID, ");
		if (table != null && table.isUUIDKeyTable())
			insert.append("T_SELECTION_UU");
		else
			insert.append("T_SELECTION_ID");
		insert.append(", COLUMNNAME , VALUE_STRING, VALUE_NUMBER , VALUE_DATE, ViewID) VALUES(?,?,?,?,?,?,?) ");
		for (Entry<NamePair,LinkedHashMap<String, Object>> records : m_values.entrySet()) {
			//set Record ID
			
				LinkedHashMap<String, Object> fields = records.getValue();
				for(Entry<String, Object> field : fields.entrySet())
				{
					List<Object> parameters = new ArrayList<Object>();
					parameters.add(AD_PInstance_ID);
					
					Object key = records.getKey();
					String viewIDValue = null;
					if(key instanceof KeyNamePair){
						KeyNamePair knp = (KeyNamePair)key;
						parameters.add(knp.getKey());
						viewIDValue = knp.getName();
					}else if(key instanceof ValueNamePair){
						ValueNamePair vnp = (ValueNamePair)key;
						parameters.add(vnp.getValue());
						viewIDValue = vnp.getName();
					}else{
						parameters.add(key);
					}

					parameters.add(field.getKey());
					
					Object data = field.getValue();
					Object strObj = null;
					Object numObj = null;
					Object timeObj = null;
					
					// set Values					
					if (data instanceof IDColumn){
						IDColumn id = (IDColumn) data;
						numObj = id.getRecord_ID();
					}else if (data instanceof UUIDColumn){
						UUIDColumn id = (UUIDColumn) data;
						strObj = id.getRecord_UU();
					}else if (data instanceof String){
						strObj = data;
					}else if (data instanceof BigDecimal || data instanceof Integer || data instanceof Double){
						if(data instanceof Double){	
							numObj = BigDecimal.valueOf((Double)data);
						}else	
							numObj = data;
					}else if (data instanceof Timestamp || data instanceof Date){
						if(data instanceof Date){
							timeObj = new Timestamp(((Date)data).getTime());
						}else 
							timeObj = data;
					}else if(data instanceof KeyNamePair){
						KeyNamePair knpData = (KeyNamePair)data;
						numObj = knpData.getKey();					
					}else if(data instanceof ValueNamePair){
						ValueNamePair vnp = (ValueNamePair)data;
						strObj = vnp.getValue();
					}else{
						strObj = data;
					}
					parameters.add (strObj);
					parameters.add (numObj);
					parameters.add (timeObj);
					
					parameters.add (viewIDValue);
					DB.executeUpdateEx(insert.toString(),parameters.toArray() , null);
						
				}
		}
	} // createT_Selection_InfoWindow

}
