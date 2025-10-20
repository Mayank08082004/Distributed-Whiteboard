package com.example.whiteboard.server.cluster;

import java.io.*;
import java.util.Base64;

public class SerializationUtils {

    // Convert a Serializable object to a Base64 string
    public static String serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // Convert a Base64 string back to an object
    public static Object deserialize(String base64String) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64String);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }
}