import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    //criar a Socket para se ligar ao servidor
    static private SocketChannel sc;

    // Buffer para enviar a mensagem para o servido
    static private ByteBuffer buffer;

    // Estado do utilizador
    static private int state;
    //init: 0; outside: 1;



    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        // Abrir uma socket channel para se ligar ao servidor
        try{
            sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(server, port));
            printMessage("Connected to: " + server + ". At port: " + port + "\n");

        }
        catch( IOException ie){
            System.err.println(ie);
            printMessage("Error connecting to server. Please retart program.\n");
        }

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor

        try{
            // Envia a mensagem para o servidor
            buffer = ByteBuffer.wrap(message.getBytes());
            sc.write(buffer);
            buffer.clear();

            // Receber a mensagem do sevidor 
            ByteBuffer receiverBuffer = ByteBuffer.allocate(16384);
            sc.read(receiverBuffer);
            receiverBuffer.flip();
            String serverMessage = new String(receiverBuffer.array()).trim();
            serverMessage+="\n";

            // Imprime a mensagem do servidor para o chatArea
            printMessage(serverMessage);
        }
        catch(IOException ie){
            printMessage("Error sending message to server. Ending connection. Please retart program.\n");
            try{
                sc.close();
            }catch(IOException ie2) { printMessage("Error closing socket.\n"); }
        }
    }

    
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        state = 0;
        while (true) {
            chatBox.requestFocusInWindow();
        }
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
