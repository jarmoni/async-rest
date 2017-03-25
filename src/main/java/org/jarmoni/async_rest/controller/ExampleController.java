package org.jarmoni.async_rest.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.jarmoni.async_rest.service.IAsyncExampleService;
import org.jarmoni.async_rest.service.IBlockingExampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
public class ExampleController {
	
	private static final Logger LOG = LoggerFactory.getLogger(ExampleController.class);

	@Value("${consumer.timeout}")
	private long consumerTimeout;

	@Autowired
	private IBlockingExampleService blocksingService;

	@Autowired
	private IAsyncExampleService asyncService;

	// Typical synchronious call. Servlet-thread is blocking until whole operation finished 
	@RequestMapping(value = "/blocking/{input}", method = RequestMethod.GET, produces = "text/html")
	public String blockingCall(@PathVariable String input) throws Exception {

		return this.blocksingService.syncCall(input);
	}

	// Servlet-thread is async. Response is served in a different thread. Call to expensive resource still blocking (see BlockingExampleService)
	@RequestMapping(value = "/callable/{input}", method = RequestMethod.GET, produces = "text/html")
	public Callable<String> asyncWithCallable(@PathVariable String input) throws Exception {

		return () -> blocksingService.syncCall(input);
	}

	// Same as above but with use of DeferredResult instead of Callable
	@RequestMapping(value = "/deferred/{input}", method = RequestMethod.GET, produces = "text/html")
	public DeferredResult<String> asyncWithDeferredResult(@PathVariable String input) throws Exception {

		final DeferredResult<String> result = new DeferredResult<>(this.consumerTimeout);
		result.onTimeout(() -> {
			LOG.info("Timeout occured");
			result.setErrorResult("error");
		});
		Executors.newSingleThreadExecutor().submit(() -> result.setResult(blocksingService.syncCall(input)));
		return result;
	}

	// Fully async. Servlet response is processed in separate thread an expensive resource is decoupled by async messaging as well (see AsyncExampleService)
	@RequestMapping(value = "/fully-async/{input}", method = RequestMethod.GET, produces = "text/html")
	public DeferredResult<String> fullyAsync(@PathVariable String input) throws Exception {

		final DeferredResult<String> result = new DeferredResult<>(this.consumerTimeout);
		result.onTimeout(() -> result.setErrorResult("error"));
		Executors.newSingleThreadExecutor().submit(() -> asyncService.asyncCall(input, result));
		return result;
	}

}
