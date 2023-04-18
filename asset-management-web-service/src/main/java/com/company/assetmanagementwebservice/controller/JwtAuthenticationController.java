package com.company.assetmanagementwebservice.controller;

import com.company.assetmanagementwebservice.service.JwtUserDetailsService;
import com.company.assetmanagementwebservice.service.UserService;
import com.company.assetmanagementwebservice.config.JwtTokenUtil;
import com.company.assetmanagementwebservice.model.jwt.JwtRequest;
import com.company.assetmanagementwebservice.model.jwt.JwtResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin
@RestController
public class JwtAuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private JwtUserDetailsService userDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/api/v1/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());

        final String token = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));


    }

    private void authenticate(String username, String password){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}