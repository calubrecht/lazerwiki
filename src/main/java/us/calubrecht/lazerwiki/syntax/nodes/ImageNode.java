package us.calubrecht.lazerwiki.syntax.nodes;

public class ImageNode extends AbstractNode {
    final String source;
    final String title;
    final String options;
    public enum ALIGN_TYPE {NONE, LEFT, RIGHT, CENTER}

    final ALIGN_TYPE alignment;

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
