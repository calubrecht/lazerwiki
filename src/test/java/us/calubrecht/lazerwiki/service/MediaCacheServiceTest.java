package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.util.IOSupplier;
import us.calubrecht.lazerwiki.util.ImageUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {MediaCacheService.class})
@ActiveProfiles("test")
public class MediaCacheServiceTest {

    @Autowired
    MediaCacheService underTest;
    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @MockBean
    ImageUtil mockImageUtil;

    ImageUtil realImageUtil = new ImageUtil();

    String getFileDimensions(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] bytesRead = fis.readAllBytes();
        fis.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytesRead);
        return realImageUtil.getImageDimension(bis).toString();
    }

    byte[] loadFile(File f) throws IOException {
        try (FileInputStream fin = new FileInputStream(f)) {
            return fin.readAllBytes();
        }
    }

    @Test
    public void testGetBinaryFile() throws IOException {
        when(mockImageUtil.scaleImage(any(), any(), anyInt(), anyInt())).thenAnswer( (inv) -> {
            return realImageUtil.scaleImage(inv.getArgument(0, InputStream.class),
                    inv.getArgument(1, String.class),
                    inv.getArgument(2, Integer.class),
                    inv.getArgument(3, Integer.class));
        });

        Path cacheLocation = Paths.get(staticFileRoot, "default", "media-cache");
        Path originalLocation = Paths.get(staticFileRoot, "default", "media");
        MediaRecord mediaRecord = new MediaRecord("circle.png", "default",  "","Bob", 7, 20, 20);
        File f = Paths.get(cacheLocation.toString(),"circle.png-10x15").toFile();
        File origFile = Paths.get(originalLocation.toString(),"circle.png").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        byte[] bytes = underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),10, 15);
        assertEquals("(10,15)", getFileDimensions(f));
        verify(mockImageUtil, times(1)).scaleImage(any(), any(), anyInt(), anyInt());

        underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),10, 15);
        assertEquals("(10,15)", getFileDimensions(f));
        // Should have used cache, did not need to call scale again.
        verify(mockImageUtil, times(1)).scaleImage(any(), any(), anyInt(), anyInt());
    }

    @Test
    public void testGetBinaryFileDontUpscale() throws IOException {
        MediaRecord mediaRecord = new MediaRecord("circle.png", "default",  "","Bob", 7, 20, 20);
        IOSupplier supplier = () -> new byte[] {1,2,3,4};

        assertEquals(4, underTest.getBinaryFile("default", mediaRecord, supplier ,20, 25)[3]);
        verify(mockImageUtil, never()).scaleImage(any(), any(), anyInt(), anyInt());

        assertEquals(4, underTest.getBinaryFile("default", mediaRecord, supplier,25, 0)[3]);
        verify(mockImageUtil, never()).scaleImage(any(), any(), anyInt(), anyInt());

    }

    @Test
    public void testGetBinaryFileKeepAspectRatio() throws IOException {
        when(mockImageUtil.scaleImage(any(), any(), anyInt(), anyInt())).thenAnswer( (inv) -> {
            return realImageUtil.scaleImage(inv.getArgument(0, InputStream.class),
                    inv.getArgument(1, String.class),
                    inv.getArgument(2, Integer.class),
                    inv.getArgument(3, Integer.class));
        });

        Path cacheLocation = Paths.get(staticFileRoot, "default", "media-cache");
        Path originalLocation = Paths.get(staticFileRoot, "default", "media");
        MediaRecord mediaRecord = new MediaRecord("circle.png", "default",  "","Bob", 7, 20, 20);
        File f = Paths.get(cacheLocation.toString(),"circle.png-10x0").toFile();
        File origFile = Paths.get(originalLocation.toString(),"circle.png").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        byte[] bytes = underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),10, 0);
        assertEquals("(10,10)", getFileDimensions(f));

        underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),0, 5);
        f = Paths.get(cacheLocation.toString(),"circle.png-0x5").toFile();
        assertEquals("(5,5)", getFileDimensions(f));
    }

    @Test
    public void testGetBinaryFileCropToKeepAspectRatio() throws IOException {
        when(mockImageUtil.scaleImage(any(), any(), anyInt(), anyInt())).thenAnswer( (inv) -> {
            return realImageUtil.scaleImage(inv.getArgument(0, InputStream.class),
                    inv.getArgument(1, String.class),
                    inv.getArgument(2, Integer.class),
                    inv.getArgument(3, Integer.class));
        });

        Path cacheLocation = Paths.get(staticFileRoot, "default", "media-cache");
        Path originalLocation = Paths.get(staticFileRoot, "default", "media");
        MediaRecord mediaRecord = new MediaRecord("circle.png", "default",  "","Bob", 7, 20, 20);
        File f = Paths.get(cacheLocation.toString(),"circle.png-10x5").toFile();
        File origFile = Paths.get(originalLocation.toString(),"circle.png").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        byte[] bytes = underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),10, 5);
        assertEquals("(10,5)", getFileDimensions(f));

        underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),5, 10);
        f = Paths.get(cacheLocation.toString(),"circle.png-5x10").toFile();
        assertEquals("(5,10)", getFileDimensions(f));
    }

    @Test
    public void testGetBinaryFileWithNS() throws IOException {
        when(mockImageUtil.scaleImage(any(), any(), anyInt(), anyInt())).thenAnswer( (inv) -> {
            return realImageUtil.scaleImage(inv.getArgument(0, InputStream.class),
                    inv.getArgument(1, String.class),
                    inv.getArgument(2, Integer.class),
                    inv.getArgument(3, Integer.class));
        });

        Path cacheLocation = Paths.get(staticFileRoot, "default", "media-cache", "ns");
        Path originalLocation = Paths.get(staticFileRoot, "default", "media", "ns");
        MediaRecord mediaRecord = new MediaRecord("circleWdot.png", "default",  "ns","Bob", 7, 20, 20);
        File f = Paths.get(cacheLocation.toString(),"circleWdot.png-10x10").toFile();
        File origFile = Paths.get(originalLocation.toString(),"circleWdot.png").toFile();
        Files.deleteIfExists(Path.of(f.getPath()));
        byte[] bytes = underTest.getBinaryFile("default", mediaRecord, () -> loadFile(origFile),10, 10);
        assertEquals("(10,10)", getFileDimensions(f));
        verify(mockImageUtil, times(1)).scaleImage(any(), any(), anyInt(), anyInt());
    }

}
