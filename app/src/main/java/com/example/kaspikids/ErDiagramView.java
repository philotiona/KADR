package com.example.kaspikids;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.os.Bundle;
import java.util.*;
import java.util.function.Consumer;
import com.example.kaspikids.CinemaDb.Row;

/** Pan, pinch, fit and tap a diagram built from PRAGMA metadata, never a bundled picture. */
public final class ErDiagramView extends View {
    private final Paint paint=new Paint(3);
    private final LinkedHashMap<String,RectF> boxes=new LinkedHashMap<>();
    private final Map<String,List<Row>> columns=new HashMap<>(),links=new HashMap<>();
    private final Map<String,String> counts=new HashMap<>();
    private final Consumer<String> onTable;
    private final ScaleGestureDetector scaleDetector;
    private float scale=.5f,offsetX=0,offsetY=0,lastX,lastY,downX,downY;
    private boolean moved=false,initialized=false;
    private float worldWidth=1430,worldHeight=1200;
    private String selected="",filter="";
    public ErDiagramView(Context c,DatabaseCatalog catalog,Consumer<String> onTable) {
        super(c);this.onTable=onTable;setContentDescription("ER-диаграмма: масштаб двумя пальцами, перемещение и выбор таблицы");
        Map<String,int[]> positions=new HashMap<>();
        String[][] layout={{"users","","movies","halls"},{"sessions","favorites","reviews","shows"},{"promos","bookings","","booking_seats"}};
        for(int r=0;r<layout.length;r++)for(int col=0;col<4;col++)if(!layout[r][col].isEmpty())positions.put(layout[r][col],new int[]{col,r});
        List<Row> tables=catalog.tables();float[] heights=new float[Math.max(6,(tables.size()+3)/4+3)];int fallback=12;
        for(Row t:tables){String name=t.s("name");List<Row> cs=catalog.columns(name);columns.put(name,cs);links.put(name,catalog.foreignKeys(name));counts.put(name,t.s("count"));if(!positions.containsKey(name)){positions.put(name,new int[]{fallback%4,fallback/4});fallback++;}int row=positions.get(name)[1];heights[row]=Math.max(heights[row],74+cs.size()*25);}
        float[] tops=new float[heights.length];tops[0]=35;for(int i=1;i<tops.length;i++)tops[i]=tops[i-1]+heights[i-1]+100;
        for(Row t:tables){String n=t.s("name");int[] p=positions.get(n);float x=45+p[0]*350,y=tops[p[1]];boxes.put(n,new RectF(x,y,x+270,y+74+columns.get(n).size()*25));worldHeight=Math.max(worldHeight,y+160+columns.get(n).size()*25);}
        scaleDetector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoomAt(d.getScaleFactor(),d.getFocusX(),d.getFocusY());moved=true;return true;}});

    }
    public Bundle saveState(){Bundle b=new Bundle();b.putFloat("scale",scale);b.putFloat("x",offsetX);b.putFloat("y",offsetY);b.putString("selected",selected);b.putString("filter",filter);return b;}
    public void restoreState(Bundle b){if(b==null)return;scale=Math.max(.10f,Math.min(3,b.getFloat("scale",.5f)));offsetX=b.getFloat("x");offsetY=b.getFloat("y");selected=b.getString("selected","");filter=b.getString("filter","");initialized=true;invalidate();}
    public int tableCount(){return boxes.size();}
    public int relationshipCount(){int n=0;for(List<Row> rs:links.values())n+=rs.size();return n;}
    public void search(String s){filter=s.trim().toLowerCase(Locale.ROOT);invalidate();}
    public void focusTable(String name){RectF r=boxes.get(name);if(r==null)return;selected=name;scale=Math.max(.35f,Math.min(1.2f,(getWidth()-30f)/r.width()));offsetX=getWidth()/2f-r.centerX()*scale;offsetY=25-r.top*scale;invalidate();}
    public void fit(){if(getWidth()==0||getHeight()==0)return;scale=Math.max(.10f,Math.min((getWidth()-24)/worldWidth,(getHeight()-24)/worldHeight));offsetX=(getWidth()-worldWidth*scale)/2;offsetY=12;initialized=true;invalidate();}
    public void zoom(float factor){zoomAt(factor,getWidth()/2f,getHeight()/2f);}
    private void zoomAt(float factor,float x,float y){float next=Math.max(.10f,Math.min(3f,scale*factor));offsetX=x-(x-offsetX)*next/scale;offsetY=y-(y-offsetY)*next/scale;scale=next;invalidate();}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){if(!initialized)fit();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xffeeeae1);c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);
        for(Map.Entry<String,List<Row>> e:links.entrySet())for(Row fk:e.getValue())drawEdge(c,fk.s("table"),e.getKey(),fk.s("from"));
        for(Map.Entry<String,RectF> e:boxes.entrySet()) {
            String n=e.getKey();RectF r=e.getValue();boolean match=!filter.isEmpty()&&(n.toLowerCase(Locale.ROOT).contains(filter)||columns.get(n).toString().toLowerCase(Locale.ROOT).contains(filter));
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xfffffdf8);c.drawRoundRect(r,8,8,paint);paint.setColor(n.equals(selected)?0xffff7447:match?0xfff5cc55:0xffe3e8e5);c.drawRect(r.left,r.top,r.right,r.top+43,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setColor(0xff191a1b);paint.setStrokeWidth(n.equals(selected)||match?3:1.5f);c.drawRoundRect(r,8,8,paint);paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(20);paint.setColor(0xff191a1b);c.drawText(n,r.left+12,r.top+28,paint);
            paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(14);c.drawText("записей: "+counts.get(n),r.left+12,r.top+62,paint);
            int i=0;for(Row col:columns.get(n)){boolean foreign=false;for(Row fk:links.get(n))if(fk.s("from").equals(col.s("name")))foreign=true;String key=(col.i("pk")>0?"PK ":"")+(foreign?"FK ":"");paint.setColor(key.isEmpty()?0xff585c5d:0xff123c53);paint.setTextSize(15);c.drawText(key+col.s("name")+" : "+col.s("type"),r.left+12,r.top+88+i++*25,paint);}
        }c.restore();paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);paint.setColor(0xff585c5d);c.drawText(Math.round(scale*100)+"% · "+boxes.size()+" таблиц · "+relationshipCount()+" связей",12,getHeight()-12,paint);
    }
    private void drawEdge(Canvas c,String parent,String child,String field){RectF a=boxes.get(parent),b=boxes.get(child);if(a==null||b==null)return;paint.setColor(parent.equals(selected)||child.equals(selected)?0xffcc502b:0xff6a7a80);paint.setStrokeWidth(parent.equals(selected)||child.equals(selected)?3:1.5f);paint.setStyle(Paint.Style.STROKE);Path path=new Path();float sx,sy,ex,ey;
        if(Math.abs(a.top-b.top)<10){sx=a.right;sy=a.centerY();ex=b.left;ey=b.centerY();path.moveTo(sx,sy);path.lineTo(ex,ey);}else{sx=a.centerX();sy=a.bottom;ex=b.centerX();ey=b.top;path.moveTo(sx,sy);if(b.top-a.bottom>500){path.lineTo(15,sy+35);path.lineTo(15,ey-35);path.lineTo(ex,ey-35);}else{float mid=(sy+ey)/2;path.lineTo(sx,mid);path.lineTo(ex,mid);}path.lineTo(ex,ey);}
        c.drawPath(path,paint);boolean horizontal=Math.abs(a.top-b.top)<10;for(int k=0;k<2;k++){if(horizontal)c.drawLine(sx+8+k*5,sy-6,sx+8+k*5,sy+6,paint);else c.drawLine(sx-6,sy+8+k*5,sx+6,sy+8+k*5,paint);}if(horizontal){c.drawCircle(ex-18,ey,4,paint);c.drawLine(ex,ey,ex-10,ey-8,paint);c.drawLine(ex,ey,ex-10,ey+8,paint);}else{c.drawCircle(ex,ey-18,4,paint);c.drawLine(ex,ey,ex-8,ey-10,paint);c.drawLine(ex,ey,ex+8,ey-10,paint);}paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.MONOSPACE);paint.setTextSize(12);c.drawText("1 : 0..N · "+field,(sx+ex)/2+5,horizontal?ey-10:ey-42,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent e){getParent().requestDisallowInterceptTouchEvent(true);scaleDetector.onTouchEvent(e);switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:lastX=downX=e.getX();lastY=downY=e.getY();moved=false;return true;case MotionEvent.ACTION_POINTER_DOWN:moved=true;return true;case MotionEvent.ACTION_POINTER_UP:int remaining=e.getActionIndex()==0?1:0;lastX=e.getX(remaining);lastY=e.getY(remaining);moved=true;return true;case MotionEvent.ACTION_MOVE:if(!scaleDetector.isInProgress()&&e.getPointerCount()==1){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(e.getX()-downX)+Math.abs(e.getY()-downY)>10)moved=true;offsetX+=dx;offsetY+=dy;invalidate();}lastX=e.getX();lastY=e.getY();return true;case MotionEvent.ACTION_UP:if(!moved&&!scaleDetector.isInProgress()){float x=(e.getX()-offsetX)/scale,y=(e.getY()-offsetY)/scale;for(Map.Entry<String,RectF> n:boxes.entrySet())if(n.getValue().contains(x,y)){selected=n.getKey();invalidate();onTable.accept(selected);break;}}performClick();getParent().requestDisallowInterceptTouchEvent(false);return true;default:return true;}}
    @Override public boolean performClick(){return super.performClick();}
}
