package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.model.LinkOverrideInstance;
import us.calubrecht.lazerwiki.model.MediaOverride;
import us.calubrecht.lazerwiki.service.MediaOverrideService;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser.ImageContext;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.service.renderhelpers.TypedRenderer;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.IMAGES;
import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.MEDIA_OVERRIDES;
import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.OVERRIDE_STATS;

@Component
public class ImageRenderer  extends TypedRenderer<ImageContext> {
    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    MediaOverrideService mediaOverrideService;

    @Value("#{'${lazerwiki.unscalable-image.ext}'.split(',')}")
    private Set<String> unscalableImageExts;

    @Override
    public List<Class<? extends ParseTree>> getTargets() {
        return List.of(ImageContext.class);
    }

    @Override
    public StringBuilder renderContext(ImageContext tree, RenderContext renderContext) {
        String inner = renderChildren(getChildren(tree, 1, tree.getChildCount()-1), renderContext).toString();
        return parseInner(inner, tree, renderContext);
    }

    String doOverrides(String file, ImageContext tree, RenderContext renderContext) {
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
            int startIndex = tree.inner_text(0).getStart().getStartIndex();
            String linkText = tree.inner_text(0).getText();
            startIndex += linkText.indexOf(file);
            ((List<LinkOverrideInstance>)renderContext.renderState().computeIfAbsent(OVERRIDE_STATS.name(),
                    (k) -> new ArrayList<>())).add(
                    new LinkOverrideInstance(file, override, startIndex, startIndex + file.length()));
            return override;
        }
        return file;
    }

    @Override
    public StringBuilder renderContextToPlainText(ImageContext tree, RenderContext renderContext) {
        String inner = renderChildren(getChildren(tree, 1, tree.getChildCount()-1), renderContext).toString();
        Map<INNARD_TOKEN, String> innards = splitInnards(inner);
        if (innards.get(INNARD_TOKEN.TITLE) != null) {
            return new StringBuilder(innards.get(INNARD_TOKEN.TITLE));
        }
        return new StringBuilder(innards.get(INNARD_TOKEN.FILE_NAME));
    }

    final Pattern innardsPattern = Pattern.compile("^(?<fileTok> *(?<fileName>[\\w.: \\-]+)(\\?(?<options>\\w+(&\\w+)*))? *)(\\|(?<title>.*))?$");

    enum INNARD_TOKEN {FILE_NAME, FILE_TOK, OPTIONS, TITLE}

    Map<INNARD_TOKEN, String> splitInnards(String inner) {
        Map<INNARD_TOKEN, String> ret = new HashMap<>();
        Matcher match = innardsPattern.matcher(inner);
        if (!match.matches()) {
            logger.error("Cannot parse image markup: " + inner);
            ret.put(INNARD_TOKEN.FILE_NAME, inner.split("\\?")[0]);
            ret.put(INNARD_TOKEN.FILE_TOK, inner.split("\\?")[0]);
            return ret;
        }
        ret.put(INNARD_TOKEN.FILE_TOK, match.group("fileTok"));
        ret.put(INNARD_TOKEN.FILE_NAME, match.group("fileName"));
        ret.put(INNARD_TOKEN.OPTIONS, match.group("options"));
        ret.put(INNARD_TOKEN.TITLE, match.group("title"));
        return ret;

    }

    protected StringBuilder renderChildren(List<ParseTree> trees, RenderContext renderContext) {
        StringBuilder outBuffer = new StringBuilder();
        for(ParseTree child: trees) {
            outBuffer.append(child.getText());
        }
        return outBuffer;
    }

    static final Pattern SIZE_PATTERN= Pattern.compile("([0-9]+)(x[0-9]+)?");
    String getSizeTok(String options) {
        if (options == null) {
            return "";
        }
        String[] toks = options.split("&");
        for (String tok : toks) {
            if (SIZE_PATTERN.matcher(tok).matches()) {
                return "?" + tok;
            }
        }
        return "";
    }

    String getSizeStyle(String options) {
        if (options == null) {
            return "";
        }
        String[] toks = options.split("&");
        for (String tok : toks) {
            Matcher m = SIZE_PATTERN.matcher(tok);
            if (m.matches()) {
                int width = Integer.parseInt(m.group(1));
                if (m.group(2) != null) {
                    int height = Integer.parseInt(m.group(2).substring(1));
                    if (width == 0) {
                        return "height:" + height+ "px";
                    }
                    return "width:" + width + "px; height:" + height + "px";
                }
                return "width:" + width + "px";
            }
        }
        return "";
    }

    boolean getIsLink(String options) {
        if (options == null) {
            return false;
        }
        String[] toks = options.split("&");
        for (String tok : toks) {
            if (tok.equals("fullLink")) {
                return true;
            }
        }
        return false;
    }

    boolean isLinkOnly(String options) {
        if (options == null) {
            return false;
        }
        String[] toks = options.split("&");
        for (String tok : toks) {
            if (tok.equals("linkonly")) {
                return true;
            }
        }
        return false;
    }

    StringBuilder parseInner(String inner, ImageContext tree, RenderContext renderContext) {
        StringBuilder sb = new StringBuilder();
        Map<INNARD_TOKEN, String> innards = splitInnards(inner);
        String className = "media";
        String imageTok = innards.get(INNARD_TOKEN.FILE_TOK);
        String fileName= doOverrides(innards.get(INNARD_TOKEN.FILE_NAME), tree, renderContext).trim();
        if (isLinkOnly(innards.get(INNARD_TOKEN.OPTIONS))) {
            sb.append("<a href=\"/_media/");
            sb.append(fileName);
            sb.append("\" class=\"media linkOnly\" target=\"_blank\">");
            String titleText = Strings.isBlank(innards.get(INNARD_TOKEN.TITLE)) ? fileName : innards.get(INNARD_TOKEN.TITLE).trim();
            sb.append(titleText);
            sb.append("</a>");
            ((Set<String>)renderContext.renderState().computeIfAbsent(IMAGES.name(), (k) -> new HashSet<>())).add(fileName);
            return sb;
        }
        if (imageTok.startsWith(" ") && imageTok.endsWith(" ")) {
            className = "mediacenter";
        }
        else if (imageTok.startsWith(" ")) {
           className = "mediaright";
        }
        else if (imageTok.endsWith(" ")) {
            className = "medialeft";
        }
        if (getIsLink(innards.get(INNARD_TOKEN.OPTIONS))) {
            className += " fullLink";
        }
        String inlineStyle = "";
        if (unscalableImageExts.contains(FilenameUtils.getExtension(fileName))) {
            String style = getSizeStyle(innards.get(INNARD_TOKEN.OPTIONS));
            inlineStyle = style.isBlank() ? "" : " style=\"" +style + "\"";
        }
        sb.append("<img src=\"/_media/");
        sb.append(fileName).append(getSizeTok(innards.get(INNARD_TOKEN.OPTIONS)));
        String titleText = Strings.isBlank(innards.get(INNARD_TOKEN.TITLE)) ? "" : " title=\"" + innards.get(INNARD_TOKEN.TITLE).trim() + "\"";
        sb.append("\" class=\"").append(className).append("\"").append(titleText).append(inlineStyle).append(" loading=\"lazy\">");
        ((Set<String>)renderContext.renderState().computeIfAbsent(IMAGES.name(), (k) -> new HashSet<>())).add(fileName);
        return sb;
    }
}