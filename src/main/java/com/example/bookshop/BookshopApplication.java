/*
 ^ TUTORIAL 01 — The entry point of a Spring Boot app

 * This is the only class the generator created. Everything else we write later.
 * It is a normal Java class with a normal main method. Nothing magic yet.

 ? @SpringBootApplication is one annotation that stands for three:
 ?   @Configuration        — "this class may define objects for Spring to manage"
 ?   @EnableAutoConfiguration — "set things up for me based on what is on the classpath"
 ?   @ComponentScan        — "look in this package and below for my classes"

 * An annotation is a label you attach to code. It does nothing by itself.
 * Some OTHER code reads the label and acts on it. Here, Spring reads it.

 ? SpringApplication.run(...) does, in order:
 ?   1. scans com.example.bookshop and every package under it
 ?   2. creates one object of each class it finds labelled for it (these are "beans")
 ?   3. starts an embedded Tomcat web server (because the web starter is on the classpath)
 ?   4. keeps the program alive, listening on port 8080

 ! Common mistake: moving this class into a deeper package (e.g. com.example.bookshop.app)
 ! while other classes sit OUTSIDE that package. @ComponentScan only looks DOWN from
 ! this file's package, so those classes are silently never found. No error, no bean,
 ! and later an exception like "required a bean of type ... that could not be found".
 + Keep this class in the ROOT package of the project. Put everything else below it.

 * Without the web starter this main method would run and the program would just end,
 * like any Java program. The web server is what keeps it running.

 ^ TUTORIAL 04
 ? @ConfigurationPropertiesScan finds classes annotated with
 ? @ConfigurationProperties (our BookshopProperties) and registers
 ? them as beans, the same way component scan finds @Component.
*/
package com.example.bookshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookshopApplication.class, args);
	}

}
