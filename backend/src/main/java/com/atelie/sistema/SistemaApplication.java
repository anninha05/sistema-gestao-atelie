package com.atelie.sistema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class SistemaApplication {
	public static final ZoneId FUSO_DO_ATELIE = ZoneId.of("America/Sao_Paulo");

	public static void main(String[] args) {
		configurarFusoHorario();
		SpringApplication.run(SistemaApplication.class, args);
	}

	static void configurarFusoHorario() {
		TimeZone.setDefault(TimeZone.getTimeZone(FUSO_DO_ATELIE));
	}
}
