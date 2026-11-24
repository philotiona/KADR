package com.example.kaspikids;
import java.nio.file.*;
import java.util.*;

public final class CinemaRulesTest {
 private static int checks;
 private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 private static void rejects(Runnable r,String name){try{r.run();throw new AssertionError("Accepted: "+name);}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  check(CinemaRules.login("  AdMiN ").equals("admin"),"canonical login");rejects(()->CinemaRules.login("a' OR 1=1"),"injection");rejects(()->CinemaRules.login("абв"),"non latin login");rejects(()->CinemaRules.login("aa"),"short login");
  rejects(()->CinemaRules.password("1234567"),"short password");CinemaRules.password("Cinema123!");check(CinemaRules.name("  Олжас  ").equals("Олжас"),"trim name");rejects(()->CinemaRules.name(" "),"empty name");
  String salt=CinemaRules.salt(),another=CinemaRules.salt(),hash=CinemaRules.hash("Cinema123!",salt);check(salt.length()==32&&!salt.equals(another),"random salt");check(hash.length()==64,"256 bit derived key");check(CinemaRules.matches("Cinema123!",salt,hash),"correct password");check(!CinemaRules.matches("Wrong123!",salt,hash),"wrong password");check(!hash.equals(CinemaRules.hash("Cinema123!",another)),"independent password hashes");
  check(CinemaRules.total(2500,2,15)==4250,"discount on order");check(CinemaRules.total(1,1,50)==1,"rounding half up");check(CinemaRules.total(100000,6,0)==600000,"maximum total");rejects(()->CinemaRules.total(1,0,0),"zero seats");rejects(()->CinemaRules.total(1,7,0),"seat limit");rejects(()->CinemaRules.total(-1,1,0),"negative price");
  check(!CinemaRules.overlaps(0,10,10,20),"adjacent sessions");check(CinemaRules.overlaps(0,11,10,20),"overlap");check(CinemaRules.overlaps(2,8,0,10),"contained session");
  boolean[][] qr=TicketQr.encode("KDR-1234ABCD");check(qr.length==21&&qr[0].length==21,"QR version 1");check(qr[0][0]&&qr[3][3]&&!qr[1][1]&&!qr[7][7],"finder and separator");rejects(()->TicketQr.encode("123456789012345678"),"oversize QR");
  List<String> lines=new ArrayList<>();for(boolean[] row:qr){StringBuilder s=new StringBuilder();for(boolean cell:row)s.append(cell?'1':'0');lines.add(s.toString());}Files.write(Paths.get(args[0]),lines);
  String calendar=CalendarFile.create("KDR-1234ABCD","Фильм, часть; 2", "Зал 1", String.join("",Collections.nCopies(30,"Очень длинное описание "))+"\nНовая строка",0,7200000);
  check(calendar.contains("DTSTART:19700101T000000Z"),"UTC calendar date");check(calendar.contains("TRIGGER:-PT1H"),"calendar reminder");check(calendar.contains("часть\\; 2"),"escaped calendar punctuation");for(String line:calendar.split("\r\n"))check(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=75,"RFC calendar line length");
  System.out.println("PASS: "+checks+" host rule/security/QR checks");
 }
}
