package framework;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppListener implements ServletContextListener {
    //清理WebApp 例如在此关闭数据库连接池等
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("WebApp destroyed");
    }
    //初始化WebApp 例如打开数据库连接池等
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        //ServletContext实例在同一个WebAPP中全局只存在一个 可以用于设置和共享配置信息
        System.out.println("WebApp initialized: ServletContext = " + sce.getServletContext());
    }
}
