package com.peershare.model;

import java.time.LocalDateTime;

public record SharedFile(long id, String fileName, long fileSize, LocalDateTime lastModified) {
}
