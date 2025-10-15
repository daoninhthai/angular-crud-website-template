package com.fintech.payment.util;

/**
 * Utility class for masking sensitive data in audit logs and API responses.
 * Prevents exposure of full account numbers, emails, and phone numbers.
 */
public final class MaskingUtil {

    private MaskingUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Masks an account number, preserving the prefix and last 4 digits.
     * Example: "PAY1234567890" becomes "PAY****7890"
     *
     * @param accountNumber the account number to mask
     * @return the masked account number, or the original if too short to mask
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }

        // Find where digits start (preserve prefix like "PAY")
        int prefixEnd = 0;
        for (int i = 0; i < accountNumber.length(); i++) {
            if (Character.isDigit(accountNumber.charAt(i))) {
                prefixEnd = i;
                break;
            }
        }

        String prefix = accountNumber.substring(0, prefixEnd);
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        int maskLength = accountNumber.length() - prefixEnd - 4;

        if (maskLength <= 0) {
            return accountNumber;
        }

        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < maskLength; i++) {
            masked.append('*');
        }
        masked.append(lastFour);

        return masked.toString();
    }

    /**
     * Masks an email address, preserving the first character and domain.
     * Example: "test@mail.com" becomes "t***@mail.com"
     *
     * @param email the email address to mask
     * @return the masked email, or the original if it does not contain '@'
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        String firstChar = localPart.substring(0, 1);
        int maskLength = localPart.length() - 1;

        StringBuilder masked = new StringBuilder(firstChar);
        for (int i = 0; i < maskLength; i++) {
            masked.append('*');
        }
        masked.append(domain);

        return masked.toString();
    }

    /**
     * Masks a phone number, preserving the last 4 digits.
     * Example: "+84912345678" becomes "********5678"
     * Example: "0912345678"  becomes "******5678"
     *
     * @param phone the phone number to mask
     * @return the masked phone number, or the original if too short to mask
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }

        String lastFour = phone.substring(phone.length() - 4);
        int maskLength = phone.length() - 4;

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            masked.append('*');
        }
        masked.append(lastFour);

        return masked.toString();
    }
}
