package com.example.kaspikids;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Original scalable poster artwork; no network requests or copyrighted poster assets. */
final class FilmArt extends View {
    private final int art;
    private final String title;
    private final Paint p=new Paint(3);
    FilmArt(Context c,int art,String title) {
        super(c);
        this.art=Math.floorMod(art,6);
        this.title=title;
        setContentDescription("Постер фильма «"+title+"»");
    }
    private void color(int c) {
        p.setColor(c);
        p.setStyle(Paint.Style.FILL);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth()/360f,getHeight()/480f);
        int[] bg= {
            0xff182442,0xff251f38,0xfff4c855,0xff234e64,0xffb9d3c2,0xffd6c8aa
        }
        ;
        color(bg[art]);
        canvas.drawRect(0,0,360,480,p);
        if(art==0) {
            color(0xffefb55b);
            canvas.drawCircle(242,178,111,p);
            color(0xff182442);
            canvas.drawCircle(214,160,88,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(0xfff0d8a5);
            canvas.save();
            canvas.rotate(-28,180,200);
            canvas.drawOval(20,160,340,230,p);
            canvas.restore();
            color(0xfff5f1e8);
            for(int i=0; i<50; i++)canvas.drawCircle((i*71+13)%360,(i*47+31)%300,i%3==0?2:1,p);
        }
        if(art==1) {
            for(int i=0; i<12; i++) {
                color(i%2==0?0xff554164:0xff3a304f);
                canvas.drawRect(i*32,100+(i*37)%120,i*32+30,400,p);
                color(0xfff2aa76);
                for(int j=0; j<8; j++)if((i+j)%3==0)canvas.drawRect(i*32+8,150+j*25,i*32+14,160+j*25,p);
            }
            color(0xffed795b);
            canvas.drawCircle(270,83,43,p);
            color(0xffc99fae);
            canvas.drawRect(0,340,360,344,p);
        }
        if(art==2) {
            color(0xfff17950);
            canvas.drawCircle(268,112,88,p);
            color(0xff286f83);
            canvas.drawRect(0,235,360,360,p);
            color(0xfff5edda);
            canvas.drawRoundRect(35,125,300,278,15,15,p);
            color(0xff242528);
            canvas.drawRoundRect(54,146,282,240,8,8,p);
            color(0xffd3c7af);
            canvas.drawCircle(116,191,31,p);
            canvas.drawCircle(227,191,31,p);
            color(0xff242528);
            canvas.drawCircle(116,191,14,p);
            canvas.drawCircle(227,191,14,p);
            p.setStrokeWidth(7);
            canvas.drawLine(116,191,227,191,p);
        }
        if(art==3) {
            color(0xffefb483);
            canvas.drawCircle(180,132,64,p);
            for(int i=0; i<12; i++) {
                p.setColor(i%2==0?0xff559797:0xff79b1ac);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(3);
                Path wave=new Path();
                wave.moveTo(-30,210+i*12);
                wave.cubicTo(90,170+i*12,200,260+i*12,390,190+i*12);
                canvas.drawPath(wave,p);
            }
            color(0xff163a50);
            canvas.drawRect(66,250,80,360,p);
            canvas.drawRect(52,245,176,260,p);
        }
        if(art==4) {
            color(0xfff8f2df);
            canvas.drawCircle(235,108,42,p);
            canvas.drawCircle(275,100,54,p);
            canvas.drawCircle(315,116,37,p);
            color(0xff537a61);
            canvas.drawCircle(40,355,155,p);
            canvas.drawCircle(290,390,170,p);
            color(0xffd77a3d);
            Path fox=new Path();
            fox.moveTo(115,260);
            fox.lineTo(95,162);
            fox.lineTo(154,201);
            fox.lineTo(208,172);
            fox.lineTo(206,270);
            fox.lineTo(163,304);
            fox.close();
            canvas.drawPath(fox,p);
            color(0xfff3deb4);
            Path face=new Path();
            face.moveTo(114,238);
            face.lineTo(163,278);
            face.lineTo(202,233);
            face.lineTo(163,300);
            face.close();
            canvas.drawPath(face,p);
            color(0xff242624);
            canvas.drawCircle(132,235,4,p);
            canvas.drawCircle(187,235,4,p);
            canvas.drawCircle(163,276,5,p);
        }
        if(art==5) {
            color(0xffb86744);
            canvas.drawCircle(266,105,58,p);
            int[] cs= {
                0xffa3aa91,0xff647e75,0xff304e4d
            }
            ;
            for(int i=0; i<3; i++) {
                color(cs[i]);
                Path mountain=new Path();
                mountain.moveTo(-50,400);
                mountain.lineTo(110+i*55,130+i*50);
                mountain.lineTo(240+i*70,310-i*10);
                mountain.lineTo(410,190+i*60);
                mountain.lineTo(410,480);
                mountain.lineTo(0,480);
                mountain.close();
                canvas.drawPath(mountain,p);
            }
            p.setColor(0xffedcca1);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            Path trail=new Path();
            trail.moveTo(170,390);
            trail.cubicTo(290,330,100,325,224,270);
            canvas.drawPath(trail,p);
        }
        color(0xcc101114);
        canvas.drawRect(0,355,360,480,p);
        color(0xfffaf4e8);
        p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
        p.setTextSize(30);
        String[] words=title.toUpperCase(new java.util.Locale("ru")).split(" ");
        String line="";
        int y=400;
        for(String word:words) {
            if(p.measureText(line+word)>304&&!line.isEmpty()) {
                canvas.drawText(line.trim(),25,y,p);
                line="";
                y+=35;
            }
            line+=word+" ";
        }
        canvas.drawText(line.trim(),25,y,p);
        p.setTypeface(Typeface.MONOSPACE);
        p.setTextSize(10);
        canvas.drawText("KADR ORIGINAL COLLECTION     /     0"+(art+1),25,465,p);
        canvas.restore();
    }
}
