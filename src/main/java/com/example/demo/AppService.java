package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@EnableAsync
public class AppService {
    
    @Value("${server.port}")
    private int port;
    
    @Autowired
    private WebClient webClient;
    
    @Async
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
        return webClient.post().uri("http://localhost:"+port+"/b")
                        .body(Mono.just(a), A.class)
                        .exchange()
                        .flatMap(response -> {
                            log.info("before sending BMono");
                            Mono<B> bMono = response.bodyToMono(B.class);
                            return bMono;
                        })
                        .onErrorResume(Throwable.class, e -> {
                            log.info("Inside onErrorResume", e);
                            return Mono.error(Exception::new);
                        });
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
