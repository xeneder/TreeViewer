package com.treeviewer;

import org.apache.commons.io.FileUtils;

import javax.json.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
            switch (jsonObject.getString("type")) {
                case "getFolders":
                    String relativePath = jsonObject.getString("path");
                    relativePath = relativePath.substring(2, relativePath.length());
                    String path = getServletContext().getRealPath("/root/" + relativePath);
                    if (new File(path).listFiles() != null) {
                        for (File f : Objects.requireNonNull(new File(path).listFiles())) {
                            builder.add(factory.createObjectBuilder()
                                    .add("name", f.getName())
                                    .add("type", f.isDirectory() ? "directory" : "file")
                                    .build());
                        }
                    }
                break;
                case "move":
                    String what = getAbsolutePath(jsonObject.getString("what"));
                    String where = getAbsolutePath(jsonObject.getString("where"));
                    Files.move(Paths.get(what), Paths.get(where + "/" + new File(what).getName()),
                            StandardCopyOption.ATOMIC_MOVE);
                break;
                case "delete":
                    String file = getAbsolutePath(jsonObject.getString("what"));
                    File f = new File(file);
                    if (f.isDirectory()) {
                        FileUtils.deleteDirectory(new File(file));
                    } else {
                        Files.delete(Paths.get(file));
                    }
                break;
                case "createFile":
                    String newFile = getAbsolutePath(jsonObject.getString("path") +
                            jsonObject.getString("name"));
                    FileUtils.writeStringToFile(new File(newFile), "", "UTF-8");
                break;
                case "createFolder":
                    String newFolder = getAbsolutePath(jsonObject.getString("path") +
                            jsonObject.getString("name"));
                    Files.createDirectory(Paths.get(newFolder));
                break;
                case "rename":
                    String oldName = getAbsolutePath(jsonObject.getString("path"));
                    String newName;
                    if (new File(oldName).isDirectory()) {
                        newName = oldName.substring(0, oldName.length() - 1);
                        newName = newName.substring(0, newName.lastIndexOf('\\') + 1) +
                                jsonObject.getString("name");
                    } else {
                        newName = oldName.substring(0, oldName.lastIndexOf('\\') + 1) +
                                jsonObject.getString("name");
                    }
                    System.out.println(newName + ' ' + oldName);
                    Files.move(Paths.get(oldName), Paths.get(newName), StandardCopyOption.ATOMIC_MOVE);
                break;
            }
            response.getOutputStream().write(builder.build().toString().getBytes("UTF-8"));
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getAbsolutePath(String relativePath) {
        String absolutePath = relativePath.substring(2, relativePath.length());
        absolutePath = getServletContext().getRealPath("/root/" + absolutePath);
        return absolutePath;
    }
}
