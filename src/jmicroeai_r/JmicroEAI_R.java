package jmicroeai_r;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
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
    
    String pathDest=".";
    String ext="hl7";
    String suff="";
    String host="127.0.0.1";
    int port=4200;
    boolean MLLP=false;
    boolean ACK=false;
    int bufferMAX=5242880;
    
    
    public void read_properties(String fich_prop){
    try{
        Properties p=new Properties();
        p.load(new FileReader(fich_prop));
        pathDest=p.getProperty("path", ".");
        ext=p.getProperty("ext", "hl7");
        suff=p.getProperty("suff", "");
        host=p.getProperty("host", "127.0.0.1");
        port=Integer.parseInt(p.getProperty("port", "4200"), 10);
        MLLP=Boolean.parseBoolean(p.getProperty("mllp", "false"));
        ACK=Boolean.parseBoolean(p.getProperty("ack", "false"));
        bufferMAX=Integer.parseInt(p.getProperty("buffer", "5242880"), 10);
    }catch(Exception e){}
    
    }
    
    
    //constructor
    JmicroEAI_R(String argv){
        //lire properties si definie sur ligne de cmd...
        if (argv.compareTo("vide")!=0){read_properties(argv);}
        //init socket
        server(host, port);
        //ecoute socket
        loop_socket();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String optArg="vide";
    if (args.length>0){optArg=args[0];}
    //appeler constructeur pour s'affranchir du mode static
        new JmicroEAI_R(optArg);
    }
    
    
    /*!
    * 
    */
    public boolean server(String host, int port){
        try {
            ss=new ServerSocket(port, 1000,Inet4Address.getByName(host));
            System.out.println("Connection Socket on "+host+":"+port);
            System.out.println("path: "+pathDest);
            System.out.println("extension: "+ext);
            System.out.println("suffixe: "+suff);
            System.out.println("MLLP: "+MLLP);
            System.out.println("ACK: "+ACK);
            System.out.println("buffer size: "+bufferMAX);
            System.out.println("Ready...");
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
                hl7Writer hl7=new hl7Writer(sock,pathDest,ext,suff,ACK,MLLP,bufferMAX);
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
    BufferedOutputStream bos;
    
    byte []buffer;
    byte[]bufferACK;
    
    static long compteur; //compteur de classe
    boolean lACK,lMLLP;
    int bufferMAX;
    
    long bufferSize=0;
    
    public hl7Writer(Socket sock,String path, String ext, String suff, boolean ACK, boolean MLLP,int bfMAX) {
    try {
        compteur++;
        localsock=sock;
        lACK=ACK;
        lMLLP=MLLP;
        bufferMAX=bfMAX;
        
        pw=new BufferedWriter(new FileWriter(path+"/"+suff+compteur+"."+ext));
        System.out.println(compteur+" - create file "+path+"/"+suff+compteur+"."+ext);
        //preparer les flux in/out
        bis=new BufferedInputStream(localsock.getInputStream());
        bos=new BufferedOutputStream(localsock.getOutputStream());
        //init taille du buffer max
        buffer=new byte[bufferMAX];
        bufferACK=new byte[10240]; //max 10ko
        
    } catch (Exception ex) {
        Logger.getLogger(hl7Writer.class.getName()).log(Level.SEVERE, null, ex);
    }
    }
    
    
    public void prepare_ACK(){
    //TODO remplir le bufferACK...    
    }

    public void convert_mllp(){
        //eliminer les derniers caracteres
        bufferSize=bufferSize-2;
        //decaler vers i-1
        for (int ind=1;ind<bufferSize-2;ind++){
          buffer[ind-1]=buffer[ind];
        }
        //retirer le premier caractére...
        bufferSize=bufferSize-2;
    }
    
    
    @Override
    public void run() {
        try{
        bufferSize=bis.read(buffer);
        //retirer les caracteres encapsulation MLLP
        if (lMLLP){convert_mllp();
            System.out.println(compteur+" - extract MLLP "+bufferSize+" bytes");}
        
        System.out.println(compteur+" - read "+bufferSize+" bytes");
        
        for (int i=0;i<bufferSize;i++){pw.write(buffer[i]);}
        pw.flush();
        pw.close();
        if (lACK){
            prepare_ACK();
            bos.write(bufferACK);
            System.out.println(compteur+" - send ACK");
        }
        System.out.println(compteur+" - close");
        }
        catch(Exception e){}
    }
    
}
