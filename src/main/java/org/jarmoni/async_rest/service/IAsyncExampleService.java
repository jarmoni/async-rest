package org.jarmoni.async_rest.service;

import org.springframework.web.context.request.async.DeferredResult;

public interface IAsyncExampleService {
	
	void asyncCall(String input, DeferredResult<String> result);
	
	// just for test
	int getResultCount();

}
