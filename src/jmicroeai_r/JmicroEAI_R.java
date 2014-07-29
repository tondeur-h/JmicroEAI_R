package jmicroeai_r;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author herve
 */
public class JmicroEAI_R {

    ServerSocket ss;
    Socket sock;
    
    //constructor
    JmicroEAI_R(){
        server("127.0.0.1", 4200);
        loop_socket();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new JmicroEAI_R();
    }
    
    public boolean server(String host, int port){
        try {
            ss=new ServerSocket(port, 1000,Inet4Address.getByName(host));
            System.out.println("Connection Socket ok...");
        } catch (Exception ex) {
            Logger.getLogger(JmicroEAI_R.class.getName()).log(Level.SEVERE, null, ex);
        }
   
        //connection OK
        return true;
    }
    
    
    public void loop_socket(){
        try {
        while (true){
            
                sock=ss.accept();
                hl7Writer hl7=new hl7Writer(sock);
                hl7.start();
                
        }
        
            } catch (IOException ex) {
                Logger.getLogger(JmicroEAI_R.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
}


class hl7Writer extends Thread{
Socket localsock;
    BufferedWriter pw;
    BufferedInputStream bis;
    byte []buffer;
    static long compteur;

    public hl7Writer(Socket sock) {
    try {
        compteur++;
        localsock=sock;
        pw=new BufferedWriter(new FileWriter(compteur+".hl7"));
        System.out.println("Creation du fichier "+compteur+".hl7");
        bis=new BufferedInputStream(localsock.getInputStream());
        buffer=new byte[5428880];
        
        
    } catch (Exception ex) {
        Logger.getLogger(hl7Writer.class.getName()).log(Level.SEVERE, null, ex);
    }
    }
    

    @Override
    public void run() {
        try{
        long nboctet=bis.read(buffer);
            System.out.println("Lecture de "+nboctet+" octets...");
        for (int i=0;i<nboctet;i++){pw.write(buffer[i]);}
        pw.flush();
        pw.close();
            System.out.println("Cloture...");
        }
        catch(Exception e){}
    }
    
}
