package us.calubrecht.lazerwiki.model;

import java.util.List;

public record UserDTO (String userName, String siteName, List<String> userRoles){
    
}
