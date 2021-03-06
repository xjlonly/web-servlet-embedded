package framework;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@WebServlet(urlPatterns = "/")
public class DispatcherServlet extends HttpServlet {
    private Map<String,GetDispatcher> getMappings = new HashMap<>();
    private Map<String,PostDispatcher> postMappings = new HashMap<>();
    private ViewEngine engine = null;
    private static final Set<Class<?>> supportedGetParameterTypes = Set.of(int.class, long.class, boolean.class,
            String.class, HttpServletRequest.class, HttpServletResponse.class, HttpSession.class);
    private static final Set<Class<?>> supportedPostParameterTypes = Set.of(HttpServletResponse.class, HttpServletRequest.class, HttpSession.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req,resp,this.getMappings);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req,resp,this.postMappings);
    }
    private void process(HttpServletRequest req, HttpServletResponse resp,
                         Map<String, ? extends AbstractDispatcher> dispatcherMap) throws ServletException, IOException{
            resp.setContentType("text/html");
            resp.setCharacterEncoding("utf-8");
            String path = req.getRequestURI().substring(req.getContextPath().length());
            GetDispatcher dispatcher = this.getMappings.get(path);
            if(dispatcher == null){
                resp.setStatus(404);
                resp.sendError(404);
                return;
            }
            ModelAndView mv = null;
            try {
                mv = dispatcher.invoke(req, resp);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            if(mv == null){
                return;
            }
            if(mv.view.startsWith("redirect:")){
                resp.sendRedirect(mv.view.substring(9));
                return;
            }
            PrintWriter pw = resp.getWriter();
            //通过引擎渲染模板
            this.engine.render(mv, pw);
            pw.flush();
    }

    @Override
    public void init() throws ServletException {
        try {
            Set<Class<?>> classes = getClasses("controller");
            this.getMappings = scanGetInController(classes);
            this.postMappings = scanPostInController(classes);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        this.engine = new ViewEngine(getServletContext());
    }

    //扫描所有GetMapping的方法
    private Map<String, GetDispatcher> scanGetInController(Set<Class<?>> classes){
        Map<String, GetDispatcher> stringGetDispatcherMap = new HashMap<>();
        try{
            for(Class<?> clazz : classes){
                Method[] methods = clazz.getDeclaredMethods();
                for(Method method : methods){
//                    Annotation[] annotations = method.getDeclaredAnnotations();
//                    for(Annotation annotation : annotations){
//                        if(annotation instanceof GetMapping){
//                            GetDispatcher dispatcher = new GetDispatcher();
//                            dispatcher.instance = clazz.getConstructor().newInstance();
//                            dispatcher.method = method;
//                            dispatcher.parameterNames = Arrays.stream(method.getParameters())
//                                    .map(Parameter::getName).toArray(String[]::new);
//                            dispatcher.parameterClasses = Arrays.stream(method.getParameters())
//                                    .map(Parameter::getType).toArray(Class<?>[]::new);
//                            String path = ((GetMapping) annotation).value();
//                            stringGetDispatcherMap.put(path,dispatcher);
//                        }
//                    }
                    if(method.getAnnotation(GetMapping.class) != null){
                        if(method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class){
                            throw new UnsupportedOperationException("Unsupported return type: " + method.getReturnType() +
                                    " for method:" + method);
                        }
                        for(Class<?> parameterClass : method.getParameterTypes()){
                            if(!supportedGetParameterTypes.contains(parameterClass)){
                                throw new UnsupportedOperationException(
                                        "Unsupported parameter types: " + parameterClass + " for method: " + method);
                            }
                        }
                        String[] parameterNames = Arrays.stream(method.getParameters()).map(Parameter::getName)
                                .toArray(String[]::new);
                        String path = method.getAnnotation(GetMapping.class).value();
                        stringGetDispatcherMap.put(path,
                                new GetDispatcher(clazz.getConstructor().newInstance(),
                                        method,parameterNames,method.getParameterTypes()));
                    }
                }
            }
        }
        catch (Exception e){
           e.printStackTrace();
        }
        return stringGetDispatcherMap;
    }

    private Map<String, PostDispatcher> scanPostInController(Set<Class<?>> classes)
            throws IllegalArgumentException, ReflectiveOperationException{
        Map<String, PostDispatcher> stringPostDispatcherMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for(Class<?> clazz : classes){
            Method[] methods = clazz.getDeclaredMethods();
            for(Method method : methods){
                if(method.getAnnotation(PostMapping.class) != null){
                    if(method.getReturnType() != ModelAndView.class && method.getReturnType() != void.class){
                        throw new UnsupportedOperationException("Unsupported return type: " + method.getReturnType() +
                                " for method:" + method);
                    }
                    Class<?> requestBodyClass = null;
                    for(Class<?> parameterClass : method.getParameterTypes()){
                        if(!supportedPostParameterTypes.contains(parameterClass)){
                            if(requestBodyClass == null){
                                requestBodyClass = parameterClass;
                            }else{
                                throw new UnsupportedOperationException(
                                        "Unsupported parameter types: " + parameterClass + " for method: " + method);
                            }
                        }
                    }
                    String path = method.getAnnotation(GetMapping.class).value();
                    stringPostDispatcherMap.put(path,
                            new PostDispatcher(clazz.getConstructor().newInstance(),method,
                                    method.getParameterTypes(),objectMapper));
                }
            }
        }
        return stringPostDispatcherMap;
    }

    private  Set<Class<?>> getClasses(String pack) {

        // 第一个class类的集合
        Set<Class<?>> classes = new LinkedHashSet<>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    System.err.println("file类型的扫描");
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件
                    // 定义一个JarFile
                    // System.err.println("jar类型的扫描");
                    JarFile jar;
                    try {
                        // 获取jar
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        // 从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        // 同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            // 如果是以/开头的
                            if (name.charAt(0) == '/') {
                                // 获取后面的字符串
                                name = name.substring(1);
                            }
                            // 如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                // 如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    // 获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                // 如果可以迭代下去 并且是一个包
                                if ((idx != -1) || recursive) {
                                    // 如果是一个.class文件 而且不是目录
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        // 去掉后面的".class" 获取真正的类名
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            // 添加到classes
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        // log.error("在扫描用户定义视图时从jar包获取文件出错");
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }

    private  void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive,
                                                        Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            // log.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    // classes.add(Class.forName(packageName + '.' + className));
                    // 经过回复同学的提醒，这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    classes.add(
                            Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                    e.printStackTrace();
                }
            }
        }
    }

}





