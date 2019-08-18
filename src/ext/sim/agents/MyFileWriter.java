package ext.sim.agents;

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

	   protected MyFileWriter() throws FileNotFoundException, UnsupportedEncodingException {
		fout = new File("C:\\Users\\liel\\Desktop\\ReportYarden.txt");
		fos = new FileOutputStream(fout);
		bw = new BufferedWriter(new OutputStreamWriter(fos));
	}
	   
	   public static MyFileWriter getInstance() throws FileNotFoundException, UnsupportedEncodingException {
		   if(instance == null) {
			   instance = new MyFileWriter();
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
	   
	}
