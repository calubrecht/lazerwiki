package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.PageData;

@Service
public class RenderService {

    @Autowired
    IMarkupRenderer renderer;

    @Autowired
    PageService pageService;

    public PageData getRenderedPage(String host, String sPageDescriptor, String userName) {
        PageData d = pageService.getPageData(host, sPageDescriptor, userName);
        if (!d.exists()) {
            return d;
        }
        return new PageData(renderer.render(d.source()), d.source(), d.exists());
    }
}
