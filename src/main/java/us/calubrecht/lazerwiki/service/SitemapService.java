package us.calubrecht.lazerwiki.service;

import com.redfin.sitemapgenerator.WebSitemapUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.responses.PageListResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

@Service
public class SitemapService {

    @Autowired
    PageService pageService;

    @Autowired
    MediaService mediaService;

    public String getSitemap(URL url) throws MalformedURLException, ParseException {
        String sUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();
        PageListResponse pageList = pageService.getAllPages(url.getHost(), null);
        WebSitemapGenerator wsg = new WebSitemapGenerator(url);
        for (String ns : pageList.pages().keySet().stream().sorted().toList()) {
           List<PageDesc> pages = pageList.pages().get(ns);
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
        MediaListResponse mediaList = mediaService.getAllFiles(url.getHost(), null);
        for (String ns : mediaList.media().keySet().stream().sorted().toList()) {
            List<MediaRecord> mediaItems = mediaList.media().get(ns);
            for (MediaRecord media : mediaItems) {
                String descriptor = ns.isEmpty() ? media.getFileName() : ns + ":" + media.getFileName();
                WebSitemapUrl.Options options = new WebSitemapUrl.Options(sUrl + "/_media/" + descriptor);
                if (media.getModified() != null) {
                    options.lastMod(media.getModified().toString());
                }
                WebSitemapUrl sitemapUrl = new WebSitemapUrl(options);
                wsg.addUrl(sitemapUrl);
            }
        }
        List<String> maps = wsg.writeAsStrings();
        return maps.get(0);
    }
}
