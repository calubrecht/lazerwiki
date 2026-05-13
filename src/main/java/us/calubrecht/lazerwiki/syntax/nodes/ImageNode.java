package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.List;

public class ImageNode extends AbstractNode {
    String source;
    String title;
    String options;
    public enum ALIGN_TYPE {NONE, LEFT, RIGHT, CENTER};

    ALIGN_TYPE alignment;

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
}
