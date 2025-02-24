package za.co.ntier.payment.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.compiere.model.I_GL_JournalBatch;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalBatch;
import org.compiere.model.MJournalLine;
import org.compiere.model.MPeriod;
import org.compiere.model.X_I_GLJournal;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;

@org.adempiere.base.annotation.Process(name="org.compiere.process.ImportGLJournal")
public class ImportBudgetPeriods extends SvrProcess{

	/**	Client to be imported to		*/
	@Parameter(name="AD_Client_ID")
	private int 			adClientID = 0;
	/**	Organization to be imported to	*/
	@Parameter(name="AD_Org_ID")
	private int 			adOrgID = 0;
	/**	Acct Schema to be imported to	*/
	@Parameter(name="C_AcctSchema_ID")
	private int				acctSchemaID = 0;
	/** Default Date					*/
	@Parameter(name="DateAcct")
	private Timestamp		dateAcct = null;
	/**	Delete old Imported				*/
	@Parameter(name="DeleteOldImported")
	private boolean			deleteOldImported = false;
	/**	Don't import					*/
	@Parameter(name="IsValidateOnly")
	private boolean			validateOnly = false;
	/** Import if no Errors				*/
	@Parameter(name="IsImportOnlyNoErrors")
	private boolean			importOnlyNoErrors = true;
	
	@Override
	protected void prepare() {
		
	}

	@Override
	protected String doIt() throws Exception {
		deleteOldImported(deleteOldImported, get_TrxName());
		touchNonImportedRecords(get_TrxName());
		setMandatoryFieldsFromParameters(get_TrxName());
		lookupOrgForLine(get_TrxName());
		loockupAccount(get_TrxName());
		lookupProject(get_TrxName());
		
		commitEx();// to keep result check for error
		
		int errorNo = checkError(get_TrxName());
		if (errorNo != 0) {
			if (validateOnly || importOnlyNoErrors)
				throw new Exception ("@Errors@=" + errorNo);
		}else if (validateOnly){
			StringBuilder msgreturn = new StringBuilder("@Errors@=").append(errorNo);
			return msgreturn.toString();
		}	
		log.info(String.format("Validation Errors=%1$d", errorNo));
		
		doImport(get_TrxName());
		
		return null;
	}
	
	/**
	 * Delete Old Imported
	 * @param deleteOldImported
	 * @param trxName
	 */
	public void deleteOldImported(boolean deleteOldImported, String trxName){
		if (deleteOldImported){
			String sqlDeleteOldImport = String.format("""
						DELETE FROM I_GLJournal 
						WHERE I_IsImported='Y' AND AD_Client_ID=%1$d
					""", adClientID); 
			int no = DB.executeUpdateEx(sqlDeleteOldImport, trxName);
			log.fine("Delete Old Impored =" + no);
		}
	}
	
	/**
	 * Set IsActive, Created/Updated
	 */
	public void touchNonImportedRecords(String trxName){
		String sqlTouchNonImportedRecords = String.format("""
				UPDATE I_GLJournal 
					SET 
						IsActive = COALESCE (IsActive, 'Y'),
						Created = COALESCE (Created, getDate()),
						CreatedBy = COALESCE (CreatedBy, 0),
						Updated = COALESCE (Updated, getDate()),
						UpdatedBy = COALESCE (UpdatedBy, 0),
						I_ErrorMsg = ' ',
						I_IsImported = 'N'
						WHERE (I_IsImported<>'Y' OR I_IsImported IS NULL)  AND AD_Client_ID=%1$d
				""", adClientID);

			int no = DB.executeUpdateEx(sqlTouchNonImportedRecords, trxName);
			log.info ("Reset=" + no);
	}
	
	/**
	 * Set Default Client, Doc Org, AcctSchema, DatAcct
	 * reset error record to normal for re-import
	 */
	public void setMandatoryFieldsFromParameters (String trxName) {
		String sqlUpdateMandatoryField = String.format("""
				UPDATE I_GLJournal
					SET 
						AD_Client_ID = COALESCE (AD_Client_ID, %1$d),
						DateAcct = COALESCE (DateAcct, ?),
						Updated = COALESCE (Updated, getDate())
					WHERE 
						(I_IsImported<>'Y' OR I_IsImported IS NULL) AND AD_Client_ID=%1$d
				""", adClientID);

		int no = DB.executeUpdateEx(sqlUpdateMandatoryField, new Object [] {dateAcct}, trxName);
		log.fine("Client/DocOrg/Default=" + no);
	}

	/**
	 * lookup Org for line from name and report error
	 * @param trxName
	 */
	public void lookupOrgForLine(String trxName) {
		// lookup org for line from name
		String sqlLookupOrgForLine = String.format("""
				UPDATE I_GLJournal i
					SET 
						AD_Org_ID=COALESCE(
							(SELECT o.AD_Org_ID FROM AD_Org o
						 	WHERE o.Value=i.OrgValue AND o.IsSummary='N' AND i.AD_Client_ID=o.AD_Client_ID),AD_Org_ID)
					WHERE (AD_Org_ID IS NULL OR AD_Org_ID=0) AND OrgValue IS NOT NULL
						AND I_IsImported<>'Y' AND AD_Client_ID=%1$d
				""", adClientID);
		int no = DB.executeUpdateEx(sqlLookupOrgForLine.toString(), get_TrxName());
		log.fine("Set Org from Value=" + no);

		// report records can't lookup org
		String sqlReportLookupOrgForLineError = String.format("""
				UPDATE I_GLJournal o
					SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '
					WHERE (AD_Org_ID IS NULL OR AD_Org_ID=0
						OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_Org_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))
					 	AND I_IsImported<>'Y' AND AD_Client_ID=%1$d
				""", adClientID); 

		no = DB.executeUpdateEx(sqlReportLookupOrgForLineError, get_TrxName());
		if (no != 0)
			log.warning ("Invalid Org=" + no);

	}
	
	/**
	 * 
	 * @param trxName
	 */
	public void loockupAccount(String trxName) {
		// lookup account from value
		String sqlLookupAccount = String.format("""
				UPDATE I_GLJournal i
					SET 
						Account_ID=(SELECT MAX(ev.C_ElementValue_ID) 
									FROM C_ElementValue ev
										INNER JOIN C_Element e ON (e.C_Element_ID=ev.C_Element_ID)
										INNER JOIN C_AcctSchema_Element ase ON (e.C_Element_ID=ase.C_Element_ID AND ase.ElementType='AC')
									WHERE ev.Value=i.AccountValue AND ev.IsSummary='N'
										AND i.C_AcctSchema_ID=ase.C_AcctSchema_ID AND i.AD_Client_ID=ev.AD_Client_ID)
						WHERE Account_ID IS NULL AND AccountValue IS NOT NULL
							AND I_IsImported<>'Y' AND AD_Client_ID=%1$d 
				""", adClientID);
		int no = DB.executeUpdateEx(sqlLookupAccount, get_TrxName());
		log.fine("Set Account from Value=" + no);
		
		String sqlReportLookupAccountError = String.format("""
				UPDATE I_GLJournal i 
					SET 
						I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Account, '
				        WHERE 
				        	(Account_ID IS NULL OR Account_ID=0)
				            AND I_IsImported<>'Y' AND AD_Client_ID=%1$d
				""", adClientID);
		no = DB.executeUpdateEx(sqlReportLookupAccountError.toString(), get_TrxName());
		if (no != 0)
			log.warning ("Invalid Account=" + no);

	}
	
	public void lookupProject(String trxName) {
        String sqlLookupProject = String.format("""
                UPDATE I_GLJournal i 
                    SET
                        C_Project_ID=(SELECT p.C_Project_ID FROM C_Project p
                        				WHERE p.Value=i.ProjectValue AND p.IsSummary='N' AND i.AD_Client_ID=p.AD_Client_ID)
                        WHERE 
                        	C_Project_ID IS NULL AND ProjectValue IS NOT NULL
                        	AND I_IsImported<>'Y' AND AD_Client_ID=%1$d
                """, adClientID);

			int no = DB.executeUpdateEx(sqlLookupProject.toString(), get_TrxName());
			log.fine("Set Project from Value=" + no);
			
		String sqlReporLookupProjectError = String.format("""
					UPDATE I_GLJournal i
					SET 
						I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Project, '
						WHERE C_Project_ID IS NULL AND ProjectValue IS NOT NULL
						AND I_IsImported<>'Y' AND AD_Client_ID=%1$d 
				""", adClientID);

			no = DB.executeUpdateEx(sqlReporLookupProjectError.toString(), get_TrxName());
			if (no != 0)
				log.warning ("Invalid Project=" + no);

    }
	
	public int checkError(String trxName) {
		String sqlCheckRecordsError = String.format("""
				SELECT COUNT(*) 
				FROM I_GLJournal 
				WHERE I_IsImported NOT IN ('Y','N')
					AND AD_Client_ID=%1$d
				""", adClientID);
		return DB.getSQLValue(get_TrxName(), sqlCheckRecordsError);
	}
	
	public static enum PeriodNum {
		ONE("A_Period_1"), 
		TWO("A_Period_2"), 
		THREE("A_Period_3"),
		FOUR("A_Period_4"),
		FIVE("A_Period_5"),
		SIX("A_Period_6"),
		SEVEN("A_Period_7"),
		EIGHT("A_Period_8"),
		NIGHT("A_Period_9"),
		TEN("A_Period_10"),
		ELEVENT("A_Period_11"),
		TWELVE("A_Period_12");
		private final String periodNum;; 

		PeriodNum(String periodNum) {
	        this.periodNum = periodNum;
	    }
		
		@Override
	    public String toString() {
	        return this.periodNum; 
	    }
	}
		
	public void doImport (String trxName) {
		String sqlImportedBudget = String.format("""
				SELECT 
					* 
				FROM 
					I_GLJournal
				WHERE 
					I_IsImported='N' AND AD_Client_ID=%1$d
				""", adClientID);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int noInsertLine = 0;
		try{
			pstmt = DB.prepareStatement (sqlImportedBudget, trxName);
			rs = pstmt.executeQuery ();
			MJournalBatch batch = null;
			Map<String, MJournal> mapJournal = new HashMap<>();
			while (rs.next()){
				X_I_GLJournal imp = new X_I_GLJournal (getCtx (), rs, trxName);
				
				if (batch == null) {
					batch = new MJournalBatch(getCtx(), 0, trxName);
					batch.setClientOrg(adClientID, adOrgID);
					String batchDescription = "";
					if (!StringUtils.isBlank(batchDescription))
						batchDescription = imp.getBatchDescription();
					batchDescription += LocalDateTime.now().format(DateTimeFormatter.ofPattern(DisplayType.DEFAULT_TIMESTAMP_FORMAT));
					batch.setDescription(batchDescription);
					batch.setC_DocType_ID(imp.getC_DocType_ID());
					batch.setPostingType(imp.getPostingType());
					batch.setGL_Category_ID(imp.getGL_Category_ID());
					MPeriod periodDoc = MPeriod.get(getCtx(), dateAcct, adOrgID, null);
					batch.setC_Period_ID(periodDoc.getC_Period_ID());
					batch.setDateAcct(dateAcct);
					batch.setDateDoc(dateAcct);
					batch.setC_Currency_ID(imp.getC_Currency_ID());
					batch.saveEx(trxName);
					addBufferLog(0, null, null, "ZZ_JournalBatchImported", I_GL_JournalBatch.Table_ID, batch.getGL_JournalBatch_ID());
				}
				
				
				for (int periodIndex = 0; periodIndex < PeriodNum.values().length; periodIndex++) {
					PeriodNum period = PeriodNum.values()[periodIndex];
					Object periodAmount = imp.get_Value(period.toString());
                    if (periodAmount == null) {
                    	continue;
                    }
                	//m_DateAcct.setm
					LocalDateTime perdiodMonth = dateAcct.toLocalDateTime().withMonth(periodIndex + 1);
					Timestamp periodDate = Timestamp.valueOf(perdiodMonth);
					MPeriod peridod = MPeriod.get(getCtx(), periodDate, imp.getAD_Org_ID(), null);
					
                	String journalMapKey = Integer.toString(peridod.getC_Period_ID());
                	MJournal journalPerPeriod = mapJournal.get(journalMapKey);
                	
                	if (journalPerPeriod == null) {
                		journalPerPeriod = new MJournal(getCtx(), 0, trxName);
                		journalPerPeriod.setGL_JournalBatch_ID(batch.getGL_JournalBatch_ID());
                		journalPerPeriod.setClientOrg(adClientID, adOrgID);
                		
    					String description = Msg.getMsg(Env.getCtx(), "ZZ_GLImportDesc", new Object [] {imp.getOrgValue(), peridod.getName()});
    					journalPerPeriod.setDescription(description);
    					journalPerPeriod.setC_DocType_ID (imp.getC_DocType_ID());
    					journalPerPeriod.setGL_Category_ID (imp.getGL_Category_ID());
    					journalPerPeriod.setPostingType (imp.getPostingType());
    					journalPerPeriod.setGL_Budget_ID(imp.getGL_Budget_ID());
    					journalPerPeriod.setC_Currency_ID(imp.getC_Currency_ID());
    					
    					journalPerPeriod.setDateAcct(periodDate);
    					journalPerPeriod.setDateDoc (imp.getDateAcct());
    					
    					journalPerPeriod.saveEx(trxName);
    					
    					mapJournal.put(journalMapKey, journalPerPeriod);
                	}
                	
                	MJournalLine line = new MJournalLine (journalPerPeriod);
                	line.setAD_Org_ID(imp.getAD_Org_ID());
    				line.setDescription(imp.getDescription());
    				line.setC_Currency_ID(imp.getC_Currency_ID());
    				
    				BigDecimal amt = (BigDecimal) periodAmount;
    				//line.set_ValueOfColumn(I_GL_JournalLine.COLUMNNAME_AmtSourceCr, );
    				if (amt.compareTo(BigDecimal.ZERO) < 0)
    					line.setAmtSourceCr(amt.abs());
					else
						line.setAmtSourceDr(amt);
    				
    				line.setDateAcct(periodDate);
    				line.setAccount_ID(imp.getAccount_ID());
    				line.setC_Project_ID(imp.getC_Project_ID());
    				line.saveEx(trxName);
    				noInsertLine++;
    				
    				imp.setGL_JournalBatch_ID(batch.getGL_JournalBatch_ID());
					imp.setGL_Journal_ID(journalPerPeriod.getGL_Journal_ID());
					imp.setGL_JournalLine_ID(line.getGL_JournalLine_ID());
					imp.setI_IsImported(true);
					imp.setProcessed(true);
					imp.saveEx(trxName);
                }
			}
		}catch (Exception e){
			log.log(Level.SEVERE, "", e);
			addLog (0, null, null, e.getMessage());
		}finally{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		//	Set Error to indicator to not imported
		String sqlNonImportToError = String.format("""
				UPDATE I_GLJournal
				SET I_IsImported='N', Updated=getDate()
				WHERE I_IsImported<>'Y' 
				""", adClientID);

		int no = DB.executeUpdate(sqlNonImportToError, trxName);
		addLog (0, null, new BigDecimal (no), "@Errors@");
		addLog (0, null, new BigDecimal (noInsertLine), "@GL_JournalLine_ID@: @Inserted@");
	}
}
