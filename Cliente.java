// FICHEIRO DE TESTE

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.*;


public class Cliente {

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
    static private SocketChannel sc;

    // Buffer para enviar dados. O tamanho é o mesmo do servidor
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }

    // Construtor
    public Cliente(String server, int port) throws IOException {

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
            if(message.charAt(0) == '/'){
                String words[] = message.split(" ", 2);
                if(!commands.contains(words[0])){
                    message = "/" + message;
                }
            }

            // Envia a mensagem
            buffer.clear();
            buffer.put(message.getBytes());
            buffer.flip(); // Como o buffer.put() coloca dados no buffer como buffer.read() é preciso dar flip
            sc.write(buffer);

            // Receber a mensagem do servidor
            // Mensagem do tipo: Texto + " " + estado
            //processInput();
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
        

        try {
            while (true) {
                buffer.clear();
                int bytesRead = sc.read(buffer);
                if (bytesRead > 0) {
                    buffer.flip();
                    String receivedData = decoder.decode(buffer).toString();
                    if (buffer.remaining() == 0) {
                        processInput(receivedData);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //try {
        //    String input;
        //    while (true) {
        //        buffer.clear();
        //        int a = sc.read(buffer);
        //        System.out.println(a);
        //        if(a > 0){
        //            buffer.flip();
        //            input = decoder.decode(buffer).toString();
        //            printMessage(input);
        //        }
        //    }
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
    }

    public void processInput(String serverMessage) throws IOException{
                printMessage(serverMessage + "\n");
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        Cliente client = new Cliente(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
