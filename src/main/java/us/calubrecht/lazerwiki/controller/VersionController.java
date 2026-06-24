package us.calubrecht.lazerwiki.controller;

import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/")
public class VersionController {
  private static VersionController instance;

  public VersionController() {
    instance = this;
  }

  @Value("${app.version}")
  private String appVersion;

  @RequestMapping("version")
  public String version() {
    return appVersion;
  }

  public static VersionController getInstance() {
    return instance;
  }

  public static String getVersion() {
    return instance.appVersion;
  }
}
