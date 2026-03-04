package edu.language.kbee.payload;

import edu.language.kbee.enums.GenderName;
import lombok.*;

import java.time.LocalDate;

@Getter@Setter
@NoArgsConstructor@AllArgsConstructor
@Builder
public class UserDto {

    private Long userId;

    private String username;

    private String fullName;

    private GenderName genderName;

    private byte isLocked;

    private String email;

    private LocalDate dateOfBirth;

    private String userAvatar;

}
