package ris58h.goleador.core.processor;


import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

import java.nio.file.*;

import static ris58h.goleador.core.ImageUtils.readImage;
import static ris58h.goleador.core.ImageUtils.writeImage;

public class BlurProcessor implements Processor {
    private double sigma = 1;

    @Override
    public void init(Parameters parameters) throws Exception {
        parameters.apply("sigma").ifPresent(value -> {
            sigma = Double.parseDouble(value);
        });
    }

    @Override
    public void process(String inName, String outName, String workingDir) throws Exception {
        Path dirPath = Paths.get(workingDir);
        String inGlob = FrameUtils.glob(inName, "png");
        System.out.println("Blurring frames");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, inGlob)) {
            for (Path path : stream) {
                Path outPath = FrameUtils.resolveSiblingPath(path, outName, "png");
                blur(path, outPath);
            }
        }
    }

    private void blur(Path inPath, Path outPath) throws Exception {
        ImageProcessor ip = readImage(inPath.toFile());
        GaussianBlur gaussianBlur = new GaussianBlur();
        gaussianBlur.blurGaussian(ip, sigma);
        writeImage(ip, outPath.toFile());
    }
}
