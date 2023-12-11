package us.calubrecht.lazerwiki.util;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Service
public class ImageUtil {

    public Pair<Integer, Integer> getImageDimension(InputStream is) throws IOException {
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

    public byte[] scaleImage(InputStream is, String formatName, int width, int height) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(is)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    BufferedImage oldImage = reader.read(0);
                    int initialHeight = oldImage.getHeight();
                    int initialWidth = oldImage.getWidth();
                    double initialRatio = ((double)initialWidth)/initialHeight;
                    if (width == 0) {
                        width = (int)(height * initialRatio);
                    }
                    if (height == 0) {
                        height = (int)(width / initialRatio);
                    }
                    double newAspectRatio = ((double)width)/height;
                    if (initialHeight != (int)(initialWidth / newAspectRatio)) {
                        int cropToHeight = initialHeight;
                        int cropToWidth = initialWidth;
                        // New aspect ratio is different.
                        if (newAspectRatio > initialRatio) {
                            cropToHeight = (int) (initialWidth / newAspectRatio);
                        }
                        else
                        {
                           cropToWidth = (int) (initialHeight * newAspectRatio);
                        }
                        oldImage = oldImage.getSubimage(0, 0, cropToWidth, cropToHeight);
                    }
                    Image resultingImage = oldImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    BufferedImage outputImage = new BufferedImage(width, height, oldImage.getType());
                    outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
                    ByteArrayOutputStream baos=new ByteArrayOutputStream();
                    ImageIO.write(outputImage, formatName, baos );
                    return baos.toByteArray();
                } finally {
                    reader.dispose();
                }
            }
        }
        return null;

    }
}
