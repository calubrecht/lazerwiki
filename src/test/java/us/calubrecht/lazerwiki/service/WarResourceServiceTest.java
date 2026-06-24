package us.calubrecht.lazerwiki.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {WarResourceService.class})
@ActiveProfiles(profiles = {"test-sa", "standalone", "test"})
public class WarResourceServiceTest {

  @Autowired WarResourceService service;

  @Test
  public void test_getBinaryFile() throws IOException {
    byte[] indexBytes = service.getBinaryFile("index.html");
    String indexString = new String(indexBytes);

    assertEquals("<!doctype ", indexString.substring(0, 10));
  }

  @Test
  public void test_getFileLastModified() throws IOException {
    long lastModified = service.getFileLastModified("index.html");

    assertTrue(lastModified > 0);
  }
}
