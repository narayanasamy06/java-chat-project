package com.chatapp;

import com.chatapp.DatabaseHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class Server {

    private static int port = 5000;
    private static final ArrayList<ClientHandler> clients = new ArrayList<>();
    private static HashMap<String, ClientHandler> map = new HashMap<>();
//    private static ThreadPoolExecutor tpe =
//            new ThreadPoolExecutor(4, 4,
//                    10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(1),
//                    new CustomThreadFactory(), new CustomRejectHandler());

    // When we don't know about the fixed size of Clients
    private static ExecutorService tpe  = Executors.newCachedThreadPool();

    public static DatabaseHandler dbHandler = new DatabaseHandler();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is running, waiting for clients");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                tpe.submit(()->{
                    System.out.println("New client connected  " + clientSocket);
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.run();
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            tpe.shutdown();
            dbHandler.closeConnection();
        }
    }

    /**
     * ConversationID 1 if the msg recived to the user
     * ConversationID 2 if the msg sent by the user
     */
    public static void publish(String msg, ClientHandler sender, String plainInput) {
        boolean isValidUser= true;
        /**
         * 1.Sending Only to Clients whose are mentioned using '@'- sign
         * 2. Sending msg to all
         */
        if (plainInput.startsWith("@")) {

            String[] arr = plainInput.split(" ", 2);
            String[] allClientsToSend = arr[0].split(",");

            if (allClientsToSend.length >0) {
                for (String s : allClientsToSend){
                    boolean isValidClient=true;
                    ClientHandler toClient = map.get(s);
                    toClient.sendMessage(msg);
                    isValidClient=  dbHandler.storeMessage(toClient.userName, msg, 1);
                    if (!isValidClient){
                        System.out.println("Not a valid user");
                        return;
                    }
                }
               isValidUser=  dbHandler.storeMessage(sender.userName, msg, 2);
                if (!isValidUser){
                    System.out.println("Not a valid user");
                }
            }
        } else {

            for (ClientHandler client : clients) {
                if (client != sender) {
                    boolean isValidClient=true;
                    client.sendMessage(msg);
                  isValidClient=  dbHandler.storeMessage(client.userName, msg, 1);
                  if(!isValidClient) {
                      System.out.println("Not a valid user");
                      return;
                  }
                }
            }
          isValidUser=  dbHandler.storeMessage(sender.userName, msg, 2);
            if (!isValidUser){
                System.out.println("Not a valid user");
            }
        }
    }



    private static class ClientHandler implements Runnable {
        Socket clientSocket;
        PrintWriter out;
        BufferedReader in;
        String userName;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                boolean authenticated = false;
                while (!authenticated) {
                    out.println("Enter 'login' or 'signup':");
                    String action = in.readLine().trim();

                    if ("login".equalsIgnoreCase(action)) {
                        authenticated = handleLogin();
                    } else if ("signup".equalsIgnoreCase(action)) {
                        handleSignUp();
                    } else {
                        out.println("Invalid command. Please enter 'login' or 'signup'.");
                    }
                }

                Server.map.put("@" + userName, this);
                System.out.println("User " + userName + " Connected");

                out.println("Do you want to retrieve your previous messages? (yes/no)");
                String response = in.readLine().trim();
                if ("yes".equalsIgnoreCase(response)) {
                    retrievePreviousMessages();
                }

                out.println(userName + " Connected");
                out.println("Enter your message:");

                String input;
                while ((input = in.readLine()) != null && !input.equals("--close")) {
                    System.out.println("(" + userName + ") " + input);



                    Server.publish("[" + userName + "]: " + input, this, input);
                }

                Server.clients.remove(this);
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleClientDisconnection();
            }
        }
        private void handleClientDisconnection() {
            try {
                System.out.println("User " + userName + " disconnected.");
                Server.clients.remove(this);
                Server.map.remove("@" + userName);
                notifyOthers(userName + " has left the chat.");
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void notifyOthers(String message) {
            for (ClientHandler client : Server.clients) {
                if (client != this) {
                    client.sendMessage(message);
                }
            }
        }

        private boolean handleLogin() {
            try {
                out.println("Enter username:");
                String username = in.readLine().trim();
                out.println("Enter password:");
                String password = in.readLine().trim();

                if (Server.dbHandler.authenticateUser(username, password)) {
                    this.userName = username;
                    Server.dbHandler.updateLastLogin(username);
                    out.println("Login successful!");
                    return true;
                } else {
                    out.println("Invalid username or password.");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void handleSignUp() {
            try {
                out.println("Enter username:");
                String username = in.readLine().trim();
                out.println("Enter password:");
                String password = in.readLine().trim();
                out.println("Enter email (optional):");
                String email = in.readLine().trim();

                if (Server.dbHandler.registerUser(username, password, email)) {
                    out.println("Sign-up successful! You can now log in.");
                } else {
                    out.println("Sign-up failed. Username might already be taken.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void retrievePreviousMessages() {
            try {
                List<String> oldMessages = Server.dbHandler.getMessagesByConversation(userName);
                if (oldMessages.isEmpty()) {
                    out.println("No previous messages found.");
                } else {
                    out.println("Here are your previous messages:");
                    for (String message : oldMessages) {
                        out.println(message);
                    }
                }
            } catch (Exception e) {
                out.println("Error retrieving messages.");
                e.printStackTrace();
            }
        }

        public void sendMessage(String msg) {
            out.println(msg);
            out.println("Enter your message:");
        }
    }
}

class CustomThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread th = new Thread(r);
        System.out.println("ThreadFactoryCalled");
        th.setPriority(Thread.NORM_PRIORITY);
        return th;
    }
}

class CustomRejectHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("Task rejected " + r.toString());
    }
}
