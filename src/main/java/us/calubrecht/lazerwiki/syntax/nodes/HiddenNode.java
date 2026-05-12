package us.calubrecht.lazerwiki.syntax.nodes;

import java.util.Map;

public class HiddenNode extends ContainerNode {
    Map<String, String> options;

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public Map<String, String> getOptions() {
        return options;
    }
}
