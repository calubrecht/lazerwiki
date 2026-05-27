package us.calubrecht.lazerwiki.syntax.renderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.LinkOverrideInstance;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.service.MediaOverrideService;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeNode;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode;
import us.calubrecht.lazerwiki.syntax.nodes.ImageNode.ALIGN_TYPE;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.*;

@Component("customSynImageRenderer")
public class ImageRenderer extends AbstractRenderer {
    final MediaOverrideService mediaOverrideService;

    public ImageRenderer(@Autowired MediaOverrideService mediaOverrideService) {
        this.mediaOverrideService = mediaOverrideService;
    }

    @Value("#{'${lazerwiki.unscalable-image.ext}'.split(',')}")
    private Set<String> unscalableImageExts;

    @Override
    public Collection<Class<? extends ITreeNode>> getTargets() {
        return List.of(ImageNode.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StringBuilder renderHtml(ITreeNode node, RenderContext renderContext) {
        ImageNode imgNode = (ImageNode)node;
        String src = imgNode.getSource();
        if (!validateSrc(src)) {
            String error = String.format("Suspicious img tag src at %s. Raw text =[%s]", imgNode.getPosition().getLeft(), src);
            addError(renderContext, error);
            src = "invalidSource.none";
        }
        if (suspiciousSrc(src)) {
            String error = String.format("Suspicious img tag src at %s. Raw text =[%s]", imgNode.getPosition().getLeft(), src);
            addError(renderContext, error);
        }
        src = doOverrides(src, imgNode, renderContext);
        Map<String, String> options = parseOptions(imgNode.getOptions(), imgNode.getSource());
        String size = options.getOrDefault("size", "");
        ((Set<String>)renderContext.renderState().computeIfAbsent(IMAGES.name(), (k) -> new HashSet<>())).add(src);
        if (options.getOrDefault("linkType", "").equals("linkonly")) {
            return renderLinkOnly(src, imgNode.getTitle(), size);
        }
        String cssClass = getCssClass(options, imgNode.getAlignment());
        StringBuilder buffer = new StringBuilder();
        String sizeStyle = options.getOrDefault("sizeStyle", "");
        buffer.append(String.format("<img src=\"/_media/%s%s\" class=\"%s\"%s", src, size, cssClass, sizeStyle));
        if (imgNode.getTitle() != null) {
            String title =  sanitize(imgNode.getTitle());
            if (!title.equals(imgNode.getTitle())) {
                String error = String.format("Suspicious img tag title at %s. Raw text =[%s]", imgNode.getPosition().getLeft(), imgNode.getTitle());
                addError(renderContext, error);
            }
            buffer.append(String.format(" title=\"%s\"", title));
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

    String getExt(String source) {
        String[] parts = source.split("\\.");
        return parts[parts.length-1];
    }

    final Pattern sizePattern = Pattern.compile("(c)?([0-9]+)(x([0-9]+))?");
    Map<String, String> parseOptions(String options, String source) {
        String[] allOptions = options.split("&");
        Map<String, String> optionMap = new HashMap<>();
        for (String option : allOptions) {
            Matcher sizeMatcher = sizePattern.matcher(option);
            if (sizeMatcher.matches()) {
                String containsKey = sizeMatcher.group(1) == null ? "" : sizeMatcher.group(1);
                String s1 = sizeMatcher.group(2);
                String s2 = sizeMatcher.group(4) == null ? "" : sizeMatcher.group(4);
                optionMap.put("size", !s2.isEmpty() ? String.format("?%s%sx%s", containsKey,s1, s2) : String.format("?%s%s", containsKey,s1));
                String extension = getExt(source);
                if (unscalableImageExts.contains(extension)) {
                    List<String> styles = new ArrayList<>();
                    if (!s1.equals("0")) {
                        styles.add(String.format("width:%spx", s1));
                    }
                    if (!s2.isEmpty() && !s2.equals("0")) {
                        styles.add(String.format("height:%spx", s2));
                    }
                    optionMap.put("sizeStyle", String.format(" style=\"%s\"", String.join("; ", styles)));
                }
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

    final Pattern srcPattern = Pattern.compile("[A-z0-9-_:.&? ]+");
    boolean validateSrc(String src) {
        Matcher m = srcPattern.matcher(src.strip());
        return m.matches();
    }

    /**
     * No exploit with these patterns (will be parsed and treated as a namespace), but likely
     * an attempt at an exploit.
     */
    final Pattern suspiciousSrcPattern = Pattern.compile("^(javascript|data|vbscript):.*");
    private boolean suspiciousSrc(String src) {
        Matcher m = suspiciousSrcPattern.matcher(src.strip());
        return m.matches();
    }

    @SuppressWarnings("unchecked")
    String doOverrides(String file, ImageNode node, RenderContext renderContext) {
        Map<String, MediaOverride> overrides = (Map<String, MediaOverride>) renderContext.renderState().get(MEDIA_OVERRIDES.name());
        if (overrides == null) {
            List<MediaOverride> mediaOverrideList = mediaOverrideService.getOverrides(renderContext.host(), renderContext.page());
            overrides = mediaOverrideList.stream().collect(
                    Collectors.toMap(MediaOverride::getTarget, Function.identity(), (a, b) -> b)
            );
            renderContext.renderState().put(MEDIA_OVERRIDES.name(), overrides);
        }
        if (overrides.containsKey(file)) {
            String override = overrides.get(file).getNewTarget();
            int startIndex = node.getSourcePosition().getLeft();
            int endIndex = node.getSourcePosition().getRight() + 1;
            ((List<LinkOverrideInstance>)renderContext.renderState().computeIfAbsent(OVERRIDE_STATS.name(),
                    (k) -> new ArrayList<>())).add(
                    new LinkOverrideInstance(file, override, startIndex, endIndex));
            return override;
        }
        return file;
    }



    @Override
    public StringBuilder renderPlaintext(ITreeNode node, RenderContext renderContext) {
        ImageNode imgNode = (ImageNode)node;
        return new StringBuilder(imgNode.getTitle() == null ? imgNode.getSource() : imgNode.getTitle());
    }
}
