/**
 *  PROJECT : Socket Programming and Concurrency (File Download Server)
 * 
 *  FILENAME : myfileserver.java
 * 
 *  DESCRIPTION :
 *      Implement frame work for Client/server communication using TCP, and handling 
 *      multi-theading/thread queues.
 * 
 *  FUNCTIONS :
 *      myfileserver.main()
 *      multiThreadingServer.multiThreadingServer()
 *      ClientWorkerThread.ClientWorkerThread()
 *      ClientWorkerThread.run()
 *      serverTime.current_time()
 *      fileLogging.log_exception()
 * 
 *  NOTES :
 *      - In the ThreadPoolExecutor, core pool size is the minimum number of threads to keep alive, while 
 *      the max pool size is the maximum number of threads to be run at once. Our pool is too small for it to matter.
 * 
 *  AUTHOR(S) : Noah Arcand Da Silva    START DATE : 2022.09.21 (YYYY.MM.DD)
 *
 *  CHANGES :
 *      - Moved from using individual functions for each task, to running as a whole in the 
 *      run() function. Made it a lot simpler to manage. Might also be more efficient?
 *      - Altered the file transfer mechanism to address big file download issues. Needed to
 *      implement a buffer size to segment files into chunks, and transfer one chunk at a time.
 * 
 *  VERSION     DATE        WHO             DETAILS
 *  0.0.1a      2022.09.21  Noah            Creation of project.
 *  0.0.1b      2022.09.23  Noah            Allows for the downloading of server files.
 *  0.0.1c      2022.09.26  Noah            Breaks up files in chunks to allow large downloads.
 *  0.0.1d      2022.09.30  Noah            Server queues worker threads if there are more than 10 incoming requests.
 */

import java.net.*;
import java.text.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;


public class myfileserver 
{
    public static void main(String[] args)
    {
        Thread mts = new multiThreadServer();
        mts.start();
    }
}


class multiThreadServer extends Thread
{   
    public static ServerSocket server_socket;       // Initialize the Server Socket.

    private ThreadPoolExecutor executor;            // Initialize the thread pool for multi-tasking and queuing.
    private BlockingQueue<Runnable> blocking_queue; // Initialize the thread queue to store incoming requests.

    public final int PORT = 8000;       // Set the port number of the server.
    int nThreads = 10;                  // Set the max number of simultaneous working threads.

    Scanner sc = new Scanner(System.in);    // Enable server to listen to keyboard inputs.

    /**
     *  Create a pool of threads when the server launches, store incoming connections 
     *  in a queue, and have the threads in the pool progressively remove connections 
     *  from the queue and process them. This is particularly simple since the operating 
     *  system does in fact store the incoming connections in a queue.
     */
    public multiThreadServer()
    {
        try
        {   // Starting the server socket on designated port.
            server_socket = new ServerSocket(PORT);
            System.out.println(serverTime.current_time() + "Listening on " 
                + "127.0.0.1" + ":" + server_socket.getLocalPort());
            // Setting up the thread queue, using a
            // runnable (used by class intended to be executed by a thread).
            blocking_queue =  new LinkedBlockingQueue<Runnable>();
            // Setting up an thread pool with a max size of 10 threads.
            // Sending requests to queue when all threads are busy.
            executor = new ThreadPoolExecutor(
                nThreads, nThreads, 5, TimeUnit.SECONDS, blocking_queue, 
                new ThreadPoolExecutor.AbortPolicy());
                // Set a timeout of 5 seconds, but it is being used in this case, since it is 
                // using a fixed pool of threads, thread pool size: core pool = max pool.

            // We need to start all core threads when starting the server.
            executor.prestartAllCoreThreads();

            while(true)
            {
                // If a client want to connect, add it to queue.
                blocking_queue.offer(new ClientWorkerThread());
            }
        }
        catch (Exception e)
        {   // If an error occured, attempt to properly close the socket and executor.
            try
            {
                server_socket.close();
                executor.shutdown();
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
                fileLogging.log_exception(e);   // Write the error to the log file.
            }
            catch (Exception ee)
            {   // If an error occured while trying to close the application, write it to the log file.
                fileLogging.log_exception(ee);
            }
        }
    }
}


class ClientWorkerThread implements Runnable
{
    private String java_file_path;      // Initialize the variable to store the .java file's directory.

    private Socket client_socket;       // Setting up socket variables.
    private String client_ip;           // Client's ip address.

    // Initializing stream variables.
    private DataInputStream d_in;       // Receive data from client.
    private DataOutputStream d_out;     // Send data to client.
    private FileInputStream f_in;       // Read from a file.

    private String filename;            // Variable to receive from client.

    // Variables to send to client.
    private File server_file;           // Initialize the file variable to read from
    private boolean file_found = false; // Determine if file request by client exists.
    private int BUFFER = 4096;          // Setting up buffer size of 4KB.

    private int bytes = 0;              // Size of file data chunks.


    /**
     * Constructor function for ClientWorker Thread.
     * Establishing a socket connection between server and client.
     */
    public ClientWorkerThread()
    {
        try
        {   // Once the client passed the queue, connect it to the sever.
            client_socket = multiThreadServer.server_socket.accept();
            // Save the client's ip address.
            client_ip = client_socket.getInetAddress().toString();
        }
        catch (IOException e)
        {   // If an error occured when trying to connect to client.
            System.out.println(serverTime.current_time()
                + "An error occurred while trying to connect to client");
        }
        catch (Exception e)
        {   // If another type of error occured, write it to the log file.
            fileLogging.log_exception(e);
        }
        
    }
    /**
     * Works with client (Allows to download files)
     */
    public void run()
    {
        try
        {   // Start Data Types IO Streaming between the client and the server. 
            d_in = new DataInputStream(client_socket.getInputStream());
            d_out = new DataOutputStream(client_socket.getOutputStream());

            // Notify the server when the client connects.
            System.out.println(serverTime.current_time() + "Client " + client_ip + " connected to server");
            // Send message to client saying he is connected to server.
            d_out.writeUTF(serverTime.current_time() + "Connection established.");
            filename = d_in.readUTF();  // Receive requested filename from client.

            // Updating server statistics for file request.
            serverStatistics.tReq++;
            System.out.println(serverTime.current_time() + "REQ " + serverStatistics.tReq
                + ": File " + filename + " requested from " + client_ip);
            
            java_file_path =    // Set the .java file's directory to the variable.
                myfileserver.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            // Remove the filename from the path.
            java_file_path = java_file_path.substring(0, java_file_path.lastIndexOf("/") + 1);
            
            server_file = new File(java_file_path + "files/" + filename);  // Import file data.
            if (server_file.exists())
            {   // Update the client, file found.
                d_out.writeUTF(serverTime.current_time() +"File " + filename + " found at server");
                file_found = true;
                // Updating server statistics for file lookup request.
                serverStatistics.sReq++;
                System.out.println(serverTime.current_time() + "REQ " + serverStatistics.tReq 
                    + ": Successful");
                System.out.println(serverTime.current_time() + "REQ " + serverStatistics.tReq 
                    + ": Total Successful requests so far = " + serverStatistics.sReq);
                
            }
            else
            {   // Update the client, file not found.
                d_out.writeUTF(serverTime.current_time() + "File " + filename 
                    + " not found at server");
                file_found = false;
                // Updating server statistics for file lookup request.
                System.out.println(serverTime.current_time() + "REQ "
                    + serverStatistics.tReq + ": Not Successful");
            }

            // Send out the request statistics to the client
            d_out.writeUTF(serverTime.current_time() + "Server handled " + serverStatistics.tReq
                + " requests, " + serverStatistics.sReq + " requests were successful");
             
            d_out.writeBoolean(file_found); // Tell the client if file exists.
            // If the file is not found, we can skip to closing the client socket and thread.
            if (!file_found) return;

            // Telling the client we are starting the process of downloading the file.
            d_out.writeUTF(serverTime.current_time() + "Downloading file " + filename);

            // Start the File and Buffered Streams needed for file transfers.
            f_in = new FileInputStream(server_file);

            d_out.writeLong(server_file.length());  // Send filesize to client.
            d_out.writeInt(BUFFER);                 // Send buffer size to client.
            
            bytes = 0;
            // Initialize a byte array same size as the buffer.
            byte[] buffer_data_array  = new byte[BUFFER];
            // Read file input data, breaking it into chunks.
            while ((bytes = f_in.read(buffer_data_array)) != -1)
            {   // Write chunks into data stream for client to download.
                d_out.write(buffer_data_array, 0, bytes);
                d_out.flush();  // Clear the stream for next chunk.
            }
            f_in.close();   // Close the streams needed for the transfer.

            d_out.writeUTF(serverTime.current_time() + "Download complete");

            System.out.println(serverTime.current_time() + "REQ " + serverStatistics.tReq 
                + ": File transfer complete");
            
        }
        catch (Exception e)
        {   // If the program fails, write it to the log file.
            fileLogging.log_exception(e);
        }
        finally 
        {
            try 
            {   // Attempt to close the socket and other tools.
                d_out.writeUTF(serverTime.current_time() + "Closing connection...");
                if (d_in != null) d_in.close();
                if (d_out != null) d_out.close();
                if (f_in != null) f_in.close();
                client_socket.close();
                // Notify the server when the client disconnects.
                System.out.println(serverTime.current_time() + "Client " + client_ip + " disconnected");
            }
            catch (Exception e)
            {   // If an error occured while trying to close the application, write it to the log file.
                fileLogging.log_exception(e);
            }
        }
    }
}


class serverStatistics
{
    public static int tReq; // Maintains count of total requests.
    public static int sReq; // Maintains count of successful requests.
}


class serverTime
{   // Setting up variables to show time of ouputs.
    private static Date sys_time;
    private static SimpleDateFormat time_format = 
        new SimpleDateFormat("[HH:mm:ss.SSS] ");

    public static String current_time()
    {
        sys_time = new Date();
        return time_format.format(sys_time);
    }
}


class fileLogging
{
    private static File log_file;           // File to store log of exception information.
    private static PrintStream ps;          // Setting up print stream for the error output stream.
    private static FileOutputStream fos;    // Initializing FOS to write to the log file.

    private static PrintWriter pw;          // Turns stack trace object to a text-output.
    private static StringWriter sw;         // Character stream to convert exception stack trace to string.

    /*
     * Function to print the stack trace from exceptions into a log file,
     * instead of printing it to the screen.
     */
    public static void log_exception(Exception ex)
    {
        try
        {
            // Open file named server.log
            log_file = new File("server.log");
            fos = new FileOutputStream(log_file);   // Start file output stream to write the logs to.
            ps = new PrintStream(fos);              // Start a print stream pointing to the file output.
            System.setErr(ps);                      // Set the system error output stream to our file.
            
            // Turn the exception stack trace into a string.
            sw = new StringWriter();    // Start character stream.
            pw = new PrintWriter(sw);   // Write the text-output to the String Writer.
            ex.printStackTrace(pw);     // Send the esception's stack trace to the Print Writer.
            // Finally, get the string from the String Writer, and print it to the log file.
            System.err.println(sw.toString());
            
            // Close the opened streams if opened.
            if (pw != null) pw.close();
            if (sw != null) sw.close();
            if (ps != null) ps.close();
            if (fos != null) fos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}