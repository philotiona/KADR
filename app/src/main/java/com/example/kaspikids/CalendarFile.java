package com.example.kaspikids;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
/** RFC 5545 calendar export with UTC timestamps, escaped text and UTF-8-safe line folding. */
public final class CalendarFile {
    private CalendarFile() {
    }
    public static String create(String code,String title,String location,String description,long start,long end) {
        SimpleDateFormat utc=new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'",Locale.ROOT);
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        String[] lines= {
            "BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//KADR//Cinema//RU","CALSCALE:GREGORIAN","BEGIN:VEVENT","UID:"+escape(code)+"@kadr.local","DTSTAMP:"+utc.format(new Date()),"DTSTART:"+utc.format(new Date(start)),"DTEND:"+utc.format(new Date(end)),"SUMMARY:"+escape("Кино: "+title),"LOCATION:"+escape(location),"DESCRIPTION:"+escape(description),"STATUS:CONFIRMED","BEGIN:VALARM","TRIGGER:-PT1H","ACTION:DISPLAY","DESCRIPTION:Сеанс KADR через час","END:VALARM","END:VEVENT","END:VCALENDAR"
        }
        ;
        StringBuilder file=new StringBuilder();
        for(String line:lines) {
            int bytes=0;
            for(int at=0; at<line.length();) {
                int cp=line.codePointAt(at);
                String ch=new String(Character.toChars(cp));
                int n=ch.getBytes(StandardCharsets.UTF_8).length;
                if(bytes+n>75) {
                    file.append("\r\n ");
                    bytes=1;
                }
                file.append(ch);
                bytes+=n;
                at+=Character.charCount(cp);
            }
            file.append("\r\n");
        }
        return file.toString();
    }
    private static String escape(String s) {
        return s.replace("\\","\\\\").replace(";","\\;").replace(",","\\,").replace("\r","").replace("\n","\\n");
    }
}
