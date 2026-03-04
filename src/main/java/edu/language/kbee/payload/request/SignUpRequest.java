package edu.language.kbee.payload.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank
    @Size(min = 5, max = 30, message = "Username must be between 5 and 30 characters")
    private String username;

    @Size(min = 6, message = "Password must have at least 6 characters")
    private String password;

    @NotBlank
    @Email(message = "Email format is incorrect")
    private String email;


}
