package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.RenderResult;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.util.List;

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
          Could make these renderable templates;
         */
        if (!d.exists()) {
            return d;
        }
        if (!d.userCanRead()) {
            return d;
        }
        sw.split();
        long queryMillis = sw.getSplitTime();
        try {
            PageData pd = new PageData(renderer.renderToString(d.source(), host, site, userName), d.source(), d.tags(), d.exists(), d.userCanRead(), d.userCanWrite());
            sw.stop();
            long totalMillis = sw.getTime();
            logger.info("Render " + sPageDescriptor + " took (" + totalMillis + "," + queryMillis + "," + (totalMillis-queryMillis) + ")ms (Total,Query,Render)");
            return pd;
        }
        catch (Exception e) {
            sw.stop();
            long totalMillis = sw.getTime();
            logger.error("Render failed! host= " + host + " sPageDescriptor= " + sPageDescriptor + " user=" + userName + ".", e);
            String sanitizedSource =  StringEscapeUtils.escapeHtml4(d.source()).replaceAll("&quot;", "\"");

            return new PageData("<h1>Error</h1>\n<div>There was an error rendering this page! Please contact an admin, or correct the markup</div>\n<code>%s</code>".formatted(sanitizedSource),
                    d.source(), d.tags(), true, true, true);

        }

    }

    public void savePage(String host, String sPageDescriptor,String text, List<String> tags, String userName) throws PageWriteException {
        String site = siteService.getSiteForHostname(host);
        RenderResult res = renderer.renderWithInfo(text, host, site, userName);
        pageService.savePage(host, sPageDescriptor, text, tags, res.getTitle(), userName);
    }
}
