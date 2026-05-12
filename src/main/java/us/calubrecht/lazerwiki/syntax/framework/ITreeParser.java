package us.calubrecht.lazerwiki.syntax.framework;

import java.util.List;

public interface ITreeParser {
    /**
     * Parse the markupLines into an ITreeNode, or null if Parser cannot parse this input;
     * Remove any parsed lines from markupLines.
     */
    ITreeNode parse(List<String> markupLines);

    String parserKey();
    default int priority() {
        return 100;
    }
}
