package controller;

import framework.ModelAndView;
import bean.User;
import framework.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.Map;


public class UserController {

    @GetMapping("/embedded/user/profile")
    public ModelAndView profile(HttpServletResponse response, HttpSession session, int a) throws IOException {
        User user = (User) session.getAttribute("user");
        if(user == null){
            return new ModelAndView("redirect:/embedded/user/sign");
        }
        if(!user.isManager()){
            response.sendError(403);
            return  null;
        }

        return  new ModelAndView("profile.html", Map.of("user", user));
    }

    @GetMapping("/embedded/user/sign")
    public ModelAndView sign(HttpServletRequest request, HttpServletResponse response, HttpSession session, String username, String password){
        User user = new User();
        user.Name = username;
        user.UserId = 1;

        request.removeAttribute("user");
        request.getSession().setAttribute("user", user);

        return  new ModelAndView("redirect:/embedded/user/index?id=1",Map.of("user",user));
    }

    @GetMapping("/embedded/user/index")
    public ModelAndView index(HttpServletRequest request, HttpServletResponse response, HttpSession session,int id) throws IOException{
        User user = (User) session.getAttribute("user");
        if(user == null){
            return new ModelAndView("redirect:/embedded/user/sign");
        }
        if(!user.isManager()){
            response.sendError(403);
            return  null;
        }

        return  new ModelAndView("index.html", Map.of("user", user));
    }
}
