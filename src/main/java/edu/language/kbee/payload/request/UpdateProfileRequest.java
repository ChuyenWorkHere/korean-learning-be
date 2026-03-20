package edu.language.kbee.payload.request;

import edu.language.kbee.enums.GenderName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String fullName;
    private GenderName genderName;
    private LocalDate dateOfBirth;
    private String userAvatar;
}
