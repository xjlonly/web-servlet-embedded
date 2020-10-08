package filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("AuthFilter: check authentication");
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        if(!request.getRequestURI().equals("embedded/user/sign") && request.getSession().getAttribute("user") == null){
            System.out.println("AuthFilter: not sign");
            response.sendRedirect("/embedded/user/sign");
        }else{
            filterChain.doFilter(request,response);
        }
    }
}
