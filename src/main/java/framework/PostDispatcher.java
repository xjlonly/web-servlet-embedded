package framework;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PostDispatcher extends AbstractDispatcher{
    Object instance;//Controller实例
    Method method;
    Class<?>[] parameterClasses;
    ObjectMapper objectMapper;

    public PostDispatcher(Object instance, Method method, Class<?>[] parameterClasses, ObjectMapper objectMapper) {
        super();
        this.instance = instance;
        this.method = method;
        this.parameterClasses = parameterClasses;
        this.objectMapper = objectMapper;
    }

    @Override
    public ModelAndView invoke(HttpServletRequest req, HttpServletResponse resp) throws IllegalAccessException, IOException, InvocationTargetException {
        Object[] arguments = new Object[parameterClasses.length];
        for(int i= 0; i < parameterClasses.length; i++){
            Class<?> parameterClass = parameterClasses[i];
            if(parameterClass == HttpServletRequest.class){
                arguments[i] = req;
            }
            else if(parameterClass == HttpServletResponse.class){
                arguments[i] = resp;
            }
            else if(parameterClass == HttpSession.class){
                arguments[i] = req.getSession();
            }
            else{
                var reader = req.getReader();
                arguments[i] = this.objectMapper.readValue(reader, parameterClass);
            }
        }
        return  (ModelAndView)this.method.invoke(instance, arguments);
    }
}