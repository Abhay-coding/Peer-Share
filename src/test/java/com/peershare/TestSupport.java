package com.peershare;

public final class TestSupport {
    private TestSupport() {
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void expectIllegalArgument(Runnable action, String expectedMessage) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            assertEquals(expectedMessage, exception.getMessage(), "The validation message must explain the problem.");
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException: " + expectedMessage);
    }
}
