package io.sapl.server.ce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@SpringBootApplication
@ComponentScan("io.sapl.server")
@Theme(value = "sapl", variant = Lumo.DARK)
public class SaplServerCeApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(SaplServerCeApplication.class, args);
	}
}
