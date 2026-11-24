package com.example.kaspikids;
import android.graphics.*;
import java.util.*;
/** Builds an actual shareable ticket image with a dynamic height for long names and titles. */
final class TicketImage {
    private TicketImage() {
    }
    static Bitmap qr(String code,int size) {
        boolean[][] q=TicketQr.encode(code);
        int unit=Math.max(1,size/29);
        Bitmap b=Bitmap.createBitmap(unit*29,unit*29,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);
        c.drawColor(Color.WHITE);
        Paint p=new Paint();
        p.setColor(Color.BLACK);
        for(int y=0; y<21; y++)for(int x=0; x<21; x++)if(q[y][x])c.drawRect((x+4)*unit,(y+4)*unit,(x+5)*unit,(y+5)*unit,p);
        return b;
    }
    static Bitmap create(String code,String details) {
        Paint p=new Paint(3);
        p.setColor(0xff191a1b);
        p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        p.setTextSize(36);
        List<String> lines=new ArrayList<>();
        for(String paragraph:details.split("\n")) {
            String rest=paragraph;
            while(!rest.isEmpty()) {
                int count=p.breakText(rest,true,850,null);
                if(count==0)count=1;
                if(count<rest.length()) {
                    int space=rest.lastIndexOf(' ',count);
                    if(space>0)count=space;
                }
                lines.add(rest.substring(0,count));
                rest=rest.substring(count).trim();
            }
        }
        int qrTop=250+lines.size()*56,height=qrTop+530;
        Bitmap b=Bitmap.createBitmap(1000,height,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);
        c.drawColor(0xfff5f1e8);
        p.setTextSize(68);
        c.drawText("KADR.",70,120,p);
        p.setTextSize(36);
        int y=215;
        for(String line:lines) {
            c.drawText(line,70,y,p);
            y+=56;
        }
        Bitmap qr=qr(code,464);
        c.drawBitmap(qr,(1000-qr.getWidth())/2f,qrTop,p);
        qr.recycle();
        return b;
    }
}
