package us.calubrecht.lazerwiki.service;

import static us.calubrecht.lazerwiki.model.RenderStateKeys.LINKS;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import us.calubrecht.lazerwiki.model.RenderStateKeys;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.SearchResult;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;

@Service
public class MacroService {
  final Logger logger = LogManager.getLogger(getClass());
  final Map<String, Macro> macros = new HashMap<>();

  @Autowired MacroCssService macroCssService;

  @Autowired PageService pageService;

  @Autowired PageSearchService pageSearchService;

  @Autowired LinkService linkService;

  @Autowired LinkOverrideService linkOverrideService;

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
    macroClasses.forEach(
        (cl) -> {
          try {
            Macro macro = (Macro) cl.getDeclaredConstructor().newInstance();
            registerMacro(macro);

          } catch (Exception e) {
            logger.error("Failed to instantiate a macro of type {}.", cl, e);
          }
        });
  }

  protected String sanitize(String input) {
    return StringEscapeUtils.escapeHtml4(input).replace("&quot;", "\"");
  }

  @SuppressWarnings("unchecked")
  public String renderMacro(String macroText, String fullText, RenderContext renderContext) {
    String[] parts = macroText.split(":", 2);
    String macroName = parts[0];
    String macroArgs = parts.length > 1 ? parts[1] : "";

    boolean forCache =
        BooleanUtils.isTrue((Boolean) renderContext.renderState().get(RenderStateKeys.FOR_CACHE));

    Macro macro = macros.get(macroName);
    if (macro == null) {
      return "MACRO- Unknown Macro " + sanitize(macroName);
    }
    if (forCache && !macro.allowCache(new MacroContextImpl(renderContext), macroArgs)) {
      return fullText;
    }
    Set<String> runningMacros =
        (Set<String>)
            renderContext
                .renderState()
                .computeIfAbsent(RenderStateKeys.MACRO_GUARD, (k) -> new HashSet<>());
    if (!runningMacros.add(macroName)) {
      // Prevent recursive macro calls
      return "";
    }
    try {
      return macro.render(new MacroContextImpl(renderContext), macroArgs);
    } finally {
      runningMacros.remove(macroName);
    }
  }

  final Pattern macroPattern =
      Pattern.compile("~~MACRO~~(.*?)~~/MACRO~~", Pattern.MULTILINE | Pattern.DOTALL);

  public String postRender(String fullText, RenderContext context) {
    Matcher matcher = macroPattern.matcher(fullText);
    return matcher.replaceAll(
        matched -> {
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
      PageData page =
          pageService.getPageData(renderContext.site(), pageDescriptor, renderContext.user());
      if (!page.flags().exists() || !page.flags().userCanRead()) {
        return new RenderOutputImpl("", new HashMap<>(page.flags().toMap()));
      }
      PageCache pageCache = pageService.getCachedPage(renderContext.site(), pageDescriptor);
      if (pageCache != null && pageCache.useCache) {
        Map<String, Object> renderState = new HashMap<>(page.flags().toMap());
        renderState.put(RenderStateKeys.TITLE.name(), page.title());
        String rendered = postRender(pageCache.renderedCache, renderContext);
        return new RenderOutputImpl(rendered, renderState);
      }

      return doRender(page, pageDescriptor);
    }

    @NotNull
    private RenderOutput doRender(PageData page, String pageDescriptor) {
      RenderContext subrenderContext =
          new RenderContext(
              renderContext.site(),
              pageDescriptor,
              renderContext.user(),
              renderContext.renderer(),
              new HashMap<>());
      subrenderContext.renderState().putAll(renderContext.renderState());
      // Allow inner page render to generate its own title
      subrenderContext.renderState().remove(RenderStateKeys.TITLE);
      RenderResult res = renderContext.renderer().renderWithInfo(page.source(), subrenderContext);
      Map<String, Object> renderState = toPublicState(res.renderState());
      renderState.putAll(page.flags().toMap());
      return new RenderOutputImpl(res.renderedText(), renderState);
    }

    @Override
    public RenderOutput getCachedRender(String pageDescriptor) {
      long start = System.currentTimeMillis();
      PageData page =
          pageService.getPageData(renderContext.site(), pageDescriptor, renderContext.user());
      long fetchedPageData = System.currentTimeMillis();
      if (!page.flags().exists() || !page.flags().userCanRead()) {
        return new RenderOutputImpl("", new HashMap<>(page.flags().toMap()));
      }
      PageCache pageCache = pageService.getCachedPage(renderContext.site(), pageDescriptor);
      long fetchedCache = System.currentTimeMillis();
      if (pageCache != null) { // In this case, ignore useCache flag
        Map<String, Object> renderState = new HashMap<>(page.flags().toMap());
        renderState.put(RenderStateKeys.TITLE.name(), page.title());
        String rendered = postRender(pageCache.renderedCache, renderContext);
        long end = System.currentTimeMillis();
        logger.info(
            "getCachedRender({}) total= {} fetchPageData= {} fetchCache= {} else={}",
            pageDescriptor,
            end - start,
            fetchedPageData - start,
            fetchedCache - fetchedPageData,
            end - fetchedCache);
        return new RenderOutputImpl(rendered, renderState);
      }
      return doRender(page, pageDescriptor);
    }

    @Override
    public Map<String, RenderOutput> getCachedRenders(List<String> pageDescriptors) {
      long start = System.currentTimeMillis();
      Map<PageDescriptor, PageData> pages =
          pageService.getPageData(renderContext.site(), pageDescriptors, renderContext.user());
      long gotPageData = System.currentTimeMillis();
      List<PageCache> pageCaches =
          pageService.getCachedPages(renderContext.site(), pageDescriptors);
      long gotCacheData = System.currentTimeMillis();
      Map<String, PageData> pageMap =
          pages.entrySet().stream()
              .collect(Collectors.toMap(pd -> pd.getKey().toString(), Map.Entry::getValue));
      Map<String, PageCache> pageCacheMap =
          pageCaches.stream()
              .collect(
                  Collectors.toMap(
                      pc -> new PageDescriptor(pc.namespace, pc.pageName).toString(), pc -> pc));
      Map<String, RenderOutput> outputMap = new HashMap<>();
      AtomicLong totalRenderTime = new AtomicLong(0);
      AtomicLong numCachedPages = new AtomicLong(0);
      AtomicLong numRenderedPages = new AtomicLong(0);
      pageDescriptors.forEach(
          pd -> {
            PageData page = pageMap.get(pd);
            if (page == null) {
              outputMap.put(pd, new RenderOutputImpl("", new HashMap<>()));
              return;
            }
            if (!page.flags().exists() || !page.flags().userCanRead()) {
              outputMap.put(pd, new RenderOutputImpl("", new HashMap<>(page.flags().toMap())));
              return;
            }
            PageCache pageCache = pageCacheMap.get(pd);
            if (pageCache != null) { // In this case, ignore useCache flag
              numCachedPages.addAndGet(1);
              Map<String, Object> renderState = new HashMap<>(page.flags().toMap());
              renderState.put(RenderStateKeys.TITLE.name(), page.title());
              String rendered = postRender(pageCache.renderedCache, renderContext);
              outputMap.put(pd, new RenderOutputImpl(rendered, renderState));
              return;
            }
            long renderStart = System.currentTimeMillis();
            outputMap.put(pd, doRender(page, pd));
            long renderEnd = System.currentTimeMillis();
            numRenderedPages.addAndGet(1);
            totalRenderTime.addAndGet(renderEnd - renderStart);
          });
      logger.info(
          "getCachedRenders. {} pages. getPageData={}ms. getCachedPages={}ms. {} cachedPages. {} renderedPages. renderTime={}",
          pageDescriptors.size(),
          gotPageData - start,
          gotCacheData - gotPageData,
          numCachedPages.get(),
          numRenderedPages.get(),
          totalRenderTime.get());
      return outputMap;
    }

    @Override
    public List<String> getPagesByNSAndTag(String ns, String tag) {
      return pageSearchService
          .searchPages(renderContext.site(), renderContext.user(), Map.of("tag", tag, "ns", ns))
          .get("tag")
          .stream()
          .map(SearchResult::getDescriptor)
          .toList();
    }

    @Override
    public List<String> getAllPages() {
      return pageService.getAllPagesFlat(renderContext.site(), renderContext.user());
    }

    @Override
    public boolean isReadable(String pageDescriptor) {
      return pageService.isReadable(renderContext.site(), pageDescriptor, renderContext.user());
    }

    @Override
    public List<String> getLinksOnPage(String page) {
      List<String> links = linkService.getLinksOnPage(renderContext.site(), page);
      Map<String, LinkOverride> overrides =
          linkOverrideService.getOverrides(renderContext.site(), page).stream()
              .collect(Collectors.toMap(LinkOverride::getTarget, Function.identity()));
      List<String> realLinks = new ArrayList<>();
      for (String link : links) {
        if (overrides.containsKey(link)) {
          realLinks.add(overrides.get(link).getNewTarget());
        } else {
          realLinks.add(link);
        }
      }
      return realLinks;
    }

    @Override
    public RenderOutput renderMarkup(String markup) {
      RenderContext subrenderContext =
          new RenderContext(
              renderContext.site(),
              renderContext.page(),
              renderContext.user(),
              renderContext.renderer(),
              new HashMap<>());
      subrenderContext.renderState().putAll(renderContext.renderState());
      // Allow inner page render to generate its own title
      subrenderContext.renderState().remove(RenderStateKeys.TITLE);
      // Suppress TOC on inner page render
      subrenderContext.renderState().put(RenderStateKeys.TOC, false);
      RenderResult res = renderContext.renderer().renderWithInfo(markup, subrenderContext);
      return new RenderOutputImpl(res.renderedText(), toPublicState(res.renderState()));
    }

    @Override
    public void setPageDontCache() {
      renderContext.renderState().put(RenderStateKeys.DONT_CACHE, true);
    }

    @Override
    public boolean isPlaintextRender() {
      return Boolean.TRUE.equals(renderContext.renderState().get(RenderStateKeys.PLAIN_TEXT));
    }

    @Override
    public void addLinks(Collection<String> newLinks) {
      if (newLinks == null) {
        return;
      }
      @SuppressWarnings("unchecked")
      Collection<String> existingLinks =
          (Collection<String>)
              renderContext.renderState().computeIfAbsent(LINKS, (_) -> new HashSet<>());
      existingLinks.addAll(newLinks);
    }
  }

  /**
   * Convert internal enum-keyed render state into the String-keyed map exposed by the public
   * Macro API ({@link Macro.MacroContext.RenderOutput#getState()}).
   */
  private static Map<String, Object> toPublicState(Map<RenderStateKeys, Object> internalState) {
    Map<String, Object> publicState = new HashMap<>();
    internalState.forEach((key, value) -> publicState.put(key.name(), value));
    return publicState;
  }

  public static class RenderOutputImpl extends Macro.MacroContext.RenderOutput {
    final String html;
    final Map<String, Object> state;

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
