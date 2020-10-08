package framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractDispatcher {
    public abstract ModelAndView invoke(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ReflectiveOperationException;
}
