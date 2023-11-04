package us.calubrecht.lazerwiki.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    public void addUser(String userName, String password, List<GrantedAuthority> roles) {

    }
}
