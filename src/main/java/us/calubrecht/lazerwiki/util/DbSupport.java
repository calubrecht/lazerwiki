package us.calubrecht.lazerwiki.util;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DbSupport {

    public static <T> List<T> toList(Iterable<T> iterable) {
        return toStream(iterable).toList();
    }

    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
