import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class MessageThreadUI {
    private JFrame frame;
    private JList<MessageThread> threadList;
    private DefaultListModel<MessageThread> listModel;
    private MessageThreadManager messageThreadManager;
    private Map<String, JDialog> openDialogs; // Track open dialogs by topic
    private Map<String, JTextArea> openTextAreas; // Track text areas by topic

    public MessageThreadUI(MessageThreadManager messageThreadManager) {
        this.messageThreadManager = messageThreadManager;
        this.openDialogs = new HashMap<>();
        this.openTextAreas = new HashMap<>();
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Message Threads");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        listModel = new DefaultListModel<>();
        threadList = new JList<>(listModel);
        threadList.setCellRenderer(new ThreadListCellRenderer());
        threadList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    MessageThread selectedThread = threadList.getSelectedValue();
                    if (selectedThread != null) {
                        showMessagesDialog(selectedThread);
                    }
                }
            }
        });
        frame.add(new JScrollPane(threadList), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static class ThreadListCellRenderer extends JLabel implements ListCellRenderer<MessageThread> {
        @Override
        public Component getListCellRendererComponent(JList<? extends MessageThread> list, MessageThread thread,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            String text = thread.getTopic() + " (" + thread.getUnreadMessagesCount() + " unread)";
            setText(text);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setOpaque(true);
            return this;
        }
    }

    public void showMessagesDialog(MessageThread thread) {
        String topic = thread.getTopic();
        JDialog dialog = openDialogs.get(topic);
        JTextArea textArea = openTextAreas.get(topic);

        if (dialog == null) {
            dialog = new JDialog(frame, "Messages for " + topic, false); // Use false for non-modal dialog
            dialog.setSize(400, 300);
            textArea = new JTextArea();
            textArea.setEditable(false);
            dialog.add(new JScrollPane(textArea));

            // Add a window listener to clean up when the dialog is closed
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    openDialogs.remove(topic);
                    openTextAreas.remove(topic);
                }
            });

            openDialogs.put(topic, dialog);
            openTextAreas.put(topic, textArea);
        }

        // Clear and repopulate the text area
        textArea.setText("");
        for (Message message : thread.getMessages()) {
            textArea.append(
                    String.format(
                            "[%s] %s: %s\n",
                            message.getTimestamp(),
                            message.getMessageSenderUserId(),
                            message.getMessage()
                    )
            );
            message.setIsUnread(false);
        }

        // Scroll to the bottom
        textArea.setCaretPosition(textArea.getDocument().getLength());

        if (!dialog.isVisible()) {
            dialog.setVisible(true);
        }
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (MessageThread thread : messageThreadManager.getMessageThreads()) {
                listModel.addElement(thread);
            }
        });
    }

    // Call this method when a new message arrives for a thread
    public void updateOpenDialog(MessageThread thread, Message newMessage) {
        String topic = thread.getTopic();
        JTextArea textArea = openTextAreas.get(topic);
        if (textArea != null) {
            SwingUtilities.invokeLater(() -> {
                textArea.append(
                        String.format(
                                "[%s] %s: %s\n",
                                newMessage.getTimestamp(),
                                newMessage.getMessageSenderUserId(),
                                newMessage.getMessage()
                        )
                );
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }
}
