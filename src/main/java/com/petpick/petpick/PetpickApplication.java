package tw.petpick.petpick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PetpickApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetpickApplication.class, args);
	}

}
