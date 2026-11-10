package dev.frostclient;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class FrostClient implements ClientModInitializer {
    public static final String MOD_ID="frostclient";
    public static final FrostConfig CONFIG=FrostConfig.load();
    public static final ClickTracker CLICKS=new ClickTracker();
    public static final FrostMovement MOVEMENT=new FrostMovement();
    private static final Pattern COORDINATES=Pattern.compile("(?<![0-9])(-?[0-9]{1,7})[,\\s]+(-?[0-9]{1,7})[,\\s]+(-?[0-9]{1,7})(?![0-9])");
    private static KeyMapping open;
    private static String lastChatCoords="";
    private static long lastCoordsAt=0;

    @Override public void onInitializeClient() {
        KeyMapping.Category category=KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID,"general"));
        open=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.frostclient.open_gui",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,category));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            CLICKS.tick(); MOVEMENT.tick(mc);
            while(open.consumeClick()) {
                if(mc.screen instanceof FrostScreen) mc.setScreen(null);
                else if(mc.player!=null) mc.setScreen(new FrostScreen());
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID,"hud"), FrostHud::render);
        ClientReceiveMessageEvents.CHAT.register((message,signedMessage,sender,params,receivedAt)-> detectCoordinates(message.getString()));
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{ if(!overlay) detectCoordinates(message.getString()); });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,registryAccess)->dispatcher.register(
            literal("frostwaypoint")
                .then(argument("x",IntegerArgumentType.integer())
                .then(argument("y",IntegerArgumentType.integer())
                .then(argument("z",IntegerArgumentType.integer())
                    .executes(ctx->{
                        int x=IntegerArgumentType.getInteger(ctx,"x"),y=IntegerArgumentType.getInteger(ctx,"y"),z=IntegerArgumentType.getInteger(ctx,"z");
                        Minecraft.getInstance().setScreen(new WaypointScreen(x,y,z));
                        return 1;
                    }))))));
    }
    private static void detectCoordinates(String text) {
        if(!CONFIG.enabled("Chat Coordinate Detection"))return;
        Matcher match=COORDINATES.matcher(text);
        if (!match.find()) return;
        String coords=match.group(1)+" "+match.group(2)+" "+match.group(3);
        long now=System.currentTimeMillis();
        if(coords.equals(lastChatCoords) && now-lastCoordsAt<1000) return;
        lastChatCoords=coords;lastCoordsAt=now;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        int x=Integer.parseInt(match.group(1)), y=Integer.parseInt(match.group(2)), z=Integer.parseInt(match.group(3));
        if(CONFIG.enabled("Quick Waypoint")) {
            addWaypoint("Chat waypoint",x,y,z,"#69DDF5");
            mc.player.displayClientMessage(Component.literal("§b[Frost] Quick waypoint saved: "+coords),false);
        } else {
            Component clickable=Component.literal("§b[Frost] Click to save waypoint: "+coords)
                .withStyle(s->s.withClickEvent(new ClickEvent.RunCommand("/frostwaypoint "+coords)));
            mc.gui.getChat().addMessage(clickable);
        }
    }
    public static void addWaypoint(String name,int x,int y,int z,String color) {
        Minecraft mc=Minecraft.getInstance();
        String dimension=mc.level==null?"minecraft:overworld":mc.level.dimension().identifier().toString();
        CONFIG.waypoints.add(new FrostConfig.Waypoint(name,dimension,x,y,z,color));
        CONFIG.save();
    }
}
