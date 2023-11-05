package us.calubrecht.lazerwiki.service.renderhelpers;

import ch.qos.logback.core.joran.sanity.Pair;

public interface RenderHelper_Inline {
    /**
     * @param line of text to look for matching
     * @return Pair consisting of first character of match, and length of match
     */
    public Pair<Integer, Integer> matches(String line);
    public String render(String line);
}
