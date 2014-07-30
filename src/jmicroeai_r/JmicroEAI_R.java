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
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author herve
 */
public class JmicroEAI_R {

    //definir objet serversocket et clientSocket
    ServerSocket ss;
    Socket sock;
    
    //variable parametrage
    String pathDest="."; //path de destination des fichiers
    String ext="hl7"; //extension des fichiers
    String pref=""; //prefixe des nom de fichiers
    int sl=0x0d;
    String host="127.0.0.1"; //host du pc nom ou ip
    int port=4200; //port d'ecoute de la socket
    boolean MLLP=false; //decode l'encapsulation MMLP
    boolean ACK=false; //envoyer un ACK a l'envoyeur
    int bufferMAX=5242880; //taille du buffer de reception des fichiers
    
    
    /**!
     * fonction de lecture du fichier properties
    */
    public void read_properties(){
    try{
        String fich_prop="JmicroEAI.properties";
        Properties p=new Properties();
        p.load(new FileReader(fich_prop));
        pathDest=p.getProperty("path", ".");
        ext=p.getProperty("ext", "hl7");
        pref=p.getProperty("pref", "");
        sl=Integer.parseInt(p.getProperty("sep", "13"), 10);
        host=p.getProperty("host", "127.0.0.1");
        port=Integer.parseInt(p.getProperty("port", "4200"), 10);
        MLLP=Boolean.parseBoolean(p.getProperty("mllp", "false"));
        ACK=Boolean.parseBoolean(p.getProperty("ack", "false"));
        bufferMAX=Integer.parseInt(p.getProperty("buffer", "5242880"), 10);
    }catch(Exception e){e.printStackTrace();/*muet utiliser les valeurs par défaut*/}
    }
    
    
    //constructeur
    JmicroEAI_R(){
        //lire properties si definie dans rep courant...
        read_properties();
        //init socketServer
        server();
        //boucle d'ecoute clients
        loop_socket();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    //appeler constructeur pour s'affranchir du mode static
        new JmicroEAI_R();
    }
    
    /**!
     * version de l'application
     */
    public void version(){
        System.out.println("JmicroEAI_R version 1.0");
        System.out.println("Tondeur Hervé copyright (c) 2014");
        System.out.println("General Public License version 3.0");
        System.out.println("----------------------------------");
    }
    
    /**!
    * Initialise le server socket avec le paramétres définis...
    */
    public boolean server(){
        try {
            version();
            ss=new ServerSocket(port, 1000,Inet4Address.getByName(host));
            //affichage du parametrage en cours...
            System.out.println("Connection Socket on "+host+":"+port);
            System.out.println("path: "+pathDest);
            System.out.println("extension: "+ext);
            System.out.println("prefixe: "+pref);
            System.out.println("cr/lf: "+sl);
            System.out.println("MLLP: "+MLLP);
            System.out.println("ACK: "+ACK);
            System.out.println("buffer size: "+bufferMAX);
            System.out.println("Ready...");
        } catch (Exception ex) {
            System.out.println("Impossible de démarrer le server socket sur "+host+":"+port);
        }
   
        //connection OK
        return true;
    }
    
    /**!
    * Boucle d'ecoute des clients
    */
    public void loop_socket(){
        try {
        while (true){
                //attente clients
                sock=ss.accept();
                //run thread pour chaque client
                hl7Writer hl7=new hl7Writer(sock,pathDest,ext,pref,sl,ACK,MLLP,bufferMAX);
                hl7.start();
                
        }
            } catch (IOException ex) {
                System.out.println("Erreur connection client...");
            }
    }
    
}

/**!
 * Classe de gestion des clients et fichiers
 * Thread client
 * @author herve
 */
class hl7Writer extends Thread{
    Socket localsock; //socket client
    
    //port et adr du client
    String adrclient;
    int portclient;
    
    //ouverture des streams vers fichiers et socket
    BufferedWriter pw;
    BufferedInputStream bis;
    BufferedOutputStream bos;
    
    //buffer locaux pour message et ACK
    byte []buffer;
    byte[]bufferACK;
    
    static long compteur; //compteur de classe
    long cname;
    
    //variable locale à la classe
    boolean lACK,lMLLP;
    int bufferMAX;
    long bufferSize=0;
    String MSA;
    String sep="|";
    int sautligne;
    
    //tableau des segment MSH
    String [] ArrayMSH;
    
    public hl7Writer(Socket sock,String path, String ext, String suff,int sl, boolean ACK, boolean MLLP,int bfMAX) {
    try {
        compteur++;
        cname=compteur;
        localsock=sock;
        lACK=ACK;
        lMLLP=MLLP;
        bufferMAX=bfMAX;
        sautligne=sl;
        
//recuperer les port et adresse du client pour envoyer l'ack vers ce port.
        portclient=localsock.getPort(); 
        adrclient=localsock.getInetAddress().getHostAddress();
        
        pw=new BufferedWriter(new FileWriter(path+"/"+suff+cname+"."+ext));
        System.out.println(cname+" - create file "+path+"/"+suff+cname+"."+ext);
        //preparer les flux in/out
        bis=new BufferedInputStream(localsock.getInputStream());
        bos=new BufferedOutputStream(localsock.getOutputStream());
        //init taille du buffer max
        buffer=new byte[bufferMAX];
        bufferACK=new byte[10240]; //max 10ko
        
        ArrayMSH=new String [30];
        
    } catch (Exception ex) {
        Logger.getLogger(hl7Writer.class.getName()).log(Level.SEVERE, null, ex);
    }
    }
    
    
    public void prepare_ACK(){
        //chercher la ligne MSH
    int i=0;
    while ((i<bufferSize) && (buffer[i]!=sautligne)){i++;}
    //convertir buffer en string
    String MSH=new String (buffer, 0, i);
        System.out.println(MSH);
        
       //decouper les segments
     
        Scanner sc =new Scanner(MSH);
        sc.useDelimiter("\\|");
           
        int iseg=0;
        while (sc.hasNext()){
            ArrayMSH[iseg]=sc.next();
        //    System.out.println(ArrayMSH[iseg]);
            iseg++;
        }
        
        MSA=ArrayMSH[0]+sep+ArrayMSH[1]+"|HT|CHV|"+ArrayMSH[2]+sep+ArrayMSH[3]+sep+ArrayMSH[6]+sep+ArrayMSH[7]+sep+ArrayMSH[8]+sep+ArrayMSH[9]+sep+ArrayMSH[10]+sep+ArrayMSH[11]+sep+ArrayMSH[12]+sep+ArrayMSH[13]+sep+ArrayMSH[14]+sep+ArrayMSH[15]+sep+ArrayMSH[16]+sep+ArrayMSH[17]+sep+sautligne+"MSA|AA|"+ArrayMSH[9];
      
        bufferACK=MSA.getBytes();
      //  System.out.println(MSA);
    }

    
    /**!
     * supprime le caractére 0x0b de début et 0x1c et 0x0d de fin
     */
    synchronized public void convert_mllp(){
        //eliminer les derniers caracteres
        bufferSize=bufferSize-2;
        //decaler vers i-1 pour supprimer le premier caractére
        for (int ind=1;ind<bufferSize-2;ind++){
          buffer[ind-1]=buffer[ind];
        }
        //retaille le buffer
        bufferSize=bufferSize-2;
    }
    
    /**!
     * main run process du Thread
     */
    @Override
    public void run() {
        try{
            //lire le message dans la socket
        bufferSize=bis.read(buffer);
         System.out.println(cname+" - read "+bufferSize+" bytes");
         
        //retirer les caracteres d'encapsulation MLLP
        if (lMLLP){convert_mllp();
            System.out.println(cname+" - extract MLLP "+bufferSize+" bytes");
        }
   
        //ecrire le fichier message sur disque
        for (int i=0;i<bufferSize;i++){pw.write(buffer[i]);}
        pw.flush();
        pw.close();
        
        //si demande d'ack préparer ACK et envoyer vers client
        if (lACK){
            prepare_ACK();
            bos.write(bufferACK, 0, MSA.length()-1);
            System.out.println(cname+" - send ACK");
        }
        
        //fin processus
        System.out.println(cname+" - close");
        }
        catch(Exception e){e.printStackTrace();}
    }
}