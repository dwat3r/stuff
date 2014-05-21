package bead.dht;

import java.net.*;
import java.util.*;
import java.io.*;

public class DHTMain
{
	public static void main(String[] args)
	throws Exception
	{
		//fonok
		ServerSocket fonok = new ServerSocket(65432);
		//KEZZEL futtatjuk a DHTNode-okat,es a szero megkapja a portokat toluk:
		int n=Integer.parseInt(args[0]);
		int[] nodePorts = new int[n];
		for(int i=0;i<n;++i)
		{
			Socket s = fonok.accept();
			Scanner sc = new Scanner(s.getInputStream());
			nodePorts[i] = sc.nextInt();
        	sc.close();
        	s.close();
		}
		fonok.close();
		//megvannak a portok,sorsolunk id-t a node-oknak
		Random gen = new Random();
		int[] nodeIDs = new int[n];
		for(int i=0;i<n;++i)
		{
			nodeIDs[i] = gen.nextInt(65536);
		}
		Arrays.sort(nodeIDs);
		//szoval egy DHTNode "attributumai":
		//nodeIDs,nodePorts
		//elkeszitjuk a mutatotablat:
		int[] fingertable = new int[16];
		//a nodeIDs es a nodePorts kozott 1:1 megfeleltetest vezetunk be.
		int[] fingerports = new int[16];
		for(int s=0;s<n;++s)
		{
			//minden nodenak legeneraljuk es elkuldjuk:
			for(int i=0;i<16;++i)
			{
				int tmp = (nodeIDs[s]+(int)Math.pow(2,i)) % 65536;
				//succ tmp:
				int j=1;
				while(j<n&&tmp>nodeIDs[j])++j;
				//o lesz az:
				if(j==n)
				{
					fingertable[i]=nodeIDs[0];
					fingerports[i]=nodePorts[0];
				}
				else
				{
					fingertable[i]=nodeIDs[j];
					fingerports[i]=nodePorts[j];
				}
			}
			Socket sock = new Socket("localhost",nodePorts[s]);
			//elkuldjuk a nodeoknak:
			//azon ah,azon fh,fingertable[],fingerports[]:mind int
			PrintWriter pw = new PrintWriter(sock.getOutputStream());
			int ah;
			if(s==0)ah = nodeIDs[n-1]+1;else ah = nodeIDs[s-1]+1;
			int fh = nodeIDs[s];
			pw.println(ah);
			pw.flush();
			pw.println(fh);
			pw.flush();
			for(int i:fingertable)
			{
				pw.println(i);
	    		pw.flush();
			}
			for(int i:fingerports)
			{
				pw.println(i);
				pw.flush();
			}
			pw.close();
			sock.close();
		}
	}
}