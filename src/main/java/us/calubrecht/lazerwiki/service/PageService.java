package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.IdRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.repository.TagRepository;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = PageWriteException.class )
public class PageService {
    Logger logger = LogManager.getLogger(getClass());

    @Autowired
    PageRepository pageRepository;

    @Autowired
    IdRepository idRepository;

    @Autowired
    SiteService siteService;

    @Autowired
    EntityManagerProxy em;

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    LinkService linkService;

    @Autowired
    TagRepository tagRepository;

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

    @Transactional
    public PageData getPageData(String host, String sPageDescriptor, String userName) {
        logger.info("fetch page: host=" + host + " sPageDescriptor=" + sPageDescriptor + " userName=" + userName);
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        boolean canWrite = namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName);
        boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
        boolean canDelete = namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName) && !pageDescriptor.isHome();
        if (!canRead) {
            return new PageData("You are not permissioned to read this page", "",   Collections.emptyList(),Collections.emptyList(), PageData.EMPTY_FLAGS);
        }
        Page p = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        List<String> backlinks = linkService.getBacklinks(site, sPageDescriptor);
        if (p == null ) {
            // Add support for namespace level templates. Need templating language for pageName/namespace/splitPageName
            return new PageData("This page doesn't exist", "======" + pageDescriptor.renderedName() + "======",   Collections.emptyList(), backlinks, new PageFlags(false, false, true, canWrite, false));
        }
        if (p.isDeleted()) {
            return new PageData("This page doesn't exist", "======" + pageDescriptor.renderedName() + "======",   Collections.emptyList(), backlinks, new PageFlags(false, true, true, canWrite, false));
        }
        String source = p.getText();
        return new PageData(null, source, p.getTags().stream().map(PageTag::getTag).toList(), backlinks, new PageFlags(true, false, true, canWrite, canDelete));
    }

    public PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    @Transactional
    public void savePage(String host, String sPageDescriptor, String text, Collection<String> tags, Collection<String> links, String title, String userName) throws PageWriteException{
        String site = siteService.getSiteForHostname(host);
        // get Existing
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        if (!namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName)) {
            throw new PageWriteException("You don't have permission to write this page.");
        }
        Page p = pageRepository.getBySiteAndNamespaceAndPagename(site, pageDescriptor.namespace(), pageDescriptor.pageName());
        long id = p == null ? getNewId() : p.getId();
        long revision = p == null ? 1 : p.getRevision() + 1;
        if (p != null ) {
            p.setValidts(LocalDateTime.now());
            pageRepository.save(p);
            em.flush(); // Flush update to DB so we can do insert afterward
        }
        Page newP = new Page();
        newP.setSite(site);
        newP.setNamespace(pageDescriptor.namespace());
        newP.setPagename(pageDescriptor.pageName());
        newP.setText(text);
        newP.setTitle(title);
        newP.setId(id);
        newP.setRevision(revision);
        newP.setValidts(PageRepository.MAX_DATE);
        newP.setModifiedBy(userName);
        newP.setTags(tags.stream().map(s -> new PageTag(newP, s)).toList());
        pageRepository.save(newP);
        linkService.setLinksFromPage(site, pageDescriptor.namespace(), pageDescriptor.pageName(), links);
    }

    protected long getNewId() {

      return idRepository.getNewId();
    }

    List<String> getNamespaces(String rootNS, List<PageDesc> pages) {
        return pages.stream().map(p -> p.getNamespace()).distinct().flatMap(ns -> {
            List<String> parts = List.of(ns.split(":"));
            List<String> namespaces = new ArrayList<>();
            if (parts.size() == 1) {
                return List.of(ns).stream();
            }
            for ( int i = 0 ; i <= parts.size(); i++) {
                String namespace = parts.subList(0, i).stream().collect(Collectors.joining(":"));
                namespaces.add(namespace);
            }
            return namespaces.stream();
        }).distinct().filter(ns -> ns.startsWith(rootNS) && !ns.equals(rootNS)).
                filter(ns -> ns.substring(rootNS.length() + 1).indexOf(":") == -1).
                sorted().toList();
    }
    NsNode getNsNode(String rootNS, List<PageDesc> pages) {
        List<String> namespaces = getNamespaces(rootNS, pages);
        List<NsNode> nodes = new ArrayList();
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
        return pages.stream().map(pd -> pd.getDescriptor()).toList();
    }

    public List<String> getAllTags(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        return tagRepository.getAllActiveTags(site);
    }

    public List<PageDesc> searchPages(String host, String userName, String searchTerm) {
        String site = siteService.getSiteForHostname(host);
        String tagName = searchTerm.split(":")[1];
        return searchPages(host, userName, Map.of("tag", tagName));
    }

    public List<PageDesc> searchPages(String host, String userName, Map<String, String> searchTerms) {
        String site = siteService.getSiteForHostname(host);
        if (searchTerms.containsKey("tag")) {
            String tagName = searchTerms.get("tag");
            List<PageDesc> tagPages = namespaceService.
                    filterReadablePages(pageRepository.getByTagname(site, tagName), site, userName).stream().
                    sorted(Comparator.comparing(p -> p.getNamespace() + ":" + p.getPagename())).collect(Collectors.toList());
            if (!searchTerms.getOrDefault("ns", "*").equals("*")) {
                Pattern nsPattern = Pattern.compile(searchTerms.get("ns").replaceAll("\\*", ".*"));
                return tagPages.stream().filter(pd -> nsPattern.matcher(pd.getNamespace()).matches()).toList();
            }
            return tagPages;
        }
        return Collections.emptyList();
    }

    @Transactional
    public void deletePage(String host, String sPageDescriptor, String userName) throws PageWriteException {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        if (!namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName) || sPageDescriptor.equals("")) {
            throw new PageWriteException("You don't have permission to delete this page.");
        }

        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        if (p == null) {
            return;
        }
        long revision =  p.getRevision() + 1;
        p.setValidts(LocalDateTime.now());
        pageRepository.save(p);
        em.flush(); // Flush update to DB so we can do insert afterward
        Page newP = new Page();
        newP.setSite(site);
        newP.setNamespace(pageDescriptor.namespace());
        newP.setPagename(pageDescriptor.pageName());
        newP.setText("");
        newP.setTitle("");
        newP.setId(p.getId());
        newP.setRevision(revision);
        newP.setValidts(PageRepository.MAX_DATE);
        newP.setModifiedBy(userName);
        newP.setTags(Collections.emptyList());
        newP.setDeleted(true);
        pageRepository.save(newP);
        linkService.deleteLinks(site, sPageDescriptor);
    }
}