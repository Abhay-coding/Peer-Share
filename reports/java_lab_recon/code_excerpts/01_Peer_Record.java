// Source: src\main\java\com\peershare\model\Peer.java (lines 1-4)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: Java 17 Record

package com.peershare.model;

public record Peer(long id, String name, String ipAddress, int port) {
}
