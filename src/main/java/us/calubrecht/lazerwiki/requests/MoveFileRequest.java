package us.calubrecht.lazerwiki.requests;

public record MoveFileRequest(String oldNS, String oldFile, String newNS, String newFile) {
}
