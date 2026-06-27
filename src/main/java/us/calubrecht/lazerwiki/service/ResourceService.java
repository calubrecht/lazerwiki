package us.calubrecht.lazerwiki.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {
  final Logger logger = LogManager.getLogger(getClass());

  long bootTime = System.currentTimeMillis();

  @Value("${lazerwiki.static.file.root}")
  String staticFileRoot;

  void ensureDir(String site) throws IOException {
    Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "resources")));
  }

  public byte[] getBinaryFile(String site, String fileName) throws IOException {
    ensureDir(site);
    File f = new File(String.join("/", staticFileRoot, site, "resources", fileName));
    if (!f.exists()) {
      logger.info("Reading file from resources " + String.join("/", "static", fileName));
      try (InputStream s =
          getClass().getClassLoader().getResourceAsStream(String.join("/", "static", fileName))) {
        if (s == null) {
          throw new IOException("Error reading " + fileName);
        }
        return s.readAllBytes();
      }
    }
    logger.info("Reading file " + f.getAbsoluteFile());
    return Files.readAllBytes(f.toPath());
  }

  public long getFileLastModified(String site, String fileName) throws IOException {
    ensureDir(site);
    File f = new File(String.join("/", staticFileRoot, site, "resources", fileName));
    return f.exists() ? f.lastModified() : bootTime;
  }

  public long getBootTime() {
    return bootTime;
  }
}
