package ris58h.goleador.processor;

import ij.plugin.filter.Binary;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ris58h.goleador.processor.Utils.readImage;
import static ris58h.goleador.processor.Utils.writeImage;

public class ClearProcessor {
    private static final int BATCH_SIZE = 5;
    private static final int BATCH_COLOR_DELTA = 2;
    private static final int COLOR_BLACK = 0;
    private static final int COLOR_WHITE = 255;
    private static final int MORPH_COUNT = 7;

    public static void process(String dirName, String inSuffix, String outSuffix) throws Exception {
        Path dirPath = Paths.get(dirName);
        String inPostfix = inSuffix + ".png";
        String inGlob = "[0-9][0-9][0-9][0-9]*" + inPostfix;
        IntMatrix intensityMatrix = null;
        IntMatrix colorMatrix = null;
        ArrayDeque<ImageProcessor> batch = new ArrayDeque<>(BATCH_SIZE);
        int[] batchColors = new int[BATCH_SIZE];
        int width = -1;
        int height = -1;
        System.out.println("Building intensity matrix");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, inGlob)) {
            List<Path> sortedPaths = new ArrayList<>();
            for (Path path : stream) {
                sortedPaths.add(path);
            }
            sortedPaths.sort(Comparator.comparing(Path::toString));
            for (Path path : sortedPaths) {
                ImageProcessor ip = readImage(path.toFile());
                int imageWidth = ip.getWidth();
                int imageHeight = ip.getHeight();
                if (intensityMatrix == null) {
                    intensityMatrix = new IntMatrix(imageWidth, imageHeight);
                    colorMatrix = new IntMatrix(imageWidth, imageHeight);
                    width = imageWidth;
                    height = imageHeight;
                } else {
                    if (imageWidth != width || imageHeight != height) {
                        throw new RuntimeException();
                    }
                }
                batch.add(ip);
                if (batch.size() == BATCH_SIZE) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int index = y * width + x;
                            int sum = 0;
                            int i = 0;
                            for (ImageProcessor batchItem : batch) {
                                int batchItemColor = batchItem.get(x, y);
                                batchColors[i] = batchItemColor;
                                sum += batchItemColor;
                                i++;
                            }
                            int avgColor = sum / BATCH_SIZE;
                            boolean sameColor = true;
                            for (int batchColor : batchColors) {
                                int absDelta = Math.abs(avgColor - batchColor);
                                if (absDelta > BATCH_COLOR_DELTA) {
                                    sameColor = false;
                                    break;
                                }
                            }
                            if (sameColor) {
                                intensityMatrix.buffer[index] += 1;
                                colorMatrix.buffer[index] += avgColor;
                            }
                        }
                    }
                    batch.clear();
                }
            }
        }

        if (intensityMatrix == null) {
            throw new RuntimeException();
        }

        System.out.println("Thresholding intensity matrix");
        ByteProcessor staticGray = new ByteProcessor(width, height);
        BinaryProcessor staticBlack = new BinaryProcessor(new ByteProcessor(width, height));
        BinaryProcessor staticWhite = new BinaryProcessor(new ByteProcessor(width, height));
        int intensityThreshold = getIntensityThreshold(intensityMatrix);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int intensity = intensityMatrix.buffer[index];
                if (intensity > intensityThreshold) {
                    int sumColor = colorMatrix.buffer[index];
                    int color = sumColor / intensity;
                    staticGray.set(x, y, color);
                    if (color > 127) {
                        staticWhite.set(x, y, COLOR_WHITE);
                    } else {
                        staticBlack.set(x, y, COLOR_WHITE);
                    }
                }
            }
        }
        writeImage(staticGray, dirPath.resolve("static-gray.png").toFile());
        writeImage(staticBlack, dirPath.resolve("static-black.png").toFile());
        writeImage(staticWhite, dirPath.resolve("static-white.png").toFile());

        System.out.println("Making masks");
        fillHoles(staticBlack);
        fillHoles(staticWhite);
        writeImage(staticBlack, dirPath.resolve("filled-black.png").toFile());
        writeImage(staticWhite, dirPath.resolve("filled-white.png").toFile());
        for (int i = 0; i < MORPH_COUNT; i++) {
            staticBlack.dilate();
            staticWhite.dilate();
        }
        for (int i = 0; i < MORPH_COUNT; i++) {
            staticBlack.erode();
            staticWhite.erode();
        }
        writeImage(staticBlack, dirPath.resolve("mask-black.png").toFile());
        writeImage(staticWhite, dirPath.resolve("mask-white.png").toFile());

        System.out.println("Clearing images");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, inGlob)) {
            for (Path path : stream) {
                ImageProcessor image = readImage(path.toFile());
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                if (imageWidth != width || imageHeight != height) {
                    throw new RuntimeException();
                }
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        boolean insideBlackShape = staticBlack.get(x, y) == COLOR_WHITE;
                        boolean insideWhiteShape = staticWhite.get(x, y) == COLOR_WHITE;
                        boolean isBlack = image.get(x, y) < 127;
                        boolean masked = (insideBlackShape && !isBlack) || (insideWhiteShape && isBlack);
                        image.set(x, y, masked ? COLOR_BLACK : COLOR_WHITE);
                    }
                }
                String name = path.getName(path.getNameCount() - 1).toString();
                String prefix = name.substring(0, name.length() - inPostfix.length());
                Path outPath = path.resolveSibling(prefix + outSuffix + ".png");
                writeImage(image, outPath.toFile());
            }
        }
    }

    private static int getIntensityThreshold(IntMatrix intensityMatrix) {
        int sum = 0;
        int maxValue = 0;
        for (int intensity : intensityMatrix.buffer) {
            sum += intensity;
            if (intensity > maxValue) {
                maxValue = intensity;
            }
        }
        int avg = sum / intensityMatrix.buffer.length;
        return avg;
    }

    private static void fillHoles(ImageProcessor ip) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method fillMethod = Binary.class.getDeclaredMethod("fill", ImageProcessor.class, int.class, int.class);
        fillMethod.setAccessible(true);
        fillMethod.invoke(new Binary(), ip, COLOR_WHITE, COLOR_BLACK);
    }
}