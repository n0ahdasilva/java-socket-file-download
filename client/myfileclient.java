/**
 *  PROJECT : Socket Programming and Concurrency (File Download Server)
 * 
 *  FILENAME : myfileclient.java
 * 
 *  DESCRIPTION :
 *      Implement frame work for Client/server communication using TCP, and handling 
 *      multi-theading/thread queues.
 * 
 *  FUNCTIONS :
 *      myfileclient.main()
 *      SocketHandling.run()
 * 
 *  NOTES :
 *      - Version 0.0.1b had inconsistent issues with send/receive files over 1MB. Bytes_read variable did not
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
 *  0.0.1d      2022.09.30  Noah            Server queues worker threads if there are more than 10 incoming requests.
 */


package client;
import java.net.*;
import java.io.*;


public class myfileclient
{
    public static void main(String[] args)
    {   
        // Initializing the arguments given when executing the program.
        String server_ip = args[0];
        int server_port = Integer.valueOf(args[1]);
        String filename = args[2];

        // Creating object of the SocketHandling class.
        SocketHandling sh = new SocketHandling();
        sh.run(server_ip, server_port, filename);
    }
}


class SocketHandling extends Thread
{   
    private String java_file_path;      // Initialize the variable to store the .java file's directory.

    private Socket socket;  // Setting up client socket variable.
    
    // Initializing stream variables.
    private DataInputStream d_in;   //  Receive data from server.
    private DataOutputStream d_out; //  Send data to server.
    private FileOutputStream f_out; //  Write to a file.

    // Variables to receive from server.
    private boolean file_found = false; // Determine if file request by client exists.
    private int BUFFER = 4096;          // Setting up buffer size of 4KB.
    private long filesize;              // Server's file size to download.

    private int bytes;                  // Size of file data chunks.

    public void run(String server_ip, int server_port, String filename)
    {
        try
        {   // Open a new socket session.
            socket = new Socket(server_ip, server_port);
            // Start Data Types IO Streaming between the client and the server. 
            d_in = new DataInputStream(socket.getInputStream());
            d_out = new DataOutputStream(socket.getOutputStream());

            d_out.writeUTF(filename);   // Send filename to the server.

            System.out.println(d_in.readUTF()); // Receive message from server, file [not] found.
            System.out.println(d_in.readUTF()); // Receive message from server, server's request statistics.

            // If the server didn't find the file, end the client socket early.
            file_found = d_in.readBoolean();
            if (!file_found) return;

            System.out.println(d_in.readUTF()); // Receive message from server, downloading file.

            java_file_path =    // Set the .java file's directory to the variable.
                myfileclient.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            // Remove the filename from the path.
            java_file_path = java_file_path.substring(0, java_file_path.lastIndexOf("/") + 1);

            // Getting ready to download the file.
            f_out = new FileOutputStream(java_file_path + "downloads/" + filename);
            
            // Receive the file and buffer size from the server.
            filesize = d_in.readLong();
            BUFFER = d_in.readInt();

            bytes = 0;
            // Initialize a byte array same size as the buffer.
            byte[] buffer_data_array  = new byte[BUFFER];
            // Downloading file, reading from data stream, one chunk at time, until we reach the end.
            while (filesize > 0 && (bytes = d_in.read(buffer_data_array,
                0, (int)Math.min(buffer_data_array.length, filesize))) != -1)
            {   // Write chunks into the file stream.
                f_out.write(buffer_data_array, 0, bytes);
                filesize -= bytes;  // Decrease the size of the file, by the buffer chunk size.
            }
            f_out.close();                      // Close the file stream needed for the transfer.

            System.out.println(d_in.readUTF()); // Receive message from server, download is completed.
        }
        catch (Exception e)
        {   // If the program fails, print the error.
            e.printStackTrace();
        }
        finally 
        {
            try
            {   // Attempt to close the socket and other tools.
                if (d_in != null) d_in.close();
                if (d_out != null) d_out.close();
                if (f_out != null) f_out.close();
                socket.close();
            }
            catch (IOException e)
            {   // If it fails, print the error.
                e.printStackTrace();
            }
        }

    }
}