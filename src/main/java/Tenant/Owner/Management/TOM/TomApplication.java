package Tenant.Owner.Management.TOM;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TomApplication {

	public static void main(String[] args) {
		SpringApplication.run(TomApplication.class, args);
	}

}
