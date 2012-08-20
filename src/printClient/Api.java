package printClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Api {
	public static final String baseUrl = "http://localhost:8880/api/v1/";
	public static final String configDir = System.getProperty("user.home") + "/.print_client";
	public static final String configFilePath = configDir + "/print_client.yml";

	private String token = "";
	public static enum Driver {
		PRONTERFACE, REPLICATOR_G
	}
	private Driver driver = Driver.PRONTERFACE;

	public Api() {
		// Loading the Configuration File
		try {
			FileInputStream configFile = new FileInputStream(configFilePath);
			System.out.println("Loading configuration..");
			@SuppressWarnings("rawtypes")
			Map configMap = (Map) new Yaml().load(configFile);

			// Loading the configuration fields
			token = (String) configMap.get("token");
			if(configMap.get("driver") != null)
				driver = Driver.valueOf( (String) configMap.get("driver") );


			System.out.println("Loading configuration.. [ DONE ]");
			configMap.get("token");
		} catch (FileNotFoundException e) {
			// Creating a blank configuration file if one didn't previously
			System.out.println("No configuration found, creating a new configuration file.");
			saveConfiguration();
		}
	}

	/*
	private static String slurp (InputStream in) throws IOException {
	    StringBuffer out = new StringBuffer();
	    byte[] b = new byte[4096];
	    for (int n; (n = in.read(b)) != -1;) {
	        out.append(new String(b, 0, n));
	    }
	    return out.toString();
	}
	*/

	private static File slurpToFile(InputStream in, File f) throws IOException {
		OutputStream out=new FileOutputStream(f);
		byte buf[]=new byte[1024];
		int len;
		while((len=in.read(buf))>0)
		out.write(buf,0,len);
		out.close();
		in.close();
		return f;
	}
	
	private HttpURLConnection get(String urlBlob) throws Exception {
		if(token == "") throw new Exception("No token set, please set a token before using the api.");
		URL url = null;
		url = new URL(baseUrl + token + urlBlob);
		System.out.println(url.toString());
		return (HttpURLConnection) url.openConnection();
	}
	
	/*
	private String getString(String urlBlob) throws Exception {
		return slurp( get(urlBlob).getInputStream() );
	}
	*/

	private void saveConfiguration() {
		// Generating the yaml's property hash map
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("token", token);
		data.put("driver", driver.name());

		// Creating the config directory if it doesn't exist
		new File(configFilePath).getAbsoluteFile().getParentFile().mkdirs();
		// Writing the yaml to the config file
		try {
			PrintWriter out = new PrintWriter(configFilePath);
			out.print(new Yaml().dump(data));
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	public String getToken() {
		return token;
	}

	public boolean setToken(String token) throws Exception {
		String previousToken = this.token;
		this.token = token;
		if(hasValidToken()) {
			saveConfiguration();
			return true;
		}
		else {
			this.token = previousToken;
			return false;
		}
	}

	public Driver getDriver() {
		return driver;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
		saveConfiguration();
	}
	
	public boolean startPrinting() throws Exception {
		return get("/start_printing.json").getResponseCode() == 200;
	}

	public boolean finishPrinting() throws Exception {
		return get("/finish_printing.json").getResponseCode() == 200;
	}

	public File loadNextJob() throws Exception {
		HttpURLConnection connection = get("/load_next_job.json");
		System.out.println(connection.getResponseCode());
		if (connection.getResponseCode() != 200) throw new Exception("Unable to download gcode");
		// delete the previous GCode file
		File f = new File(configDir + "/" + token  + ".gcode");
		if( f.exists() ) f.delete();
		// write the GCode to the file
		slurpToFile( connection.getInputStream(), f );
		
		// Loading the gcode in to Pronterface or ReplicatorG
		ProcessBuilder pb = null;

		if(driver == Driver.PRONTERFACE) {
			String pronterface = "./lib/pronterface/pronterface.py";
			pb = new ProcessBuilder(pronterface, "-e", "load "+f.getPath());
		}
		if(driver == Driver.REPLICATOR_G) {
			String replicatorG;
			String os = System.getProperty("os.name").toLowerCase();
			System.out.println(os);
			if (os.indexOf("mac") >= 0) {
				replicatorG = "/Applications/ReplicatorG/ReplicatorG.app/Contents/MacOS/ReplicatorG";
			}
			else if (os.indexOf("win") >= 0) {
				// TODO: Test this on an actual windows box
				replicatorG = System.getProperty("user.home") + "\\Documents\\ReplicatorG\\ReplicatorG";
			}
			else {
				replicatorG = "replicatorg";
			}
			pb = new ProcessBuilder(replicatorG, f.getPath());
		}
		if(pb != null) pb.start();

		return f;
	}

	public boolean hasValidToken() throws Exception {
		return get(".json").getResponseCode() == 200;
	}
}
