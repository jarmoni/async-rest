package org.jarmoni.async_rest.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.jarmoni.async_rest.resource.ExampleResource;
import org.jarmoni.async_rest.service.IAsyncExampleService;
import org.junit.After;
import org.junit.Before;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExampleControllerTest {

	private static final Logger LOG = LoggerFactory.getLogger(ExampleControllerTest.class);
	
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
	public void testBlockingCall() throws Exception {

		this.helper.makeTestCall("blocking", this.validator());
	}

	@Test
	public void testAsyncWithCallable() throws Exception {

		this.helper.makeTestCall("callable", this.validator());
	}

	@Test
	public void testAsyncWithDeferredResult() throws Exception {

		this.helper.makeTestCall("deferred", this.validator());
	}

	@Test
	public void testFullyAsync() throws Exception {

		this.helper.makeTestCall("fully-async", this.validator());
		assertThat(this.asyncService.getResultCount(), is(0));
	}
	
	private BiConsumer<ResponseEntity<String>, String> validator() {
		
		return (response, input) -> {
			LOG.info("Status-Code={}", response.getStatusCode().toString());
			LOG.info("Body={}", response.getBody());
			
			assertThat(response.getStatusCode(), is(HttpStatus.OK));
			assertThat(response.getBody(), is(ExampleResource.RETURN_VALUE + input));
		};
	}
}
