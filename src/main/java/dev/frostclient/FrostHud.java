package dev.frostclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FrostHud {
    private static final int ICE=0xFF6ADCF3, TEXT=0xFFF3FBFF, PANEL=0xD70C1C2D, DANGER=0xFFFF536A;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static long lastSoundAt=0L;
    private FrostHud() {}
    public static void render(GuiGraphics g,DeltaTracker delta) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.options.hideGui)return;
        FrostConfig c=FrostClient.CONFIG;
        if(c.enabled("CPS Counter")) panel(g,c.pos("CPS Counter"),"CPS  "+FrostClient.CLICKS.leftCps()+" | "+FrostClient.CLICKS.rightCps());
        if(c.enabled("Ping Counter")) {
            int ping=-1;
            if(mc.getConnection()!=null) {
                var info=mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if(info!=null)ping=info.getLatency();
            }
            panel(g,c.pos("Ping Counter"),"PING  "+(ping<0?"--":ping+" ms"));
        }
        if(c.enabled("Blocks Per Second")) panel(g,c.pos("Blocks Per Second"),String.format(Locale.ROOT,"SPEED  %.1f blocks/s",FrostClient.MOVEMENT.blocksPerSecond()));
        if(c.enabled("Reach Display")) {
            String reach="REACH  --";
            if(mc.hitResult instanceof EntityHitResult result && result.getEntity() instanceof LivingEntity)
                reach=String.format(Locale.ROOT,"REACH  %.2f blocks",Math.sqrt(mc.player.getEyePosition().distanceToSqr(result.getLocation())));
            panel(g,c.pos("Reach Display"),reach);
        }
        if(c.enabled("Sprint Status")&&mc.player.isSprinting()) {
            String sprint=c.sprintDisplayMode==0?"Sprint Toggled":"» SPEED «";
            var pos=c.pos("Sprint Status");
            int sx=pos.x==0?(g.guiWidth()-mc.font.width(sprint)-12)/2:pos.x;
            int sy=pos.y<0?g.guiHeight()+pos.y:pos.y;
            panelAt(g,sx,sy,sprint);
        }
        if(c.enabled("Low Health Warning")&&mc.player.getHealth()<=c.lowHealthHearts*2) {
            g.drawCenteredString(mc.font,"!",g.guiWidth()/2,g.guiHeight()/2-29,DANGER);
        }
        if(c.enabled("Low Armor Warning")) {
            int low=0;
            StringBuilder parts=new StringBuilder();
            for(int i=0;i<ARMOR_SLOTS.length;i++) {
                ItemStack item=mc.player.getItemBySlot(ARMOR_SLOTS[i]);
                if(item.isEmpty()||!item.isDamageableItem()) continue;
                int durability=item.getMaxDamage()-item.getDamageValue();
                if(durability<=c.armorDurabilityThreshold) {
                    low++;
                    if(parts.length()>0)parts.append(", ");
                    parts.append(switch(i){case 0->"Boots";case 1->"Leggings";case 2->"Chest";default->"Helmet";});
                }
            }
            if(low>0) {
                int cx=g.guiWidth()/2, cy=g.guiHeight()/2;
                g.renderFakeItem(new ItemStack(Items.NETHERITE_CHESTPLATE),cx-9,cy+16);
                g.drawCenteredString(mc.font,parts.toString(),cx,cy+35,DANGER);
                if(c.enabled("Armor Warning Sound")&&System.currentTimeMillis()-lastSoundAt>12000) {
                    lastSoundAt=System.currentTimeMillis();
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1.5f));
                }
            }
        }
        if(c.enabled("Shield Status Indicators")) {
            boolean hasShield=mc.player.getOffhandItem().is(Items.SHIELD)||mc.player.getMainHandItem().is(Items.SHIELD);
            if(hasShield) {
                boolean stunned=mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
                int sx=g.guiWidth()/2+15, sy=g.guiHeight()/2-8;
                g.fill(sx,sy,sx+11,sy+11,stunned?0xAAFF4A57:0xAA46DB9B);
                g.drawString(mc.font,stunned?"X":"S",sx+2,sy+1,0xFFFFFFFF,true);
            }
        }
        if(c.enabled("Reach Circle")) circle(g,g.guiWidth()/2,g.guiHeight()/2,c.reachCircleRadius,0x9078D9FF);
        if(mc.hitResult instanceof EntityHitResult result && result.getEntity() instanceof LivingEntity target && target!=mc.player) {
            if(c.enabled("Target HUD")) target(g,target,c.enabled("Target Potion Effects"));
            if(c.enabled("Hitbox Viewer")) {
                // Compact target-acquisition indicator, not an X-ray or world-space AABB.
                int cx=g.guiWidth()/2,cy=g.guiHeight()/2;
                g.fill(cx-20,cy-20,cx-18,cy-7,0xCC72DFF2);
                g.fill(cx-20,cy-20,cx-7,cy-18,0xCC72DFF2);
                g.fill(cx+18,cy+7,cx+20,cy+20,0xCC72DFF2);
                g.fill(cx+7,cy+18,cx+20,cy+20,0xCC72DFF2);
            }
        }
        if(c.enabled("Health Indicators")) nearby(g,mc);
        if(c.enabled("Potion Effects Overlay")) effects(g,mc,c);
        if(c.enabled("Potion Expiry Warning")) expiry(g,mc,c);
        if(c.enabled("Waypoints HUD")) waypoints(g,mc,c);
    }
    private static void panel(GuiGraphics g,FrostConfig.HudPosition p,String text){panelAt(g,p.x,p.y,text);}
    static void panelAt(GuiGraphics g,int x,int y,String text) {
        int w=Minecraft.getInstance().font.width(text)+14;
        g.fill(x,y,x+w,y+15,PANEL);g.fill(x,y,x+2,y+15,ICE);
        g.drawString(Minecraft.getInstance().font,text,x+6,y+3,TEXT,true);
    }
    private static void target(GuiGraphics g,LivingEntity e,boolean showEffects) {
        Minecraft mc=Minecraft.getInstance();int w=204;
        int x=(g.guiWidth()-w)/2,y=9;
        g.fill(x,y,x+w,y+48,PANEL);g.fill(x,y,x+w,y+2,ICE);
        g.drawString(mc.font,e.getName(),x+9,y+7,TEXT,true);
        float hp=Math.max(0,e.getHealth()), max=Math.max(1,e.getMaxHealth());
        g.fill(x+9,y+22,x+w-9,y+30,0xFF355068);
        g.fill(x+9,y+22,x+9+Math.round((w-18)*Math.min(1f,hp/max)),y+30,ICE);
        g.drawString(mc.font,String.format(Locale.ROOT,"%.1f / %.1f HP",hp,max),x+9,y+35,0xFFC5D9EA,false);
        if(showEffects&&!e.getActiveEffects().isEmpty()) {
            StringBuilder line=new StringBuilder("Effects: ");int count=0;
            for(MobEffectInstance effect:e.getActiveEffects()) {
                if(count++>2)break;
                if(count>1)line.append(" | ");
                line.append(effect.getEffect().value().getDisplayName().getString());
            }
            g.drawCenteredString(mc.font,line.toString(),g.guiWidth()/2,y+52,0xFFB2E8F6);
        }
    }
    private static void nearby(GuiGraphics g,Minecraft mc) {
        List<Player> players=new ArrayList<>();
        for(Player p:mc.level.players()) if(!p.equals(mc.player)&&p.distanceToSqr(mc.player)<=20*20) players.add(p);
        players.sort(Comparator.comparingDouble(p->p.distanceToSqr(mc.player)));
        int y=g.guiHeight()-76;
        if(!players.isEmpty()) panelAt(g,g.guiWidth()-142,y-17,"NEARBY HEALTH");
        for(int i=0;i<Math.min(3,players.size());i++) {
            Player p=players.get(i);
            panelAt(g,g.guiWidth()-142,y+i*18,p.getName().getString()+"  "+String.format(Locale.ROOT,"%.1f",p.getHealth()/2)+" ♥");
        }
    }
    private static void circle(GuiGraphics g,int cx,int cy,int radius,int color) {
        int count=Math.max(36,radius*3);
        for(int i=0;i<count;i++) {
            double a=i*Math.PI*2/count;
            int x=cx+(int)Math.round(Math.cos(a)*radius),y=cy+(int)Math.round(Math.sin(a)*radius);
            g.fill(x,y,x+1,y+1,color);
        }
    }
    private static String roman(int n) {
        int v=n+1; if(v<=0)return "I";
        StringBuilder s=new StringBuilder();
        int[] nums={10,9,5,4,1};String[] forms={"X","IX","V","IV","I"};
        for(int i=0;i<nums.length;i++)while(v>=nums[i]){v-=nums[i];s.append(forms[i]);}
        return s.toString();
    }
    private static String duration(int ticks) {
        int seconds=Math.max(0,ticks)/20;
        return String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
    }
    private static void effects(GuiGraphics g,Minecraft mc,FrostConfig c) {
        var p=c.pos("Potion Effects Overlay");int n=0;
        for(MobEffectInstance e:mc.player.getActiveEffects()) {
            if(n++>=9)break;
            int x=p.x+((n-1)%3)*49,y=p.y+((n-1)/3)*48;
            g.fill(x,y,x+45,y+43,PANEL);
            g.fill(x,y,x+45,y+2,ICE);
            String title=e.getEffect().value().getDisplayName().getString();
            String letter=title.isEmpty()?"?":title.substring(0,1).toUpperCase(Locale.ROOT);
            g.drawCenteredString(mc.font,letter,x+14,y+7,ICE);
            g.drawString(mc.font,roman(e.getAmplifier()),x+30,y+4,TEXT,false);
            g.drawCenteredString(mc.font,duration(e.getDuration()),x+22,y+30,TEXT);
        }
    }
    private static void expiry(GuiGraphics g,Minecraft mc,FrostConfig c) {
        int n=0;
        for(MobEffectInstance e:mc.player.getActiveEffects()) {
            if(!e.endsWithin(c.potionExpirySeconds*20))continue;
            int x=g.guiWidth()/2+30+n*23,y=g.guiHeight()/2-5;
            g.fill(x,y,x+21,y+22,0xCAAE3C50);
            String text=e.getEffect().value().getDisplayName().getString();
            g.drawCenteredString(mc.font,text.isEmpty()?"!":text.substring(0,1),x+10,y+3,TEXT);
            g.drawCenteredString(mc.font,"!",x+10,y+13,TEXT);
            if(++n>=4)break;
        }
    }
    private static void waypoints(GuiGraphics g,Minecraft mc,FrostConfig c) {
        String current=mc.level.dimension().identifier().toString();int displayed=0;
        var p=c.pos("Waypoints HUD");
        for(FrostConfig.Waypoint w:c.waypoints) {
            if(!current.equals(w.dimension))continue;
            double dx=mc.player.getX()-w.x,dz=mc.player.getZ()-w.z;
            int dist=(int)Math.sqrt(dx*dx+dz*dz);
            panelAt(g,p.x,p.y+displayed*17,"   "+w.name+" "+dist+"m  "+w.x+" "+w.y+" "+w.z);
            int color=ICE;try{color=0xFF000000|Integer.parseInt(w.color.substring(1),16);}catch(Exception ignored){}
            g.fill(p.x+5,p.y+displayed*17+5,p.x+11,p.y+displayed*17+11,color);
            if(++displayed>=5)break;
        }
    }
}
