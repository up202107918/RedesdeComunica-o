import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User {
    String nome;
    SocketChannel sc;
    State estado;
    String room;

    User(String nome, SocketChannel sc) {
        this.nome = nome;
        this.sc = sc;
        this.estado = State.INIT;
        this.room = null;
    }
}

class Sala {
    String nome;
    List<String> RoomUsers;

    Sala(String nome) {
        this.nome = nome;
        this.RoomUsers = new ArrayList<String>();
    }

    final void updateRoom(String s) {
        this.RoomUsers.add(s);
    }

    final void removeFromRoom(String s) {
        this.RoomUsers.remove(s);
    }
}

enum State {
    INIT,INSIDE,OUTSIDE;
}

public class ChatServer
{
    // A pre-allocated buffer for the received data
    static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

    // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();

    // Lista para guardar nomes de utilizador
    static private final List<String> nomes = new ArrayList<>();
    static private final Map<String, User> userMap = new HashMap<>();
    static private final Map<String, Sala> salaMap = new HashMap<>();

    static public void main(String args[]) throws Exception {
        // Parse port from command line
        int port = Integer.parseInt(args[0]);
        //int clientCounter = 0;

        try {
            // Instead of creating a ServerSocket, create a ServerSocketChannel
            ServerSocketChannel ssc = ServerSocketChannel.open();

            // Set it to non-blocking, so we can use select
            ssc.configureBlocking(false);

            // Get the Socket connected to this channel, and bind it to the
            // listening port
            ServerSocket ss = ssc.socket();
            InetSocketAddress isa = new InetSocketAddress(port);
            ss.bind(isa);

            // Create a new Selector for selecting
            Selector selector = Selector.open();

            // Register the ServerSocketChannel, so we can listen for incoming
            // connections
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on port "+ port);

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
                        sc.register(selector, SelectionKey.OP_READ, new User("",sc));

                        //clientCounter ++;
                        //buffer.clear();
                        //buffer.put(("Client nº: " + clientCounter).getBytes());
                        //buffer.flip();
                        //sc.write(buffer);

                    } 
                    else if (key.isReadable()) {
                        SocketChannel sc = null;
                        try {
                            // It's incoming data on a connection -- process it
                            sc = (SocketChannel)key.channel();
                            boolean ok = processInput(sc, selector, key);

                            // If the connection is dead, remove it from the selector
                            // and close it
                            if (!ok) {
                                key.cancel();
                                Socket s = null;
                                processBye(sc, key, true);
                                closeConnection(s, sc);
                                /*try {
                                    s = sc.socket();
                                    System.out.println( "Closing connection to "+s );
                                    //clientCounter --;
                                    s.close();
                                } catch(IOException ie) {
                                    System.err.println( "Error closing socket "+s+": "+ie );
                                }*/
                            }

                        } catch( IOException ie ) {

                            // On exception, remove this channel from the selector
                            key.cancel();

                            try {
                                processBye(sc, key, true);
                                sc.close();
                            } 
                            catch(IOException ie2) { 
                                System.out.println( ie2 ); 
                            }

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
    static private boolean processInput( SocketChannel sc, Selector select, SelectionKey keysource) throws IOException {
        // Read the message to the buffer
        buffer.clear();
        sc.read(buffer);
        buffer.flip();

        // If no data, close the connection
        if (buffer.limit()==0) {
            return false;
        }

        // Decode and print the message to stdout
        String message = decoder.decode(buffer).toString();
        Set<Integer> newset = new HashSet<>();
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '\n') {
                newset.add(i+1);
            }
        }
 
        // Dividir a mensagem por palavras
        message = message.replaceAll("[\\n\\t]", " ").substring(0, message.length() - 1);
        String[] messageWords = message.split(" ");
        String msg = "";
        int charCount = -1;
        for (int i = 0; i < messageWords.length; i++) {
            switch (messageWords[i]) {
                case "/nick":
                    if (i == messageWords.length - 1) {
                        errorMessage(sc);
                        break;
                    }
                    processMessage(sc, messageWords[i], keysource, messageWords[i+1], "");
                    charCount += messageWords[i].length() + 1;
                    charCount += messageWords[i + 1].length() + 1;
                    i++;
                    break;
                case "/join":
                    if (i == messageWords.length - 1) {
                        errorMessage(sc);
                        break;
                    }
                    processMessage(sc, messageWords[i], keysource, messageWords[i+1], "");
                    charCount += messageWords[i].length() + 1;
                    charCount += messageWords[i + 1].length() + 1;
                    i++;
                    break;
                case "/priv":
                    if ((i == messageWords.length - 1) || (i == messageWords.length - 2)) {
                        errorMessage(sc);
                        break;
                    }
                    charCount += messageWords[i].length() + messageWords[i + 1].length() + 2;
                    int words = 0;
                    for (int j = i + 2; j < messageWords.length; j++, words++) {
                        if (!newset.contains(charCount + 1) || messageWords[j].charAt(0) != '/') {
                            if (!msg.isEmpty())
                                msg += " " + messageWords[j];
                            else
                                msg = messageWords[j];
                            charCount += messageWords[j].length() + 1;
                            if (newset.contains(charCount + 1)) 
                                break;
                        }
                    }
                    processMessage(sc, messageWords[i], keysource, messageWords[i + 1], msg);
                    msg = "";
                    i += words + 2;
                    break;
                case "/leave":
                    processMessage(sc, messageWords[i], keysource, "", "");
                    charCount += messageWords[i].length() + 1;
                    break;
                case "/bye":
                    processMessage(sc, messageWords[i], keysource, "", "");
                    charCount += messageWords[i].length() + 1;
                    break;
                default:
                    charCount += messageWords[i].length() + 1;
                    if ((i < messageWords.length - 1) && (messageWords[i + 1].charAt(0) != '/')) {
                        if (!msg.isEmpty())
                            msg += " " + messageWords[i];
                        else
                            msg = messageWords[i];
                    } 
                    else {
                        if (!msg.isEmpty())
                            msg += " " + messageWords[i];
                        else
                            msg = messageWords[i];
                        processMessage(sc, msg, keysource, "", "");
                        msg = "";
                    }
                    break;
            }
        }
        return true;
    }

    static boolean disponivel(String nome) {
        for(String s : nomes) {
            if (s.equals(nome))
                return false;
        }
        return true;
    }

    static private void errorMessage(SocketChannel sc) throws IOException {
        buffer.clear();
        buffer.put("ERROR\n".getBytes(charset));
        buffer.flip();
        sc.write(buffer);
  }

    static private void processMessage(SocketChannel sc, String command, SelectionKey keysource, String nick, String message) throws IOException {
        // Comandos
        if(command.charAt(0) == '/' && command.charAt(1) != '/') {
            switch (command) {
                case "/nick":
                    if (nick.isEmpty() || !disponivel(nick)) {
                        errorMessage(sc);
                        return;
                    }
                    processNick(sc,keysource,nick);
                    break;
                case "/join":
                    if (nick.isEmpty()) {
                        errorMessage(sc);
                        return;
                    }
                    processJoin(sc, keysource, nick);
                    break;
                case "/priv":
                    if (message.isEmpty() || nick.isEmpty()) {
                        errorMessage(sc);
                        return;
                    }
                    processPriv(sc, keysource, nick, message);
                    break;
                case "/leave":
                    processLeave(sc, keysource, false);
                    break;
                case "/bye":
                    processBye(sc, keysource, false);
                    break;
            }
        }
        else {
            if (command.charAt(0) == '/')
                command = command.substring(1, command.length());
            message(sc, keysource, command);
        }
    }

    static private void message(SocketChannel sc, SelectionKey keySource, String message) throws IOException {
        User sender = (User) keySource.attachment();
        if (sender.estado != State.INSIDE) {
          errorMessage(sc);
          return;
        }
        // System.out.println(sender.name + " " + sender.room + " " + sender.state);
        for (String user : salaMap.get(sender.room).RoomUsers) {
          if (sender.nome == user)
            continue;
          User cur = userMap.get(user);
          buffer.clear();
          buffer.put(("MESSAGE " + sender.nome + " " + message + "\n").getBytes(charset));
          buffer.flip();
          cur.sc.write(buffer);
        }
      }

    static private void processNick(SocketChannel sc, SelectionKey keySource, String newName) throws IOException {
        User atual = (User) keySource.attachment();
        if (userMap.containsKey(newName)) {
            errorMessage(sc);
            return;
        }
        userMap.remove(atual.nome);
        String oldNick = atual.nome;
        atual.nome = newName;
        if (atual.estado == State.INSIDE) {
            Sala updateRoom = salaMap.get(atual.room);
            updateRoom.updateRoom(atual.nome);
            updateRoom.removeFromRoom(oldNick);
            salaMap.put(atual.room, updateRoom);
            for (String user : salaMap.get(atual.room).RoomUsers) {
                if (atual.nome == user)
                    continue;
                User cur = userMap.get(user);
                buffer.clear();
                buffer.put(("NEWNICK " + oldNick + " " + atual.nome + "\n").getBytes(charset));
                buffer.flip();
                cur.sc.write(buffer);
            }
        }
        if (atual.estado == State.INIT)
            atual.estado = State.OUTSIDE;
        userMap.put(atual.nome, atual);
        buffer.clear();
        buffer.put("OK\n".getBytes(charset));
        buffer.flip();
        sc.write(buffer);
    }

    static private void processJoin(SocketChannel sc, SelectionKey keysource, String roomName) throws IOException {
        User joinin = (User) keysource.attachment();
        if (joinin.estado == State.INIT) {
            buffer.clear();
            buffer.put("ERROR\n".getBytes(charset));
            buffer.flip();
            sc.write(buffer);
            return;
        }
        if ((!roomName.equals(joinin.room)) == (joinin.estado == State.INSIDE)) {
            processLeave(sc, keysource, true);
            processJoin(sc, keysource, roomName);
            return;
        }
        if (!salaMap.keySet().contains(roomName)) {
            Sala newRoom = new Sala(roomName);
            newRoom.updateRoom(joinin.nome);
        salaMap.put(roomName, newRoom);
        }
        else {
            Sala joiningRoom = salaMap.get(roomName);
            if (joiningRoom.RoomUsers.contains(joinin.nome)) {
                errorMessage(sc);
                return;
            }
            joiningRoom.updateRoom(joinin.nome);
            salaMap.put(roomName, joiningRoom);
        }
        joinin.room = roomName;
        joinin.estado = State.INSIDE;
        userMap.put(joinin.nome, joinin);
        buffer.clear();
        buffer.put("OK\n".getBytes(charset));
        buffer.flip();
        sc.write(buffer);
        for (String user : salaMap.get(roomName).RoomUsers) {
            if (user != joinin.nome) {
                buffer.clear();
                buffer.put(("JOINED " + joinin.nome + "\n").getBytes(charset));
                buffer.flip();
                userMap.get(user).sc.write(buffer);
            }
        }
    }

    static private void processLeave(SocketChannel sc, SelectionKey keySource, Boolean joiningOther) throws IOException {
        User leaving = (User) keySource.attachment();
        if (leaving.estado != State.INSIDE) {
            errorMessage(sc);
            return;
        }
        Sala leavingRoom = salaMap.get(leaving.room);
        leavingRoom.removeFromRoom(leaving.nome);
        salaMap.put(leaving.room, leavingRoom);
        for (String user : salaMap.get(leaving.room).RoomUsers) {
            if (leaving.nome != user) {
                buffer.clear();
                buffer.put(("LEFT " + leaving.nome + "\n").getBytes(charset));
                buffer.flip();
                userMap.get(user).sc.write(buffer);
            }
        }
        leaving.room = null;
        leaving.estado = State.OUTSIDE;
        userMap.put(leaving.nome, leaving);
        if (!joiningOther) {
            buffer.clear();
            buffer.put("OK\n".getBytes(charset));
            buffer.flip();
            sc.write(buffer);
        }
    }

    static private void processBye(SocketChannel sc, SelectionKey keySource, Boolean dc) throws IOException {
        User u = (User) keySource.attachment();
        if (u.estado == State.INSIDE)
            processLeave(sc, keySource, true);
        userMap.remove(u.nome);
        if (!dc) {
            buffer.clear();
            buffer.put("BYE\n".getBytes(charset));
            buffer.flip();
            sc.write(buffer);
        }
        closeConnection(null, sc);
    }

    static private void processPriv(SocketChannel sc, SelectionKey keysource, String dest, String message) throws IOException {
        User sender = (User) keysource.attachment();
        if (sender.estado != State.INIT) {
            if (!userMap.keySet().contains(dest)) {
                errorMessage(sc);
                return;
            }
            buffer.clear();
            buffer.put("OK\n".getBytes(charset));
            buffer.flip();
            sc.write(buffer);
            User recUser = userMap.get(dest);
            buffer.clear();
            buffer.put(("PRIVATE " + sender.nome + " " + message + "\n").getBytes(charset));
            buffer.flip();
            recUser.sc.write(buffer);
        }
}

    static private void closeConnection(Socket s, SocketChannel sc) {
        try {
            s = sc.socket();
            System.out.println("Closing connection to " + s);
            s.close();
        }
        catch (IOException ie) {
            System.err.println("Error closing socket " + s + ": " + ie);
        }
    }
}
