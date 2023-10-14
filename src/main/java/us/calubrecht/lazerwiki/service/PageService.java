package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

@Service
public class PageService {

    public boolean exists(String pageName) {
        return false;
    }

    public String getTitle(String pageName) {
        if (!exists(pageName)) {
            return Arrays.stream(pageName.split(":")).reduce((first, second) -> second)
                    .orElse(null);
        }
        return "";
    }

    public String getSource(String pageName) {
        return "";
    }
}
