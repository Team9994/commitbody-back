package team9499.commitbody;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CommitBodyApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommitBodyApplication.class, args);
	}

}
