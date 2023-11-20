package us.calubrecht.lazerwiki.service;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.MediaListResponse;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {MediaService.class})
@ActiveProfiles("test")
class MediaServiceTest {

    @Autowired
    MediaService underTest;

    @MockBean
    SiteService siteService;

    @MockBean
    MediaRecordRepository mediaRecordRepository;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @Test
    void getBinaryFile() throws IOException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        byte[] bytes = underTest.getBinaryFile("localhost", "Bob", "circle.png");
        assertTrue(bytes != null);
        assertEquals(768, bytes.length);

        bytes = underTest.getBinaryFile("localhost", "Bob", "ns:circleWdot.png");
        assertTrue(bytes != null);
        assertEquals(4486, bytes.length);
    }

    @Test
    void saveFile() throws IOException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        byte[] bytesToSave = new byte[] {1, 2, 3, 4, 5, 10, 20};
        MockMultipartFile file = new MockMultipartFile("file", "small.bin", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "small.bin").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        underTest.saveFile("localhost", "Bob", file, "");
        // Not real image so dimensions recorded as 0, 0
        MediaRecord newRecord = new MediaRecord("small.bin", "default",  "","Bob", 7, 0, 0);
        verify(mediaRecordRepository).save(eq(newRecord));

        FileInputStream fis = new FileInputStream(f);
        byte[] bytesRead = fis.readAllBytes();
        fis.close();

        assertEquals(bytesToSave.length, bytesRead.length);
        for (int i = 0; i < bytesToSave.length; i++) {
            assertEquals(bytesToSave[i], bytesRead[i]);
        }
    }

    @Test
    void saveFile_wNS() throws IOException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        byte[] bytesToSave = new byte[] {1, 2, 3, 4, 5, 10, 20};
        MockMultipartFile file = new MockMultipartFile("file", "other.bin", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "ns1", "other.bin").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        underTest.saveFile("localhost", "Bob", file, "ns1");
        // Not real image so dimensions recorded as 0, 0
        MediaRecord newRecord = new MediaRecord("other.bin", "default",  "ns1","Bob", 7, 0, 0);
        verify(mediaRecordRepository).save(eq(newRecord));

        FileInputStream fis = new FileInputStream(f);
        byte[] bytesRead = fis.readAllBytes();
        fis.close();

        assertEquals(bytesToSave.length, bytesRead.length);
        for (int i = 0; i < bytesToSave.length; i++) {
            assertEquals(bytesToSave[i], bytesRead[i]);
        }

        // Compound
        File f2 = Paths.get(staticFileRoot, "default", "media", "ns2", "ns3", "other.bin").toFile();
        Files.deleteIfExists(Path.of(f2.getPath()));
        MockMultipartFile file2 = new MockMultipartFile("file", "other.bin", null, bytesToSave);
        underTest.saveFile("localhost", "Bob", file2, "ns2:ns3");

        assertTrue(f2.exists());
    }


    @Test
    void saveFile_real() throws IOException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        byte[] bytesToSave = underTest.getBinaryFile("localhost", "Bob", "circle.png");
        MockMultipartFile file = new MockMultipartFile("file", "circle2.png", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "circle2.png").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        underTest.saveFile("localhost", "Bob", file, "");
        MediaRecord newRecord = new MediaRecord("circle2.png", "default", "", "Bob", 768, 20, 20);
        verify(mediaRecordRepository).save(eq(newRecord));
    }

    @Test
    void testListFiles() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        MediaRecord file1 = new MediaRecord("file1.jpg", "default", "","bob", 0, 0, 0);
        MediaRecord file2 = new MediaRecord("afile2.jpg", "default", "", "bob", 0, 0, 0);
        when(mediaRecordRepository.findAllBySiteOrderByFileName("default")).thenReturn(List.of(file1, file2));

        MediaListResponse files = underTest.getAllFiles("host.com", "user1");
        assertEquals(0, files.namespaces.getChildren().size());
        assertEquals("", files.namespaces.getNamespace());
        assertEquals(2, files.media.get("").size());
    }

    @Test
    void testListFilesNestedNSes() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        MediaRecord file1 = new MediaRecord("file1.jpg", "default", "ns1","bob", 0, 0, 0);
        MediaRecord file2 = new MediaRecord("afile2.jpg", "default", "ns2:ns4", "bob", 0, 0, 0);
        when(mediaRecordRepository.findAllBySiteOrderByFileName("default")).thenReturn(List.of(file1, file2));

        MediaListResponse files = underTest.getAllFiles("host.com", "user1");
        assertEquals(2, files.namespaces.getChildren().size());
        assertEquals("", files.namespaces.getNamespace());
        assertEquals(null, files.media.get(""));
        assertEquals(0, files.namespaces.getChildren().get(0).getChildren().size());
        assertEquals("ns1", files.namespaces.getChildren().get(0).getNamespace());
        assertEquals(1, files.namespaces.getChildren().get(1).getChildren().size());
    }

    @Test
    void testDeleteFile() throws IOException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        File f = Paths.get(staticFileRoot, "default", "media", "test.write").toFile();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(1);
        }
        assertTrue(f.exists());
        underTest.deleteFile("host", "test.write", "bob");

        verify(mediaRecordRepository).deleteBySiteAndFilenameAndNamespace("default","test.write", "");
        assertFalse(f.exists());

        f = Paths.get(staticFileRoot, "default", "media", "ns", "test.write2").toFile();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(1);
        }

        assertTrue(f.exists());
        underTest.deleteFile("host", "ns:test.write2", "bob");
        assertFalse(f.exists());

    }
}