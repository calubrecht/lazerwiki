package us.calubrecht.lazerwiki.util;

import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ImageUtil {

    public static Pair<Integer, Integer> getImageDimension(InputStream is) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(is)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return Pair.of(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }
        return Pair.of(0,0);
    }
}
