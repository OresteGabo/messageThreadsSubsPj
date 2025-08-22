import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.Timestamp;
import org.eclipse.paho.client.mqttv3.MqttMessage;


class MessageThreadManager {
    private List<MessageThread> messageThreads = new ArrayList<>();
    private MessageThreadUI ui;

    public List<MessageThread> getMessageThreads() {
        return this.messageThreads;
    }

    public void addMessageThread(MessageThread messageThread) {
        this.messageThreads.add(messageThread);
    }

    public void removeMessageThread(MessageThread messageThread) {
        this.messageThreads.remove(messageThread);
    }

    public void setUI(MessageThreadUI ui) {
        this.ui = ui;
    }

    void addMessage(MqttMessage mqttMessage, String topic) {
        Message newMessage = new Message(mqttMessage);
        boolean topicExists = false;
        for (MessageThread messageThread : messageThreads) {
            if (messageThread.getTopic().equals(topic)) {
                topicExists = true;
                messageThread.addMessage(newMessage);
                if (ui != null) {
                    ui.updateOpenDialog(messageThread, newMessage); // Update open dialog
                }
                break;
            }
        }
        if (!topicExists) {
            MessageThread newMessageThread = new MessageThread();
            newMessageThread.setTopic(topic);
            newMessageThread.addMessage(newMessage);
            messageThreads.add(newMessageThread);
        }
        if (ui != null) {
            ui.refresh();
        }
    }

}


public class Main {
    public static void main(String[] args) throws MqttException, InterruptedException {
        MqttClient client = new MqttClient("ssl://broker.hivemq.com:8883", "subss");
        MessageThreadManager messageThreadManager = new MessageThreadManager();
        MessageThreadUI ui = new MessageThreadUI(messageThreadManager);
        messageThreadManager.setUI(ui);

        client.connect();
        client.subscribe("schoolbridge/#");
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                System.out.println("Message arrived on topic: " + topic);
                System.out.println("Message payload: " + new String(message.getPayload()));
                messageThreadManager.addMessage(message, topic);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Not used for subscribers
            }
        });

        System.out.println("Subscribed to all topics. Waiting for messages...");
        while (true) {
            Thread.sleep(1000);
        }
    }
}




class Message {
    private String messageSenderUserId;
    private String messageContent;
    private Timestamp timestamp;
    private Boolean isUnread;

    public Message(MqttMessage mqttMessage) {
        try {
            String payload = new String(mqttMessage.getPayload());
            JSONObject json = new JSONObject(payload);
            this.messageSenderUserId = json.optString("sender", "unknown");
            this.messageContent = json.optString("message", payload);
        } catch (Exception e) {
            this.messageSenderUserId = "unknown";
            this.messageContent = new String(mqttMessage.getPayload());
        }
        this.timestamp = new Timestamp(new Date().getTime());
        this.isUnread = true;
    }

    public String getMessage() {
        return this.messageContent;
    }

    public String getMessageSenderUserId() {
        return this.messageSenderUserId;
    }

    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public Boolean getIsUnread() {
        return this.isUnread;
    }

    public void setIsUnread(Boolean isUnread) {
        this.isUnread = isUnread;
    }
}



/**
 * Represents a thread of messages associated with a specific topic.
 * A MessageThread keeps track of multiple Message objects
 * and provides methods to interact with the messages in the thread.
 */


class MessageThread {
    private List<Message> messages = new ArrayList<>();
    private String topic;

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    public Message getLatestMessage() {
        if (this.messages.isEmpty()) {
            return null;
        }
        return this.messages.get(this.messages.size() - 1);
    }

    public int getUnreadMessagesCount() {
        int unreadMessagesCount = 0;
        for (Message message : this.messages) {
            if (message.getIsUnread()) {
                unreadMessagesCount++;
            }
        }
        return unreadMessagesCount;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
