package org.jarmoni.async_rest.controller;

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.jarmoni.async_rest.service.IAsyncExampleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ControllerTestHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(ControllerTestHelper.class);

	@Value("${num.executions}")
	private int numExecutions;

	@Value("${num.client.threads}")
	private int numClientThreads;

	@Value("${consumer.timeout}")
	private long consumerTimeout;

	@Value("${resource.execution.duration}")
	private Long resourceExecutionDuration;
	
	@Autowired
	private TestRestTemplate restTemplate;

	private ExecutorService executor;
	
	void setUp() throws Exception {
		
		this.executor = Executors.newFixedThreadPool(this.numClientThreads);
	}

	void tearDown() throws Exception {

		this.executor.shutdown();
	}


	void makeTestCall(String pathSuffix, BiConsumer<ResponseEntity<String>, String> validator) throws Exception {

		if (this.numClientThreads > this.numExecutions) {
			throw new RuntimeException("'NUM_CLIENT_THREADS > NUM_EXECUTIONS' makes no sense");
		}

		final CountDownLatch cdl = new CountDownLatch(this.numExecutions);

		long start = System.currentTimeMillis();

		for (int i = 0; i < this.numExecutions; i++) {
			executor.submit(new CallerThread(pathSuffix, String.valueOf(i), cdl, validator));
		}

		if (!cdl.await((this.numExecutions * this.resourceExecutionDuration / this.numClientThreads + 1000),
				TimeUnit.MILLISECONDS)) {
			fail("cdl > 0 (" + cdl.getCount() + ")");
		}

		LOG.info("*** Duration={} ms", String.valueOf(System.currentTimeMillis() - start));
	}

	private class CallerThread implements Runnable {

		private String pathSuffix;
		private String input;
		private CountDownLatch cdl;
		private BiConsumer<ResponseEntity<String>, String> validator;

		public CallerThread(String pathSuffix, String input, CountDownLatch cdl, BiConsumer<ResponseEntity<String>, String> validator) {

			this.pathSuffix = pathSuffix;
			this.input = input;
			this.cdl = cdl;
			this.validator = validator;
		}

		@Override
		public void run() {
			// String response = restTemplate.getForObject("/" + pathSuffix +
			// "/" + input, String.class);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_HTML);
			HttpEntity<Object> entity = new HttpEntity<Object>(headers);
			ResponseEntity<String> response = restTemplate.exchange("/" + pathSuffix + "/" + input, HttpMethod.GET,
					entity, String.class);
			try {
				this.validator.accept(response, input);
			} catch (Throwable t) {
				LOG.error("Exception during validation", t);
				return;
			}
			this.cdl.countDown();
		}
	}

}
