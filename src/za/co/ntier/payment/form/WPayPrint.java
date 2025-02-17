package za.co.ntier.payment.form;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.util.Callback;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.Dialog;
import org.adempiere.webui.window.SimplePDFViewer;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.util.Env;
import org.compiere.util.Msg;

@org.idempiere.ui.zk.annotation.Form(name = "org.compiere.apps.form.VPayPrint")
public class WPayPrint extends org.adempiere.webui.apps.form.WPayPrint {
	
	@Override
	protected void loadPaymentRule() {
		super.loadPaymentRule();
		bProcess.setEnabled(true);
	}
	
	@Override
	protected void getPluginFeatures() {
		super.getPluginFeatures();
		bPrint.setEnabled(true);
	}
	
	@Override
	protected void confirm_cmd_print() {
		String PaymentRule = fPaymentRule.getSelectedItem().toValueNamePair().getValue();
		if (!getChecks(PaymentRule))
			return;
		
		SimplePDFViewer remitViewer = null;
		
		List<File> pdfList = createRemittanceDocuments();

		try
		{
			File outFile = File.createTempFile("WPayPrint", null);
			AEnv.mergePdf(pdfList, outFile);
			String name = Msg.translate(Env.getCtx(), "Remittance");
			remitViewer = new SimplePDFViewer(name, new FileInputStream(outFile));
			remitViewer.setAttribute(Window.MODE_KEY, Window.MODE_EMBEDDED);
			ZKUpdateUtil.setWidth(remitViewer, "100%");
			dispose();
			SessionManager.getAppDesktop().showWindow(remitViewer);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			Dialog.error(m_WindowNo, Msg.getMsg(Env.getCtx(), "ZZ_PrintRemittanceError"), e.getLocalizedMessage(), 
					new Callback<Integer>() {
						@Override
						public void onCallback(Integer result) {
							dispose();
						}
					}, Msg.getMsg(Env.getCtx(), "ZZ_PrintRemittanceErrorTitle"));
		}
	}
	
	@Override
	protected void cmd_EFT() {
		String PaymentRule = fPaymentRule.getSelectedItem().toValueNamePair().getValue();
		if (!getChecks(PaymentRule))
			return;
		
		int startDocumentNo = ((Number)fDocumentNo.getValue()).intValue();
		if (log.isLoggable(Level.CONFIG)) log.config("DocumentNo=" + startDocumentNo);

		//	for all checks
		//List<File> pdfList = null;
		try
		{
			//int lastDocumentNo = startDocumentNo;
			for (int i = 0; i < m_checks.length; i++){				
				//	Update BankAccountDoc
				MPaySelectionCheck.confirmPrint(m_checks[i], m_batch);
			}
			
			Dialog.info(m_WindowNo, Msg.getMsg(Env.getCtx(), "ZZ_PaymentCreatedCompleted"), null, Msg.getMsg(Env.getCtx(), "PaymentCreated"), 
					new Callback<Integer>() {
						@Override
						public void onCallback(Integer result) {
							dispose();
						}
					});
		}catch (Exception e){
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			Dialog.error(m_WindowNo, Msg.getMsg(Env.getCtx(), "PaymentError"), e.getLocalizedMessage());
		}
	}
}
