package org.jarmoni.async_rest.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jarmoni.async_rest.resource.IExampleResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

@Service
public class AsyncExampleService implements IAsyncExampleService {
	
	@Value("${to.resource.consumer.thread.count:12}")
	private int toResourceConsumerThreadCount;
	
	// 1 thread should be sufficient for this task
	@Value("${from.resource.consumer.thread.count:1}")
	private int fromResourceConsumerThreadCount;
	
	private static final Logger LOG = LoggerFactory.getLogger(AsyncExampleService.class);
	
	@Autowired
	private IExampleResource resource;
	
	private ConcurrentMap<String, DeferredResult<String>> results = Maps.newConcurrentMap();
	
	// Replacement for "real" messaging (JMS, Kafka,....)
	// contains requests for resource
	private ConcurrentLinkedQueue<QueueEntry> toResource = Queues.newConcurrentLinkedQueue();
	// contains results from resource
	private ConcurrentLinkedQueue<QueueEntry> fromResource = Queues.newConcurrentLinkedQueue();
	
	private ExecutorService toResourceConsumerExecutor;
	private ExecutorService fromResourceConsumerExecutor;

	@Override
	public void asyncCall(String input, DeferredResult<String> result) {
		
		String correlationId = UUID.randomUUID().toString();
		this.results.putIfAbsent(correlationId, result);
		toResource.offer(new QueueEntry(correlationId, input));
	}
	
	@Override
	public int getResultCount() {
		return this.results.size();
	}
	
	@PostConstruct
	public void init() {
		
		this.toResourceConsumerExecutor = Executors.newFixedThreadPool(this.toResourceConsumerThreadCount);
		this.fromResourceConsumerExecutor = Executors.newFixedThreadPool(this.fromResourceConsumerThreadCount);
		
		for(int i = 0; i < this.toResourceConsumerThreadCount; i++) {
			this.toResourceConsumerExecutor.submit(new ConsumerThread(this.toResource, entry -> {
				String result = resource.expensiveCall(entry.payload);
				fromResource.offer(new QueueEntry(entry.correlationId, result));
			}));
		}
		
		for(int i = 0; i < this.fromResourceConsumerThreadCount; i++) {
			this.fromResourceConsumerExecutor.submit(new ConsumerThread(this.fromResource, entry -> {
				DeferredResult<String> result = results.get(entry.correlationId);
				if(result != null) {
					results.remove(entry.correlationId);
					result.setResult(entry.payload);
				}
			}));
		}
	}
	
	@PreDestroy
	public void shutdown() {
		
		this.toResourceConsumerExecutor.shutdown();
		this.fromResourceConsumerExecutor.shutdown();
	}
	
	
	private class ConsumerThread implements Runnable {
		
		private Consumer<QueueEntry> consumer;
		private ConcurrentLinkedQueue<QueueEntry> sourceQueue;
		
		public ConsumerThread(ConcurrentLinkedQueue<QueueEntry> sourceQueue, Consumer<QueueEntry> consumer) {
			
			this.consumer = consumer;
			this.sourceQueue = sourceQueue;
		}
		
		@Override
		public void run() {
			
			while(true) {
				try {
					QueueEntry entry = sourceQueue.poll();
					if(entry != null) {
						consumer.accept(entry);;
					}
					else {
						Thread.sleep(10L);
					}
				}
				catch(Exception e) {
					LOG.error("Exception during consumption", e);
				}
			}
			
		}
	}
	
	private static class QueueEntry {
		
		private String correlationId;
		private String payload;
		
		public QueueEntry(String correlationId, String payload) {
			
			this.correlationId = correlationId;
			this.payload = payload;
		}
	}

}
