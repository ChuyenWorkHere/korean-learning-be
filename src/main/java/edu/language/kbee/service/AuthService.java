package edu.language.kbee.service;


import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.LoginRequest;
import edu.language.kbee.payload.request.SignUpRequest;
import edu.language.kbee.payload.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    String signup(SignUpRequest signUpRequest);

    UserDto getLoggedInUser();
}
