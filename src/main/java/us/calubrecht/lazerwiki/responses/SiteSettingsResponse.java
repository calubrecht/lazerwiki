package us.calubrecht.lazerwiki.responses;

import us.calubrecht.lazerwiki.model.Site;

public record SiteSettingsResponse(Site site, boolean success, String msg) {
}
