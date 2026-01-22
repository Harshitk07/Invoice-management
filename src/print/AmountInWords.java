package print;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountInWords {

    private static final String[] units = {
            "", "ONE", "TWO", "THREE", "FOUR", "FIVE",
            "SIX", "SEVEN", "EIGHT", "NINE", "TEN",
            "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN",
            "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN",
            "NINETEEN"
    };

    private static final String[] tens = {
            "", "", "TWENTY", "THIRTY", "FORTY",
            "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
    };

    private AmountInWords() {}

    /** FINAL PAYABLE AMOUNT → WORDS (INR, WITH PAISE) */
    public static String rupees(double amount) {

        BigDecimal bd = BigDecimal
                .valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP);

        long rupees = bd.longValue();
        int paise = bd
                .subtract(BigDecimal.valueOf(rupees))
                .movePointRight(2)
                .intValue();

        // 🔑 Edge case: 99.995 → 100 paise
        if (paise == 100) {
            rupees += 1;
            paise = 0;
        }

        StringBuilder sb = new StringBuilder();

        if (rupees > 0) {
            sb.append(convert(rupees)).append(" RUPEES");
        }

        if (paise > 0) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append(convert(paise)).append(" PAISE");
        }

        if (rupees == 0 && paise == 0) {
            sb.append("ZERO RUPEES");
        }

        sb.append(" ONLY");

        return sb.toString();
    }

    private static String convert(long n) {

        if (n == 0) return "ZERO";

        if (n < 20) return units[(int) n];

        if (n < 100)
            return tens[(int) (n / 10)] +
                    (n % 10 != 0 ? " " + units[(int) (n % 10)] : "");

        if (n < 1000)
            return units[(int) (n / 100)] + " HUNDRED" +
                    (n % 100 != 0 ? " AND " + convert(n % 100) : "");

        if (n < 100000)
            return convert(n / 1000) + " THOUSAND" +
                    (n % 1000 != 0 ? " " + convert(n % 1000) : "");

        if (n < 10000000)
            return convert(n / 100000) + " LAKH" +
                    (n % 100000 != 0 ? " " + convert(n % 100000) : "");

        return convert(n / 10000000) + " CRORE" +
                (n % 10000000 != 0 ? " " + convert(n % 10000000) : "");
    }
}
