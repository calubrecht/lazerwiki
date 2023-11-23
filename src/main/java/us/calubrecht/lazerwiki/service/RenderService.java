package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.PageData;

@Service
public class RenderService {
    Logger logger = LogManager.getLogger(getClass());

    @Autowired
    IMarkupRenderer renderer;

    @Autowired
    PageService pageService;

    public PageData getRenderedPage(String host, String sPageDescriptor, String userName) {
        StopWatch sw = StopWatch.createStarted();
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
            PageData pd = new PageData(renderer.render(d.source()), d.source(), d.exists(), d.userCanRead(), d.userCanWrite());
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
                    d.source(), true, true, true);

        }

    }
}
