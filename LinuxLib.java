import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import java.nio.ByteBuffer;
import java.io.*;

interface CLibrary extends Library {

    public void printf(String format, Object... args);
    public int getpid();
    public int getppid();
    public int fork();
    public int execv(String path, String[]... options);
    public int execvp(String file, String[]... options);
    public int dup2(int oldfd, int newfd);
    public int write(int fd, ByteBuffer buf, int count);
    public int read(int fd, ByteBuffer buf, int count);
    public int pipe(int[] filedes);
    public int kill(int pid, int sig);
    public int close(int fd);
}

public class LinuxLib {

    public static void main(String[] args) throws Exception {
        CLibrary INSTANCE = (CLibrary)
            Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"),
                               CLibrary.class);

        int parent = INSTANCE.getppid();
        int child = INSTANCE.getpid();
        int[] p1 = new int[] {-1, -1}; //pipe downstream from parent
        int in = INSTANCE.pipe(p1);
        INSTANCE.printf("p1[0]=%d\n",p1[0]);
        int id = INSTANCE.fork();

        if (id == 0) { //then we are inside the process we forked
            //the goal of p1 is to send the input the parent gets to scheme.
            INSTANCE.dup2(p1[0], 0); //point stdin to pipe's read side
            INSTANCE.close(p1[0]);
            INSTANCE.close(p1[1]);
            INSTANCE.execv("/usr/bin/petite", new String[]{"petite", null});//args);
            INSTANCE.printf("after exec \n");
        } else {
            INSTANCE.close(p1[0]);
        }

        // ctrl c "\u0003"
        BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
        String s = is.readLine();

        while (s != null) {

            if (INSTANCE.kill(parent, 0) != 0) { //make sure that the parent hasn't been killed
                INSTANCE.kill(child, 9);
            } else {

		s = s + "\n";
		ByteBuffer buf = ByteBuffer.wrap(s.getBytes());
                if(s.indexOf("\u0003") != -1) { //see if we can find ctrl c in the string
                    INSTANCE.printf("ctrl c pressed \n");
                } else {
                    INSTANCE.write(p1[1] , buf, buf.capacity());
		}
                    s = is.readLine();
                }
            }
        }
    }

