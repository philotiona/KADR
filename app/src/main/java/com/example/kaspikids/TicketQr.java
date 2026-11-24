package com.example.kaspikids;
import java.nio.charset.StandardCharsets;
/** QR Code Model 2 version 1, error correction L, byte mode, fixed mask 0 (17 UTF-8 bytes). */
public final class TicketQr {
    private TicketQr() {
    }
    public static boolean[][] encode(String text) {
        byte[] data=text.getBytes(StandardCharsets.UTF_8);
        CinemaRules.require(data.length<=17,"QR payload too long");
        int[] words=new int[26];
        int bit=0;
        int[] bits=new int[208];
        bit=append(bits,bit,4,4);
        bit=append(bits,bit,data.length,8);
        for(byte b:data)bit=append(bits,bit,b&255,8);
        bit=append(bits,bit,0,Math.min(4,152-bit));
        while(bit%8!=0)bit=append(bits,bit,0,1);
        for(int i=0; i<bit; i++)words[i/8]|=bits[i]<<(7-i%8);
        for(int i=(bit+7)/8; i<19; i++)words[i]=((i-(bit+7)/8)%2==0)?236:17;
        int[] divisor=new int[7];
        divisor[6]=1;
        int root=1;
        for(int i=0; i<7; i++) {
            for(int j=0; j<7; j++) {
                divisor[j]=multiply(divisor[j],root);
                if(j+1<7)divisor[j]^=divisor[j+1];
            }
            root=multiply(root,2);
        }
        int[] rem=new int[7];
        for(int i=0; i<19; i++) {
            int factor=words[i]^rem[0];
            System.arraycopy(rem,1,rem,0,6);
            rem[6]=0;
            for(int j=0; j<7; j++)rem[j]^=multiply(divisor[j],factor);
        }
        System.arraycopy(rem,0,words,19,7);
        boolean[][] grid=new boolean[21][21],fn=new boolean[21][21];
        for(int i=0; i<21; i++) {
            set(grid,fn,6,i,i%2==0);
            set(grid,fn,i,6,i%2==0);
        }
        finder(grid,fn,3,3);
        finder(grid,fn,17,3);
        finder(grid,fn,3,17);
        int format=(1<<3),value=format<<10;
        for(int i=14; i>=10; i--)if(((value>>>i)&1)!=0)value^=0x537<<(i-10);
        format=((format<<10)|value)^0x5412;
        for(int i=0; i<=5; i++)set(grid,fn,8,i,((format>>>i)&1)!=0);
        set(grid,fn,8,7,((format>>>6)&1)!=0);
        set(grid,fn,8,8,((format>>>7)&1)!=0);
        set(grid,fn,7,8,((format>>>8)&1)!=0);
        for(int i=9; i<15; i++)set(grid,fn,14-i,8,((format>>>i)&1)!=0);
        for(int i=0; i<8; i++)set(grid,fn,20-i,8,((format>>>i)&1)!=0);
        for(int i=8; i<15; i++)set(grid,fn,8,6+i,((format>>>i)&1)!=0);
        set(grid,fn,8,13,true);
        int index=0;
        for(int right=20; right>=1; right-=2) {
            if(right==6)right=5;
            for(int vert=0; vert<21; vert++)for(int j=0; j<2; j++) {
                int x=right-j,y=((right+1)&2)==0?20-vert:vert;
                if(!fn[y][x]) {
                    boolean v=index<208&&((words[index>>>3]>>>(7-(index&7)))&1)!=0;
                    grid[y][x]=v^((x+y)%2==0);
                    index++;
                }
            }
        }
        return grid;
    }
    private static int append(int[] a,int at,int v,int count) {
        for(int i=count-1; i>=0; i--)a[at++]=(v>>>i)&1;
        return at;
    }
    private static void set(boolean[][] g,boolean[][] f,int x,int y,boolean v) {
        g[y][x]=v;
        f[y][x]=true;
    }
    private static void finder(boolean[][] g,boolean[][] f,int x,int y) {
        for(int dy=-4; dy<=4; dy++)for(int dx=-4; dx<=4; dx++) {
            int xx=x+dx,yy=y+dy;
            if(xx>=0&&xx<21&&yy>=0&&yy<21) {
                int d=Math.max(Math.abs(dx),Math.abs(dy));
                set(g,f,xx,yy,d!=2&&d!=4);
            }
        }
    }
    private static int multiply(int a,int b) {
        int z=0;
        for(int i=7; i>=0; i--) {
            z=(z<<1)^((z>>>7)*0x11d);
            z^=((b>>>i)&1)*a;
        }
        return z;
    }
}
