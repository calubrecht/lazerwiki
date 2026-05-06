package us.calubrecht.lazerwiki.util;

import us.calubrecht.lazerwiki.service.exception.MediaReadException;

import java.io.IOException;

@FunctionalInterface
public interface IOSupplier<T>  {

    T get() throws IOException, MediaReadException;
}
