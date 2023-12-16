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

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

    //criar a Socket para se ligar ao servidor
    static private SocketChannel sc;

    // Buffer para enviar dados. O tamanho é o mesmo do servidor
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    // Identificador do cliente
    static private int id;
    
    // Estado do utilizador
    static private int state;
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

            //receber numero de cliente
            buffer.clear();
            sc.read(buffer);
            buffer.flip();

            // Imprimir numero de cliente na gui
            String serverMessage = decoder.decode(buffer).toString().trim();
            String[] serverWords = serverMessage.split(" ");
            id = Integer.parseInt(serverWords[serverWords.length - 1]);
            serverMessage+="\n";

            // Imprime a mensagem do servidor para o chatArea
            printMessage(serverMessage);
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
            // Envia a mensagem, com o estado e o id, para o servidor
            //buffer = ByteBuffer.wrap(message.getBytes());
            message += " " + state + " " + id;
            buffer.clear();
            buffer.put(message.getBytes());
            buffer.flip(); // Como o buffer.put() coloca dados no buffer como buffer.read() é preciso dar flip
            sc.write(buffer);

            // Receber a mensagem do servidor
            // Mensagem do tipo: Texto + " " + estado
            buffer.clear();
            sc.read(buffer);
            buffer.flip();
            String serverMessage = decoder.decode(buffer).toString().trim();

            // Processar resposta
            String[] serverMessageWords = serverMessage.split(" ");
            state = Integer.parseInt(serverMessageWords[serverMessageWords.length - 1]);
            if(state == -1){
                printMessage("Logged off. Good bye");
                try{
                    sc.close();
                }catch(IOException ie2) { printMessage("Error closing socket.\n"); }
            }
            serverMessage = "";
            for(int i = 0; i < serverMessageWords.length - 1; i++)
                serverMessage += serverMessageWords[i] + " ";

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
        catch (Exception e) {
            // Handle other exceptions
            printMessage("An unexpected error occurred. Ending connection. Please restart the program.\n");
            try {
                sc.close();
            } catch (IOException e2) {
                printMessage("Error closing socket.\n");
            }
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
