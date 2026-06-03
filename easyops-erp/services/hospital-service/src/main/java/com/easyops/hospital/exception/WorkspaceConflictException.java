package com.easyops.hospital.exception;

import lombok.Getter;

/**
 * Thrown when a client submits a workspace save with a stale version number,
 * indicating another session has already written a newer version.
 * Results in HTTP 409 Conflict with the current server version in the body.
 */
@Getter
public class WorkspaceConflictException extends RuntimeException {

    private final int serverVersion;

    public WorkspaceConflictException(int serverVersion) {
        super("EP workspace version conflict: server is at version " + serverVersion);
        this.serverVersion = serverVersion;
    }
}
