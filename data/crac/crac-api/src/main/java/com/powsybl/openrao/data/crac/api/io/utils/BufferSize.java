
package com.powsybl.openrao.data.crac.api.io.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Easy buffering Input/Outputstreams
 *
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 *
 */

public final class BufferSize {

    /**
     * Typical buffer size:
     * - Files: MEDIUM - LARGE
     * - Parsing JSONS: SMALL - MEDIUM
     * - Network streams: LARGE
     *
     * Note: Going beyond EXTRA_LARGE rarely helps and probably wastes memory
     */

    public static final BufferSize UNBUFFERED = new BufferSize(-1);
    public static final BufferSize DEFAULT = new BufferSize(0);
    public static final BufferSize SMALL = new BufferSize(8192);
    public static final BufferSize MEDIUM = new BufferSize(16384);
    public static final BufferSize LARGE = new BufferSize(65536);
    public static final BufferSize EXTRA_LARGE = new BufferSize(131072);

    private final int value;

    private BufferSize(int value) {
        this.value = value;
    }

    public static BufferSize custom(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Buffer size must be at least 1");
        }
        return new BufferSize(value);
    }

    public InputStream apply(InputStream is) {
        if (value < 0) {
            return is;
        }
        if (value == 0) {
            return new BufferedInputStream(is);
        }
        return new BufferedInputStream(is, value);
    }

    public OutputStream apply(OutputStream os) {
        if (value < 0) {
            return os;
        }
        if (value == 0) {
            return new BufferedOutputStream(os);
        }
        return new BufferedOutputStream(os, value);
    }

    @Override
    public String toString() {
        return "" + value;
    }

}
