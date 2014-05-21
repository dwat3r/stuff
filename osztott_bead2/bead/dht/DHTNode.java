package bead.dht;

import java.net.*;
import java.util.*;
import java.io.*;


public class DHTNode{
	public static void main(String[] args)
	throws Exception
	{
		//a fonok cime
    	Socket fonok = new Socket("localhost",65432);
    	//kuldunk neki egy portszamot (ami parameter)
    	int port = Integer.parseInt(args[0]);
    	PrintWriter toFonok = new PrintWriter(fonok.getOutputStream());
    	toFonok.println(port);
    	//kapcs bont
    	toFonok.flush();
    	toFonok.close();
    	fonok.close();
        //inditjuk a szervert:
        ServerSocket nodeListen = new ServerSocket(port);
        //fogadjuk a kezdeti uzenetet:
        //ah,fh,targets[] :mind int
        Socket client = nodeListen.accept();
        Scanner fromClient = new Scanner(client.getInputStream());
        int ah = fromClient.nextInt();
        int fh = fromClient.nextInt(); //fh az egyben a node IDja is
        int[] fingertable = new int[16];
        for(int i=0;i<16;++i)
        {
            fingertable[i] = fromClient.nextInt();
        }
        int[] fingerports = new int[16];
        for(int i=0;i<16;++i)
        {
            fingerports[i] = fromClient.nextInt();
        }
        fromClient.close();
        client.close();
        //fogadja a kapcsolatokat,amik kuldhetnek:
        //upload <fajlnev>
        //<fajl tartalma soronkent>
        //vagy:
        //lookup <fajlnev>
        //fajlok tarolasa:
        Map<String,String> files = new HashMap<>();
        for(;;)
        {
            Socket user = nodeListen.accept();
            Scanner fromUser = new Scanner(user.getInputStream());
            String input = fromUser.nextLine();
            if (input.startsWith("upload"))
            //upload <fajlnev>
            //<fajl tartalma soronkent>
            {
                //<fajlnev>
                String fnev=input.substring(7);
                //<fajl tartalma soronkent>
                String ftart = "";
                while(fromUser.hasNext())
                {
                    ftart+=fromUser.next();
                    ftart+="\n";
                }
                fromUser.close();
                user.close();
                //ez a node felelos ezert a fajlert?
                int fID = Crc16.crc(fnev);
                //ha a legkisebb IDju noderol van szo,
                //akkor o a legnagyobb id+1 .. onmaga kozti id-kert felelos:
                boolean own=false;
                if(ah>fh&&(fID>=ah||fID<=fh))own=true;
                if(ah<fh&&fID>=ah&&fID<=fh)own=true;
                //ha igen,akkor eltaroljuk:
                if(own)
                    files.put(fnev,ftart);
                //ha nem,akkor elkuldjuk a kovetkezo node-nak:
                else
                {
                    //eloszor kiszamoljuk ki az:
                    int target;
                    int targetport;
                    if(fh<fID&&fID<fingertable[0])
                    {   
                        target=fingertable[0];
                        targetport=fingerports[0];
                    }
                    else
                    {
                        int i=1;
                        while(i<16&&fID>fingertable[i])++i;
                        target=fingertable[i-1];
                        targetport=fingerports[i-1];
                    } 
                    //aztan elkuldjuk:
                    Socket sibling = new Socket("localhost",targetport);
                    PrintWriter toSibling = new PrintWriter(sibling.getOutputStream());
                    String msg = "upload ".concat(fnev);
                    toSibling.println(msg);
                    toSibling.flush();
                    Scanner reader = new Scanner(ftart);
                    while(reader.hasNextLine())
                    {
                        toSibling.println(reader.nextLine());
                        toSibling.flush();
                    }
                    toSibling.close();
                    reader.close();
                    user.close();
                }
            }
            else if(input.startsWith("lookup"))
            //lookup <fajlnev>
            {
                String fnev=input.substring(7);
                int fID = Crc16.crc(fnev);
                //ha a legkisebb IDju noderol van szo,
                //akkor o a legnagyobb id+1 .. onmaga kozti id-kert felelos:
                boolean own=false;
                if(ah>fh&&(fID>=ah||fID<=fh))own=true;
                if(ah<fh&&fID>=ah&&fID<=fh)own=true;
                //ha igen,akkor elkuldjuk:
                if(own)
                {
                    PrintWriter toUser = new PrintWriter(user.getOutputStream());
                    if(files.containsKey(fnev))
                    {
                        Scanner reader = new Scanner(files.get(fnev));
                        toUser.println("found");
                        toUser.flush();
                        while(reader.hasNextLine())
                        {
                            toUser.println(reader.nextLine());
                            toUser.flush();
                        }
                    }
                    else
                    {
                        toUser.println("not found");
                        toUser.flush();

                    }
                    toUser.close();
                    user.close();
                }
                else
                //ha nem,akkor atadjuk a kerest a neki megfelelo node-nak:
                {
                    //eloszor kiszamoljuk ki az:
                    int target;
                    int targetport;
                    if(fh<fID&&fID<fingertable[0])
                    {   
                        target=fingertable[0];
                        targetport=fingerports[0];
                    }
                    else
                    {
                        int i=1;
                        while(i<16&&fID>fingertable[i])++i;
                        target=fingertable[i-1];
                        targetport=fingerports[i-1];
                    }
                    System.out.println(target + " " + targetport);
                    //aztan megkerdezzuk tole:
                    Socket sibling = new Socket("localhost",targetport);
                    PrintWriter toSibling = new PrintWriter(sibling.getOutputStream());
                    String msg = "lookup " + fnev;
                    toSibling.println(msg);
                    toSibling.flush();
                    Scanner fromSibling = new Scanner(sibling.getInputStream());
                    PrintWriter toUser = new PrintWriter(user.getOutputStream());
                    String file = fromSibling.nextLine();
                    if(file.equals("found"))
                    {
                        
                        toUser.println(file);
                        toUser.flush();
                        while(fromSibling.hasNext())
                        {
                            file = fromSibling.next();
                            toUser.println(file);
                            toUser.flush();
                        }
                    }
                    else
                    {
                        toUser.println("not found");
                        toUser.flush();
                    }
                    toSibling.close();
                    fromSibling.close();
                    sibling.close();

                    fromUser.close();
                    toUser.close();
                    user.close();
                }
            }
        }
	}
}