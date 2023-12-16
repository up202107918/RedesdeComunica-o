import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer
{
    // A pre-allocated buffer for the received data
    static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

    // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();

    // Lista para guardar nomes de utilizador
    static private final List<String> nomes = new ArrayList<String>();


    static public void main( String args[] ) throws Exception {
        // Parse port from command line
        int port = Integer.parseInt( args[0] );
        int clientCounter = 0;

        try {
            // Instead of creating a ServerSocket, create a ServerSocketChannel
            ServerSocketChannel ssc = ServerSocketChannel.open();

            // Set it to non-blocking, so we can use select
            ssc.configureBlocking( false );

            // Get the Socket connected to this channel, and bind it to the
            // listening port
            ServerSocket ss = ssc.socket();
            InetSocketAddress isa = new InetSocketAddress( port );
            ss.bind( isa );

            // Create a new Selector for selecting
            Selector selector = Selector.open();

            // Register the ServerSocketChannel, so we can listen for incoming
            // connections
            ssc.register( selector, SelectionKey.OP_ACCEPT );
            System.out.println( "Listening on port "+port );

            while (true) {
                // See if we've had any activity -- either an incoming connection,
                // or incoming data on an existing connection
                int num = selector.select();

                // If we don't have any activity, loop around and wait again
                if (num == 0) {
                    continue;
                }

                // Get the keys corresponding to the activity that has been
                // detected, and process them one by one
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    // Get a key representing one of bits of I/O activity
                    SelectionKey key = it.next();

                    // What kind of activity is it?
                    if (key.isAcceptable()) {

                        // It's an incoming connection.  Register this socket with
                        // the Selector so we can listen for input on it
                        Socket s = ss.accept();
                        System.out.println( "Got connection from "+s );

                        // Make sure to make it non-blocking, so we can use a selector
                        // on it.
                        SocketChannel sc = s.getChannel();
                        sc.configureBlocking( false );

                        // Register it with the selector, for reading
                        sc.register( selector, SelectionKey.OP_READ );

                        clientCounter ++;
                        buffer.clear();
                        buffer.put(("Client nº: " + clientCounter).getBytes());
                        buffer.flip();
                        sc.write(buffer);

                    } else if (key.isReadable()) {

                        SocketChannel sc = null;

                        try {

                            // It's incoming data on a connection -- process it
                            sc = (SocketChannel)key.channel();
                            boolean ok = processInput( sc, clientCounter );

                            // If the connection is dead, remove it from the selector
                            // and close it
                            if (!ok) {
                                key.cancel();

                                Socket s = null;
                                try {
                                    s = sc.socket();
                                    System.out.println( "Closing connection to "+s );
                                    clientCounter --;
                                    s.close();
                                } catch( IOException ie ) {
                                    System.err.println( "Error closing socket "+s+": "+ie );
                                }
                            }

                        } catch( IOException ie ) {

                            // On exception, remove this channel from the selector
                            key.cancel();

                            try {
                                sc.close();
                            } catch( IOException ie2 ) { System.out.println( ie2 ); }

                            System.out.println( "Closed " + sc );
                        }
                    }
                }
                // We remove the selected keys, because we've dealt with them.
                keys.clear();
            }
        } catch( IOException ie ) {
            System.err.println( ie );
        }
    }


    // Lê a mensagem recebida e imprime no stdout, bem como volta a enviar para o cliente
    static private boolean processInput( SocketChannel sc, int clientCounter ) throws IOException {
        // Read the message to the buffer
        buffer.clear();
        sc.read( buffer );
        buffer.flip();

        // If no data, close the connection
        if (buffer.limit()==0) {
            return false;
        }

        // Decode and print the message to stdout
        String message = decoder.decode(buffer).toString().trim(); //message = texto + estado + id
        System.out.println( message );

        // Dividir a mensagem por palavras
        String[] messageWords = message.split(" ");
        int estado = Integer.parseInt(messageWords[messageWords.length - 2]);
        int id = Integer.parseInt(messageWords[messageWords.length - 1]);

        // Processar mensagem to utilizador
        message = processMessage(messageWords, estado, id);

        // Caso o cliente tenha dado o comando bye
        if(Integer.parseInt(message.split(" ")[message.split(" ").length - 1]) == -1){
            clientCounter --;
            if(nomes.size() != 0)
                nomes.remove(id-1);
        }


        // Clear the buffer before writing the message back to the client
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        // Send the data back in the socket
        sc.write(buffer);

        return true;
    }

    static boolean disponivel(String nome){
        for(String s : nomes){
            if(s.equals(nome))
                return false;
        }
        return true;
    }

    static String processMessage(String[] messageWords, int estado, int id){

        String message = "";
        String command = messageWords[0];

        // Comandos
        if(command.equals("/nick")){
            for(int i = 1; i < messageWords.length - 2; i++){
                message += messageWords[i] + " ";
            }
            if(disponivel(message)){
                if(estado == 0){
                    nomes.add(message);
                    message += "joined! ";
                    estado = 1;
                }
                else{
                    System.out.println(nomes.size());
                    String nome_antigo = nomes.get(id-1);
                    nomes.add(id-1, message);
                    message = "NEWNICK " + nome_antigo + message + " ";
                }
            }
            else
                message = "Name not available ";
        }

        else if(command.equals("/join")){
            for(int i = 1; i < messageWords.length - 2; i++){
                message += messageWords[i] + " ";
            }
            if(estado == 1){}
            else if(estado  == 2){}
        }

        else if(command.equals("/leave")){}

        else if(command.equals("/bye"))
            estado = -1;

        else{
            for(int i = 0; i < messageWords.length - 2; i++){
                message += messageWords[i] + " ";
            }
        }

        return message + estado;
    }
}
