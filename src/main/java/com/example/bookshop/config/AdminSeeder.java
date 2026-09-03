/*
 ^ TUTORIAL 15 — bootstrapping the first admin

 ? Chicken-and-egg: admins are created by admins, so who creates the
 ? first one? Answer everywhere (your Node repo's seed-admin script
 ? included): a seed step reading credentials from configuration.
 ? Here it is a CommandLineRunner - a bean Spring calls once, right
 ? after startup.

 ? Behavior: both values blank -> skip quietly (prod may manage
 ? admins differently). Account already exists -> do nothing, so it
 ? is safe to run on every start.
 ! The password is used to make a hash and never logged. Log the
 !   EMAIL, never the credential.
*/
package com.example.bookshop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.bookshop.model.Role;
import com.example.bookshop.model.UserAccount;
import com.example.bookshop.repository.UserAccountRepository;

@Component
public class AdminSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final UserAccountRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final BookshopProperties properties;

	public AdminSeeder(UserAccountRepository userRepository, PasswordEncoder passwordEncoder,
			BookshopProperties properties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	public void run(String... args) {
		String email = properties.getAdmin().getEmail();
		String password = properties.getAdmin().getPassword();
		if (email.isBlank() || password.isBlank()) {
			log.info("Admin seeding skipped: bookshop.admin.* not configured");
			return;
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			return;
		}
		userRepository.save(new UserAccount("Administrator", email.toLowerCase(),
				passwordEncoder.encode(password), Role.ADMIN));
		log.info("Seeded first admin account: {}", email);
	}
}
