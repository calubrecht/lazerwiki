package us.calubrecht.lazerwiki.requests;

public record MovePageRequest (String oldNS, String oldPage, String newNS, String newPage) {
}
