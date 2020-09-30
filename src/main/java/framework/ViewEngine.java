package framework;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ServletLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class ViewEngine {

    private final PebbleEngine engine;
    public ViewEngine(ServletContext context){

        //初始化pebble引擎
        //定义一个ServletLoader 用于加载模板
        ServletLoader loader = new ServletLoader(context);
        loader.setCharset("utf-8");
        // 模板前缀，这里默认模板必须放在`/WEB-INF/templates`目录:
        loader.setPrefix("WEB-INF/templates");
        // 模板后缀:
        loader.setSuffix("");
        //创建Pebble 引擎实例:
        engine = new PebbleEngine.Builder()
                .autoEscaping(true)// 默认打开HTML字符转义，防止XSS攻击
                .cacheActive(false)// 禁用缓存使得每次修改模板可以立刻看到效果
                .loader(loader)
                .build();
    }

    //通过model和输出渲染相应的模板
    public void render(ModelAndView mv, Writer writer) throws IOException {
        String view = mv.view;
        Map<String, Object> map = mv.model;
        //根据模板渲染html
        PebbleTemplate template = this.engine.getTemplate(mv.view);
        template.evaluate(writer, map);
    }
}


