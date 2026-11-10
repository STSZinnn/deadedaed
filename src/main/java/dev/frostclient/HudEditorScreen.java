package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Drag HUD panels while previewing their placement, then save. */
public final class HudEditorScreen extends Screen {
    private static final List<String> EDITABLE=List.of("CPS Counter","Ping Counter","Blocks Per Second",
        "Reach Display","Sprint Status","Potion Effects Overlay","Waypoints HUD");
    private String dragging=null;
    private int grabX,grabY;
    public HudEditorScreen(){super(Component.literal("Frost Client HUD Editor"));}
    @Override protected void init(){
        addRenderableWidget(Button.builder(Component.literal("SAVE & BACK"),b->{FrostClient.CONFIG.save();minecraft.setScreen(new FrostScreen());}).bounds(width/2-65,height-34,130,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xE60B1828);
        g.drawCenteredString(font,"DRAG THE MODULES TO MOVE THEM",width/2,15,0xFF77E5FA);
        g.drawCenteredString(font,"Positions are saved. Use the buttons at the bottom to return.",width/2,29,0xFF9EAEBB);
        for(String key:EDITABLE){
            var p=FrostClient.CONFIG.pos(key);
            int x=p.x,y=p.y<0?height+p.y:p.y;
            int boxW=Math.max(145,font.width(key)+18);
            g.fill(x,y,x+boxW,y+18,key.equals(dragging)?0xFF365D7D:0xE52A435A);
            g.fill(x,y,x+3,y+18,0xFF66DCF5);
            g.drawString(font,key,x+7,y+5,0xFFE5F9FF,false);
        }
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()==0){
            for(int i=EDITABLE.size()-1;i>=0;i--){
                String key=EDITABLE.get(i);var p=FrostClient.CONFIG.pos(key);
                int y=p.y<0?height+p.y:p.y;
                int w=Math.max(145,font.width(key)+18);
                if(e.x()>=p.x && e.x()<=p.x+w && e.y()>=y && e.y()<=y+18){
                    dragging=key;grabX=(int)e.x()-p.x;grabY=(int)e.y()-y;return true;
                }
            }
        }
        return super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging!=null){
            int x=Math.max(0,Math.min(width-155,(int)e.x()-grabX));
            int y=Math.max(48,Math.min(height-58,(int)e.y()-grabY));
            var p=FrostClient.CONFIG.pos(dragging);p.x=x;p.y=y;return true;
        }
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){
        if(dragging!=null){dragging=null;FrostClient.CONFIG.save();return true;}
        return super.mouseReleased(e);
    }
    @Override public boolean isPauseScreen(){return false;}
}
