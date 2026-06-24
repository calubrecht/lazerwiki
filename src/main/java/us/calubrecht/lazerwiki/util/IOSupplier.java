package us.calubrecht.lazerwiki.util;

import java.io.IOException;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;

@FunctionalInterface
public interface IOSupplier<T> {

  T get() throws IOException, MediaReadException;
}
