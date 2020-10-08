package filter;

import framework.ReReadableHttpServletRequest;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
* 上传文件过滤器
* */
@WebFilter("/upload/*")
public class ValidateUploadFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        String digest = request.getHeader("Signature-Method");
        String signature = request.getHeader("Signature");
        if(digest == null || digest.isEmpty() || signature == null || signature.isEmpty()){
            sendErrorPage(response, "Missing signature");
            return;
        }
        //读取request的body并验证签名
        MessageDigest md = getMessageDigest(digest);
        InputStream inputStream = new DigestInputStream(request.getInputStream(), md);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (;;){
            int len = inputStream.read(buffer);
            if(len == -1){
                break;
            }
            outputStream.write(buffer,0,len);
        }
        String actual = toHexString(md.digest());
        if(!signature.equals(actual)){
            sendErrorPage(response, "Invalid signature");
            return;
        }
        filterChain.doFilter(new ReReadableHttpServletRequest(request, outputStream.toByteArray()),response);
    }

    //将byte[]转换成hex String
    private String toHexString(byte[] digest){
        StringBuilder sb = new StringBuilder();
        for(byte b : digest){
            sb.append(String.format("%02x",b));
        }
        return sb.toString();
    }

    private MessageDigest getMessageDigest(String digest) throws ServletException{
        try {
            return  MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException e) {
            throw  new ServletException();
        }
    }
    //发生错误响应
    private void sendErrorPage(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter pw = response.getWriter();
        pw.write("<html><body><h1>");
        pw.write(errorMessage);
        pw.write("</h1></body></html>");
        pw.flush();
    }


}
