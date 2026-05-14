package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode.ALIGN_TYPE;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.IMAGES;

@Component("customSynImageRenderer")
public class ImageRenderer extends AbstractRenderer {
    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(ImageNode.class);
    }

    @Override
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        ImageNode imgNode = (ImageNode)node;
        String src = imgNode.getSource();
        Map<String, String> options = parseOptions(imgNode.getOptions());
        String size = options.getOrDefault("size", "");
        ((Set<String>)renderContext.renderState().computeIfAbsent(IMAGES.name(), (k) -> new HashSet<>())).add(src);
        if (options.getOrDefault("linkType", "").equals("linkonly")) {
            return renderLinkOnly(src, imgNode.getTitle(), size);
        }
        String cssClass = getCssClass(options, imgNode.getAlignment());
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("<img src=\"/_media/%s%s\" class=\"%s\"", src, size, cssClass));
        if (imgNode.getTitle() != null) {
            buffer.append(String.format(" title=\"%s\"", imgNode.getTitle().replaceAll("\"", "\\")));
        }
        buffer.append(" loading=\"lazy\">");
        return buffer;
    }

    StringBuilder renderLinkOnly(String src, String title, String size) {
        String linkText = title == null ? src : title;
        return new StringBuilder(String.format("<a href=\"/_media/%s%s\" class=\"media linkOnly\" target=\"_blank\">%s</a>", src, size, linkText));
    }

    final Map<ALIGN_TYPE, String> alignClasses = Map.of(
            ALIGN_TYPE.NONE, "media",
            ALIGN_TYPE.CENTER, "mediacenter",
            ALIGN_TYPE.LEFT, "medialeft",
            ALIGN_TYPE.RIGHT, "mediaright");

    String getCssClass(Map<String, String> options, ALIGN_TYPE alignment) {
        List<String> classes = new ArrayList<>();
        classes.add(alignClasses.get(alignment));
        if (options.getOrDefault("linkType", "").equals("full")) {
            classes.add("fullLink");
        }
        return String.join(" ", classes);
    }

    final Pattern sizePattern = Pattern.compile("([0-9]+)(x(0-9]+))?");
    Map<String, String> parseOptions(String options) {
        String[] allOptions = options.split("&");
        Map<String, String> optionMap = new HashMap<>();
        for (String option : allOptions) {
            Matcher sizeMatcher = sizePattern.matcher(option);
            if (sizeMatcher.matches()) {
                String s1 = sizeMatcher.group(1);
                String s2 = sizeMatcher.group(3);
                optionMap.put("size", s2 == null ? String.format("?%s", s1) : String.format("?%sx%s", s1, s2));
            }
            if (option.equals("fullLink")) {
                optionMap.put("linkType", "full");
            }
            if (option.equals("linkonly")) {
                optionMap.put("linkType", "linkonly");
            }
        }
        return optionMap;
    }

    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        ImageNode imgNode = (ImageNode)node;
        return new StringBuilder(imgNode.getTitle() == null ? imgNode.getSource() : imgNode.getTitle());
    }
}
