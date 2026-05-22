package server.store;

import java.time.DateTimeException;
import java.time.LocalDate;

public final class SaIdValidator {
    private SaIdValidator() {
    }

    public static boolean isValid(String idNumber) {
        if (idNumber == null || !idNumber.matches("\\d{13}")) {
            return false;
        }

        return hasValidDate(idNumber) && hasValidChecksum(idNumber);
    }

    private static boolean hasValidDate(String idNumber) {
        int year = Integer.parseInt(idNumber.substring(0, 2));
        int month = Integer.parseInt(idNumber.substring(2, 4));
        int day = Integer.parseInt(idNumber.substring(4, 6));
        int currentYear = LocalDate.now().getYear() % 100;
        int century = year <= currentYear ? 2000 : 1900;

        try {
            LocalDate birthDate = LocalDate.of(century + year, month, day);
            return !birthDate.isAfter(LocalDate.now());
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean hasValidChecksum(String idNumber) {
        int oddSum = 0;
        StringBuilder evenDigits = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            int digit = Character.digit(idNumber.charAt(i), 10);
            if (i % 2 == 0) {
                oddSum += digit;
            } else {
                evenDigits.append(digit);
            }
        }

        int doubledEven = Integer.parseInt(evenDigits.toString()) * 2;
        int evenSum = String.valueOf(doubledEven).chars()
                .map(character -> character - '0')
                .sum();
        int checkDigit = (10 - ((oddSum + evenSum) % 10)) % 10;

        return checkDigit == Character.digit(idNumber.charAt(12), 10);
    }
}
