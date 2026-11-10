package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class WaypointScreen extends Screen {
    private final int initialX,initialY,initialZ;
    private EditBox name, coords, color;
    private int listScroll=0;
    private String info="";
    public WaypointScreen(){this(0,64,0);}
    public WaypointScreen(int x,int y,int z){super(Component.literal("Frost Waypoints"));initialX=x;initialY=y;initialZ=z;}
    @Override protected void init(){
        int x=width/2-155,y=height/2-136;
        name=new EditBox(font,x+12,y+40,285,20,Component.literal("Waypoint name"));name.setMaxLength(44);name.setValue("New Waypoint");addRenderableWidget(name);
        coords=new EditBox(font,x+12,y+80,285,20,Component.literal("XYZ"));coords.setMaxLength(100);coords.setValue(initialX+" "+initialY+" "+initialZ);addRenderableWidget(coords);
        color=new EditBox(font,x+12,y+120,110,20,Component.literal("Color"));color.setMaxLength(7);color.setValue("#69DDF5");addRenderableWidget(color);
        addRenderableWidget(Button.builder(Component.literal("SAVE WAYPOINT"),b->saveWaypoint()).bounds(x+133,y+120,164,20).build());
        addRenderableWidget(Button.builder(Component.literal("PREV"),b->{listScroll=Math.max(0,listScroll-4);refresh();}).bounds(x+12,y+246,78,19).build());
        addRenderableWidget(Button.builder(Component.literal("NEXT"),b->{listScroll+=4;refresh();}).bounds(x+95,y+246,78,19).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"),b->minecraft.setScreen(new FrostScreen())).bounds(x+200,y+246,97,19).build());
        int max=Math.min(4,Math.max(0,FrostClient.CONFIG.waypoints.size()-listScroll));
        for(int i=0;i<max;i++){
            int index=listScroll+i;
            addRenderableWidget(Button.builder(Component.literal("X"),b->{FrostClient.CONFIG.waypoints.remove(index);FrostClient.CONFIG.save();refresh();}).bounds(x+278,y+166+i*18,19,17).build());
        }
    }
    private void refresh(){clearWidgets();init();}
    private void saveWaypoint(){
        try{
            String[] pieces=coords.getValue().trim().split("[\\s,]+");
            if(pieces.length<3){info="Provide three coordinates (X Y Z)";return;}
            int x=(int)Math.floor(Double.parseDouble(pieces[0])),y=(int)Math.floor(Double.parseDouble(pieces[1])),z=(int)Math.floor(Double.parseDouble(pieces[2]));
            String hex=color.getValue().trim();if(!hex.matches("#[0-9a-fA-F]{6}")){info="Color must be #RRGGBB";return;}
            FrostClient.addWaypoint(name.getValue().isBlank()?"Waypoint":name.getValue(),x,y,z,hex);
            info="Saved waypoint";listScroll=Math.max(0,FrostClient.CONFIG.waypoints.size()-4);
            refresh();
        }catch(Exception ignored){info="Invalid coordinates";}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xE8142536);
        int x=width/2-155,y=height/2-136;
        g.fill(x-11,y-12,x+309,y+277,0xEE233B50);
        g.drawString(font,"WAYPOINT MANAGER",x+12,y-2,0xFF79DEFA,true);
        g.drawString(font,"Name",x+12,y+28,0xFFEAFBFF,false);
        g.drawString(font,"X Y Z (paste allowed)",x+12,y+67,0xFFEAFBFF,false);
        g.drawString(font,"Color",x+12,y+107,0xFFEAFBFF,false);
        g.drawString(font,"SAVED WAYPOINTS",x+12,y+148,0xFF8DE5F7,true);
        for(int i=0;i<4 && listScroll+i<FrostClient.CONFIG.waypoints.size();i++){
            var wp=FrostClient.CONFIG.waypoints.get(listScroll+i);
            String line=wp.name+"  ("+wp.x+", "+wp.y+", "+wp.z+")";
            if(font.width(line)>253)line=line.substring(0,Math.min(30,line.length()))+"...";
            g.drawString(font,line,x+13,y+169+i*18,0xFFD0E5F1,false);
        }
        g.drawString(font,info,x+12,y+230,0xFF79DEFA,false);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
