package vct.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import hre.config.Option;
import hre.config.OptionParser;
import hre.config.StringListSetting;
import hre.config.StringSetting;

import java.io.BufferedInputStream;

public class TestcaseVisitor extends SimpleFileVisitor<Path>{
  
  public final HashMap<String,Testcase> testsuite=new HashMap();
  
  public HashSet<Path> unmarked=new HashSet();
  
  public static String extension(Path path){
    Path file=path.getFileName();
    String name=file.toString();
    int dot=name.lastIndexOf('.');
    if (dot<0) {
      return "";
    } else {
      return name.substring(dot+1);
    }
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
      throws IOException
  {
      String type="unknown";
      if (attrs.isOther()) {
        type="other";
      } else if (attrs.isRegularFile()){
        type="regular";
      } else if (attrs.isDirectory()){
        type="folder";
      } else if (attrs.isSymbolicLink()){
        type="symlink";
      }
      String ext=extension(file);
      switch(ext){
      case "c":
      case "java":
        {
          BufferedReader is=new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile())));
          String line;
          HashSet<String> cases=new HashSet();
          while((line=is.readLine())!=null){
            line=line.trim();
            if (line.startsWith("//::")){
              //System.err.printf("%s: %s%n", file, line);
              String cmds[]=line.substring(4).trim().split("[ ]+");
              switch(cmds[0]){
              case "case":
              case "cases":
                cases.clear();
                for(int i=1;i<cmds.length;i++) {
                  cases.add(cmds[i]);
                  Testcase test=testsuite.get(cmds[i]);
                  if (test==null){
                    test=new Testcase();
                    testsuite.put(cmds[i],test);
                  }
                  test.files.add(file);
                }
                break;
              case "tool":
              case "tools":
                for(int i=1;i<cmds.length;i++) {
                  for(String test:cases){
                    Testcase tc=testsuite.get(test);
                    tc.tools.add(cmds[i]);
                  }
                }
                break;
              case "verdict":
                for(String test:cases){
                  Testcase tc=testsuite.get(test);
                  tc.verdict=cmds[1];
                }                
                break;
              case "option":
              case "options":
                for(int i=1;i<cmds.length;i++) {
                  for(String test:cases){
                    Testcase tc=testsuite.get(test);
                    tc.options.add(cmds[i]);
                  }
                }
                break;
              default:
                System.err.printf("ignoring %s in %s: %s%n",cmds[0],file,line);
              }
            } else {
              continue;
            }
          }
          is.close();
          if (cases.isEmpty()){
            unmarked.add(file);
          }
        }                       
      }
      //System.err.printf("file: %s (%s/%s)%n", file, type,ext);
      return FileVisitResult.CONTINUE;
  }
  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
      throws IOException
  {
          //System.err.printf("enter folder: %s%n", dir);
          return FileVisitResult.CONTINUE;
  }
  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException e)
      throws IOException
  {
      if (e == null) {
          //System.err.printf("leave folder: %s%n", dir);
          return FileVisitResult.CONTINUE;
      } else {
          // directory iteration failed
          throw e;
      }
  }


}
