package us.calubrecht.lazerwiki.service;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.responses.PageListResponse;

import java.net.URL;
import java.util.List;

@Service
public class ExportService {

    @Autowired
    PageService pageService;

    @Autowired
    MediaService mediaService;

    public void createExportBundle(String hostName, String user) {
        PageListResponse pageList = pageService.getAllPages(hostName, user);
        WebSitemapGenerator wsg = new WebSitemapGenerator(url);
        for (String ns : pageList.pages.keySet().stream().sorted().toList()) {
            List<PageDesc> pages = pageList.pages.get(ns);
            for (PageDesc page : pages) {
                String descriptor = page.getDescriptor();
                String path = descriptor.isEmpty() ? "" : "/page/" + descriptor;
                WebSitemapUrl.Options options = new WebSitemapUrl.Options(sUrl + path);
                if (page.getModified() != null) {
                    options.lastMod(page.getModified().toString());
                }
                WebSitemapUrl sitemapUrl = new WebSitemapUrl(options);
                wsg.addUrl(sitemapUrl);
            }
        }
    }

    record PageMetaData(String namespace, String name, List<String> tags, String markupFormat, List<String> authors) {
        //Metadata (Page name, Namespace, Tags, Markup Format, anything else?)
    }
}
