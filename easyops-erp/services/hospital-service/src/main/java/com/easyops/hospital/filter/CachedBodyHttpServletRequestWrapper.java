package com.easyops.hospital.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Caches the request body on construction so it can be read by both the
 * {@link WebhookSignatureFilter} (for HMAC validation) and again by Spring
 * MVC / Jackson (for controller deserialization).
 *
 * <p>Without caching, {@link HttpServletRequest#getInputStream()} can only be
 * consumed once — the second read (by the controller) would return empty bytes.
 */
public class CachedBodyHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /** Returns a defensive copy of the cached body bytes. */
    public byte[] getCachedBody() {
        return Arrays.copyOf(cachedBody, cachedBody.length);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    // -------------------------------------------------------------------------

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final InputStream source;

        CachedBodyServletInputStream(byte[] body) {
            this.source = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            try {
                return source.available() == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async reading is not supported on cached body stream");
        }

        @Override
        public int read() throws IOException {
            return source.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return source.read(b, off, len);
        }
    }
}
