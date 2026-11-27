package com.dorm.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Kazakhstan IIN (individual identification number): 12 digits.
 * YYMMDD + century/gender digit (1-6, odd = male) + 4 serial digits + 1 check digit.
 */
public final class Iin {
    private static final int[] FIRST_WEIGHTS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] SECOND_WEIGHTS = {3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2};

    private Iin() { }

    public static boolean isValid(String iin) {
        return iin != null && iin.matches("\\d{12}") && birthDate(iin).isPresent() && checkDigitOk(iin);
    }

    public static Optional<LocalDate> birthDate(String iin) {
        if (iin == null || !iin.matches("\\d{12}")) return Optional.empty();
        int marker = iin.charAt(6) - '0';
        int century = switch (marker) {
            case 1, 2 -> 1800;
            case 3, 4 -> 1900;
            case 5, 6 -> 2000;
            default -> -1;
        };
        if (century < 0) return Optional.empty();
        try {
            return Optional.of(LocalDate.of(century + Integer.parseInt(iin.substring(0, 2)),
                    Integer.parseInt(iin.substring(2, 4)), Integer.parseInt(iin.substring(4, 6))));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> gender(String iin) {
        if (iin == null || !iin.matches("\\d{12}")) return Optional.empty();
        int marker = iin.charAt(6) - '0';
        if (marker < 1 || marker > 6) return Optional.empty();
        return Optional.of(marker % 2 == 1 ? "MALE" : "FEMALE");
    }

    private static boolean checkDigitOk(String iin) {
        int check = iin.charAt(11) - '0';
        int s = weighted(iin, FIRST_WEIGHTS) % 11;
        if (s == 10) s = weighted(iin, SECOND_WEIGHTS) % 11;
        return s != 10 && s == check;
    }

    private static int weighted(String iin, int[] weights) {
        int sum = 0;
        for (int i = 0; i < 11; i++) sum += (iin.charAt(i) - '0') * weights[i];
        return sum;
    }
}
