import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.bhagat.ai.supervised.NeuralNetwork;
import io.bhagat.util.ArrayUtil;
import io.bhagat.util.StreamUtil;

public class Server {

	public static final int PORT = 80;

	public static void main(String[] args) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
		
		HashMap<String, NeuralNetwork> map = new HashMap<>();
		
		server.createContext("/create", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if(!exchange.getRequestMethod().equals("POST"))
				{
					System.out.println("NOT POST");
					String response = "ERROR: only support POST requests";
					exchange.sendResponseHeaders(500, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				InputStream inputStream = exchange.getRequestBody();
				String jsonStr = StreamUtil.readString(inputStream);
				JSONObject obj = new JSONObject(jsonStr);

				try {
					if(obj.getJSONArray("shape") == null)
					{
						String response = "ERROR: shape must be defined in request body";
						exchange.sendResponseHeaders(400, response.length());
						OutputStream outputStream = exchange.getResponseBody();
						outputStream.write(response.getBytes());
						outputStream.close();
						return;
					}
				} catch(JSONException e) {
					String response = "ERROR: shape must be defined in request body";
					exchange.sendResponseHeaders(400, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				JSONArray jsonArr = obj.getJSONArray("shape");
				int[] shape = new int[jsonArr.length()];
				for(int i = 0; i < shape.length; i++)
					shape[i] = jsonArr.getInt(i);
				
				NeuralNetwork neuralNetwork = new NeuralNetwork(shape);
				String rand = getRandomString();
				map.put(rand, neuralNetwork);
				
				shape[0] = neuralNetwork.getNumOfInputs();
				for(int i = 0; i < neuralNetwork.getNumsOfHiddens().length; i++)
					shape[i + 1] = neuralNetwork.getNumsOfHiddens()[i];
				shape[shape.length - 1] = neuralNetwork.getNumOfOutputs();
				
				System.out.println("Created Neural Network with shape: " + ArrayUtil.newArrayList(shape).toString() + " at " + rand);
				
				String response = "{\"key\":\""+rand+"\"}";
				exchange.sendResponseHeaders(200, response.length());
				OutputStream outputStream = exchange.getResponseBody();
				outputStream.write(response.getBytes());
				outputStream.close();
			}
			
		});
		
		server.createContext("/destroy", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if(!exchange.getRequestMethod().equals("POST"))
				{
					System.out.println("NOT POST");
					String response = "ERROR: only support POST requests";
					exchange.sendResponseHeaders(500, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				InputStream inputStream = exchange.getRequestBody();
				String jsonStr = StreamUtil.readString(inputStream);
				JSONObject obj = new JSONObject(jsonStr);

				try {
					if(obj.getString("key") == null || !map.containsKey(obj.getString("key")))
					{
						String response = "ERROR: invalid data or key";
						exchange.sendResponseHeaders(400, response.length());
						OutputStream outputStream = exchange.getResponseBody();
						outputStream.write(response.getBytes());
						outputStream.close();
						return;
					}
				} catch(JSONException e) {
					String response = "ERROR: invalid data or key";
					exchange.sendResponseHeaders(400, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				String key = obj.getString("key");
					
				String response = "Success!";
				map.remove(key);
				
				System.out.println("Destroyed Neural Network at key " + key);
				
				exchange.sendResponseHeaders(200, response.length());
				OutputStream outputStream = exchange.getResponseBody();
				outputStream.write(response.getBytes());
				outputStream.close();
				
			}
			
		});

		server.createContext("/predict", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if(!exchange.getRequestMethod().equals("POST"))
				{
					System.out.println("NOT POST");
					String response = "ERROR: only support POST requests";
					exchange.sendResponseHeaders(500, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				InputStream inputStream = exchange.getRequestBody();
				String jsonStr = StreamUtil.readString(inputStream);
				JSONObject obj = new JSONObject(jsonStr);

				try {
					if(obj.getJSONArray("input") == null || obj.getString("key") == null || !map.containsKey(obj.getString("key")))
					{
						String response = "ERROR: invalid data or key";
						exchange.sendResponseHeaders(400, response.length());
						OutputStream outputStream = exchange.getResponseBody();
						outputStream.write(response.getBytes());
						outputStream.close();
						return;
					}
				} catch(JSONException e) {
					String response = "ERROR: invalid data or key";
					exchange.sendResponseHeaders(400, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				JSONArray jsonArr = obj.getJSONArray("input");
				String key = obj.getString("key");
				double[] input = new double[jsonArr.length()];
				for(int i = 0; i < input.length; i++)
					input[i] = jsonArr.getDouble(i);
				
				if(map.get(key).getNumOfInputs() != input.length)
				{
					String response = "ERROR: invalid input or key";
					exchange.sendResponseHeaders(400, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
					
				double[] output = map.get(key).feedForward(input);
				JSONArray jsonOutput = new JSONArray(output);
				JSONObject response = new JSONObject();
				response.put("output", jsonOutput);
				
				System.out.println("Prediction: " + ArrayUtil.newArrayList(input).toString() + " -> " + ArrayUtil.newArrayList(output).toString() + " using neural network at " + key);
				
				exchange.sendResponseHeaders(200, response.toString().length());
				OutputStream outputStream = exchange.getResponseBody();
				outputStream.write(response.toString().getBytes());
				outputStream.close();
				
			}
			
		});
		
		server.createContext("/train", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				if(!exchange.getRequestMethod().equals("POST"))
				{
					System.out.println("NOT POST");
					String response = "ERROR: only support POST requests";
					exchange.sendResponseHeaders(500, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				InputStream inputStream = exchange.getRequestBody();
				String jsonStr = StreamUtil.readString(inputStream);
				JSONObject obj = new JSONObject(jsonStr);
				try {
					if(obj.getJSONArray("data") == null || obj.getString("key") == null || !map.containsKey(obj.getString("key")))
					{
						String response = "ERROR: invalid data or key";
						exchange.sendResponseHeaders(400, response.length());
						OutputStream outputStream = exchange.getResponseBody();
						outputStream.write(response.getBytes());
						outputStream.close();
						return;
					}
				} catch(JSONException e) {
					String response = "ERROR: invalid data or key";
					exchange.sendResponseHeaders(400, response.length());
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(response.getBytes());
					outputStream.close();
					return;
				}
				
				JSONArray data = obj.getJSONArray("data");
				String key = obj.getString("key");
				NeuralNetwork neuralNetwork = map.get(key);
				int epoch = 1;
				try {
					epoch = obj.getInt("epoch");
				} catch(JSONException e) {}

				try {
					for(int i_ = 0; i_ < epoch; i_++)
						for(int i = 0; i < data.length(); i++)
						{
							JSONObject d = data.getJSONObject(i);
							JSONArray jsonInput = d.getJSONArray("input");
							JSONArray jsonOutput = d.getJSONArray("output");
							if(jsonInput.length() == neuralNetwork.getNumOfInputs() && jsonOutput.length() == neuralNetwork.getNumOfOutputs())
							{
								double[] inputs = new double[neuralNetwork.getNumOfInputs()];
								double[] outputs = new double[neuralNetwork.getNumOfOutputs()];
								
								for(int j = 0; j < inputs.length; j++)
									inputs[j] = jsonInput.getDouble(j);
								for(int j = 0; j < outputs.length; j++)
									outputs[j] = jsonOutput.getDouble(j);
								
								neuralNetwork.train(inputs, outputs);
							}
						}
				} catch(Exception e) {
					e.printStackTrace();
				}
					
				System.out.println("Trained " + epoch * data.length() + " examples in neural network at " + key);
				String response = "Trained " + epoch * data.length() + " examples in neural network";
				exchange.sendResponseHeaders(200, response.toString().length());
				OutputStream outputStream = exchange.getResponseBody();
				outputStream.write(response.toString().getBytes());
				outputStream.close();
				
			}
			
		});
	
		System.out.println("Server running on port " + PORT);
		server.start();
		
	}
	
	public static String getRandomString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (Math.random()* SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
		
}
