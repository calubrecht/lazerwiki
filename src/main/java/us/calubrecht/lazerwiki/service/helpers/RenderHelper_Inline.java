package us.calubrecht.lazerwiki.service.helpers;


import org.apache.commons.lang3.tuple.Pair;

public interface RenderHelper_Inline {
    /**
     * @param line of text to look for matching
     * @return Pair consisting of first character of match, and length of match
     */
    public Pair<Integer, Integer> matches(String line);
    public String render(String line);
}
