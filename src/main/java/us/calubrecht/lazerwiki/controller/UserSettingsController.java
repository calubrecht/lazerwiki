package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.model.UserRequest;
import us.calubrecht.lazerwiki.responses.SetPasswordResponse;
import us.calubrecht.lazerwiki.service.UserService;

import java.security.Principal;

@RestController
@RequestMapping("api/users/")
public class UserSettingsController {

    @Autowired
    UserService userService;

    @PostMapping("setPassword")
    public ResponseEntity<SetPasswordResponse> setPassword(Principal principal, @RequestBody UserRequest passwordRequest) {
        User user = userService.getUser(principal.getName());
        if (!user.userName.equals(passwordRequest.userName()))
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.resetPassword(user.userName, passwordRequest.password());
        return ResponseEntity.ok(new SetPasswordResponse(true, ""));
    }
}
