package com.hellfire.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;


public class JwtTokenValidator extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt=request.getHeader(JwtConstant.JWT_HEADER);
        if (jwt != null && jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
            try{
                SecretKey key= Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key) // Set the signing key
                        .build()
                        .parseClaimsJws(jwt) // Parse and validate the JWT
                        .getBody();


                String email=claims.get("email",String.class);
                String authorities=claims.get("authorities",String.class);
                String roles=claims.get("roles",String.class);

                List<GrantedAuthority> auth= AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
                Authentication authentication= new UsernamePasswordAuthenticationToken(email,null,auth);
                SecurityContextHolder.getContext().setAuthentication(authentication);

//                System.out.println("Authorities added: " + authorities);
//                System.out.println("Authenticated: " + authentication);
//                System.out.println("Current user: " + SecurityContextHolder.getContext().getAuthentication().getName());
//                System.out.println("Current user principal: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
//                System.out.println("Current user authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
//                System.out.println("Current User roles: " + roles);
            }catch (Exception e){

                throw  new BadCredentialsException("invalid token....");
            }


        }

        filterChain.doFilter(request,response);
    }
}
