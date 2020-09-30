package framework;

import java.util.Map;

public class ModelAndView {
    public Map<String,Object> model;
    public String view;
    public ModelAndView(String view){
        this.view = view;
    }

    public ModelAndView(String view, Map<String, Object> map){
        this.view = view;
        this.model = map;
    }
}
