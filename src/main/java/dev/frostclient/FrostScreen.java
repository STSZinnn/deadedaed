package dev.frostclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Main panel with module toggles, numeric settings and utility screens. */
public final class FrostScreen extends Screen {
    private static final int ICE=0xFF69DDF5,TEXT=0xFFEAFBFF,MUTED=0xFF82A7C0;
    private final long opened=System.currentTimeMillis();
    private int page=0;
    public FrostScreen(){super(Component.literal("Frost Client"));}
    private void button(String label,int x,int y,int width,int h,Runnable callback) {
        addRenderableWidget(Button.builder(Component.literal(label),b->callback.run()).bounds(x,y,width,h).build());
    }
    private void rebuild() { clearWidgets();init(); }
    @Override protected void init() {
        int w=Math.min(570,width-26),h=Math.min(380,height-24),x=(width-w)/2,y=(height-h)/2;
        button("MODS",x+12,y+56,100,20,()->{page=0;rebuild();});
        button("SETTINGS",x+12,y+82,100,20,()->{page=1;rebuild();});
        button("HUD EDITOR",x+12,y+108,100,20,()->minecraft.setScreen(new HudEditorScreen()));
        button("CALCULATOR",x+12,y+134,100,20,()->minecraft.setScreen(new CalculatorScreen()));
        button("NETHER CONV.",x+12,y+160,100,20,()->minecraft.setScreen(new NetherConverterScreen()));
        button("WAYPOINTS",x+12,y+186,100,20,()->minecraft.setScreen(new WaypointScreen()));
        button("CLOSE",x+12,y+h-32,100,20,()->minecraft.setScreen(null));
        if(page==0) {
            int baseX=x+130,startY=y+62;
            for(int i=0;i<Math.min(rowsPerPage(),FrostConfig.MODULES.length-pageOffset());i++) {
                String key=FrostConfig.MODULES[i+pageOffset()];
                final String f=key;
                button(FrostClient.CONFIG.enabled(key)?"ON":"OFF",x+w-76,startY+i*27,61,19,()->{FrostClient.CONFIG.toggle(f);rebuild();});
            }
            if(pageOffset()>0)button("< PREV",x+134,y+h-35,80,20,()->{scroll=Math.max(0,scroll-rowsPerPage());rebuild();});
            if(pageOffset()+rowsPerPage()<FrostConfig.MODULES.length)button("NEXT >",x+w-91,y+h-35,80,20,()->{scroll+=rowsPerPage();rebuild();});
        } else {
            int startX=x+282;
            numeric(x+138,y+68,startX,"Low Health Hearts",()->String.format("%.1f",FrostClient.CONFIG.lowHealthHearts),
                ()->{FrostClient.CONFIG.lowHealthHearts=Math.max(.5,FrostClient.CONFIG.lowHealthHearts-.5);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.lowHealthHearts=Math.min(20,FrostClient.CONFIG.lowHealthHearts+.5);FrostClient.CONFIG.save();rebuild();});
            numeric(x+138,y+101,startX,"Armor Durability",()->""+FrostClient.CONFIG.armorDurabilityThreshold,
                ()->{FrostClient.CONFIG.armorDurabilityThreshold=Math.max(1,FrostClient.CONFIG.armorDurabilityThreshold-5);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.armorDurabilityThreshold=Math.min(1000,FrostClient.CONFIG.armorDurabilityThreshold+5);FrostClient.CONFIG.save();rebuild();});
            numeric(x+138,y+134,startX,"Potion Expiry (s)",()->""+FrostClient.CONFIG.potionExpirySeconds,
                ()->{FrostClient.CONFIG.potionExpirySeconds=Math.max(1,FrostClient.CONFIG.potionExpirySeconds-1);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.potionExpirySeconds=Math.min(120,FrostClient.CONFIG.potionExpirySeconds+1);FrostClient.CONFIG.save();rebuild();});
            numeric(x+138,y+167,startX,"Reach Circle Radius",()->""+FrostClient.CONFIG.reachCircleRadius,
                ()->{FrostClient.CONFIG.reachCircleRadius=Math.max(8,FrostClient.CONFIG.reachCircleRadius-2);FrostClient.CONFIG.save();rebuild();},
                ()->{FrostClient.CONFIG.reachCircleRadius=Math.min(120,FrostClient.CONFIG.reachCircleRadius+2);FrostClient.CONFIG.save();rebuild();});
            button("Sprint Display: "+(FrostClient.CONFIG.sprintDisplayMode==0?"Text":"Icon"),x+139,y+210,260,20,()->{
                FrostClient.CONFIG.sprintDisplayMode=1-FrostClient.CONFIG.sprintDisplayMode;FrostClient.CONFIG.save();rebuild();});
        }
    }
    private int scroll=0;
    private int pageOffset(){return scroll;}
    private int rowsPerPage(){return Math.max(3, Math.min(9,(Math.min(380,height-24)-126)/27));}
    private interface ValueLabel {String get();}
    private void numeric(int x,int y,int right,String name,ValueLabel label,Runnable minus,Runnable plus){
        button("-",right,y+13,24,20,minus);button("+",right+116,y+13,24,20,plus);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        float t=Math.min(1f,(System.currentTimeMillis()-opened)/230f), eased=1f-(1f-t)*(1f-t);
        g.fill(0,0,width,height,((int)(145*eased)<<24)|0x07101F);
        int w=Math.min(570,width-26),h=Math.min(380,height-24),x=(width-w)/2,y=(height-h)/2;
        int pw=Math.round(w*(.94f+.06f*eased)),ph=Math.round(h*(.94f+.06f*eased));
        int px=x+(w-pw)/2,py=y+(h-ph)/2;
        g.fill(px,py,px+pw,py+ph,0xF0122439);
        g.fill(px,py,px+pw,py+3,ICE);
        g.drawString(font,"FROST CLIENT",x+15,y+15,TEXT,true);
        g.drawString(font,"FABRIC 1.21.11  /  PvP & utility",x+15,y+31,MUTED,false);
        g.fill(x+125,y+54,x+126,y+h-12,0xFF37657A);
        if(page==0) {
            g.drawString(font,"CLIENT MODULES",x+140,y+40,ICE,false);
            for(int i=0;i<Math.min(rowsPerPage(),FrostConfig.MODULES.length-pageOffset());i++) {
                int ry=y+62+i*27;String name=FrostConfig.MODULES[i+pageOffset()];
                g.fill(x+134,ry-2,x+w-12,ry+22,0x80304458);
                g.drawString(font,name,x+143,ry+6,TEXT,false);
            }
            g.drawString(font,"Page "+(scroll/rowsPerPage()+1)+" / "+((FrostConfig.MODULES.length+rowsPerPage()-1)/rowsPerPage()),x+235,y+h-31,MUTED,false);
        } else {
            g.drawString(font,"CONFIGURATION",x+140,y+40,ICE,false);
            String[] titles={"Low Health Hearts","Armor Durability","Potion Expiry (s)","Reach Circle Radius"};
            String[] vals={""+FrostClient.CONFIG.lowHealthHearts,""+FrostClient.CONFIG.armorDurabilityThreshold,
                ""+FrostClient.CONFIG.potionExpirySeconds,""+FrostClient.CONFIG.reachCircleRadius};
            for(int i=0;i<4;i++) {
                int sy=y+68+i*33;
                g.drawString(font,titles[i],x+142,sy,TEXT,false);
                g.drawString(font,vals[i],x+337,sy+19,ICE,true);
            }
            g.drawString(font,"Settings save to config/frostclient.json",x+142,y+244,MUTED,false);
        }
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==GLFW.GLFW_KEY_RIGHT_SHIFT){minecraft.setScreen(null);return true;}
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen(){return false;}
}
