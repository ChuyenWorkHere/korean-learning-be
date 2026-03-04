package edu.language.kbee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailService userDetailService;

    public static Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            logger.info("Request medthod: {} Request URI: {}", request.getMethod(), request.getRequestURI());
            if(isBypassToken(request)) {
                filterChain.doFilter(request, response);
                return;
            }

//            final String authHeader = request.getHeader("Authorization");
//
//            logger.info("Authorization Header: {}", authHeader);
//
//            if (authHeader == null || !authHeader.startsWith("Bearer")) {
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
//                return;
//            }
//            final String token = authHeader.substring(7);
            String token = null;

            if(request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if("accessToken".equals(cookie.getName())){
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtTokenUtil.extractUsername(token);
            if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                CustomUserDetails userDetails = (CustomUserDetails) userDetailService.loadUserByUsername(username);

                if (!userDetails.isAccountNonLocked()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account is being locked");
                }

                boolean isValidated = this.jwtTokenUtil.validateToken(token, userDetails);
                if(isValidated) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e ) {
            e.printStackTrace();
            logger.error("Error: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }

    public boolean isBypassToken(HttpServletRequest request) {

        final List<Pair<String, String>> bypassToken = Arrays.asList(
                Pair.of("/api/v1/auth/login", "POST"),
                Pair.of("/api/v1/auth/signup", "POST"),
                Pair.of("/api/v1/public", "GET"));

        final List<String> staticResourcePrefixes = Arrays.asList(
                "/images/",
                "/static/",
                "/public/",
                "/css/",
                "/js/",
                "/uploads/"
        );

        for (Pair<String, String> token : bypassToken) {
            if(request.getServletPath().contains(token.getFirst())
            && request.getMethod().equals(token.getSecond())) {
                return true;
            }
        }

        String servletPath = request.getServletPath();
        for (String prefix : staticResourcePrefixes) {
            if (servletPath.startsWith(prefix)) {
                logger.info("Bypassing static resource: {}", servletPath);
                return true;
            }
        }


        return false;
    }
}
