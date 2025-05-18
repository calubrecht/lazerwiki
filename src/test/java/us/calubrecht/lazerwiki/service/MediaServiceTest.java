package us.calubrecht.lazerwiki.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Limit;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.MediaHistoryRepository;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.responses.MoveStatus;
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

    @MockBean
    ActivityLogService activityLogService;

    @MockBean
    MediaOverrideService mediaOverrideService;

    @MockBean
    RegenCacheService regenCacheService;

    @Test
    void getBinaryFile() throws IOException, MediaReadException, MediaWriteException {
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
    void getBinaryFileWrongSize() throws IOException, MediaReadException, MediaWriteException {
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
    void getBinaryFileNoRecord() throws IOException, MediaReadException, MediaWriteException {
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
        when(namespaceService.joinNS("", "small.bin")).thenReturn("small.bin");
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
        MediaHistoryRecord newHistoryRecord = new MediaHistoryRecord("small.bin", "default", "", user, ActivityType.ACTIVITY_PROTO_UPLOAD_MEDIA);
        verify(mediaHistoryRepository).save(eq(newHistoryRecord));
        verify(cacheService).clearCache(eq("default"), eq(newRecord));
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_UPLOAD_MEDIA, "default", user, "small.bin");

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
        MediaHistoryRecord newHistoryRecord = new MediaHistoryRecord("small.bin", "default", "", user,  ActivityType.ACTIVITY_PROTO_REPLACE_MEDIA);
        verify(mediaHistoryRepository).save(eq(newHistoryRecord));
    }

    @Test
    void saveFile_PathTraversal() {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canUploadInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        when(namespaceService.canDeleteInNamespace(eq("default"), any(), eq("Bob"))).thenReturn(true);
        User user = new User("Bob", "hash");
        when(userService.getUser("Bob")).thenReturn(user);
        byte[] bytesToSave = new byte[] {1, 2, 3, 4, 5, 10, 20};
        MockMultipartFile file = new MockMultipartFile("file", "../../small.bin", null, bytesToSave);
        // Refuse to save file if outside sandbox
        assertThrows(MediaWriteException.class, () ->underTest.saveFile("localhost", "Bob", file, ""));
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
        MediaHistoryRecord newHistoryRecord = new MediaHistoryRecord("test.write", "default", "", user,  ActivityType.ACTIVITY_PROTO_DELETE_MEDIA);
        verify(mediaHistoryRepository).save(eq(newHistoryRecord));
        verify(activityLogService).log(ActivityType.ACTIVITY_PROTO_DELETE_MEDIA, "default", user, "test.write");
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
    void getFileLastModified() throws IOException, MediaWriteException {
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        long modifiedtime = underTest.getFileLastModified("localhost",  "circle.png");

        File f = Paths.get(staticFileRoot, "default", "media", "circle.png").toFile();
        assertEquals(f.lastModified(), modifiedtime);

        modifiedtime = underTest.getFileLastModified("localhost",  "ns:circleWdot.png");
        f = Paths.get(staticFileRoot, "default", "media","ns","circleWdot.png").toFile();
        assertEquals(f.lastModified(), modifiedtime);
    }

    @Test
    void getRecentChanges() {
        User user = new User ("Bob", "hash");
        when(mediaHistoryRepository.findAllBySiteAndNamespaceInOrderByTsDesc(any(), any(), eq(List.of("ns1","ns2")))).thenReturn(
                List.of(new MediaHistoryRecord("img1.jpg", "site1", "ns1", user,  ActivityType.ACTIVITY_PROTO_UPLOAD_MEDIA),
                        new MediaHistoryRecord("img2.jpg", "site1", "ns1", user,  ActivityType.ACTIVITY_PROTO_UPLOAD_MEDIA)
                        )
        );
        when(siteService.getSiteForHostname("defaultHost")).thenReturn("site1");
        when(namespaceService.getReadableNamespaces("site1", "Bob")).thenReturn(List.of("ns1", "ns2"));
        List<MediaHistoryRecord> changes = underTest.getRecentChanges("defaultHost", "Bob");

        assertEquals(2, changes.size());
        assertEquals("img1.jpg", changes.get(0).getFileName());
    }

    @Test
    void testMoveImage() throws MediaWriteException, IOException {
        User oldUser = new User("Charlie", "");
        User newUser = new User("Bob", "");
        when(siteService.getSiteForHostname(any())).thenReturn("default");
        when(namespaceService.canUploadInNamespace(any(), eq("ns1"), eq("Bob"))).thenReturn(true);
        when(namespaceService.canUploadInNamespace(any(), eq("ns2"), eq("Bob"))).thenReturn(true);
        when(userService.getUser("Bob")).thenReturn(newUser);
        MediaRecord oldRecord = new MediaRecord("img1.jpg", "default", "ns1", oldUser, 10000L, 10, 10);
        oldRecord.setId(12L);
        when(mediaRecordRepository.findBySiteAndNamespaceAndFileName("default", "ns1", "img1.jpg")).
                thenReturn(oldRecord);
        File oldFile = Paths.get(staticFileRoot, "default", "media", "ns1", "img1.jpg").toFile();
        File newFile = Paths.get(staticFileRoot, "default", "media", "ns2", "img2.jpg").toFile();
        Files.deleteIfExists(Path.of(oldFile.getPath()));
        Files.deleteIfExists(Path.of(newFile.getPath()));
        try (FileOutputStream fos = new FileOutputStream(oldFile)) {
            fos.write(1);
        }


        MoveStatus status = underTest.moveImage("localhost", "Bob", "ns1", "img1.jpg", "ns2", "img2.jpg");
        assertTrue(status.success());

        verify(mediaOverrideService).createOverride("localhost", "ns1", "img1.jpg", "ns2", "img2.jpg");
        MediaRecord newRecord = new MediaRecord("img2.jpg", "default", "ns2", newUser, 10000L, 10, 10);
        verify(mediaRecordRepository).save(newRecord);
        verify(mediaRecordRepository).deleteById(12L);
        verify(cacheService).clearCache("default", oldRecord);
        verify(cacheService).clearCache("default", newRecord);
        verify(regenCacheService).regenCachesForImageRefs("default", "ns1:img1.jpg", "ns2:img2.jpg");

        assertFalse(oldFile.exists());
        assertTrue(newFile.exists());
        FileInputStream fis = new FileInputStream(newFile);
        byte[] bytesRead = fis.readAllBytes();
        fis.close();

        assertEquals(1, bytesRead[0]);


        // User can't write in ns1
        status = underTest.moveImage("localhost", "Joe", "ns1", "img1.jpg", "ns2", "img2.jpg");
        assertFalse(status.success());
        assertEquals("You don't have permission to upload in ns1", status.message());
        verify(mediaOverrideService, times(1)).createOverride(any(), any(), any(), any(), any());
        // User can't write in ns2
        when(namespaceService.canUploadInNamespace(any(), eq("ns1"), eq("Frank"))).thenReturn(true);
        status = underTest.moveImage("localhost", "Frank", "ns1", "img1.jpg", "ns2", "img2.jpg");
        assertFalse(status.success());
        assertEquals("You don't have permission to upload in ns2", status.message());
        verify(mediaOverrideService, times(1)).createOverride(any(), any(), any(), any(), any());
        // File doesn't exist
        when(namespaceService.canUploadInNamespace(any(), eq("ns1"), eq("Frank"))).thenReturn(true);
        status = underTest.moveImage("localhost", "Bob", "ns1", "noImg.jpg", "ns2", "img2.jpg");
        assertFalse(status.success());
        assertEquals("noImg.jpg does not exist", status.message());
        verify(mediaOverrideService, times(1)).createOverride(any(), any(), any(), any(), any());

        // Target file already exists
        when(mediaRecordRepository.findBySiteAndNamespaceAndFileName("default", "ns2", "img3.jpg")).
                thenReturn(new MediaRecord("img3.jpg", "default", "ns2", newUser, 10000L, 10, 10));
        status = underTest.moveImage("localhost", "Bob", "ns1", "img1.jpg", "ns2", "img3.jpg");
        assertFalse(status.success());
        assertEquals("img3.jpg already exists, move cannot overwrite it", status.message());
        verify(mediaOverrideService, times(1)).createOverride(any(), any(), any(), any(), any());
    }
}