package org.jarmoni.async_rest.resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExampleResource implements IExampleResource {
	
	@Value("${resource.execution.duration}")
	private Long resourceExecutionDuration;
	
	
	public static final String RETURN_VALUE = "response to: ";
	
	@Override
	public String expensiveCall(String input) {
		
		try {
			Thread.sleep(this.resourceExecutionDuration);
		} catch (InterruptedException e) {
			//
		}
		return RETURN_VALUE + input;
	}

}
