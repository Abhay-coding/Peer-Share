package com.peershare.model;

import java.time.LocalDateTime;

public record DownloadHistory(long id, String fileName, String peerName,
                              LocalDateTime downloadedAt, String status) {
}
