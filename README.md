# Java-socket-chat-application


A simple Java Socket example

the application is a simple chat room between several clients sending messages between each others through server.

The application contains server with TCP threads and client which make connected to server. 

-Server first ask user to enter number of port that it will lestin to. 

-Client first ask user to enter username and enter port number of server. Server use localhost 127.0.0.1 and client is connected to that ip and server port.
Client sends messages to each connected clients in chat room. It can enter two commands:

1- WHOISIN -> to list all users connected to server.

2- LOGOUT -> to close socket and leave chat room.


