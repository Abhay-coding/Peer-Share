// Source: src\main\java\com\peershare\util\InputValidator.java (lines 31-45)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: Path-Traversal & Reserved Name Validation

    public static boolean isSafeFileName(String fileName) {
        return fileName != null
                && !fileName.isBlank()
                && !fileName.equals(".")
                && !fileName.equals("..")
                && !fileName.contains("/")
                && !fileName.contains("\\")
                && !fileName.contains("|")
                && !fileName.contains(":")
                && !fileName.matches(".*[<>\"?*].*")
                && !fileName.matches("(?i)^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\\..*)?$")
                && !fileName.endsWith(" ")
                && !fileName.endsWith(".")
                && fileName.chars().noneMatch(c -> c < 32 || c == 127);
    }
