package cf.terminator.chunkwatcher;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class CommandHandler extends CommandBase{

    @Override
    public String getCommandName() {
        return "chunkwatcher";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "see: /chunkwatcher";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if(args.length == 0){
            sendHelp(sender);
            return;
        }
        if(args[0].equalsIgnoreCase("reload")){
            try {
                Main.CONFIG = Main.loadConfig();
                sender.addChatMessage(new ChatComponentText("Reloaded!"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new CommandException("Error! Check console for stacktrace.");
            }

            World[] worlds = DimensionManager.getWorlds();
            for(World world : worlds) {
                ImmutableSetMultimap<ChunkCoordIntPair, ForgeChunkManager.Ticket> loadedChunks = ForgeChunkManager.getPersistentChunksFor(world);
                for (Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> pos : loadedChunks.entries()) {
                    if(TicketManager.checkTicket(pos.getValue()) == false){
                        new Unloader(pos.getKey(), pos.getValue()).unload();
                    }
                }
            }



        }else if(args[0].equalsIgnoreCase("list")){
            World world;
            if(isPlayer(sender) && args.length == 1) {
                world = sender.getEntityWorld();
            }else{
                if(args.length == 2) {
                    try {
                        world = MinecraftServer.getServer().worldServerForDimension(Integer.valueOf(args[1]));
                    } catch (NumberFormatException e) {
                        throw new CommandException(args[1] + " is not a number!");
                    }
                }else{
                    throw new CommandException("Please provide a dimension id! (EG: Overworld is '0')");
                }
            }
            HashMap<String, Integer> count = new HashMap<String, Integer>();
            ImmutableSetMultimap<ChunkCoordIntPair, ForgeChunkManager.Ticket> loadedChunks = ForgeChunkManager.getPersistentChunksFor(world);
            for(Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> pos : loadedChunks.entries()) {
                Integer oldValue = count.get(pos.getValue().getModId());
                if (oldValue == null){
                    count.put(pos.getValue().getModId(), 1);
                }else{
                    count.put(pos.getValue().getModId(), oldValue + 1);
                }
            }
            sender.addChatMessage(new ChatComponentText("-- Loaded chunks:"));
            int i = 0;
            int m = 0;
            for(Map.Entry<String, Integer> entry : count.entrySet()){
                sender.addChatMessage(new ChatComponentText(entry.getKey() + ": " + entry.getValue()));
                i = i + entry.getValue();
                m++;
            }
            sender.addChatMessage(new ChatComponentText("Total forced chunks: " + i));
            sender.addChatMessage(new ChatComponentText("Total mods forcing: " + m));

        }else if(args[0].equalsIgnoreCase("show")){
            World world;
            if(args.length == 1){
                throw new CommandException("Missing argument: mod name");
            }else if(args.length == 2 && isPlayer(sender)){
                world = sender.getEntityWorld();
            }else if(args.length == 2){
                throw new CommandException("Please provide a dimension id! (EG: Overworld is '0')");
            }else if(args.length == 3){
                try {
                    world = MinecraftServer.getServer().worldServerForDimension(Integer.valueOf(args[2]));
                } catch (NumberFormatException e) {
                    throw new CommandException(args[2] + " is not a number!");
                }
            }else{
                throw new CommandException("Too many arguments!");
            }
            String mod = args[1].toLowerCase();
            ImmutableSetMultimap<ChunkCoordIntPair, ForgeChunkManager.Ticket> loadedChunks = ForgeChunkManager.getPersistentChunksFor(world);

            ChatComponentText text = new ChatComponentText("[X,Z]");
            boolean toggle = true;
            for(Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> pos : loadedChunks.entries()) {
                if(pos.getValue().getModId().toLowerCase().equals(mod)){
                    toggle = !toggle;
                    int x = pos.getKey().getCenterXPos();
                    int z = pos.getKey().getCenterZPosition();
                    ChatStyle style = new ChatStyle();

                    style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + x + " 100 " + z));
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("/tp " + x + " 100 " + z)));
                    ChatComponentText msg = new ChatComponentText(" [" + x + "," + z + "]");
                    if(toggle){
                        style.setColor(EnumChatFormatting.AQUA);
                    }else{
                        style.setColor(EnumChatFormatting.GREEN);
                    }
                    msg.setChatStyle(style);
                    text.appendSibling(msg);
                }
            }
            sender.addChatMessage(text);
        }else if(args[0].equalsIgnoreCase("block")){
            if(args.length != 2){
                throw new CommandException("Usage: /chunkwatcher block [Mod name]");
            }
            addToBlacklist(args[1].toLowerCase());
            sender.addChatMessage(new ChatComponentText(args[1] + " can no longer force-chunkload."));
            sender.addChatMessage(new ChatComponentText("If you want to unload the chunks now, use /chunkwatcher reload"));
        }else if(args[0].equalsIgnoreCase("allow")){
            if(args.length != 2){
                throw new CommandException("Usage: /chunkwatcher allow [Mod name]");
            }
            removeFromBlacklist(args[1].toLowerCase());
            sender.addChatMessage(new ChatComponentText(args[1] + " is now allowed to force-chunkload."));
        }else{
            sendHelp(sender);
        }
    }

    private void addToBlacklist(String mod) throws CommandException{
        for(JsonElement blacklisted :Main.CONFIG.getAsJsonArray("Blacklist")){
            if(mod.equals(blacklisted.getAsString().toLowerCase())){
                throw new CommandException("Mod was already blocked!");
            }
        }
        JsonArray old = Main.CONFIG.getAsJsonArray("Blacklist");
        old.add(new JsonPrimitive(mod));
        Main.CONFIG.add("Blacklist",old);
        try {
            Main.writeConfig(Main.CONFIG);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommandException("Error! Check console for stacktrace.");
        }
    }

    private void removeFromBlacklist(String mod) throws CommandException{
        boolean didRemove = false;
        JsonArray list = new JsonArray();
        for(JsonElement e : Main.CONFIG.getAsJsonArray("Blacklist")){
            if (mod.equals(e.getAsString())) {
                didRemove = true;
            }else{
                list.add(e.getAsJsonPrimitive());
            }
        }
        if(didRemove == false){
            throw new CommandException("Mod was already allowed to force-load chunks!");
        }
        Main.CONFIG.add("Blacklist", list);
        try {
            Main.writeConfig(Main.CONFIG);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommandException("Error! Check console for stacktrace.");
        }
    }

    private void sendHelp(ICommandSender sender){
        sender.addChatMessage(new ChatComponentText("Usage:"));
        sender.addChatMessage(new ChatComponentText("/chunkwatcher reload"));
        sender.addChatMessage(new ChatComponentText("     reloads the config"));
        sender.addChatMessage(new ChatComponentText("/chunkwatcher list [DIM_ID]"));
        sender.addChatMessage(new ChatComponentText("     Shows forced chunks"));
        sender.addChatMessage(new ChatComponentText("/chunkwatcher show <mod name> [DIM_ID]"));
        sender.addChatMessage(new ChatComponentText("     Shows chunk locations"));
        sender.addChatMessage(new ChatComponentText("/chunkwatcher block <mod name>"));
        sender.addChatMessage(new ChatComponentText("     Prevent a mod to force chunks"));
        sender.addChatMessage(new ChatComponentText("/chunkwatcher allow <mod name>"));
        sender.addChatMessage(new ChatComponentText("     Allow a mod to force chunks"));
    }

    private static boolean isPlayer(ICommandSender sender) {
        String name = sender.getCommandSenderName();
        return !((name.equals("Server")) || (name.equals("Rcon")));
    }


}
