package za.co.ntier.payment.osgi.component;

import org.adempiere.base.AnnotationBasedEventManager;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = {AnnotationEventManager.class})
public class AnnotationEventManager extends AnnotationBasedEventManager {
	@Override
	public String[] getPackages() {
		// TODO Auto-generated method stub
		return new String [] {"za.co.ntier.payment.event.delegate"};
	}

}
