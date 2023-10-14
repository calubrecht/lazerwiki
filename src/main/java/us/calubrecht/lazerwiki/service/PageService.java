package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

@Service
public class PageService {

    public boolean exists(String pageName) {
        return true;
    }

    public String getSource(String pageName) {
        return "";
    }
}
