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

	@RequestMapping(value = "/blocking/{input}", method = RequestMethod.GET, produces = "text/html")
	public String blockingCall(@PathVariable String input) throws Exception {

		return this.blocksingService.syncCall(input);
	}

	@RequestMapping(value = "/callable/{input}", method = RequestMethod.GET, produces = "text/html")
	public Callable<String> asyncWithCallable(@PathVariable String input) throws Exception {

		return () -> blocksingService.syncCall(input);
	}

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

	@RequestMapping(value = "/fully-async/{input}", method = RequestMethod.GET, produces = "text/html")
	public DeferredResult<String> fullyAsync(@PathVariable String input) throws Exception {

		final DeferredResult<String> result = new DeferredResult<>(this.consumerTimeout);
		result.onTimeout(() -> result.setErrorResult("error"));
		Executors.newSingleThreadExecutor().submit(() -> asyncService.asyncCall(input, result));
		return result;
	}

}
