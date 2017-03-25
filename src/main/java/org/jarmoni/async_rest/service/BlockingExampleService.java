package org.jarmoni.async_rest.service;

import org.jarmoni.async_rest.resource.IExampleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlockingExampleService implements IBlockingExampleService {
	
	@Autowired
	private IExampleResource resource;
	
	@Override
	public String syncCall(String input) throws Exception {
		
		return this.resource.expensiveCall(input);
	}

}
