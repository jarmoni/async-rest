package org.jarmoni.async_rest.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jarmoni.async_rest.resource.ExampleResource;
import org.jarmoni.async_rest.service.IAsyncExampleService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ExampleControllerTest {

	private static final Logger LOG = LoggerFactory.getLogger(ExampleControllerTest.class);

	private static final int NUM_EXECUTIONS = 1;

	private static final int NUM_CLIENT_THREADS = 1;

	@Value("${consumer.timeout}")
	private long consumerTimeout;
	
	@Value("${resource.execution.duration}")
	private Long resourceExecutionDuration;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private IAsyncExampleService asyncService;

	private ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_THREADS);

	@After
	public void tearDown() throws Exception {

		this.executor.shutdown();
	}

	@Test
	public void testBlockingCall() throws Exception {

		this.makeTestCall("blocking");
	}

	@Test
	public void testAsyncWithCallable() throws Exception {

		this.makeTestCall("callable");
	}

	@Test
	public void testAsyncWithDeferredResult() throws Exception {

		this.makeTestCall("deferred");
	}

	@Test
	public void testAsyncWithDeferredResultTimedOut() throws Exception {
		
		this.makeTestCall("deferred");
	}

	@Test
	public void testFullyAsync() throws Exception {

		this.makeTestCall("fully-async");
		assertThat(this.asyncService.getResultCount(), is(0));
	}

	private void makeTestCall(String pathSuffix) throws Exception {

		final CountDownLatch cdl = new CountDownLatch(NUM_EXECUTIONS);

		long start = System.currentTimeMillis();

		for (int i = 0; i < NUM_EXECUTIONS; i++) {
			executor.submit(new CallerThread(pathSuffix, String.valueOf(i), cdl));
		}

		if (!cdl.await((NUM_EXECUTIONS * this.resourceExecutionDuration + 1000), TimeUnit.MILLISECONDS)) {
			fail("cdl > 0 (" + cdl.getCount() + ")");
		}

		LOG.info("*** Duration={} ms", String.valueOf(System.currentTimeMillis() - start));
	}

	private void validateResponse(ResponseEntity<String> response, String input) {

		LOG.info("Status-Code={}", response.getStatusCode().toString());
		LOG.info("Body={}", response.getBody());
		
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody(), is(ExampleResource.RETURN_VALUE + input));
	}

	private class CallerThread implements Runnable {

		private String pathSuffix;
		private String input;
		private CountDownLatch cdl;

		public CallerThread(String pathSuffix, String input, CountDownLatch cdl) {

			this.pathSuffix = pathSuffix;
			this.input = input;
			this.cdl = cdl;
		}

		@Override
		public void run() {
			//String response = restTemplate.getForObject("/" + pathSuffix + "/" + input, String.class);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_HTML);
			HttpEntity<Object> entity = new HttpEntity<Object>(headers);
			ResponseEntity<String> response = restTemplate.exchange("/" + pathSuffix + "/" + input, HttpMethod.GET, entity, String.class);
			try {
				validateResponse(response, input);
			}
			catch(Throwable t) {
				LOG.error("Exception during validation", t);
				this.cdl.countDown();
				fail();
				
			}
			this.cdl.countDown();
		}
	}

}
