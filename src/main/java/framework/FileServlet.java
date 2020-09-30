package framework;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.plaf.synth.SynthDesktopIconUI;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet(urlPatterns = {"/favicon.ico","/static/*"})
public class FileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        var context = req.getServletContext();
        System.out.println("getContextPath:" + context.getContextPath());
        System.out.println("getRequestURI:"+ req.getRequestURI());
        System.out.println("getRequestURL:"+ req.getRequestURL());
        System.out.println("getPathInfo:"+ req.getPathInfo());
        String urlPath = req.getRequestURI().substring(context.getContextPath().length());
        String filepath = context.getRealPath(urlPath);
        if(filepath == null){
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        var path = Paths.get(filepath);
        if(!path.toFile().isFile()){
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mime = Files.probeContentType(path);
        if(mime == null){
            mime = "application/octet-stream";
        }

        resp.setContentType(mime);

        OutputStream output = resp.getOutputStream();
        try(InputStream input = new BufferedInputStream(new FileInputStream(filepath))){
            input.transferTo(output);
        }

        output.flush();
    }
}
