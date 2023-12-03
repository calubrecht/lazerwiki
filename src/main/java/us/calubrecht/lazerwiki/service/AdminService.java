package us.calubrecht.lazerwiki.service;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class AdminService {
    Logger logger = LogManager.getLogger(getClass());

    @Autowired
    LinkService linkService;

    @Autowired
    IMarkupRenderer renderer;

    @Autowired
    PageRepository pageRepository;

    @Transactional
    public void regenLinks(String site) {
        logger.info("Regening link table for " +site);
        List<PageDesc> pages = pageRepository.getAllValid(site);
        pages.forEach(pd -> {
            Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pd.getNamespace(), pd.getPagename(), false);
            RenderResult res = renderer.renderWithInfo(p.getText(), "", site, "");
            Collection<String> links = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.LINKS.name(), Collections.emptySet());
            logger.info("Setting " + links.size() + " links for " + pd);
            linkService.setLinksFromPage(site, pd.getNamespace(), pd.getPagename(), links);
        });
    }
}
