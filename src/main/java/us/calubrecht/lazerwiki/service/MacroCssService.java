package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;

@Service
public class MacroCssService {

    final StringBuilder macroCss = new StringBuilder();
    String constructedCss = null;

    public void addCss(String css) {
        macroCss.append(css).append('\n');
    }

    public String getCss() {
        if (constructedCss == null) {
            constructedCss = macroCss.toString();
        }
        return constructedCss;
    }
}
