package com.peershare;

public final class AllTests {
    private AllTests() {
    }

    public static void main(String[] args) throws Exception {
        AppConfigTest.run();
        AppLoggerTest.run();
        ModelAndValidationTest.run();
        DatabaseDaoTest.run();
        FileScannerTest.run();
        NetworkTransferTest.run();
        TransferServiceTest.run();
        System.out.println("All tests passed.");
    }
}
