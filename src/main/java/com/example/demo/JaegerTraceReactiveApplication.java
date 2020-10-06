package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@RestController
@Slf4j
public class JaegerTraceReactiveApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(JaegerTraceReactiveApplication.class, args);
	}
	
	@Autowired
	private AppService appService;
	
	@Bean
	public WebClient webClient() {
		WebClient webClient = WebClient.builder()
									   .exchangeFunction(clientRequest -> Mono.just(dummyResponseToAvoidPost()))
									   .build();
		return webClient;
	}
	
	private ClientResponse dummyResponseToAvoidPost() {
		String bJson = null;
		try {
			bJson = new ObjectMapper().writeValueAsString(new AppService.B("b"));
			ClientResponse clientResponse =
				ClientResponse.create(HttpStatus.OK)
							  .header("Content-Type", "application/json")
							  .body(bJson)
							  .build();
			return clientResponse;
		} catch (JsonProcessingException e) {
			return null;
		}
	}
	
	@PostMapping(value = "/b",
				 consumes = MediaType.APPLICATION_JSON_VALUE,
				 produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> api(@RequestBody AppService.A a) throws Exception {
		log.info("inside api ");
		AppService.B b = new AppService.B("b" + a.a);
		return ResponseEntity.ok().body(b);
	}
	
	@Scheduled(fixedRate = 5000L)
	public void job() {
		appService.task();
	}
}