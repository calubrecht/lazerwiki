package us.calubrecht.lazerwiki.service.helpers.doku;

import us.calubrecht.lazerwiki.service.helpers.RenderHelper_Line;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DokuHeader implements RenderHelper_Line {
    static final Pattern pattern = Pattern.compile("\\s*(={2,})(.*?)(={2,})\\s*");

    @Override
    public boolean matches(String line) {
        return pattern.matcher(line).matches();
    }

    @Override
    public String render(String line) {
        Matcher matcher = pattern.matcher(line);
        matcher.matches();
        int headerSize = matcher.group(1).length();
        headerSize = headerSize > 6 ? 6 : headerSize;
        String hTag = "h" + (7 - headerSize);
        return "<%s>%s</%s>".formatted(hTag, matcher.group(2).strip(), hTag);
    }
}
