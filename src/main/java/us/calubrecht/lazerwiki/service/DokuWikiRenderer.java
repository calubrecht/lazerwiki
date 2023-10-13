package us.calubrecht.lazerwiki.service;

import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.service.helpers.RenderHelper;
import us.calubrecht.lazerwiki.service.helpers.doku.DokuHeader;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An implementation of IMarkupRenderer that speaks DokuWiki's markup language.
 */
@Service
public class DokuWikiRenderer implements IMarkupRenderer {
    @Override
    public String render(String markup) {
        return Arrays.stream(markup.split("\n")).map(line -> parseLine(line)).collect(Collectors.joining("\n"));
    }

    protected String parseLine(String line) {
        RenderHelper helper = new DokuHeader();
        if (helper.matches(line)) {
            return helper.render(line);
        }
        return line;
    }
}
