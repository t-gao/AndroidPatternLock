package com.tg.androidpatternlock;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Encrypter {

    public static String convertIntegerArrayToString(ArrayList<Integer> pattern) {
        if (pattern == null) {
            return null;
        }
        StringBuilder patternBuilder = new StringBuilder();
        int size = pattern.size();
        for (int i = 0; i < size; i++) {
            patternBuilder.append(pattern.get(i).intValue());
        }
        String patternStr = patternBuilder.toString();
        return patternStr;
    }

    public static String generateMD5(String message)
            throws HashGenerationException {
        return hashString(message, "MD5");
    }

    public static String generateSHA1(String message)
            throws HashGenerationException {
        return hashString(message, "SHA-1");
    }

    public static String generateSHA256(String message)
            throws HashGenerationException {
        return hashString(message, "SHA-256");
    }

    private static String hashString(String message, String algorithm)
            throws HashGenerationException {

        if (message == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));

            return convertByteArrayToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new HashGenerationException(
                    "Could not generate hash from String", ex);
        }
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString(
                    (arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

    static class HashGenerationException extends Exception {

        /**
         * 
         */
        private static final long serialVersionUID = -5242375518357520154L;

        public HashGenerationException() {
            super();
        }

        public HashGenerationException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public HashGenerationException(String detailMessage) {
            super(detailMessage);
        }

        public HashGenerationException(Throwable throwable) {
            super(throwable);
        }
    }
}
