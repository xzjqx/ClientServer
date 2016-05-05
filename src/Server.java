/**
 * Created by xzjqx on 5/4/2016.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Server extends ServerSocket {
    private static final int SERVER_PORT =2013;

    private static boolean isPrint =false;//是否输出消息标志
    private static List user_list =new ArrayList();//登录用户集合
    private static List<ServerThread> thread_list =new ArrayList<ServerThread>();//服务器已启用线程集合
    private static LinkedList<String> message_list =new LinkedList<String>();//存放消息队列

    /**
     * 创建服务端Socket,创建向客户端发送消息线程,监听客户端请求并处理
     */
    public Server()throws IOException{
        super(SERVER_PORT);//创建ServerSocket
        new PrintOutThread();//创建向客户端发送消息线程

        try {
            while(true){//监听客户端请求，启个线程处理
                Socket socket = accept();
                new ServerThread(socket);
            }
        }catch (Exception e) {
        }finally{
            close();
        }
    }

    /**
     * 监听是否有输出消息请求线程类,向客户端发送消息
     */
    class PrintOutThread extends Thread{

        public PrintOutThread(){
            start();
        }

        @Override
        public void run() {
            while(true){
                if(isPrint){//将缓存在队列中的消息按顺序发送到各客户端，并从队列中清除。
                    String message = message_list.getFirst();
                    for (ServerThread thread : thread_list) {
                        thread.sendMessage(message);
                    }
                    message_list.removeFirst();
                    isPrint = message_list.size() >0 ?true :false;
                }
            }
        }
    }

    /**
     * 服务器线程类
     */
    class ServerThread extends Thread{
        private Socket client;
        private PrintWriter out;
        private BufferedReader in;
        private String name;

        public ServerThread(Socket s)throws IOException{
            client = s;
            out =new PrintWriter(client.getOutputStream(),true);
            in =new BufferedReader(new InputStreamReader(client.getInputStream()));
            in.readLine();
            out.println("成功连上聊天室,请输入你的名字：");
            start();
        }

        @Override
        public void run() {
            try {
                int flag =0;
                String line = in.readLine();
                while(!"bye".equals(line)){
                    //查看在线用户列表
                    if ("showuser".equals(line)) {
                        out.println(this.listOnlineUsers());
                        line = in.readLine();
                    }
                    //第一次进入，保存名字
                    if(flag++ ==0){
                        name = line;
                        user_list.add(name);
                        thread_list.add(this);
                        out.println(name +"你好,可以开始聊天了...");
                        this.pushMessage("Client<" + name +">进入聊天室...");
                    }else{
                        this.pushMessage("Client<" + name +"> say : " + line);
                    }
                    line = in.readLine();
                }
                out.println("byeClient");
            }catch (Exception e) {
                e.printStackTrace();
            }finally{//用户退出聊天室
                try {
                    client.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
                thread_list.remove(this);
                user_list.remove(name);
                pushMessage("Client<" + name +">退出了聊天室");
            }
        }

        //放入消息队列末尾，准备发送给客户端
        private void pushMessage(String msg){
            message_list.addLast(msg);
            isPrint =true;
        }

        //向客户端发送一条消息
        private void sendMessage(String msg){
            out.println(msg);
        }

        //统计在线用户列表
        private String listOnlineUsers() {
            String s ="--- 在线用户列表 ---";
            for (int i =0; i < user_list.size(); i++) {
                s +="[" + user_list.get(i) +"]";
            }
            s +="--------------------";
            return s;
        }
    }

    public static void main(String[] args)throws IOException {
        new Server();//启动服务端
    }
}
