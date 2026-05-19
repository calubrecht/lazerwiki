package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ParseContext;

public class ImageNode extends AbstractNode {
    final String source;
    final String title;
    final String options;
    public enum ALIGN_TYPE {NONE, LEFT, RIGHT, CENTER}

    final ALIGN_TYPE alignment;
    Pair<Integer, Integer> sourcePosition;

    public ImageNode(String source, String title, String options, ALIGN_TYPE alignment) {
        this.source = source;
        this.title = title;
        this.options = options;
        this.alignment = alignment;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getOptions() {
        return options;
    }

    public ALIGN_TYPE getAlignment() {
        return alignment;
    }

    public void setSourcePosition(Pair<Integer, Integer> sourcePosition) {
        this.sourcePosition = sourcePosition;
    }
    public Pair<Integer, Integer> getSourcePosition() {
        return sourcePosition;
    }

    public String getSourceSourceFromContext() {
        if (getParseContext() == null || getPosition() == null) {
            // Node has not been fully initialized, cannot get source
            return null;
        }
        ParseContext context = getParseContext();
        Pair<Integer, Integer> position = getSourcePosition();
        // Position uses inclusive endpoints, substring's end is exclusive
        return context.getFullText().substring(position.getLeft(), position.getRight() + 1);
    }
}
