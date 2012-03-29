package hre.io;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides communication with a interactive external process.
 * 
 * @author sccblom
 *
 */
public class MessageProcess {

  private PrintStream process_input;
  private Process process;
  private BlockingQueue<Message> queue;
  
  /**
   * Wraps a system process as an interactive resources.
   * Every input message is printed to the input of the process.
   * Every line on the standard error and standard output is returned
   * as a reply message.
   * 
   * @param command_line
   */
  public MessageProcess(String ... command_line){
    Runtime runtime=Runtime.getRuntime();
    queue=new LinkedBlockingQueue<Message>();
    try {
      process=runtime.exec(command_line);
    } catch (IOException e){
      queue.add(new Message("exec error %s",e.getMessage()));
      return;
    }
    Thread stdout_parser=new StreamConverterThread("stdout",process.getInputStream(),queue);
    stdout_parser.start();
    Thread stderr_parser=new StreamConverterThread("stderr",process.getErrorStream(),queue);
    stderr_parser.start();
    process_input=new PrintStream(process.getOutputStream());
    new ProcessWatcher(process,queue,stdout_parser,stderr_parser).start();
  }

  public void send(String format,Object ... args){
    System.err.print("sending ");
    System.err.printf(format,args);
    System.err.println();
    process_input.printf(format,args);
    process_input.println();
    process_input.flush();
  }

  public Message recv(){
    Message result=null;
    while(result==null) try {
      result=queue.take();
    } catch (InterruptedException e) {}
    return result;
  }
}
