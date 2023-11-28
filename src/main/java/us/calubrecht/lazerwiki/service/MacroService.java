package us.calubrecht.lazerwiki.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.*;

@Service
public class MacroService {
    Logger logger = LogManager.getLogger(getClass());
    Map<String, Macro> macros = new HashMap<>();

    @Autowired
    MacroCssService macroCssService;

    @Autowired
    PageService pageService;

    @Value("#{'${lazerwiki.plugin.scan.packages}'.split(',')}")
    private List<String> macroPackageds;

    public void registerMacro(Macro macro) {
        logger.info("Registering macro " + macro.getName() + " as " + macro.getClass());
        macros.put(macro.getName(), macro);
        macro.getCSS().ifPresent(css -> macroCssService.addCss(css));
    }

    @PostConstruct
    public void registerMacros() {
        AnnotatedTypeScanner scanner= new AnnotatedTypeScanner(CustomMacro.class);
        Set<Class<?>> macroClasses = scanner.findTypes(macroPackageds);
        macroClasses.forEach((cl) -> {
            try {
                Macro macro = (Macro)cl.getDeclaredConstructor().newInstance();
                registerMacro(macro);

            } catch (Exception e) {
                logger.error("Failed to instantiate a macro of type " + cl + ".", e);
            }
        });
    }

    protected String sanitize(String input) {
        return StringEscapeUtils.escapeHtml4(input).replaceAll("&quot;", "\"");
    }

    public String renderMacro(String macroText, RenderContext renderContext) {
        String[] parts = macroText.split(":", 2);
        String macroName = parts[0];
        String macroArgs = parts.length > 1 ? parts[1] : "";

        Macro macro = macros.get(macroName);
        if (macro == null) {
            return "MACRO- Unknown Macro " + sanitize(macroName);
        }
        String macroKey = "macroRunning:" + macroName;
        if (renderContext.renderState().containsKey(macroKey)) {
            // Prevent recursive macro calls
            return "";
        }
        renderContext.renderState().put(macroKey, "1");
        try{
            return macro.render(new MacroContextImpl(renderContext), macroArgs);
        } finally
        {
            renderContext.renderState().remove(macroKey);
        }
    }

    class MacroContextImpl implements Macro.MacroContext {
        private final RenderContext renderContext;

        public MacroContextImpl(RenderContext renderContext) {
            this.renderContext = renderContext;
        }

        @Override
        public String sanitize(String input) {
            return MacroService.this.sanitize(input);
        }

        @Override
        public Pair<String, Map<String, Object>> renderPage(String pageDescriptor) {
            PageData page = pageService.getPageData(renderContext.host(), pageDescriptor, renderContext.user());
            if (!page.exists() || !page.userCanRead()) {
                return Pair.of("", Collections.emptyMap());
            }

            RenderContext subrenderContext = new RenderContext(renderContext.host(), renderContext.site(),
                    renderContext.user(),renderContext.renderer(), new HashMap<>());
            subrenderContext.renderState().putAll(renderContext.renderState());
            // Allow inner page render to generate its own title
            subrenderContext.renderState().remove(RenderResult.RENDER_STATE_KEYS.TITLE.name());
            RenderResult res = renderContext.renderer().renderWithInfo(page.source(),subrenderContext);
            return Pair.of(res.renderedText(), res.renderState());

        }

        @Override
        public List<String> getPagesByNSAndTag(String ns, String tag) {
            return pageService.searchPages(renderContext.host(), renderContext.user(), Map.of("tag", tag, "ns", ns)).
                    stream().map(pd -> pd.getDescriptor()).toList();
        }

        @Override
        public Pair<String, Map<String, Object>> renderMarkup(String markup) {
            RenderContext subrenderContext = new RenderContext(renderContext.host(), renderContext.site(),
                    renderContext.user(),renderContext.renderer(), new HashMap<>());
            subrenderContext.renderState().putAll(renderContext.renderState());
            // Allow inner page render to generate its own title
            subrenderContext.renderState().remove(RenderResult.RENDER_STATE_KEYS.TITLE.name());
            RenderResult res = renderContext.renderer().renderWithInfo(markup,subrenderContext);
            return Pair.of(res.renderedText(), res.renderState());
        }
    }
}
