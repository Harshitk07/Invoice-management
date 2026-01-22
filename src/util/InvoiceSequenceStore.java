package util;

import java.io.IOException;
import java.nio.file.*;

public class InvoiceSequenceStore {

    private static final Path FILE =
            Paths.get(System.getProperty("user.home"),
                    ".shree_uma_invoice_seq.txt");

    public static int nextSequence(String financialYear) {

        try {
            if (!Files.exists(FILE)) {
                Files.writeString(FILE, financialYear + ":101");
                return 101;
            }

            String content = Files.readString(FILE).trim();
            String[] parts = content.split(":");

            String storedFY = parts[0];
            int seq = Integer.parseInt(parts[1]);

            if (!storedFY.equals(financialYear)) {
                Files.writeString(FILE, financialYear + ":101");
                return 101;
            }

            int next = seq + 1;
            Files.writeString(FILE, financialYear + ":" + next);
            return next;

        } catch (IOException e) {
            throw new RuntimeException("Unable to generate invoice number", e);
        }
    }
}
