package client.referendum.tests;

import client.referendum.Connection;
import client.referendum.Message;

import java.io.IOException;

public class fakeConnection implements Connection {

    private final Message response; // ce qui doit renvoyer
    private final boolean failOnSend; // simule un éche de connection au réseau
    public boolean sendCalled = false; // vérifie la connection
    public Message lastSent = null;

    public fakeConnection(Message response) {
        this(response, false);
    }

    public fakeConnection(Message response, boolean failOnSend) {
        this.response = response;
        this.failOnSend = failOnSend;
    }

    @Override
    public void send(Message m) throws IOException {
        sendCalled = true;
        if (failOnSend) throw new IOException("Réseau coupé (simulé)");
    }

    @Override
    public Message receive(int timeoutMs) {
        return response;
    }

    @Override
    public void close() { }
}