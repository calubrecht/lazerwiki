package us.calubrecht.lazerwiki.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public record UserDTO (String userName, @JsonInclude(JsonInclude.Include.NON_NULL) String siteName, List<String> userRoles, Map<String, Object> settings){
    
}
