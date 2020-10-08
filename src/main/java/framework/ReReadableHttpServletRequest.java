package framework;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ReReadableHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] body;
    private boolean open = false;

    public ReReadableHttpServletRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if(open){
            throw new IllegalStateException("Cannot re-open input stream!");
        }
         open = true;
        return  new ServletInputStream() {
            private int offset = 0;
            @Override
            public boolean isFinished() {
                return offset >= body.length;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                if(offset >= body.length){
                    return  -1;
                }
                int n = body[offset] & 0xff;
                offset++;
                return n;
            }
        };
    }

    public BufferedReader getReader() throws IOException{
        if(open){
            throw  new IllegalStateException("Cannot re-open reader");
        }
        open = true;
        return  new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
