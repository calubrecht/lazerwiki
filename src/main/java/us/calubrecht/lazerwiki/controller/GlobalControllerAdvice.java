package us.calubrecht.lazerwiki.controller;


import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalControllerAdvice {


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleFileSizeException(MaxUploadSizeExceededException ex) {
        long maxBytes = ex.getMaxUploadSize();
        if (maxBytes == -1) {
            if (ex.getCause().getCause() instanceof FileSizeLimitExceededException) {
               maxBytes =  ((FileSizeLimitExceededException)(ex.getCause().getCause())).getPermittedSize();
            }
            else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Max file size of exceeded.");
            }

        }
        String maxSize = Long.toString(maxBytes) +  " bytes";
        if (maxBytes >= 1024) {
            double maxKB = maxBytes/1024.0;
            maxSize = Double.toString(maxKB) + " KB";
            if (maxKB >= 1024) {
                double maxMB = maxKB/1024;
                maxSize = Double.toString(maxMB) + " MB";
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Max file size of " + maxSize + " exceeded.");

    }

}
