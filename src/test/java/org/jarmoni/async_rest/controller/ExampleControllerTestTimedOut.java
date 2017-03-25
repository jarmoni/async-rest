package org.jarmoni.async_rest.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.BiConsumer;

import org.jarmoni.async_rest.service.IAsyncExampleService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-timed-out")
public class ExampleControllerTestTimedOut {
	
	private static final Logger LOG = LoggerFactory.getLogger(ExampleControllerTestTimedOut.class);
	
	@Autowired
	private ControllerTestHelper helper;
	
	@Autowired
	private IAsyncExampleService asyncService;
	
	@Before
	public void setUp() throws Exception {
		
		this.helper.setUp();
	}

	@After
	public void tearDown() throws Exception {

		this.helper.tearDown();
	}

	
	@Test
	public void testAsyncWithDeferredResultTimedOut() throws Exception {
		this.helper.makeTestCall("deferred", this.validatator());
	}
	
	@Test
	public void testFullyAsyncTimedOut() throws Exception {

		this.helper.makeTestCall("fully-async", this.validatator());
		// This is ugly :-(
		Thread.sleep(2000L);
		assertThat(this.asyncService.getResultCount(), is(0));
	}
	
	private BiConsumer<ResponseEntity<String>, String> validatator() {
		
		return (response, input) -> {
			LOG.info("Status-Code={}", response.getStatusCode().toString());
			LOG.info("Body={}", response.getBody());
			assertThat(response.getStatusCode(), is(HttpStatus.OK));
			assertThat(response.getBody(), is("error"));
		};
	}

}
