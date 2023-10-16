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

    public String getSource(String host, String pageDescriptor) {
        return "<div><h1>Header is!</h1><div>This is a page</div><div>This page is %s</div></div>".formatted(pageDescriptor);
    }

    @Deprecated
    public String getSource(String pageName) {
        return getSource(null, pageName);
    }
}
