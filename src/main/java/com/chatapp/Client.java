package com.chatapp;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    final static String address = "127.0.0.1";
    final static int port = 5000;

    public static void main(String[] args) {
        try {

            Socket socket = new Socket(address,port);
            // in- read from server with socket input stream
            //out - writing in server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

            // Reading from server
            new Thread(()->{
                String servermsg;
                try{
                    while ((servermsg =in.readLine()) != null){
                        System.out.println(servermsg);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            // Sending to server from console
            String userMsg="";
            Scanner sc = new Scanner(System.in);
            while (!userMsg.equals("over")){
                userMsg = sc.nextLine();
                out.println(userMsg);
            }



        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
