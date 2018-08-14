package com.treeviewer;

import javax.json.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.StringReader;
import java.util.Objects;

public class Main extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            int length = Integer.parseInt(request.getHeader("content-length"));
            char chars[] = new char[length];
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            in.read(chars, 0, length);

            JsonReader jsonReader = Json.createReader(new StringReader(new String(chars)));
            JsonObject jsonObject = jsonReader.readObject();

            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonArrayBuilder builder = factory.createArrayBuilder();

            if (jsonObject.getString("type").equals("getFolders")) {
                String relativePath = jsonObject.getString("path");
                relativePath = relativePath.substring(2, relativePath.length());
                String path = getServletContext().getRealPath("/root/" + relativePath);
                for (File f : new File(path).listFiles()) {
                    System.out.println(f.toString());
                    builder.add(factory.createObjectBuilder()
                            .add("name", f.getName())
                            .add("type", f.isDirectory() ? "directory" : "file")
                            .build());
                }
            }
            //System.out.println(builder.build().toString());
            response.getOutputStream().write(builder.build().toString().getBytes("UTF-8"));
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
