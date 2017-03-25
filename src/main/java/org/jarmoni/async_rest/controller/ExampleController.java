package org.jarmoni.async_rest.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.jarmoni.async_rest.service.IExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
public class ExampleController {
	
	@Autowired
	private IExampleService service;
	
	@RequestMapping(value = "/blocking/{input}", method = RequestMethod.GET, produces = "text/html")
	public String blockingCall(@PathVariable String input) throws Exception {
		
		return this.service.syncCall(input);
	}
	
	@RequestMapping(value = "/callable/{input}", method = RequestMethod.GET, produces = "text/html")
	public Callable<String> asyncWithCallable(@PathVariable String input) throws Exception {
		
		return () -> service.syncCall(input);
	}
	
	@RequestMapping(value = "/deferred/{input}", method = RequestMethod.GET, produces = "text/html")
	public DeferredResult<String> asyncWithDeferredResult(@PathVariable String input) throws Exception {
		
		DeferredResult<String> result = new DeferredResult<>();
		Executors.newSingleThreadExecutor().submit(() -> result.setResult(service.syncCall(input)));
		return result;
	}

}
