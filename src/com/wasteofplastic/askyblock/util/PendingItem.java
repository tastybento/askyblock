package com.wasteofplastic.askyblock.util;

import java.nio.file.Path;

public class PendingItem {
    /**
     * @return the source
     */
    public Path getSource() {
        return source;
    }

    /**
     * @return the dest
     */
    public Path getDest() {
        return dest;
    }

    private Path source;
    private Path dest;

    public PendingItem(Path source, Path dest) {
        this.source = source;
        this.dest = dest;
    }
}