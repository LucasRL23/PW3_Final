package br.edu.ifsp.prw3.consertos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class Application {

	public static void main(String[] args) {

		// PRW3 - Avaliação 4 - API REST de consertos com Spring Security e JWT
		// Integrantes:
		//   Guilherme Ozaki
		//   Lucas Rodrigues

		SpringApplication.run(Application.class, args);
	}

}
