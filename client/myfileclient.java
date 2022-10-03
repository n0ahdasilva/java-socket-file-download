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
 *      clientTime.curent_time()
 *      fileLogging.log_exception()
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
import java.text.*;
import java.io.*;
import java.util.*;


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
        {
            System.out.println(clientTime.current_time() + "Connecting to " + server_ip + ":" + server_port);
            
            try
            {   // Open a new socket session.
                socket = new Socket(server_ip, server_port);
            }
            catch (ConnectException c_e)
            {   // If the socket cannot establish a connection, close the client program.
                System.out.println(clientTime.current_time() + "Cannot connect to the server "+ server_ip + ":" + 
                    server_port + "\n" + "Either the given address/port is invalid or the server is closed.");
                fileLogging.log_exception(c_e); // Write error to the log file.
                System.exit(0);
            }
            catch (Exception e)
            {   // If another type of error occured, write it to the log file.
                fileLogging.log_exception(e);
            }

            // Start Data Types IO Streaming between the client and the server. 
            d_in = new DataInputStream(socket.getInputStream());
            d_out = new DataOutputStream(socket.getOutputStream());

            System.out.println(d_in.readUTF()); // Receive message from server, client is connected.
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
        catch (SocketException e)
        {   // If the error is a 'SocketException: Connection reset' error,
            // tell the client that they have lost connection to the server.
            if (e.toString().contains("java.net.SocketException: Connection reset"))
            {
                System.out.println(clientTime.current_time() + "Lost connection to server");
            }
            // Then writ the error to the log file.
            fileLogging.log_exception(e);
        }
        catch (Exception e)
        {   // If the program fails, write it to the log file.
            fileLogging.log_exception(e);
        }
        finally 
        {
            try
            {   // Attempt to close the socket and other tools.
                System.out.println(d_in.readUTF());
                if (d_in != null) d_in.close();
                if (d_out != null) d_out.close();
                if (f_out != null) f_out.close();
                socket.close();
            }
            catch (SocketException e)
            {   // SocketException already catched above, no duplicates.
                return;
            }
            catch (Exception e)
            {   // If another type of error occured, write it to the log file.
                fileLogging.log_exception(e);
            }
        }

    }
}


class clientTime
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
    private static File log_file;           // 
    private static PrintStream ps;          // 
    private static FileOutputStream fos;    // 

    private static StringWriter sw;
    private static PrintWriter pw;
    private static String stack_trace_str;

    /*
     * Function to print the stack trace from exceptions into a log file,
     * instead of printing it to the screen.
     */
    public static void log_exception(Exception ex)
    {
        try
        {
            // Open file named server.log
            log_file = new File("client.log");
            fos = new FileOutputStream(log_file);   // Start file output stream to write the logs to.
            ps = new PrintStream(fos);              // Start a print stream pointing to the file output.
            System.setErr(ps);                      // Set the system error output stream to our file.
            
            // Turn the exception stack trace into a string.
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            stack_trace_str = sw.toString();

            System.err.println(stack_trace_str); // Print the logs into the file.
            
            // Close our streams.
            pw.close();
            sw.close();
            ps.close();
            fos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}