package us.calubrecht.lazerwiki.requests;

import us.calubrecht.lazerwiki.model.Namespace;

public record NamespaceRestrictionRequest(String site, String namespace, Namespace.RESTRICTION_TYPE restrictionType) {
}
