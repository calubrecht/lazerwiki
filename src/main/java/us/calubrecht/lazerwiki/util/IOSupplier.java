package us.calubrecht.lazerwiki.util;

import java.io.IOException;

@FunctionalInterface
public interface IOSupplier<T>  {

    T get() throws IOException;
}
