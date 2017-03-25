package org.jarmoni.async_rest.resource;

import org.springframework.stereotype.Service;

@Service
public class ExampleResource implements IExampleResource {
	
	public static final String RETURN_VALUE = "response to: ";
	
	@Override
	public String expensiveCall(String input) throws Exception {
		
		Thread.sleep(1000L);
		return RETURN_VALUE + input;
	}

}
