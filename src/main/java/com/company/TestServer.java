package com.company;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.*;

public class TestServer extends AbstractHandler{

    static String db, user, pass;
    static{
        JsonObject cred = readCredentials();
        db = cred.get("db").getAsString();
        user  = cred.get("user").getAsString();
        pass  = cred.get("pass").getAsString();
    }

    private static int cnt = 0;

    private void writeImgToFile(BufferedImage img)
    {
        cnt++;
        File outputFile = new File("image" + cnt + ".jpg");
        try {
            ImageIO.write(img, "jpg", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @org.jetbrains.annotations.Nullable
    private BufferedImage decodeB64Image(String imgStr)
    {
        imgStr = imgStr.split(",")[1];
        BufferedImage img;
        byte[] imageBytes;
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            imageBytes = decoder.decodeBuffer(imgStr);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        try {
            img = ImageIO.read(bis);
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return img;
    }

    private static String encodeB64Image(BufferedImage img)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            ImageIO.write(img,"jpg",bos);
            byte[] bytes = bos.toByteArray();
            BASE64Encoder encoder = new BASE64Encoder();
            String b64 = encoder.encode(bytes);
            bos.close();
            System.out.println(b64);
            return b64;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(111);
        return "";
    }

    private static String getImageB64()
    {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("C:\\Users\\ecorona\\Desktop\\p\\BQ4R9318.jpg"));
        } catch (IOException e) {
            System.err.println("Couldn't read image!");
            System.exit(199);
        }

        return encodeB64Image(img);
    }

    public static void main(String[] args) throws Exception{

        saveImagesToDB();
        Server server = new Server(8088);

        server.setHandler(new TestServer());
        server.start();
        server.join();
        System.out.println("Test");
    }

    static private JsonObject getImgSets()
    {
        JsonObject ret = new JsonObject();
        JsonArray names = new JsonArray();

        for(int i = 1; i <= 20; i++)
            names.add("Set_" + i);
        ret.add("sets", names);
        return ret;
    }

    public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        String jsonText = request.getReader().readLine();

        System.out.println("Received: " + jsonText);

        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(jsonText).getAsJsonObject();

        JsonObject retObj = new JsonObject();
        if(!obj.has("requestType") && obj.get("requestType").isJsonPrimitive())
            retObj.addProperty("response", "no 'requestType' field found");
        else {
            switch (obj.get("requestType").getAsString()) {
                case "ping":
                    retObj.addProperty("ping", "backAtYou");
                    break;
                case "getImage":
                    retObj.addProperty("img", getImageB64());
                    break;
                case "saveImages":
                    retObj = saveImages(obj.get("content").getAsJsonObject());
                    break;
                case "imageSets":
                    retObj = getImgSets();
                    break;
                default:
                    System.err.println("Problem: Request Not Supported");
                    System.exit(99);
            }
        }

        String toReturn = retObj.toString();
        System.out.println("Returning: " + toReturn);
        response.getWriter().println(toReturn);
    }

    private ArrayList<BufferedImage> extractBase64Images(JsonArray jImgs)
    {
        ArrayList<BufferedImage> imgs = new ArrayList<>(jImgs.size());
        for(JsonElement img : jImgs)
            imgs.add(decodeB64Image(img.getAsString()));
        return imgs;
    }

    private static JsonObject readCredentials()
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader("json/credentials.json"));
            String jsonText = "";
            while(in.ready())
                jsonText += in.readLine();

            return new JsonParser().parse(jsonText).getAsJsonObject();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("ERROR!: File not found!");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR!: Could not read line from file!");
        }
        return null;
    }

    static void saveImagesToDB() {
//        String setName = json.get("setName").getAsString();
        try {
            System.err.println(db);
            System.err.println(user);
            System.err.println(pass);
            Connection conn = DriverManager.getConnection(db+"?useSSL=false", user, pass);
            Statement stmt = conn.createStatement();
            String sq = "SELECT * FROM imagein.images";
            ResultSet rs = stmt.executeQuery(sq);
            while(rs.next()){
                System.err.println("Found set:" + rs.getString("setName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error when getting a connection to the sql sever");
        }
    }

    private JsonObject saveImages(JsonObject content) {
        JsonObject ret = new JsonObject();
        if(!content.has("images"))
        {
            ret.addProperty("reponse", "content is missing 'images' field");
            return ret;
        }

//        saveImagesToDB(content);
        ArrayList<BufferedImage> imgs = extractBase64Images(content.get("images").getAsJsonArray());
        for(BufferedImage img : imgs)
        {
            System.out.println(img.getWidth() + " x " + img.getHeight());
            writeImgToFile(img);
        }

        return ret;
    }
}
