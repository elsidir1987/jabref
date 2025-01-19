package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

public class MedlineFormatChecker {

    private static final Locale ENGLISH = Locale.ENGLISH;

    public static boolean isMedlineFormat(BufferedReader reader) throws IOException {
        String line;
        int linesChecked = 0;

        while ((line = reader.readLine()) != null && linesChecked < 50) {
            if (line.toLowerCase(ENGLISH).contains("<pubmedarticle>")
                    || line.toLowerCase(ENGLISH).contains("<pubmedbookarticle>")) {
                return true;
            }
            linesChecked++;
        }
        return false;
    }
}
