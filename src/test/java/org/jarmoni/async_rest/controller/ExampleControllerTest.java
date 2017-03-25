package org.jarmoni.async_rest.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jarmoni.async_rest.resource.ExampleResource;
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
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ExampleControllerTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(ExampleControllerTest.class);
	
	private static final int NUM_EXECUTIONS = 100;
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	@Value("${server.port}")
	private String serverPort;
	
	private ExecutorService executor = Executors.newFixedThreadPool(8);
	
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
	
	private void makeTestCall(String pathSuffix) throws Exception {
		
		final CountDownLatch cdl = new CountDownLatch(NUM_EXECUTIONS);
		
		long start = System.currentTimeMillis();
		
		for(int i = 0; i < NUM_EXECUTIONS; i++) {
			executor.submit(new CallerThread(pathSuffix, String.valueOf(i), cdl));
		}
		
		if(!cdl.await(20, TimeUnit.SECONDS)) {
			fail("cdl > 0 (" + cdl.getCount() + ")");
		}
				
		LOG.info("*** Duration={} ms", String.valueOf(System.currentTimeMillis() - start));
	}
	
	private void validateResponse(String response, String input) {
		
		assertThat(response, is(ExampleResource.RETURN_VALUE + input));
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
			String response = restTemplate.getForObject("/" + pathSuffix + "/" + input, String.class);
			LOG.info(response);
			validateResponse(response, input);
			this.cdl.countDown();
		}
	}

}
