# Socket Programming and Concurrency (File Download Server)

To run the server, simply run the command
`java myfileserver.java`
from the project directory.

To run the client(s), simply run the command
`java myfileclient.java`*`"host_ip" port_number "filename"`*
from the project directory. **In this example, we use *`"localhost" 8000 "ascii.txt"` for our arguments.***

## File content

The *`myfileclient.java`* file contains the big ***SocketHandling*** class; where all of the socket and IO streaming code exists. It also has its main functions that call for the arguments for server IP, port number, and file name.

There is also a folder name *downloads* along with the java file, where the downloaded files from the server are stored.

The `myfileserver.java` file contains the ***multiThreadServer*** class; where we handle all of our server/client socket, multi-threading, and thread queuing code. The ***ClientWorkerThread***, which is ran by the ***multiThreadServer*** class on demand, contains all of our server function code when communicating with the client.

There is also a folder name *files* which contains all the data from the server that a client could download when contacting the server.

**The server has its port hard coded at port 8000.**

## View repository

https://github.com/n0ahdasilva/java-socket-file-download