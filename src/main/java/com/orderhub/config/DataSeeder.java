package com.orderhub.config;

import com.orderhub.entity.Role;
import com.orderhub.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository) {
        return args -> {
            if (!roleRepository.existsByName("USER")) {
                Role userRole = new Role();
                userRole.setName("USER");
                roleRepository.save(userRole);
            }

            if (!roleRepository.existsByName("ADMIN")) {
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                roleRepository.save(adminRole);
            }
        };
    }
}