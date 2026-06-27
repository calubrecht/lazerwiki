package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {ResourceService.class})
@ActiveProfiles("test")
class ResourceServiceTest {

  @Autowired ResourceService underTest;

  @Value("${lazerwiki.static.file.root}")
  String staticFileRoot;

  @Test
  void test_getBinaryFile() throws IOException {
    byte[] bytes = underTest.getBinaryFile("default", "bluecircle.png");
    assertTrue(bytes != null);
    assertEquals(768, bytes.length);

    // File from src/main/resources/static
    bytes = underTest.getBinaryFile("default", "site.css");
    assertTrue(bytes != null);
    assertEquals("/* Site specific css to override defaults */", new String(bytes).trim());

    // Missing File
    assertThrows(IOException.class, () -> underTest.getBinaryFile("default", "what.css"));
  }

  @Test
  void test_getFileLastModified() throws IOException {
    long modifiedtime = underTest.getFileLastModified("default", "bluecircle.png");

    File f = Paths.get(staticFileRoot, "default", "resources", "bluecircle.png").toFile();
    assertEquals(f.lastModified(), modifiedtime);

    underTest.bootTime = 5;

    assertEquals(5, underTest.getFileLastModified("default", "thisfileisnthere.jpg"));
  }

  @Test
  void test_bootTime() {
    assertTrue(underTest.getBootTime() > 0);
  }
}
