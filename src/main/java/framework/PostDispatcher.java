package framework;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PostDispatcher{
    Object instance;//Controller实例
    Method method;
    Class<?>[] parameterClasses;
    ObjectMapper objecMapper;

    public ModelAndView invoke(HttpServletRequest req, HttpServletResponse resp) throws IllegalAccessException, IOException, InvocationTargetException {
        Object[] argsment = new Object[parameterClasses.length];
        for(int i= 0; i < parameterClasses.length; i++){
            Class<?> parameterClass = parameterClasses[i];
            if(parameterClass == HttpServletRequest.class){
                argsment[i] = req;
            }
            else if(parameterClass == HttpServletResponse.class){
                argsment[i] = resp;
            }
            else if(parameterClass == HttpSession.class){
                argsment[i] = req.getSession();
            }
            else{
                var reader = req.getReader();
                argsment[i] = this.objecMapper.readValue(reader, parameterClass);
            }
        }
        return  (ModelAndView)this.method.invoke(instance, argsment);
    }
}