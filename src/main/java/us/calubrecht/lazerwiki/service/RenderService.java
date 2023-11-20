package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.time.StopWatch;
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
        if (!d.exists()) {
            return d;
        }
        sw.split();
        long queryMillis = sw.getSplitTime();
        PageData pd = new PageData(renderer.render(d.source()), d.source(), d.exists());
        sw.stop();
        long totalMillis = sw.getTime();
        logger.info("Render " + sPageDescriptor + " took (" + totalMillis + "," + queryMillis + "," + (totalMillis-queryMillis) + ")ms (Total,Query,Render)");
        return pd;
    }
}
