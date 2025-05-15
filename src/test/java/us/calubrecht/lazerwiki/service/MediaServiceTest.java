package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Limit;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.MediaHistoryRecord;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.MediaHistoryRepository;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;
import us.calubrecht.lazerwiki.util.IOSupplier;
import us.calubrecht.lazerwiki.util.ImageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@SpringBootTest(classes = {MediaService.class, ImageUtil.class})
@ActiveProfiles("test")
class MediaServiceTest {

    @Autowired
    MediaService underTest;

    @MockBean
    MediaCacheService cacheService;

    @MockBean
    SiteService siteService;

    @MockBean
    MediaRecordRepository mediaRecordRepository;

    @MockBean
    MediaHistoryRepository mediaHistoryRepository;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @MockBean
    NamespaceService namespaceService;

    @MockBean
    UserService userService;

    @Test
    void getBinaryFile() throws IOException, MediaReadException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        byte[] bytes = underTest.getBinaryFile("localhost", "Bob", "circle.png", null);
        assertNotNull(bytes);
        assertEquals(768, bytes.length);

        bytes = underTest.getBinaryFile("localhost", "Bob", "ns:circleWdot.png", null);
        assertNotNull(bytes);
        assertEquals(4486, bytes.length);

        when(namespaceService.canReadNamespace(eq("default"), any(), eq("Joe"))).thenReturn(false);

        assertThrows(MediaReadException.class, () -> underTest.getBinaryFile("localhost", "Joe", "any.file", null));
        verify(cacheService, never()).getBinaryFile(anyString(), any(MediaRecord.class), any(IOSupplier.class), anyInt(), anyInt());
    }

    @Test
    void getBinaryFileWrongSize() throws IOException, MediaReadException {
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        MediaRecord newRecord = new MediaRecord("circle.png", "default",  "",user, 7, 10, 10);
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(mediaRecordRepository.findBySiteAndNamespaceAndFileName("default", "", "circle.png")).thenReturn(newRecord);
        byte[] bytes = underTest.getBinaryFile("localhost", "Bob", "circle.png", "10x10");
        // If size matches record size, don't bother calling cache
        verify(cacheService, never()).getBinaryFile(any(), any(), any(), anyInt(), anyInt());
        // Can supply only a single size;
        bytes = underTest.getBinaryFile("localhost", "Bob", "circle.png", "10");
        verify(cacheService, never()).getBinaryFile(any(), any(), any(), anyInt(), anyInt());
        bytes = underTest.getBinaryFile("localhost", "Bob", "circle.png", "0x10");
        verify(cacheService, never()).getBinaryFile(any(), any(), any(), anyInt(), anyInt());

        byte[] scaledBytes = new byte[] {1,2,3,4};
        when(cacheService.getBinaryFile(eq("default"), eq(newRecord), any(), eq(5), eq(5))).thenReturn(scaledBytes);
        bytes = underTest.getBinaryFile("localhost", "Bob", "circle.png", "5x5");
        assertEquals(scaledBytes.length, bytes.length);
        assertEquals(scaledBytes[3], bytes[3]);
        verify(cacheService).getBinaryFile(eq("default"), eq(newRecord), any(), eq(5), eq(5));

        underTest.getBinaryFile("localhost", "Bob", "circle.png", "5x10");
        verify(cacheService).getBinaryFile(eq("default"), eq(newRecord), any(), eq(5), eq(10));
        underTest.getBinaryFile("localhost", "Bob", "circle.png", "10x5");
        verify(cacheService).getBinaryFile(eq("default"), eq(newRecord), any(), eq(10), eq(5));
        underTest.getBinaryFile("localhost", "Bob", "circle.png", "0x5");
        verify(cacheService).getBinaryFile(eq("default"), eq(newRecord), any(), eq(0), eq(5));
        underTest.getBinaryFile("localhost", "Bob", "circle.png", "5x0");
        verify(cacheService).getBinaryFile(eq("default"), eq(newRecord), any(), eq(5), eq(0));

    }

    @Test
    void getBinaryFileNoRecord() throws IOException, MediaReadException {
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        assertThrows(IOException.class, () -> underTest.getBinaryFile("localhost", "Bob", "nothere.png", "10x10"));
        // No MediaRecord, can't try to resize, don't look in cache.
        verify(cacheService, never()).getBinaryFile(any(), any(), any(), anyInt(), anyInt());

        underTest.getBinaryFile("localhost", "Bob", "circle.png", "10x10");
        verify(cacheService, never()).getBinaryFile(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void saveFile() throws IOException, MediaWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canUploadInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        byte[] bytesToSave = new byte[] {1, 2, 3, 4, 5, 10, 20};
        MockMultipartFile file = new MockMultipartFile("file", "small.bin", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "small.bin").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        underTest.saveFile("localhost", "Bob", file, "");
        // Not real image so dimensions recorded as 0, 0
        MediaRecord newRecord = new MediaRecord("small.bin", "default",  "",user, 7, 0, 0);
        verify(mediaRecordRepository).save(eq(newRecord));
        MediaHistoryRecord newHistoryRecord = new MediaHistoryRecord("small.bin", "default", "", user, "Uploaded");
        verify(mediaHistoryRepository).save(eq(newHistoryRecord));
        verify(cacheService).clearCache(eq("default"), eq(newRecord));

        FileInputStream fis = new FileInputStream(f);
        byte[] bytesRead = fis.readAllBytes();
        fis.close();

        assertEquals(bytesToSave.length, bytesRead.length);
        for (int i = 0; i < bytesToSave.length; i++) {
            assertEquals(bytesToSave[i], bytesRead[i]);
        }

        when(namespaceService.canWriteNamespace(eq("default"), any(), eq("Joe"))).thenReturn(false);
        assertThrows(MediaWriteException.class, () -> underTest.saveFile("localhost", "Joe", file, ""));
    }

    @Test
    void saveFile_wNS() throws IOException, MediaWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canUploadInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        byte[] bytesToSave = new byte[] {1, 2, 3, 4, 5, 10, 20};
        MockMultipartFile file = new MockMultipartFile("file", "other.bin", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "ns1", "other.bin").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        underTest.saveFile("localhost", "Bob", file, "ns1");
        // Not real image so dimensions recorded as 0, 0
        MediaRecord newRecord = new MediaRecord("other.bin", "default",  "ns1",user, 7, 0, 0);
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
    void saveFile_real() throws IOException, MediaReadException, MediaWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canUploadInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canReadNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        byte[] bytesToSave = underTest.getBinaryFile("localhost", "Bob", "circle.png", null);
        MockMultipartFile file = new MockMultipartFile("file", "circle2.png", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "circle2.png").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        underTest.saveFile("localhost", "Bob", file, "");
        MediaRecord newRecord = new MediaRecord("circle2.png", "default", "", user, 768, 20, 20);
        verify(mediaRecordRepository).save(eq(newRecord));
    }

    @Test
    void saveFile_Existing() throws IOException, MediaWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canUploadInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canDeleteInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canUploadInNamespace(eq("default"), any(), eq("Frank"))).thenReturn(true);
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        byte[] bytesToSave = new byte[] {1, 2, 3, 4, 5, 10, 20};
        MockMultipartFile file = new MockMultipartFile("file", "small.bin", null, bytesToSave);
        File f = Paths.get(staticFileRoot, "default", "media", "small.bin").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        MediaRecord existingRecord = new MediaRecord("small.bin", "default",  "",user, 7, 0, 0);
        existingRecord.setId(10L);
        when(mediaRecordRepository.findBySiteAndNamespaceAndFileName("default", "", "small.bin")).thenReturn(existingRecord);
        // Frank cannot delete, so cannot overwrite.
        assertThrows(MediaWriteException.class, () ->underTest.saveFile("localhost", "Frank", file, ""));

        underTest.saveFile("localhost", "Bob", file, "");

        MediaRecord newRecord = new MediaRecord("small.bin", "default",  "",user, 7, 0, 0);
        newRecord.setId(10L);
        verify(mediaRecordRepository).save(eq(newRecord));
        MediaHistoryRecord newHistoryRecord = new MediaHistoryRecord("small.bin", "default", "", user, "Replaced");
        verify(mediaHistoryRepository).save(eq(newHistoryRecord));
    }


    @Test
    void testListFiles() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canReadNamespace(any(), any(), eq("user1"))).thenReturn(true);
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        MediaRecord file1 = new MediaRecord("file1.jpg", "default", "",user, 0, 0, 0);
        MediaRecord file2 = new MediaRecord("afile2.jpg", "default", "", user, 0, 0, 0);
        when(mediaRecordRepository.findAllBySiteOrderByFileName("default")).thenReturn(List.of(file1, file2));
        when(namespaceService.filterReadableMedia(any(), eq("default"), eq("user1"))).thenReturn(List.of(file1, file2));

        MediaListResponse files = underTest.getAllFiles("host.com", "user1");
        assertEquals(0, files.namespaces.getChildren().size());
        assertEquals("", files.namespaces.getNamespace());
        assertEquals(2, files.media.get("").size());
    }

    @Test
    void testListFilesNestedNSes() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        MediaRecord file1 = new MediaRecord("file1.jpg", "default", "ns1",user, 0, 0, 0);
        MediaRecord file2 = new MediaRecord("afile2.jpg", "default", "ns2:ns4", user, 0, 0, 0);
        when(namespaceService.canReadNamespace(any(), any(), eq("user1"))).thenReturn(true);
        when(mediaRecordRepository.findAllBySiteOrderByFileName("default")).thenReturn(List.of(file1, file2));
        when(namespaceService.filterReadableMedia(any(), eq("default"), eq("user1"))).thenReturn(List.of(file1, file2));

        MediaListResponse files = underTest.getAllFiles("host.com", "user1");
        assertEquals(2, files.namespaces.getChildren().size());
        assertEquals("", files.namespaces.getNamespace());
        assertNull(files.media.get(""));
        assertEquals(0, files.namespaces.getChildren().get(0).getChildren().size());
        assertEquals("ns1", files.namespaces.getChildren().get(0).getNamespace());
        assertEquals(1, files.namespaces.getChildren().get(1).getChildren().size());

        when(namespaceService.canReadNamespace(any(), eq("ns1"), eq("user2"))).thenReturn(true);
        when(namespaceService.canReadNamespace(any(), eq(""), eq("user2"))).thenReturn(true);
        when(namespaceService.canReadNamespace(any(), eq("ns2:ns4"), eq("user2"))).thenReturn(false);
        when(namespaceService.filterReadableMedia(any(), eq("default"), eq("user2"))).thenReturn(List.of(file1));

        files = underTest.getAllFiles("host.com", "user2");
        assertEquals(1, files.namespaces.getChildren().size());
        assertEquals("", files.namespaces.getNamespace());
        assertNull(files.media.get(""));
        assertEquals(0, files.namespaces.getChildren().get(0).getChildren().size());
        assertEquals("ns1", files.namespaces.getChildren().get(0).getNamespace());
    }

    @Test
    void testDeleteFile() throws IOException, MediaWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canDeleteInNamespace(eq("default"), any(), eq("bob"))).thenReturn(true);
        User user = new User("bob", "hash");
        when(userService.getUser("bob")).thenReturn(user);
        File f = Paths.get(staticFileRoot, "default", "media", "test.write").toFile();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(1);
        }
        assertTrue(f.exists());
        underTest.deleteFile("host", "test.write", "bob");

        verify(mediaRecordRepository).deleteBySiteAndFilenameAndNamespace("default","test.write", "");
        MediaHistoryRecord newHistoryRecord = new MediaHistoryRecord("test.write", "default", "", user, "Deleted");
        verify(mediaHistoryRepository).save(eq(newHistoryRecord));
        assertFalse(f.exists());

        f = Paths.get(staticFileRoot, "default", "media", "ns", "test.write2").toFile();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(1);
        }

        assertTrue(f.exists());
        underTest.deleteFile("host", "ns:test.write2", "bob");
        assertFalse(f.exists());


        when(namespaceService.canDeleteInNamespace(eq("default"), any(), eq("joe"))).thenReturn(false);
        assertThrows(MediaWriteException.class, () ->  underTest.deleteFile("host", "test.write", "joe"));

    }

    @Test
    void getFileLastModified() throws IOException {
        long modifiedtime = underTest.getFileLastModified("localhost",  "circle.png");

        File f = Paths.get(staticFileRoot, "circle.png").toFile();
        assertEquals(f.lastModified(), modifiedtime);

        modifiedtime = underTest.getFileLastModified("localhost",  "ns:circleWdot.png");
        f = Paths.get(staticFileRoot, "ns","circleWdot.png").toFile();
        assertEquals(f.lastModified(), modifiedtime);
    }

    @Test
    void getRecentChanges() {
        User user = new User ("Bob", "hash");
        when(mediaHistoryRepository.findAllBySiteAndNamespaceInOrderByTsDesc(any(), any(), eq(List.of("ns1","ns2")))).thenReturn(
                List.of(new MediaHistoryRecord("img1.jpg", "site1", "ns1", user, "Uploaded"),
                        new MediaHistoryRecord("img2.jpg", "site1", "ns1", user, "Uploaded")
                        )
        );
        when(siteService.getSiteForHostname("defaultHost")).thenReturn("site1");
        when(namespaceService.getReadableNamespaces("site1", "Bob")).thenReturn(List.of("ns1", "ns2"));
        List<MediaHistoryRecord> changes = underTest.getRecentChanges("defaultHost", "Bob");

        assertEquals(2, changes.size());
        assertEquals("img1.jpg", changes.get(0).getFileName());
    }
}