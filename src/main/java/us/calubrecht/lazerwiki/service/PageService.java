package us.calubrecht.lazerwiki.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
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
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        if (p == null ) {
            return new PageData("This page doesn't exist", "", false);
        }
        String source = p.getText();
        return new PageData(null, source, true);
    }

    public PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    @Transactional
    public void savePage(String host, String sPageDescriptor, String text, String user) throws PageWriteException{
        if (user.equals("Joe")) {
            throw new PageWriteException("You don't have permission to write this page.");
        }
        String site = siteService.getSiteForHostname(host);
        // get Existing
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
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
        newP.setModifiedBy(user);
        pageRepository.save(newP);
    }

    protected long getNewId() {

      return idRepository.getNewId();
    }

    List<String> getNamespaces(String rootNS, List<PageDesc> pages) {
        return pages.stream().map(p -> p.getNamespace()).distinct().flatMap(ns -> {
            List<String> parts = List.of(ns.split(":"));
            List<String> namespaces = new ArrayList<>();
            if (parts.size() == 0) {
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
    PageNode getPageNode(String rootNS, List<PageDesc> pages) {
        List<String> namespaces = getNamespaces(rootNS, pages);
        List<PageNode> nodes = new ArrayList();
        namespaces.forEach(ns ->
                nodes.add(getPageNode(ns, pages)));
        nodes.addAll(
                pages.stream().filter(p -> p.getNamespace().equals(rootNS)).
                        sorted(Comparator.comparing(p -> p.getPagename().toLowerCase())).
                        map(page -> new PageNode.TerminalNode(page)).
                        toList());
        PageNode node = new PageNode(rootNS);
        node.setChildren(nodes);
        return node;

    }

    public PageNode getAllPages(String host) {
        String site = siteService.getSiteForHostname(host);
        List<PageDesc> pages = pageRepository.getAllValid(site);
        List<String> ns = getNamespaces("", pages);
        return getPageNode("", pages);
    }
}