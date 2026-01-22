package print;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PrintFormat {

    private PrintFormat() {}

    public static final double A4_WIDTH  = 595;
    public static final double A4_HEIGHT = 842;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /* ================= DATE ================= */

    public static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    /* ================= MONEY ================= */

    public static String money(double v) {
        return String.format("%.2f", v);
    }

    /* ================= QTY ================= */

    public static String qty(double v) {
        if (v == Math.floor(v)) {
            return String.format("%.0f", v);
        }
        return String.format("%.2f", v);
    }

    /* ================= RATE / GST ================= */

    public static String rate(double v) {
        return String.format("%.2f", v);
    }

    public static String percent(double v) {
        if (v == Math.floor(v)) {
            return String.format("%.0f", v);
        }
        return String.format("%.2f", v);
    }
}
