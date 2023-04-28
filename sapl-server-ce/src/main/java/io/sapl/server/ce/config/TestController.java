package io.sapl.server.ce.config;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class TestController {

	@GetMapping("test")
	String string() {
		return "HELLO";
	}

	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) {
		var applicationContext           = event.getApplicationContext();
		var requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
		var map                          = requestMappingHandlerMapping.getHandlerMethods();
		log.debug("---------------------------------------");
		map.forEach((key, value) -> log.error("{} {}", key, value));
		log.debug("---------------------------------------");
	}
}
