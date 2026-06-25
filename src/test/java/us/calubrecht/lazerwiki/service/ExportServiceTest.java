package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;

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

    String getStringContents(Path base, String fileName) throws IOException {
        Path filePath = base.resolve(fileName);
        try (FileReader fr = new FileReader(filePath.toFile());) {
            return fr.readAllAsString();
        }
    }

    byte[] getBinaryFile(String path) throws IOException {
        Path staticDir = Path.of(staticFileRoot, "default", "media");
        File f = staticDir.resolve(path).toFile();
        return Files.readAllBytes(f.toPath());
    }

    @BeforeEach
    void setup() {
        underTest.staticFileRoot = staticFileRoot;
    }

    @Test
    void test_createExportBundle() throws IOException, MediaReadException, MediaWriteException {
        when(siteService.getHostForSitename(anyString())).thenReturn("localhost");
        Map<String, List<PageDesc>> pages =
                Map.of(
                        "", List.of(desc("", ""), desc("", "FirstPage")),
                        "ns1:nsdeep", List.of(desc("ns1:nsdeep", "OtherPage")));
        PageData rootPage = new PageData("", "This is the root page", List.of("root"), null, null);
        PageData firstPage =
                new PageData("", "This is the first page", List.of("first", "example"), null, null);
        PageData otherPage = new PageData("", "This is the other page", List.of(), null, null);
        when(pageService.getPageData(eq("default"), eq(""), eq("george"))).thenReturn(rootPage);
        when(pageService.getPageData(eq("default"), eq("FirstPage"), eq("george")))
                .thenReturn(firstPage);
        when(pageService.getPageData(eq("default"), eq("ns1:nsdeep:OtherPage"), eq("george")))
                .thenReturn(otherPage);
        PageListResponse response = new PageListResponse(pages, null);
        when(pageService.getAllPages(eq("default"), eq("george"))).thenReturn(response);

        MediaRecord circle = new MediaRecord("circle.png", "default", "", null, 0, 0, 0);
        MediaRecord circleDot = new MediaRecord("circleWdot.png", "default", "ns", null, 0, 0, 0);
        MediaListResponse mediaList =
                new MediaListResponse(
                        Map.of(
                                "", List.of(circle),
                                "ns", List.of(circleDot)),
                        null);
        when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaList);
        when(mediaService.getBinaryFile(anyString(), anyString(), eq("circle.png"), isNull()))
                .thenReturn(getBinaryFile("circle.png"));
        when(mediaService.getBinaryFile(anyString(), anyString(), eq("ns/circleWdot.png"), isNull()))
                .thenReturn(getBinaryFile("ns/circleWdot.png"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        underTest.createExportBundle("default", "george", baos);
        byte[] bytes = baos.toByteArray();

        // Write bytes to temporary file for extraction
        Path tempArchive = Files.createTempFile("export", ".tar.gz");
        Path extractDir = Files.createTempDirectory("export-extracted");
        try {
            int fileCount = extractFiles(tempArchive, bytes, extractDir);

            // Now you can verify the extracted files
            assertTrue(Files.exists(extractDir), "Extraction directory should exist");
            assertEquals(9, fileCount);
            List<String> expectedPages =
                    List.of(
                            "pages/.txt",
                            "pages/.meta",
                            "pages/FirstPage.txt",
                            "pages/FirstPage.meta",
                            "pages/ns1/nsdeep/OtherPage.txt",
                            "pages/ns1/nsdeep/OtherPage.meta");
            for (String page : expectedPages) {
                assertTrue(
                        Files.exists(extractDir.resolve(page)), "Archive did not contain [" + page + "]");
            }
            assertEquals("This is the root page", getStringContents(extractDir, "pages/.txt"));
            assertEquals(
                    """
                            Name: FirstPage
                            NameSpace:\s
                            Tags: first,example
                            Format: Doku
                            """,
                    getStringContents(extractDir, "pages/FirstPage.meta"));
            assertEquals(
                    """
                            Name: OtherPage
                            NameSpace: ns1:nsdeep
                            Tags:\s
                            Format: Doku
                            """,
                    getStringContents(extractDir, "pages/ns1/nsdeep/OtherPage.meta"));

            List<String> expectedImgs = List.of("media/circle.png", "media/ns/circleWdot.png");
            for (String img : expectedImgs) {
                assertTrue(Files.exists(extractDir.resolve(img)), "Archive did not contain [" + img + "]");
            }

            assertTrue(
                    Files.exists(extractDir.resolve("resources/bluecircle.png")),
                    "Archive did not contain [resources/bluecircle.png]");

            Path staticDir = Path.of(staticFileRoot);

            FileUtils.contentEquals(
                    staticDir.resolve("media/default/circle.png").toFile(),
                    extractDir.resolve("media/circle.png").toFile());
            FileUtils.contentEquals(
                    staticDir.resolve("media/default/ns/circleWdot.png").toFile(),
                    extractDir.resolve("media/ns/circleWdot.png").toFile());

        } finally {
            // Cleanup
            Files.deleteIfExists(tempArchive);
            FileUtils.deleteDirectory(extractDir.toFile());
        }
    }

    @Test
    void test_createExportBunde_MissingFile()
            throws MediaReadException, MediaWriteException, IOException {
        when(siteService.getHostForSitename(anyString())).thenReturn("localhost");
        Map<String, List<PageDesc>> pages = Map.of("", List.of(desc("", "")));
        PageData rootPage = new PageData("", "This is the root page", List.of("root"), null, null);
        when(pageService.getPageData(eq("default"), eq(""), eq("george"))).thenReturn(rootPage);
        PageListResponse response = new PageListResponse(pages, null);
        when(pageService.getAllPages(eq("default"), eq("george"))).thenReturn(response);

        MediaRecord circle = new MediaRecord("circle.png", "default", "", null, 0, 0, 0);
        MediaRecord circleDot = new MediaRecord("circleWdot.png", "default", "ns", null, 0, 0, 0);
        MediaListResponse mediaList =
                new MediaListResponse(
                        Map.of(
                                "", List.of(circle),
                                "ns", List.of(circleDot)),
                        null);
        when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaList);

        when(mediaService.getBinaryFile(anyString(), anyString(), eq("circle.png"), isNull()))
                .thenReturn(getBinaryFile("circle.png"));
        when(mediaService.getBinaryFile(anyString(), anyString(), eq("ns/circleWdot.png"), isNull()))
                .thenThrow(new NoSuchFileException("ns/circleWdot.png"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        underTest.createExportBundle("default", "george", baos);
        byte[] bytes = baos.toByteArray();

        // Write bytes to temporary file for extraction
        Path tempArchive = Files.createTempFile("export", ".tar.gz");
        Path extractDir = Files.createTempDirectory("export-extracted");
        try {
            int fileCount = extractFiles(tempArchive, bytes, extractDir);

            // Now you can verify the extracted files
            assertEquals(4, fileCount);
            List<String> expectedImgs = List.of("media/circle.png");
            for (String img : expectedImgs) {
                assertTrue(Files.exists(extractDir.resolve(img)), "Archive did not contain [" + img + "]");
            }

            Path staticDir = Path.of(staticFileRoot);

            // Succcessfully packaged 1 file without missing file.
            FileUtils.contentEquals(
                    staticDir.resolve("media/default/circle.png").toFile(),
                    extractDir.resolve("media/circle.png").toFile());

        } finally {
            // Cleanup
            Files.deleteIfExists(tempArchive);
            FileUtils.deleteDirectory(extractDir.toFile());
        }
    }

    @Test
    void test_createExportBunde_MediaReadException() throws MediaReadException, IOException {
        when(siteService.getHostForSitename(anyString())).thenReturn("localhost");
        Map<String, List<PageDesc>> pages = Map.of("", List.of(desc("", "")));
        PageData rootPage = new PageData("", "This is the root page", List.of("root"), null, null);
        when(pageService.getPageData(eq("default"), eq(""), eq("george"))).thenReturn(rootPage);
        PageListResponse response = new PageListResponse(pages, null);
        when(pageService.getAllPages(eq("default"), eq("george"))).thenReturn(response);

        MediaRecord circle = new MediaRecord("circle.png", "default", "", null, 0, 0, 0);
        MediaRecord circleDot = new MediaRecord("circleWdot.png", "default", "ns", null, 0, 0, 0);
        MediaListResponse mediaList =
                new MediaListResponse(
                        Map.of(
                                "", List.of(circle),
                                "ns", List.of(circleDot)),
                        null);
        when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaList);

        when(mediaService.getBinaryFile(anyString(), anyString(), eq("circle.png"), isNull()))
                .thenReturn(getBinaryFile("circle.png"));
        when(mediaService.getBinaryFile(anyString(), anyString(), eq("ns/circleWdot.png"), isNull()))
                .thenThrow(new MediaReadException("ns/circleWdot.png"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        assertThrows(
                RuntimeException.class, () -> underTest.createExportBundle("default", "george", baos));
    }

    private static int extractFiles(Path tempArchive, byte[] bytes, Path extractDir)
            throws IOException {
        Files.write(tempArchive, bytes);

        // Extract tar.gz and inspect contents

        int fileCount = 0;
        try (InputStream is = Files.newInputStream(tempArchive)) {
            try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(is)) {
                try (org.apache.commons.compress.archivers.tar.TarArchiveInputStream tar =
                             new org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzip)) {
                    org.apache.commons.compress.archivers.tar.TarArchiveEntry entry;
                    while ((entry = tar.getNextEntry()) != null) {
                        Path filePath = extractDir.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            Files.createDirectories(filePath);
                        } else {
                            Files.createDirectories(filePath.getParent());
                            Files.copy(tar, filePath);
                            fileCount++;
                        }
                    }
                }
            }
        }
        return fileCount;
    }

    @Test
    public void test_createExportBundle_MissingResources() throws IOException {
        underTest.staticFileRoot = Path.of(staticFileRoot, "missingDir").toString();
        when(siteService.getHostForSitename(anyString())).thenReturn("localhost");
        Map<String, List<PageDesc>> pages =
                Map.of(
                        "", List.of(desc("", "")));
        PageData rootPage = new PageData("", "This is the root page", List.of("root"), null, null);
        when(pageService.getPageData(eq("default"), eq(""), eq("george"))).thenReturn(rootPage);
        PageListResponse response = new PageListResponse(pages, null);
        when(pageService.getAllPages(eq("default"), eq("george"))).thenReturn(response);
        MediaListResponse mediaList = new MediaListResponse(Map.of(), null);
        when(mediaService.getAllFiles(eq("localhost"), isNull())).thenReturn(mediaList);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        underTest.createExportBundle("default", "george", baos);
        byte[] bytes = baos.toByteArray();

        // Write bytes to temporary file for extraction
        Path tempArchive = Files.createTempFile("export", ".tar.gz");
        Path extractDir = Files.createTempDirectory("export-extracted");
        try {
            int fileCount = extractFiles(tempArchive, bytes, extractDir);

            // Now you can verify the extracted files
            assertTrue(Files.exists(extractDir), "Extraction directory should exist");
            assertEquals(2, fileCount);
            List<String> expectedPages =
                    List.of(
                            "pages/.txt",
                            "pages/.meta");
            for (String page : expectedPages) {
                assertTrue(
                        Files.exists(extractDir.resolve(page)), "Archive did not contain [" + page + "]");
            }
            assertFalse(
                    Files.exists(extractDir.resolve("resources")), "Archive should not include any resources");

        } finally {
            // Cleanup
            Files.deleteIfExists(tempArchive);
            FileUtils.deleteDirectory(extractDir.toFile());
        }
    }
}
