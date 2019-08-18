package ext.sim.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class MyFileWriter {
	   private static MyFileWriter instance = null;
	   
	   private File fout;
	   FileOutputStream fos;
	   BufferedWriter bw;

	   protected MyFileWriter(String header) throws IOException {
		fout = new File("C:\\Users\\liel-\\Desktop\\Report.csv");
		fos = new FileOutputStream(fout);
		bw = new BufferedWriter(new OutputStreamWriter(fos));
		write(header);
	}
	   
	   
	   
	   public static MyFileWriter getInstance(String header) throws IOException {
		   if(instance == null) {
			   instance = new MyFileWriter(header);
		   }
		   return instance;
	   }
	   
	   public void write(String string) throws IOException
	   {
		   bw.write(string);
		   bw.newLine();
	   }
	   
	   public void close() throws IOException
	   {
		   bw.close();
		   instance = null;
	   }
	   
	   public void finalize() throws IOException
	   {
		   close();
	   }
	   
	   
	   public static String printVector(long[] vec)
	   {
			String ans = "[";
			for (int i=0; i<vec.length; i++) 
			{ 
				ans = ans +""+ vec[i] +";";
			}	
			ans = ans +"]";
			
			return ans;
		}
	   
	}
