package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageListResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ExportService.class})
@ActiveProfiles("test")
class ExportServiceTest {

    @Autowired
    ExportService underTest;

    @MockitoBean
    PageService pageService;

    @MockitoBean
    MediaService mediaService;

    @MockitoBean
    SiteService siteService;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    PageDesc desc(String ns, String pageName) {
        return new PageDesc() {
            @Override
            public String getNamespace() {
                return ns;
            }

            @Override
            public String getPagename() {
                return pageName;
            }

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
            public LocalDateTime getModified() {
                return null;
            }

            @Override
            public boolean isDeleted() {
                return false;
            }
        };
    }

    @Test
    void createExportBundle() throws IOException {
        when(siteService.getSiteForHostname(anyString())).thenReturn("default");
        Map<String, List<PageDesc>> pages = Map.of(
                "", List.of(desc("", ""), desc("", "FirstPage")),
                "ns1:nsdeep", List.of(desc("ns1:nsdeep", "OtherPage"))
        );
        PageData rootPage = new PageData("", "This is the root page", List.of("root"), null, null);
        PageData firstPage = new PageData("", "This is the first page", List.of("first", "example"), null, null);
        PageData otherPage = new PageData("", "This is the other page", List.of(), null, null);
        when(pageService.getPageData(eq("localhost"), eq(""), eq("george"))).thenReturn(rootPage);
        when(pageService.getPageData(eq("localhost"), eq("FirstPage"), eq("george"))).thenReturn(firstPage);
        when(pageService.getPageData(eq("localhost"), eq("ns1:nsdeep:OtherPage"), eq("george"))).thenReturn(otherPage);
        PageListResponse response = new PageListResponse(pages, null);
        when(pageService.getAllPages(eq("localhost"), eq("george"))).thenReturn(response);
        
        MediaListResponse mediaList = new MediaListResponse(Map.of(), null);
        when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaList);

        File f = Paths.get(staticFileRoot, "default", "tmp", "export.tar.gz").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));

        byte[] bytes = underTest.createExportBundle("localhost", "george");

    }
}