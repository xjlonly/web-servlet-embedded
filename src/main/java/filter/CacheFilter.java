package filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
* 需要修改响应的场景
*对于某些请求，由于业务逻辑比较复杂，处理该请求将耗费很长时间
*每次响应的内容是固定的
*
*想要实现的效果
*通过Filter读取该请求
*第一次接到该请求的时候，正常送给对应的Servlet处理
*在filter中，将处理完后的HttpServletResponse中的内容读取出来
*由于响应内容不变，因此，存入缓存
*再一次接收到相同的请求时
*根据请求的路径，查找对应的缓存
*读出缓存，直接缓存写入该浏览器请求的ServletResponse中，完成该http请求
*因此，不用送给对应的Servlet处理，大大提高Web应用程序运行效率
*
*存在的问题
*在Filter中，如果没找到缓存的内容，需要送给对应的Servlet处理
*如果将Filter获取的原始HttpServletResponse送给了下一级对用的Servlet的话，就无法获取下游组件写入响应的内容了
*
*解决办法
*使用自己的CachedHttpServletResponse
*在第一次接收到该http请求的时候，并不传入原始的HttpServletResponse
*而是传入自己构造的 伪造的 HttpServletResponse （CachedHttpServletResponse）
*所以，可以在下游组件处理完成后，读取出响应的内容，并存入缓存中
*下一次再遇到相同的请求，就将不在送给下游组件处理，而是直接返回缓存
* */
@WebFilter("/slow/*")
public class CacheFilter implements Filter {

    //Path到byte[]的缓存
    private Map<String, byte[]> cache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        String url = request.getRequestURI();
        byte[] data = this.cache.get(url);
        response.setHeader("X-Cache-Hit", data == null ? "No" : "Yes");
        if(data == null){
            //缓存未找到 构造一个伪造的Response
            CachedHttpServletResponse wrapper = new CachedHttpServletResponse(response);
            //让下游组件写入数据到伪造的Response中；
            filterChain.doFilter(request, wrapper);
            //从伪造的Response中读取写入的内容并放入缓存
            data = wrapper.getContent();
            cache.put(url,data);
        }

        //写入到原始Response;
        ServletOutputStream output = response.getOutputStream();
        output.write(data);
        output.flush();
    }
}


class CachedHttpServletResponse extends HttpServletResponseWrapper{
    private boolean open = false;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public CachedHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if(open){
            throw new IllegalStateException("Cannot re-open output stream");
        }

        open = true;
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            //不写入socket 实际写入我们定义的流中
            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if(open){
            throw new IllegalStateException("Cannot re-open writer");
        }
        open = true;
        return new PrintWriter(outputStream, false, StandardCharsets.UTF_8);
    }

    //返回写入的byte[]
    public byte[] getContent() {
        return  outputStream.toByteArray();
    }
}
