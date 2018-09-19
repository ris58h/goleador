package ris58h.goleador.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ris58h.goleador.core.Utils.readInputToString;

public class ScoresProcessor {

    public static void process(String dirName, String inSuffix, String outSuffix) throws Exception {
        Path dirPath = Paths.get(dirName);
        String inPostfix = inSuffix + ".txt";
        String inGlob = "*" + inPostfix;
        System.out.println("Extracting scores");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, inGlob)) {
            for (Path path : stream) {
                String text;
                try (InputStream is = new BufferedInputStream(new FileInputStream(path.toFile()))) {
                    text = readInputToString(is);
                }
                Score score = ScoreMatcher.find(text);
                if (score != null) {
                    String name = path.getName(path.getNameCount() - 1).toString();
                    String prefix = name.substring(0, name.length() - inPostfix.length());
                    Path outPath = path.resolveSibling(prefix + outSuffix + ".txt");
                    String scoreString = score.left + "-" + score.right;
                    Files.write(outPath, scoreString.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
}
