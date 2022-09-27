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
 * 
 *  NOTES :
 *      Version 0.0.1b had inconsistent issues with send/receive files over 1MB. Bytes_read variable did not
 *      match the file data array 'mybytearray' causing EOTF exceptions. Need to find a solution. 
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
 */


package server;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;


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
    private ServerSocket server_socket; // Initialize the Server Socket.
    
    public final int PORT = 8000;       // Set the port number of the server.
    int nThreads = 2;                   // Set the max number of simultaneous working threads.

    /**
     * Create a pool of threads when the server launches, store incoming connections 
     * in a queue, and have the threads in the pool progressively remove connections 
     * from the queue and process them. This is particularly simple since the operating 
     * system does in fact store the incoming connections in a queue.
     */
    public multiThreadServer()
    {
        try
        {   
            server_socket = new ServerSocket(PORT);
            ExecutorService executor = new ThreadPoolExecutor
            (
                nThreads, 
                nThreads, 
                0L, 
                TimeUnit.MILLISECONDS, 
                new ArrayBlockingQueue<>(nThreads),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );

            while(true)
            {
                Socket c_socket = server_socket.accept();
                executor.execute(new ClientWorkerThread(c_socket));
            }
            //executor.close();
        }
        catch (Exception ex)
        {
            try
            {
                server_socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}


class ClientWorkerThread extends Thread
{
    private Socket client_socket;       // Setting up socket variables.

    // Initializing stream variables.
    private DataInputStream d_in;       // Receive data from client.
    private DataOutputStream d_out;     // Send data to client.
    private FileInputStream f_in;       // Read from a file.

    private String filename;            // Variable to receive from client.

    // Variables to send to client.
    private File server_file;           // Initialize the file variable to read from
    private boolean file_found = false; // Determine if file request by client exists.
    private int BUFFER = 1024;          // Setting up buffer size of 1KB.

    private int bytes = 0;              // Size of file data chunks.

    ClientWorkerThread(Socket c_socket)
    {
        client_socket = c_socket;
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

            filename = d_in.readUTF();  // Receive requested filename from client.

            // Updating server statistics for file request.
            serverStatistics.tReq++;
            System.out.println("REQ " + serverStatistics.tReq + ": File " + filename 
                + " requested from " + client_socket.getInetAddress());
            
            server_file = new File("files/" + filename);   // Import file data.
            if (server_file.exists())
            {   // Update the client, file found.
                d_out.writeUTF("File " + filename + " found at server");
                file_found = true;
                // Updating server statistics for file lookup request.
                serverStatistics.sReq++;
                System.out.println("REQ " + serverStatistics.tReq + ": Successful");
                System.out.println("REQ " + serverStatistics.tReq 
                    + ": Total Successful requests so far = " + serverStatistics.sReq);
                
            }
            else
            {   // Update the client, file not found.
                d_out.writeUTF("File " + filename + " not found at server");
                file_found = false;
                // Updating server statistics for file lookup request.
                System.out.println("REQ " + serverStatistics.tReq + ": Not Successful");
            }

            // Send out the request statistics to the client
            d_out.writeUTF("Server handled " + serverStatistics.tReq + " requests, "
                + serverStatistics.sReq + " requests were successful");
             
            d_out.writeBoolean(file_found); // Tell the client if file exists.
            // If the file is not found, we can skip to closing the client socket and thread.
            if (!file_found) return;

            // Telling the client we are starting the process of downloading the file.
            d_out.writeUTF("Downloading file " + filename);

            // Start the File and Buffered Streams needed for file transfers.
            f_in = new FileInputStream(server_file);

            d_out.writeLong(server_file.length());  // Send filesize to client.
            d_out.writeInt(BUFFER);                 // Send buffer size to client.
            
            bytes = 0;
            // Initialize a byte array same size as the buffer.
            byte[] buffer_data_array  = new byte[4 * BUFFER];
            // Read file input data, breaking it into chunks.
            while ((bytes = f_in.read(buffer_data_array)) != -1)
            {   // Write chunks into data stream for client to download.
                d_out.write(buffer_data_array, 0, bytes);
                d_out.flush();  // Clear the stream for next chunk.
            }
            f_in.close();   // Close the streams needed for the transfer.

            d_out.writeUTF("Download complete");

            System.out.println("REQ " + serverStatistics.tReq + ": File transfer complete");
            
        }
        catch (IOException e)
        {   // If the program fails, print the error.
            e.printStackTrace();
        }
        finally 
        {
            try 
            {   // Attempt to close the socket and other tools.
                if (d_in != null) d_in.close();
                if (d_out != null) d_out.close();
                if (f_in != null) f_in.close();
                client_socket.close();
            } 
            catch (IOException e) 
            {   // If it fails, print the error.
                e.printStackTrace();
            }
        }
    }
}


class serverStatistics
{
    public static int tReq; // Maintains count of total requests.
    public static int sReq; // Maintains count of successful requests.
}