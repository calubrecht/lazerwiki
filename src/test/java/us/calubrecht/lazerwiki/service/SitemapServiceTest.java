package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.responses.PageListResponse;

@SpringBootTest(classes = SitemapService.class)
@ActiveProfiles("test")
public class SitemapServiceTest {

  @Autowired SitemapService underTest;

  @MockitoBean PageService pageService;

  @MockitoBean MediaService mediaService;

  @Test
  public void testGetSitemap_pages()
      throws IOException, ParseException, ParserConfigurationException, SAXException {
    Map<String, List<PageDesc>> pageMap = new HashMap<>();
    pageMap.put("", List.of(getDesc("", "", 1010), getDesc("", "page1", 130)));
    pageMap.put("ns1", List.of(getDesc("ns1", "page3", 0), getDesc("ns1", "page4", 130)));
    PageListResponse pageResponse = new PageListResponse(pageMap, null);
    when(pageService.getAllPages(eq("localhost"), isNull())).thenReturn(pageResponse);
    Map<String, List<MediaRecord>> mediaMap = new HashMap<>();
    MediaListResponse mediaResponse = new MediaListResponse(mediaMap, null);
    when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaResponse);
    String res = underTest.getSitemap(new URL("http://localhost:8080/sitemap.xml"));
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(new InputSource(new StringReader(res)));
    doc.getDocumentElement().normalize();

    NodeList urls = doc.getElementsByTagName("url");
    assertEquals(4, urls.getLength());
    assertEquals("loc", urls.item(0).getChildNodes().item(1).getNodeName());
    assertEquals("lastmod", urls.item(0).getChildNodes().item(3).getNodeName());
    assertEquals("http://localhost:8080", getUrl(urls.item(0)));
    assertEquals("2026-10-10", getModified(urls.item(0)));
    assertEquals("http://localhost:8080/page/page1", getUrl(urls.item(1)));
    assertEquals("http://localhost:8080/page/ns1:page3", getUrl(urls.item(2)));
    assertEquals(3, urls.item(2).getChildNodes().getLength()); // no lastmodified
  }

  @Test
  public void testGetSitemap_media()
      throws IOException, ParseException, ParserConfigurationException, SAXException {
    Map<String, List<PageDesc>> pageMap = new HashMap<>();
    PageListResponse pageResponse = new PageListResponse(pageMap, null);
    when(pageService.getAllPages(eq("localhost"), isNull())).thenReturn(pageResponse);
    Map<String, List<MediaRecord>> mediaMap = new HashMap<>();
    mediaMap.put("", List.of(mediaRecord("", "file.jpg", 1010)));
    mediaMap.put("ns1", List.of(mediaRecord("ns1", "file2.jpg", 0)));
    MediaListResponse mediaResponse = new MediaListResponse(mediaMap, null);
    when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaResponse);
    String res = underTest.getSitemap(new URL("http://localhost:8080/sitemap.xml"));
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(new InputSource(new StringReader(res)));
    doc.getDocumentElement().normalize();

    NodeList urls = doc.getElementsByTagName("url");
    assertEquals(2, urls.getLength());
    assertEquals(5, urls.item(0).getChildNodes().getLength());
    assertEquals("loc", urls.item(0).getChildNodes().item(1).getNodeName());
    assertEquals("lastmod", urls.item(0).getChildNodes().item(3).getNodeName());
    assertEquals("http://localhost:8080/_media/file.jpg", getUrl(urls.item(0)));
    assertEquals("2026-10-10", getModified(urls.item(0)));
    assertEquals(3, urls.item(1).getChildNodes().getLength()); // no lastmodified
    assertEquals("http://localhost:8080/_media/ns1:file2.jpg", getUrl(urls.item(1)));
  }

  String getUrl(Node node) {
    return node.getChildNodes().item(1).getTextContent();
  }

  String getModified(Node node) {
    return node.getChildNodes().item(3).getTextContent();
  }

  record SimplePageDesc(String getNamespace, String getPagename, LocalDateTime getModified)
      implements PageDesc {

    @Override
    public Long getRevision() {
      return 0L;
    }

    @Override
    public String getTitle() {
      return "";
    }

    @Override
    public String getModifiedByUserName() {
      return "";
    }

    @Override
    public boolean isDeleted() {
      return false;
    }
  }

  PageDesc getDesc(String namespace, String pagename, int date) {
    LocalDateTime dateTime =
        date == 0 ? null : LocalDateTime.of(2026, date / 100, date % 100, 0, 0);
    return new SimplePageDesc(namespace, pagename, dateTime);
  }

  MediaRecord mediaRecord(String namespace, String filename, int date) {
    LocalDateTime dateTime =
        date == 0 ? null : LocalDateTime.of(2026, date / 100, date % 100, 0, 0);
    MediaRecord mr = new MediaRecord(filename, "", namespace, null, 0, 0, 0);
    mr.setModified(dateTime);
    return mr;
  }
}
