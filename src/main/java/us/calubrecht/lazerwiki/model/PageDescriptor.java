package us.calubrecht.lazerwiki.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record PageDescriptor(String namespace, String pageName) {

    @Override
    public String toString() {
        return this.namespace + ":" + this.pageName;
    }

    final static Pattern WORD_FINDER = Pattern.compile("([A-Z]?[a-z]+)|[A-Z]|[0-9]+");

    public String renderedName() {
        if (pageName.isEmpty()) {
            return "Home";
        }
        Matcher matcher = WORD_FINDER.matcher(pageName);
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group(0));
        }
        return words.stream().flatMap(a -> Arrays.stream(a.split("_")).map(w -> ("" +w.charAt(0)).toUpperCase() + w.substring(1))).collect(Collectors.joining(" "));
    }

    public boolean isHome() {
        return (pageName + namespace).isEmpty();
    }
}
