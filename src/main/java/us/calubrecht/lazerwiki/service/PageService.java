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
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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

    public PageData getPageData(String host, String sPageDescriptor, String userName) {
        String site = siteService.getSiteForHostname(host);
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        boolean canWrite = namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName);
        boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
        if (!canRead) {
            return new PageData("You are not permissioned to read this page", "", true, false, false);
        }
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        if (p == null ) {
            return new PageData("This page doesn't exist", "", false, canRead, canWrite);
        }
        String source = p.getText();
        return new PageData(null, source, true, canRead, canWrite);
    }

    public PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    @Transactional
    public void savePage(String host, String sPageDescriptor, String text, String userName) throws PageWriteException{
        String site = siteService.getSiteForHostname(host);
        // get Existing
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        if (!namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName)) {
            throw new PageWriteException("You don't have permission to write this page.");
        }
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
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
        newP.setId(id);
        newP.setRevision(revision);
        newP.setValidts(PageRepository.MAX_DATE);
        newP.setModifiedBy(userName);
        pageRepository.save(newP);
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
}