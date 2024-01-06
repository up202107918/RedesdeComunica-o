import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.*;

public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica
    
    // Decoder
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();
    java.util.List commands = Arrays.asList("/nick", "/join", "/leave", "/bye", "/priv");


    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    //criar a Socket para se ligar ao servidor
    static private SocketChannel sc = null;

    // Buffer para enviar dados. O tamanho é o mesmo do servidor
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    // Identificador do cliente
    //static private int id;
    
    // Estado do utilizador
    //static private int state;
    //init: 0; outside: 1;

    // Nome do utilizador 
    // static private String nome;


    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    
    // Construtor
    public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica 
        // * NÃO MODIFICAR *
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
                } 
                catch (IOException ex) {
                } 
                finally {
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
        try { 
            sc = SocketChannel.open(new InetSocketAddress(server, port));
            //sc.connect(new InetSocketAddress(server, port));

            //receber numero de cliente
            buffer.clear();
            sc.read(buffer);
            buffer.flip();
        }
        catch(IOException ie) {
            System.err.println(ie);
            printMessage("Error connecting to server. Please retart program.\n");
        }

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        //try{
            if ((message.charAt(0) == '/')) {
                String notCommand[] = message.split(" ", 2);
                if (!commands.contains(notCommand[0])) {
                    message = "/" + message;
                }
            }
            buffer.clear();
            sc.write(charset.encode(message + '\n'));
    }

    
    private String processResponse(String response) {
        if (response.startsWith("JOINED ")) {
            return responseJoined(response.substring(7));
        }
        if (response.startsWith("LEFT ")) {
            return responseLeft(response.substring(5));
        }
        if (response.startsWith("MESSAGE ")) {
            return responseMessage(response.substring(8).split(" ", 2));
        }
        if (response.startsWith("NEWNICK ")) {
            return responseNewnick(response.substring(8).split(" ", 2));
        }
        if (response.startsWith("PRIVATE ")) {
            return responsePrivate(response.substring(8).split(" ", 2));
        }
        return response;
    }

    private String responseJoined(String nick) {
        return nick + " has joined\n";
    }


    private String responsePrivate(String[] response) {
        try {
            return "(private) " + response[0] + ": " + response[1] + "\n";
        } catch (IndexOutOfBoundsException e) {
            return "(private) " + response[0] + ": \n";
        }
    }

    private String responseNewnick(String[] response) {
        try {
            return response[0] + " has changed to " + response[1] + "\n";
        } catch (IndexOutOfBoundsException e) {
            return response[0] + " has changed to \n";
        }
    }

    private String responseMessage(String[] response) {
        try {
            return response[0] + ": " + response[1] + "\n";
        } catch (IndexOutOfBoundsException e) {
            return response[0] + ": \n";
        }
    }

    private String responseLeft(String nick) {
        return nick + " has left\n";
    }

    // Método principal do objecto
    public void run() throws IOException {
        while (true) {
            try {
                buffer.clear();
                sc.read(buffer);
                buffer.flip();
                printMessage(processResponse(decoder.decode(buffer).toString().trim()) + "\n");

            }
            catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
