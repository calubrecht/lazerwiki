package us.calubrecht.lazerwiki.syntax.framework;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface ITreeParser {
    /**
     * Parse the markupLines into an ITreeNode, or null if Parser cannot parse this input;
     * Remove any parsed lines from markupLines. Increments counter by number of characters consumed (Plus newline for every line)
     */
    ITreeNode parse(List<String> markupLines, AtomicInteger counter);

    void setRegistrar(ParserRegistrar registrar);

    /**
     * Parser analyzes the next line to do a quick short-circuit analysis to tell if it can parse the block
     */
    boolean canBeginParse(String line);

    String parserKey();
    default int priority() {
        return 100;
    }
}
