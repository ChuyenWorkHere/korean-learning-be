package edu.language.kbee.service.impl;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.InternalServerException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Role;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.LoginRequest;
import edu.language.kbee.payload.request.SignUpRequest;
import edu.language.kbee.payload.response.LoginResponse;
import edu.language.kbee.repository.RoleRepository;
import edu.language.kbee.repository.UserRepository;
import edu.language.kbee.security.JwtTokenUtil;
import edu.language.kbee.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;

    public static Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            logger.error("Username: " + request.getUsername() + ", password " + request.getPassword());
            throw new BadRequestException("Tài khoản hoặc mật khẩu không chính xác");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = this.jwtTokenUtil.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return LoginResponse.builder()
                .username(userDetails.getUsername())
                .token(token)
                .roles(roles)
                .build();
    }

    @Override
    public String signup(SignUpRequest signUpRequest) {
        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            throw new BadRequestException("Email đã tồn tại");
        }
        if(userRepository.existsByUsername(signUpRequest.getEmail())){
            throw new BadRequestException("Username đã tồn tại");
        }

        User user = User.builder()
                .username(signUpRequest.getEmail())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .fullName(signUpRequest.getFullName())
                .build();

        Role role = roleRepository.findByRoleName(RoleName.USER)
                .orElseThrow(() -> new InternalServerException("Lỗi khi tạo mới người dùng, vui lòng thử lại sau!"));

        user.getRoles().add(role);
        userRepository.save(user);
        return "success";
    }

    @Override
    public UserDto getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        Set<Role> roleSet = loggedInUser.getRoles();
        List<String> roleName = roleSet.stream().map( r -> String.valueOf(r.getRoleName())).toList();

        UserDto userDto = mapper.map(loggedInUser, UserDto.class);
        userDto.setRoles(roleName);


        return userDto;
    }
}
