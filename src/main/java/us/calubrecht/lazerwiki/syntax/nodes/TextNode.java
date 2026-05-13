package us.calubrecht.lazerwiki.syntax.nodes;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;

import java.util.List;

public class TextNode extends AbstractNode {
    String content;

    public TextNode(String content) {
        this.content = content;
    }

    public String asString() {
        return content;
    }

    public String toString() {
        return content;
    }
}
