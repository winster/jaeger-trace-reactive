package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RestController
@Slf4j
public class JaegerTraceReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaegerTraceReactiveApplication.class, args);
	}
	
	@Value("${server.port}")
	private int port;
	
	@PostMapping(value = "/b",
				 consumes = MediaType.APPLICATION_JSON_VALUE,
				 produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> api(@RequestBody A a) throws Exception {
		log.info("inside api ");
		B b = new B("b" + a.a);
		return ResponseEntity.ok().body(b);
	}
	
	@Scheduled(fixedRate = 5000L)
	public void task() {
		log.info("before task");
		Mono<A> aMono = getA("test");
		Mono<B> bMono = aMono.flatMap(this::getB);
		bMono.subscribe(this::onCompleted, this::onError);
		log.info("after task");
	}
	
	private Mono<A> getA(String string) {
		log.info("inside getA");
		return Mono.just(new A("a" + string));
	}
	
	private Mono<B> getB1(A a) {
		log.info("inside getB1");
		return Mono.just(new B("b" + a.a));
	}
	
	private Mono<B> getB(A a) {
		log.info("inside getB");
		String bJson = null;
		try {
			bJson = new ObjectMapper().writeValueAsString(new B("b" + a.a));
			ClientResponse clientResponse =
				ClientResponse.create(HttpStatus.OK)
							  .header("Content-Type", "application/json")
							  .body(bJson)
							  .build();
			WebClient webClient = WebClient.builder()
										   /*.exchangeFunction(clientRequest -> Mono.just(clientResponse))*/
										   .build();
			return webClient.post().uri("http://localhost:"+port+"/b").body(Mono.just(a), A.class).exchange()
							.flatMap(response -> {
								log.info("before sending BMono");
								Mono<B> bMono = clientResponse.bodyToMono(B.class);
								return bMono;
							}).onErrorResume(Throwable.class, e -> {
					log.info("Inside onErrorResume", e);
					return Mono.error(Exception::new);
				});
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return Mono.error(Exception::new);
		}
	}
	
	private void onCompleted(B b) {
		log.info("inside onCompleted {}", b.b);
	}
	
	private void onError(Throwable t) {
		log.info("inside onError", t);
	}
	
	public static class A
	{
		public String a;
		
		public A(){}
		
		public A(String a)
		{
			this.a = a;
		}
	}
	public static class B {
		
		public String b;
		
		public B() {}
		
		public B(String b) {
			this.b = b;
		}
	}
}
