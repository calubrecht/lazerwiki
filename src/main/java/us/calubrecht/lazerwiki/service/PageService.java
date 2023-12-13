package us.calubrecht.lazerwiki.service;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.responses.SearchResult;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = PageWriteException.class )
public class PageService {
    final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    PageRepository pageRepository;


    @Autowired
    SiteService siteService;

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    LinkService linkService;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PageCacheRepository pageCacheRepository;

    public boolean exists(String host, String pageName) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(pageName);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        return p != null;
    }

    public String getTitle(String host, String pageName) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(pageName);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        return p == null ? pageDescriptor.renderedName() : (p.getTitle() == null ? pageDescriptor.renderedName() : p.getTitle());
    }

    public static String getTitle(PageDescriptor pd, Page p) {
        return p == null ? pd.renderedName() : (p.getTitle() == null ? pd.renderedName() : p.getTitle());
    }

    @Transactional
    public PageData getPageData(String host, String sPageDescriptor, String userName) {
        logger.info("fetch page: host=" + host + " sPageDescriptor=" + sPageDescriptor + " userName=" + userName);
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        boolean canWrite = namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName);
        boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
        boolean canDelete = namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName) && !pageDescriptor.isHome();
        if (!canRead) {
            return new PageData("You are not permissioned to read this page", "", getTitle(host, sPageDescriptor),  Collections.emptyList(),Collections.emptyList(), PageData.EMPTY_FLAGS);
        }
        Page p = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        List<String> backlinks = linkService.getBacklinks(site, sPageDescriptor);

        if (p == null ) {
            // Add support for namespace level templates. Need templating language for pageName/namespace/splitPageName
            return new PageData("This page doesn't exist", getTemplate(site, pageDescriptor),  getTitle(host, sPageDescriptor), Collections.emptyList(), backlinks, new PageFlags(false, false, true, canWrite, false));
        }
        if (p.isDeleted()) {
            return new PageData("This page doesn't exist", getTemplate(site, pageDescriptor),  getTitle(host, sPageDescriptor), Collections.emptyList(), backlinks, new PageFlags(false, true, true, canWrite, false));
        }
        String source = p.getText();
        return new PageData(null, source, getTitle(pageDescriptor, p),  p.getTags().stream().map(PageTag::getTag).toList(), backlinks, new PageFlags(true, false, true, canWrite, canDelete));
    }

    public PageCache getCachedPage(String host, String sPageDescriptor) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        PageCache.PageCacheKey key = new PageCache.PageCacheKey(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        return pageCacheRepository.findById(key).orElse(null);
    }

    @Transactional
    public void saveCache(String host, String sPageDescriptor, RenderResult rendered) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        Page p = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        PageCache newCache = new PageCache();
        newCache.site = site;
        newCache.namespace = p.getNamespace();
        newCache.pageName = p.getPagename();
        newCache.renderedCache = rendered.renderedText();
        newCache.plaintextCache = rendered.plainText();
        newCache.useCache = !(Boolean)rendered.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.DONT_CACHE.name(), Boolean.FALSE);
        pageCacheRepository.save(newCache);
    }

    public static PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    List<String> getNamespaces(String rootNS, List<PageDesc> pages) {
        return pages.stream().map(PageDesc::getNamespace).distinct().flatMap(ns -> {
            List<String> parts = List.of(ns.split(":"));
            List<String> namespaces = new ArrayList<>();
            if (parts.size() == 1) {
                return Stream.of(ns);
            }
            for ( int i = 0 ; i <= parts.size(); i++) {
                String namespace = String.join(":", parts.subList(0, i));
                namespaces.add(namespace);
            }
            return namespaces.stream();
        }).distinct().filter(ns -> ns.startsWith(rootNS) && !ns.equals(rootNS)).
                filter(ns -> !ns.substring(rootNS.length() + 1).contains(":")).
                sorted().toList();
    }
    NsNode getNsNode(String rootNS, List<PageDesc> pages) {
        List<String> namespaces = getNamespaces(rootNS, pages);
        List<NsNode> nodes = new ArrayList<>();
        namespaces.forEach(ns ->
                nodes.add(getNsNode(ns, pages)));
        NsNode node = new NsNode(rootNS, false);
        node.setChildren(nodes);
        return node;

    }

    public PageListResponse getAllPages(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        List<PageDesc> pages = namespaceService.filterReadablePages(pageRepository.getAllValid(site), site, userName);
        NsNode node = getNsNode("", pages);
        return new PageListResponse(pages.stream().sorted(Comparator.comparing(p -> p.getPagename().toLowerCase())).collect(Collectors.groupingBy(PageDesc::getNamespace)), node);
    }

    public List<String> getAllPagesFlat(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        List<PageDesc> pages = namespaceService.filterReadablePages(pageRepository.getAllValid(site), site, userName);
        return pages.stream().map(PageDesc::getDescriptor).toList();
    }

    public List<String> getAllTags(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        return tagRepository.getAllActiveTags(site);
    }

    public Map<String, List<SearchResult>> searchPages(String host, String userName, String searchTerm) {
        String[] searchValues = searchTerm.split(":");
        return searchPages(host, userName, Map.of(searchValues[0], searchValues[1]));
    }

    public Map<String, List<SearchResult>>  searchPages(String host, String userName, Map<String, String> searchTerms) {
        String site = siteService.getSiteForHostname(host);
        if (searchTerms.containsKey("tag")) {
            String tagName = searchTerms.get("tag");
            List<SearchResult> tagPages = namespaceService.
                    filterReadablePages(pageRepository.getByTagname(site, tagName), site, userName).stream().
                    sorted(Comparator.comparing(p -> p.getNamespace() + ":" + p.getPagename())).
                    map(pd -> new SearchResult(pd.getNamespace(), pd.getPagename(), pd.getTitle(), null)).collect(Collectors.toList());
            if (!searchTerms.getOrDefault("ns", "*").equals("*")) {
                Pattern nsPattern = Pattern.compile(searchTerms.get("ns").replaceAll("\\*", ".*"));
                return Map.of("tag", tagPages.stream().filter(pd -> nsPattern.matcher(pd.namespace()).matches()).toList());
            }
            return Map.of("tag", tagPages);
        }
        else if (searchTerms.containsKey("text")) {
            String searchTerm = searchTerms.get("text");
            String searchLower = searchTerm.toLowerCase();
            List<SearchResult> titlePages = namespaceService.
                    filterReadablePages(new ArrayList<PageDesc>(pageCacheRepository.searchByTitle(site, searchTerm)), site, userName).stream().
                    sorted(Comparator.comparing(p -> p.getNamespace() + ":" + p.getPagename())).
                    map(pd -> new SearchResult(pd.getNamespace(), pd.getPagename(), pd.getTitle(), null)).collect(Collectors.toList());
            List<SearchResult> textPages = namespaceService.
                    filterReadablePages(new ArrayList<PageDesc>(pageCacheRepository.searchByText(site, searchTerm)), site, userName).stream().
                    sorted(Comparator.comparing(p -> p.getNamespace() + ":" + p.getPagename())).
                    map(pc -> searchResultFromPlaintext((PageCache)pc, List.of(searchLower.split(" ")))).collect(Collectors.toList());
            if (!searchTerms.getOrDefault("ns", "*").equals("*")) {
                Pattern nsPattern = Pattern.compile(searchTerms.get("ns").replaceAll("\\*", ".*"));
                titlePages = titlePages.stream().filter(pd -> nsPattern.matcher(pd.namespace()).matches()).toList();
                textPages = textPages.stream().filter(pd -> nsPattern.matcher(pd.namespace()).matches()).toList();
            }
            return Map.of("title", titlePages, "text", textPages);
        }
        return Collections.emptyMap();
    }

    SearchResult searchResultFromPlaintext(PageCache pc, List<String> searchTerms) {
        Optional<String> searchLine = Stream.of(pc.plaintextCache.split("\\\n")).
                filter(line -> {
                    // Can do something smarter? make prefer if text is a word of its own?
                    String lowerLine = line.toLowerCase();
                    for (String term: searchTerms) {
                        if (lowerLine.contains(term)) {
                            return true;
                        }
                    }
                    return false;
                }).map(line -> StringEscapeUtils.escapeHtml4(line).replaceAll("&quot;", "\"")).findFirst();
        return new SearchResult(pc.getNamespace(), pc.getPagename(), pc.getTitle(), searchLine.orElse(null));
    }

    String getTemplate(String site, PageDescriptor pageDescriptor) {
        String ns = pageDescriptor.namespace();
        Page template = null;
        while ( ns != null) {
            template = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, ns, "_template", false);
            if (template != null) {
                break;
            }
            ns = namespaceService.parentNamespace(ns);
        }
        if (template == null) {
            return "======" + pageDescriptor.renderedName() + "======";
        }
        Map<String, String> replacements = Map.of("%NAME%", pageDescriptor.renderedName(), "%NAMESPACE%", pageDescriptor.namespace(), "%RAWNAME%", pageDescriptor.pageName());
        String text = template.getText();
        for (Map.Entry<String, String> entry : replacements.entrySet() ) {
           text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }
}