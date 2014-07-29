package jmicroeai_r;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author herve
 */
public class JmicroEAI_R {

    ServerSocket ss;
    Socket sock;
    int port=4200;
    String host="127.0.0.1";
    boolean mllp=false;
    String pathDest=".";
    long compteur=0;
    
    //constructor
    JmicroEAI_R(String argv){
        if (argv.compareToIgnoreCase("vide")!=0){read_properties(argv);}
        server(host, port);
        loop_socket();
    }
    
    public void read_properties(String fichier){
        try {
            Properties p=new Properties();
            p.load(new FileReader(fichier));
            
            host=p.getProperty("host", "127.0.0.1");
            port=Integer.parseInt(p.getProperty("port", "4200"),10);
            mllp=Boolean.parseBoolean(p.getProperty("port", "false"));
            pathDest=p.getProperty("path", "./");
            compteur=Integer.parseInt(p.getProperty("counter", "0"),10);
            
        } catch (Exception ex) {
            Logger.getLogger(JmicroEAI_R.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String pathopt="vide";
        if (args.length>0) pathopt=args[1];
        new JmicroEAI_R(pathopt);
    }
    
    public boolean server(String host, int port){
        try {
            ss=new ServerSocket(port, 1000,Inet4Address.getByName(host));
            System.out.println("Connection Socket ok sur "+ host+":"+port);
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
                hl7Writer hl7=new hl7Writer(sock,pathDest,compteur,mllp);
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
    static long comptr;

    public hl7Writer(Socket sock,String path,long compteur, boolean mllp) {
    try {
        comptr++;
        localsock=sock;
        pw=new BufferedWriter(new FileWriter(comptr+".hl7"));
        System.out.println(comptr+"-Creation du fichier "+path+"/"+comptr+".hl7");
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
            System.out.println(comptr+"-Lecture de "+nboctet+" octets...");
        for (int i=0;i<nboctet;i++){pw.write(buffer[i]);}
        pw.flush();
        pw.close();
            System.out.println(comptr+"-Cloture...");
        }
        catch(Exception e){}
    }
    
}
