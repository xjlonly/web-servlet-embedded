package framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class  GetDispatcher{
    Object instance;//Controller实例
    Method method;
    String[] parameterNames;
    Class<?>[] parameterClasses;

    public ModelAndView invoke(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException {
        Object[] arguments = new Object[parameterClasses.length];
        for(int i=0; i < parameterClasses.length; i++){
            String parameterName= parameterNames[i];
            Class<?> parameterClass=  parameterClasses[i];

            if(parameterClass == HttpServletRequest.class){
                arguments[i] = req;
            }
            else if(parameterClass == HttpServletResponse.class){
                arguments[i] = resp;
            }
            else if(parameterClass == HttpSession.class){
                arguments[i] = req.getSession();
            }
            else if(parameterClass == int.class){
                arguments[i] = Integer.valueOf(getOrDefault(req, parameterName, "0"));
            }
            else if(parameterClass == double.class){
                arguments[i] = Double.valueOf(getOrDefault(req, parameterName, "0"));
            }
            else if(parameterClass == long.class){
                arguments[i] = Long.valueOf(getOrDefault(req, parameterName, "0"));
            }
            else if(parameterClass == boolean.class){
                arguments[i] = Boolean.valueOf(getOrDefault(req, parameterName, "false"));
            }
            else if(parameterClass == String.class){
                arguments[i] = String.valueOf(getOrDefault(req, parameterName, ""));
            } else {
                throw new RuntimeException("Missing handler for type: " + parameterClass);
            }

        }
        try {
            return (ModelAndView)this.method.invoke(this.instance, arguments);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return  null;
    }

    private String getOrDefault(HttpServletRequest request, String name, String defaulValue){
        String s = request.getParameter(name);
        return  s == null ? defaulValue : s;
    }
}
