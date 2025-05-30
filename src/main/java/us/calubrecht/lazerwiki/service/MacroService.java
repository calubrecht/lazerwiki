package us.calubrecht.lazerwiki.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;
import us.calubrecht.lazerwiki.model.LinkOverride;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.SearchResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static us.calubrecht.lazerwiki.model.RenderResult.RENDER_STATE_KEYS.LINKS;

@Service
public class MacroService {
    final Logger logger = LogManager.getLogger(getClass());
    final Map<String, Macro> macros = new HashMap<>();

    @Autowired
    MacroCssService macroCssService;

    @Autowired
    PageService pageService;

    @Autowired
    LinkService linkService;

    @Autowired
    LinkOverrideService linkOverrideService;

    @Value("#{'${lazerwiki.plugin.scan.packages}'.split(',')}")
    private List<String> macroPackages;

    public void registerMacro(Macro macro) {
        logger.info("Registering macro " + macro.getName() + " as " + macro.getClass());
        macros.put(macro.getName(), macro);
        macro.getCSS().ifPresent(css -> macroCssService.addCss(css));
    }

    @PostConstruct
    public void registerMacros() {
        AnnotatedTypeScanner scanner = new AnnotatedTypeScanner(CustomMacro.class);
        Set<Class<?>> macroClasses = scanner.findTypes(macroPackages);
        macroClasses.forEach((cl) -> {
            try {
                Macro macro = (Macro) cl.getDeclaredConstructor().newInstance();
                registerMacro(macro);

            } catch (Exception e) {
                logger.error("Failed to instantiate a macro of type {}.", cl, e);
            }
        });
    }

    protected String sanitize(String input) {
        return StringEscapeUtils.escapeHtml4(input).replaceAll("&quot;", "\"");
    }

    public String renderMacro(String macroText, String fullText, RenderContext renderContext) {
        String[] parts = macroText.split(":", 2);
        String macroName = parts[0];
        String macroArgs = parts.length > 1 ? parts[1] : "";

        @SuppressWarnings("unchecked")
        boolean forCache = BooleanUtils.isTrue((Boolean)renderContext.renderState().get(RenderResult.RENDER_STATE_KEYS.FOR_CACHE.name()));

        Macro macro = macros.get(macroName);
        if (macro == null) {
            return "MACRO- Unknown Macro " + sanitize(macroName);
        }
        if (forCache && !macro.allowCache(new MacroContextImpl(renderContext), macroArgs)) {
            return fullText;
        }
        String macroKey = "macroRunning:" + macroName;
        if (renderContext.renderState().containsKey(macroKey)) {
            // Prevent recursive macro calls
            return "";
        }
        renderContext.renderState().put(macroKey, "1");
        try {
            return macro.render(new MacroContextImpl(renderContext), macroArgs);
        } finally {
            renderContext.renderState().remove(macroKey);
        }
    }

    Pattern macroPattern = Pattern.compile("~~MACRO~~(.*?)~~/MACRO~~", Pattern.MULTILINE | Pattern.DOTALL);
    public String postRender(String fullText, RenderContext context) {
        Matcher matcher = macroPattern.matcher(fullText);
        return matcher.replaceAll(matched -> {
            String macroText = matched.group(1);
            return renderMacro(macroText, matched.group(0), context);
        });
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
        public RenderOutput renderPage(String pageDescriptor) {
            PageData page = pageService.getPageData(renderContext.host(), pageDescriptor, renderContext.user());
            if (!page.flags().exists() || !page.flags().userCanRead()) {
                return new RenderOutputImpl("", new HashMap<String, Object>(page.flags().toMap()));
            }
            PageCache pageCache = pageService.getCachedPage(renderContext.host(), pageDescriptor);
            if (pageCache != null && pageCache.useCache) {
                Map<String, Object> renderState = new HashMap<>(page.flags().toMap());
                renderState.put(RenderResult.RENDER_STATE_KEYS.TITLE.name(), page.title());
                String rendered = postRender(pageCache.renderedCache, renderContext);
                return new RenderOutputImpl(rendered, renderState);
            }

            return doRender(page, pageDescriptor);

        }

        @NotNull
        private RenderOutput doRender(PageData page, String pageDescriptor) {
            RenderContext subrenderContext = new RenderContext(renderContext.host(), renderContext.site(), pageDescriptor,
                    renderContext.user(), renderContext.renderer(), new HashMap<>());
            subrenderContext.renderState().putAll(renderContext.renderState());
            // Allow inner page render to generate its own title
            subrenderContext.renderState().remove(RenderResult.RENDER_STATE_KEYS.TITLE.name());
            RenderResult res = renderContext.renderer().renderWithInfo(page.source(), subrenderContext);
            Map<String, Object> renderState = new HashMap<>(res.renderState());
            renderState.putAll(page.flags().toMap());
            return new RenderOutputImpl(res.renderedText(), renderState);
        }

        @Override
        public RenderOutput getCachedRender(String pageDescriptor) {
            long start = System.currentTimeMillis();
            PageData page = pageService.getPageData(renderContext.host(), pageDescriptor, renderContext.user());
            long fetchedPageData = System.currentTimeMillis();
            if (!page.flags().exists() || !page.flags().userCanRead()) {
                return new RenderOutputImpl("", new HashMap<String, Object>(page.flags().toMap()));
            }
            PageCache pageCache = pageService.getCachedPage(renderContext.host(), pageDescriptor);
            long fetchedCache = System.currentTimeMillis();
            if (pageCache != null) { // In this case, ignore useCache flag
                Map<String, Object> renderState = new HashMap<>(page.flags().toMap());
                renderState.put(RenderResult.RENDER_STATE_KEYS.TITLE.name(), page.title());
                String rendered = postRender(pageCache.renderedCache, renderContext);
                long end = System.currentTimeMillis();
                logger.info("getCachedRender(" + pageDescriptor + ") total= " + (end - start) + " fetchPageData= " + (fetchedPageData - start) + " fetchCache= " + (fetchedCache - fetchedPageData) + " else=" + (end - fetchedCache));
                return new RenderOutputImpl(rendered, renderState);
            }
            return doRender(page, pageDescriptor);
        }

        @Override
        public Map<String, RenderOutput> getCachedRenders(List<String> pageDescriptors) {
            long start = System.currentTimeMillis();
            Map<PageDescriptor, PageData> pages = pageService.getPageData(renderContext.host(), pageDescriptors, renderContext.user());
            long gotPageData = System.currentTimeMillis();
            List<PageCache> pageCaches = pageService.getCachedPages(renderContext.host(), pageDescriptors);
            long gotCacheData = System.currentTimeMillis();
            Map<String, PageData> pageMap = pages.entrySet().stream().collect(Collectors.toMap(pd -> pd.getKey().toString(), Map.Entry::getValue));
            Map<String, PageCache> pageCacheMap = pageCaches.stream().collect(Collectors.toMap(pc -> new PageDescriptor(pc.namespace, pc.pageName).toString(), pc -> pc));
            Map<String, RenderOutput> outputMap = new HashMap<>();
            AtomicLong totalRenderTime = new AtomicLong(0);
            AtomicLong numCachedPages = new AtomicLong(0);
            AtomicLong numRenderedPages = new AtomicLong(0);
            pageDescriptors.forEach(pd -> {
                        PageData page = pageMap.get(pd);
                        if (page == null) {
                            outputMap.put(pd, new RenderOutputImpl("",  new HashMap<>()));
                            return;
                        }
                        if (!page.flags().exists() || !page.flags().userCanRead()) {
                            outputMap.put(pd, new RenderOutputImpl("", new HashMap<String, Object>(page.flags().toMap())));
                            return;
                        }
                        PageCache pageCache = pageCacheMap.get(pd);
                        if (pageCache != null) { // In this case, ignore useCache flag
                            numCachedPages.addAndGet(1);
                            Map<String, Object> renderState = new HashMap<>(page.flags().toMap());
                            renderState.put(RenderResult.RENDER_STATE_KEYS.TITLE.name(), page.title());
                            String rendered = postRender(pageCache.renderedCache, renderContext);
                            outputMap.put(pd, new RenderOutputImpl(rendered, renderState));
                            return;
                        }
                        long renderStart = System.currentTimeMillis();
                        outputMap.put(pd, doRender(page, pd));
                        long renderEnd = System.currentTimeMillis();
                        numRenderedPages.addAndGet(1);
                        totalRenderTime.addAndGet(renderEnd - renderStart);
                    }
            );
            logger.info("getCachedRenders. {} pages. getPageData={}ms. getCachedPages={}ms. {} cachedPages. {} renderedPages. renderTime={}",
              pageDescriptors.size(), gotPageData - start, gotCacheData-gotPageData, numCachedPages.get(), numRenderedPages.get(), totalRenderTime.get());
            return outputMap;
        }

        @Override
        public List<String> getPagesByNSAndTag(String ns, String tag) {
            return pageService.searchPages(renderContext.host(), renderContext.user(), Map.of("tag", tag, "ns", ns)).
                    get("tag").
                    stream().map(SearchResult::getDescriptor).toList();
        }

        @Override
        public List<String> getAllPages() {
            return pageService.getAllPagesFlat(renderContext.host(), renderContext.user());
        }

        @Override
        public boolean isReadable(String pageDescriptor) {
            return pageService.isReadable(renderContext.host(), pageDescriptor, renderContext.user());
        }

        @Override
        public List<String> getLinksOnPage(String page) {
            List<String> links = linkService.getLinksOnPage(renderContext.site(), page);
            Map<String,LinkOverride> overrides = linkOverrideService.getOverrides(renderContext.host(), page).
                    stream().collect(Collectors.toMap(link -> link.getTarget(), Function.identity()));
            List<String> realLinks = new ArrayList<>();
            for (String link : links ) {
                if (overrides.containsKey(link)) {
                    realLinks.add(overrides.get(link).getNewTarget());
                }
                else {
                    realLinks.add(link);
                }
            }
            return realLinks;
        }

        @Override
        public RenderOutput renderMarkup(String markup) {
            RenderContext subrenderContext = new RenderContext(renderContext.host(), renderContext.site(), renderContext.page(),
                    renderContext.user(), renderContext.renderer(), new HashMap<>());
            subrenderContext.renderState().putAll(renderContext.renderState());
            // Allow inner page render to generate its own title
            subrenderContext.renderState().remove(RenderResult.RENDER_STATE_KEYS.TITLE.name());
            RenderResult res = renderContext.renderer().renderWithInfo(markup, subrenderContext);
            return new RenderOutputImpl(res.renderedText(), res.renderState());
        }

        @Override
        public void setPageDontCache() {
            renderContext.renderState().put(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), true);
        }

        @Override
        public boolean isPlaintextRender() {
            return Boolean.TRUE.equals(renderContext.renderState().get("plainText"));
        }

        @Override
        public void addLinks(Collection<String> newLinks) {
            if (newLinks == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            Collection<String> existingLinks = (Collection<String>) renderContext.renderState().computeIfAbsent(LINKS.name(), (k) -> new HashSet<>());
            existingLinks.addAll(newLinks);
        }
    }

    public static class RenderOutputImpl extends Macro.MacroContext.RenderOutput {
        String html;
        Map<String, Object> state;

        RenderOutputImpl(String html, Map<String, Object> state) {
            this.html = html;
            this.state = state;
        }

        @Override
        public String getHtml() {
            return html;
        }

        @Override
        public Map<String, Object> getState() {
            return state;
        }

    }
}
