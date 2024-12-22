package us.calubrecht.lazerwiki.requests;

import java.util.Map;

public record SiteSettingsRequest(String hostName, String siteSettings) {
}
