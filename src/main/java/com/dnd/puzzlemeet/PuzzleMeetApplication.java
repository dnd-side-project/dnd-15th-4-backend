package com.dnd.puzzlemeet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PuzzleMeetApplication {

  public static void main(String[] args) {
    SpringApplication.run(PuzzleMeetApplication.class, args);
  }
}
