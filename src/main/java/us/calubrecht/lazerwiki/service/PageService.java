package us.calubrecht.lazerwiki.service;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.springframework.data.domain.Limit;
import org.springframework.data.util.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.*;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.service.exception.PageReadException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;
import us.calubrecht.lazerwiki.util.DbSupport;

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
        List<String> visibleBacklinks = namespaceService.
          filterReadablePageDescriptors(backlinks.stream().map(bl -> PageDescriptor.fromFullName(bl)).toList(), site, userName).stream().
                map(PageDescriptor::toString).toList();

        if (p == null ) {
            // Add support for namespace level templates. Need templating language for pageName/namespace/splitPageName
            return new PageData("This page doesn't exist", getTemplate(site, pageDescriptor),  getTitle(host, sPageDescriptor), Collections.emptyList(), visibleBacklinks, new PageFlags(false, false, true, canWrite, false));
        }
        if (p.isDeleted()) {
            return new PageData("This page doesn't exist", getTemplate(site, pageDescriptor),  getTitle(host, sPageDescriptor), Collections.emptyList(), visibleBacklinks, new PageFlags(false, true, true, canWrite, false));
        }
        String source = p.getText();
        return new PageData(null, source, getTitle(pageDescriptor, p),  p.getTags().stream().map(PageTag::getTag).toList(), visibleBacklinks, new PageFlags(true, false, true, canWrite, canDelete), p.getId(), p.getRevision());
    }

    @Transactional
    /**
     * Bulk page get, does not retireve backlinks or tags
     */
    public Map<PageDescriptor, PageData> getPageData(String host, List<String> pageDescriptors, String userName) {
        String site = siteService.getSiteForHostname(host);
        List<String> keys = pageDescriptors.stream().map( desc ->
        {
            PageDescriptor pageDescriptor = decodeDescriptor(desc);
            return pageDescriptor.namespace() + ":" + pageDescriptor.pageName();
        }).toList();
        List<PageText> pageTexts = pageRepository.getAllBySiteAndNamespaceAndPagename(site, keys);
        return pageTexts.stream().collect(Collectors.toMap(
                pageText -> new PageDescriptor(pageText.getNamespace(), pageText.getPagename()),
                pageText -> {
            PageDescriptor pageDescriptor = new PageDescriptor(pageText.getNamespace(), pageText.getPagename());
            String sPageDescriptor= pageDescriptor.renderedName();
            boolean canWrite = namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName);
            boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
            boolean canDelete = namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName) && !pageDescriptor.isHome();
            if (!canRead) {
                return new PageData("You are not permissioned to read this page", "", sPageDescriptor,  Collections.emptyList(),Collections.emptyList(), PageData.EMPTY_FLAGS);
            }

            String title = pageText.getTitle() != null ? pageText.getTitle() : pageText.getPagename();
            return new PageData(null, pageText.getText(), title, Collections.emptyList(), Collections.emptyList(), new PageFlags(true, false, true, canWrite, canDelete));
        }));
    }

    @Transactional
    public PageData getHistoricalPageData(String host, String sPageDescriptor, long revision, String userName) {
        logger.info("fetch page: host=" + host + " sPageDescriptor=" + sPageDescriptor + " revision= " + revision + " userName=" + userName);
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
        if (!canRead) {
            return new PageData("You are not permissioned to read this page", "", getTitle(host, sPageDescriptor),  Collections.emptyList(),Collections.emptyList(), PageData.EMPTY_FLAGS);
        }
        Page p = pageRepository.findBySiteAndNamespaceAndPagenameAndRevision(site, pageDescriptor.namespace(), pageDescriptor.pageName(), revision);

        if (p == null ) {
            // Add support for namespace level templates. Need templating language for pageName/namespace/splitPageName
            return new PageData("This page doesn't exist", getTemplate(site, pageDescriptor),  getTitle(host, sPageDescriptor), Collections.emptyList(), null, PageData.EMPTY_FLAGS);
        }
        if (p.isDeleted()) {
            return new PageData("This page doesn't exist", getTemplate(site, pageDescriptor),  getTitle(host, sPageDescriptor), Collections.emptyList(), null, new PageFlags(false, true, true, false, false));
        }
        String source = p.getText();
        return new PageData(null, source, getTitle(pageDescriptor, p),  p.getTags().stream().map(PageTag::getTag).toList(), null, new PageFlags(true, false, true, false, false));
    }


    public PageCache getCachedPage(String host, String sPageDescriptor) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        PageCache.PageCacheKey key = new PageCache.PageCacheKey(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        return pageCacheRepository.findById(key).orElse(null);
    }

    List<PageCache> getCachedPages(String host, List<String> pageDescriptors) {
        String site = siteService.getSiteForHostname(host);
        List<PageCache.PageCacheKey> keys = pageDescriptors.stream().map( desc ->
        {
            PageDescriptor pageDescriptor = decodeDescriptor(desc);
            return new PageCache.PageCacheKey(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        }).toList();
        return DbSupport.toList(pageCacheRepository.findAllById(keys));
    }

    @Transactional
    public void saveCache(String host, String sPageDescriptor, String source, RenderResult rendered) {
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
        newCache.source = adjustSource(source, rendered);
        pageCacheRepository.save(newCache);
    }

    public String adjustSource(String source, RenderResult rendered) {
        return doAdjustSource(source, rendered);
    }

    public static String doAdjustSource(String source, RenderResult rendered) {
        if (rendered.renderState().containsKey("overrideStats")) {
            List<LinkOverrideInstance> overrides = new ArrayList<>((List<LinkOverrideInstance>)rendered.renderState().get("overrideStats"));
            Collections.reverse(overrides);
            StringBuilder sb = new StringBuilder(source);
            overrides.forEach(over -> {
                sb.replace(over.start(), over.stop(), over.override());
            });
            return sb.toString();
        }
        return source;
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

    public RecentChangesResponse recentChanges(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        List<String> namespaces = namespaceService.getReadableNamespaces(site, userName);
        List<PageDesc> pages = pageRepository.findAllBySiteAndNamespaceInOrderByModifiedDesc(Limit.of(10), site, namespaces);
        return new RecentChangesResponse(pages.stream().map(RecentChangesResponse::recFor).toList(), null, null);
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
            String searchTerm = prepareSearchTerm(searchTerms.get("text"));
            String searchLower = searchTerms.get("text").toLowerCase();
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

    String prepareSearchTerm(String terms) {
        return Stream.of(terms.split(" ")).map(term -> !term.endsWith("*") ? term + "*" : term).collect(Collectors.joining(" "));
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

    public List<PageDesc> getPageHistory(String host, String sPageDescriptor, String userName) throws PageReadException {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
        if (!canRead) {
            throw new PageReadException("You are not permissioned to read this namespace");
        }
        return pageRepository.findAllBySiteAndNamespaceAndPagenameOrderByRevision(site, pageDescriptor.namespace(), pageDescriptor.pageName());
    }

    public List<Pair<Integer, String>>  getPageDiff(String host, String sPageDescriptor, Long rev1, Long rev2, String userName) throws PageReadException {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
        if (!canRead) {
            throw new PageReadException("You are not permissioned to read this namespace");
        }
        Page latest = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        Long id = latest.getId();
        PageKey key1 = new PageKey(id, rev1);
        PageKey key2 = new PageKey(id, rev2);
        Page p1 = pageRepository.findById(key1).orElse(null);
        Page p2 = pageRepository.findById(key2).orElse(null);
        if (p1 == null ) {
            throw new PageReadException("Cannot read " + sPageDescriptor + " rev " + rev1);
        }
        if (p2 == null ) {
            throw new PageReadException("Cannot read " + sPageDescriptor + " rev " + rev2);
        }
        return generateDiffs(p1.getText(), p2.getText());
    }

    public List<Pair<Integer, String>> generateDiffs(String text1, String text2) {
        List<String> v1 = List.of(text1.split("\n"));
        List<String> v2 = List.of(text2.split("\n"));
        DiffRowGenerator generator = DiffRowGenerator.create()
                .showInlineDiffs(true)
                .inlineDiffByWord(true)
                .mergeOriginalRevised(true)
                .build();
        List<DiffRow> rows = generator.generateDiffRows(v1, v2);
        List<Pair<Integer, String>> result = new ArrayList<>();
        int rowNum = 1;
        for (DiffRow row : rows) {
            Integer lineNum = row.getTag() ==DiffRow.Tag.INSERT ? -1 : rowNum++;
            result.add(Pair.of(lineNum, row.getOldLine()));
        }
        return result;
    }
}