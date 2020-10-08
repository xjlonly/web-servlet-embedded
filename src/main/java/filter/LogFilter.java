package filter;


import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/*
* Filter的顺序并不是固定的 如果一定要给每个Filter指定顺序 必须在web.xml文件中对Filter再配置一遍
* */
@WebFilter("/*")
public class LogFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("LogFilter: process " + ((HttpServletRequest)servletRequest).getRequestURI());
        filterChain.doFilter(servletRequest,servletResponse);
    }
}
