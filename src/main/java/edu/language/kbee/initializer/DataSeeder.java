package edu.language.kbee.initializer;

import edu.language.kbee.enums.GenderName;
import edu.language.kbee.enums.RoleName;
import edu.language.kbee.model.Role;
import edu.language.kbee.model.User;
import edu.language.kbee.repository.RoleRepository;
import edu.language.kbee.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            if (userRepository.count() == 0 && roleRepository.count() == 0) {
                Role userRole = new Role();
                Role adminRole = new Role();

                userRole.setRoleName(RoleName.USER);
                adminRole.setRoleName(RoleName.ADMIN);

                roleRepository.save(userRole);
                roleRepository.save(adminRole);

                User user = User.builder()
                        .username("user")
                        .password(passwordEncoder.encode("abc123!"))
                        .email("user@gmail.com")
                        .userAvatar("https://i.pinimg.com/474x/62/01/0d/62010d848b790a2336d1542fcda51789.jpg?nii=t")
                        .dateOfBirth(LocalDate.of(2004, 6, 11))
                        .fullName("Lê Văn Chuyền")
                        .genderName(GenderName.MALE)
                        .isLocked((byte) 0)
                        .roles(Set.of(userRole))
                        .build();

                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("abc123!"))
                        .email("admin@gmail.com")
                        .userAvatar("https://i.pinimg.com/474x/62/01/0d/62010d848b790a2336d1542fcda51789.jpg?nii=t")
                        .dateOfBirth(LocalDate.of(2004, 6, 11))
                        .fullName("Lê Văn Admin")
                        .genderName(GenderName.MALE)
                        .isLocked((byte) 0)
                        .roles(Set.of(adminRole))
                        .build();

                userRepository.save(user);
                userRepository.save(admin);
            }
        };
    }
}
