package us.calubrecht.lazerwiki.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageData;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.IdRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(rollbackFor = PageWriteException.class )
public class PageService {

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
        pageRepository.save(newP);
    }

    protected long getNewId() {

      return idRepository.getNewId();
    }
}