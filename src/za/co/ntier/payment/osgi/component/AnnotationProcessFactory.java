package za.co.ntier.payment.osgi.component;

import org.adempiere.base.AnnotationBasedProcessFactory;
import org.adempiere.base.IProcessFactory;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IProcessFactory.class, property = {"service.ranking:Integer=1"})
public class AnnotationProcessFactory extends AnnotationBasedProcessFactory {

	public AnnotationProcessFactory() {
		super();
	}
	
	@Override
	protected String[] getPackages() {
		return new String [] {"za.co.ntier.payment.process"};
	}

}
