package it.jaschke.alexandria;

import java.util.Arrays;
import java.util.List;

/**
 * Created by carlos on 7/13/15.
 */
public class Utility {

    /**
     * Parses a scanned QR code, and returns a ISBN code number if found, otherwise returns an empty
     * string
     * @param QRCode Scanned QR code
     * @return String ISBN code
     */
    public static String getISBNNumberFromQRCode(String QRCode) {
        String parsedISBN = "";
        if (QRCode.startsWith("http")) {
            QRCode = QRCode.replaceAll("[^0-9]+", " ");
            List<String> results = Arrays.asList(QRCode.trim().split(" "));
            if (results.size() == 1 && results.get(0).length() == 13 && results.get(0).startsWith("978")) {
                parsedISBN = results.get(0);
            }
        }
        return parsedISBN;
    }
}
