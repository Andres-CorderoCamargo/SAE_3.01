package client.auth.java;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private final MessageType type;
    private final Map<String, String> fields = new HashMap<>();

    public Message(MessageType type) {
        this.type = type;
    }

    // Surcharge pour accepter un string
    public void add(String key, String value) {
        if (value == null) {
            fields.put(key, null);
        } else {
            String s = new String(value);
            fields.put(key, s);
            // On peut effacer le tableau temporaire si nécessaire, 
            // mais l'appelant s'en charge souvent via finally.
        }
    }
    public String get(String key) {
        return fields.get(key); 
    }

    public MessageType getType() {
        return type; 
    }
}