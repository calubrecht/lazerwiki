package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.util.*;

@Service
public class RenderService {
    Logger logger = LogManager.getLogger(getClass());

    @Autowired
    IMarkupRenderer renderer;

    @Autowired
    PageService pageService;

    @Autowired
    SiteService siteService;

    public PageData getRenderedPage(String host, String sPageDescriptor, String userName) {
        StopWatch sw = StopWatch.createStarted();
        String site = siteService.getSiteForHostname(host);
        PageData d = pageService.getPageData(host, sPageDescriptor, userName);
        /*
          XXX:Could make these renderable templates;
         */
        if (!d.flags().exists()) {
            return d;
        }
        if (!d.flags().userCanRead()) {
            return d;
        }
        sw.split();
        long queryMillis = sw.getSplitTime();
        PageCache cachedPage = pageService.getCachedPage(host, sPageDescriptor);
        if (cachedPage != null && cachedPage.useCache) {
            PageData pd = new PageData(cachedPage.renderedCache, d.source(), d.tags(), d.backlinks(), d.flags());
            sw.stop();
            long totalMillis = sw.getTime();
            logger.info("Render " + sPageDescriptor + " took (" + totalMillis + "," + queryMillis + "," + (totalMillis-queryMillis) + ")ms (Total,Query,QueryCache)");
            return pd;
        }
        try {
            RenderResult rendered = renderer.renderWithInfo(d.source(), host, site, userName);
            PageData pd = new PageData(rendered.renderedText(), d.source(), d.tags(), d.backlinks(), d.flags());
            sw.stop();
            long totalMillis = sw.getTime();
            pageService.saveCache(host, sPageDescriptor, rendered);
            logger.info("Render " + sPageDescriptor + " took (" + totalMillis + "," + queryMillis + "," + (totalMillis-queryMillis) + ")ms (Total,Query,Render)");
            return pd;
        }
        catch (Exception e) {
            logger.error("Render failed! host= " + host + " sPageDescriptor= " + sPageDescriptor + " user=" + userName + ".", e);
            String sanitizedSource =  StringEscapeUtils.escapeHtml4(d.source()).replaceAll("&quot;", "\"");

            return new PageData("<h1>Error</h1>\n<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n<code>%s</code>".formatted(sanitizedSource),
                    d.source(), d.tags(), d.backlinks(),d.flags());

        }

    }

    public void savePage(String host, String sPageDescriptor,String text, List<String> tags, String userName) throws PageWriteException {
        String site = siteService.getSiteForHostname(host);
        RenderResult res = renderer.renderWithInfo(text, host, site, userName);
        Collection<String> links = (Collection<String>)res.renderState().getOrDefault(RenderResult.RENDER_STATE_KEYS.LINKS.name(), Collections.emptySet());
        pageService.savePage(host, sPageDescriptor, text, tags, links, res.getTitle(), userName);
        pageService.saveCache(host, sPageDescriptor, res);
    }

    public PageData previewPage(String host, String sPageDescriptor, String text, String userName) {
        StopWatch sw = StopWatch.createStarted();
        String site = siteService.getSiteForHostname(host);
        try {
            PageData pd = new PageData(renderer.renderToString(text, host, site, userName), text, null, null, null);
            sw.stop();
            long totalMillis = sw.getTime();
            logger.info("Render preview for " + sPageDescriptor + " took " + totalMillis + "ms");
            return pd;
        }
        catch (Exception e) {
            sw.stop();
            long totalMillis = sw.getTime();
            logger.error("Render preview failed! host= " + host + " sPageDescriptor= " + sPageDescriptor + " user=" + userName + ".", e);
            String sanitizedSource =  StringEscapeUtils.escapeHtml4(text).replaceAll("&quot;", "\"");

            return new PageData("<h1>Error</h1>\n<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n<code>%s</code>".formatted(sanitizedSource),
                    text, null, null, null);

        }

    }
}
