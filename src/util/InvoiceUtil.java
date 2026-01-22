package util;

import java.time.LocalDate;

public class InvoiceUtil {

    public static String currentFinancialYear() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        // Financial year starts April 1
        if (today.getMonthValue() < 4) {
            return (year - 1) + "-" + (year % 100);
        } else {
            return year + "-" + ((year + 1) % 100);
        }
    }

    public static String formatInvoiceNumber(
            String prefix,
            String fy,
            int sequence
    ) {
        return String.format("%s/%s/%04d", prefix, fy, sequence);
    }
}

