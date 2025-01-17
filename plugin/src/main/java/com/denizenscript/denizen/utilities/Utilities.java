package com.denizenscript.denizen.utilities;

import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.objects.*;
import com.denizenscript.denizen.objects.properties.material.MaterialDirectional;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.interfaces.BlockHelper;
import com.denizenscript.denizen.npc.traits.TriggerTrait;
import com.denizenscript.denizen.tags.BukkitTagContext;
import com.denizenscript.denizen.utilities.blocks.MaterialCompat;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class has utility methods for various tasks.
 */
public class Utilities {

    public static NamespacedKey parseNamespacedKey(String input) {
        input = CoreUtilities.toLowerCase(input);
        int colonIndex = input.indexOf(':');
        if (colonIndex != -1) {
            return new NamespacedKey(input.substring(0, colonIndex), cleanseNamespaceID(input.substring(colonIndex + 1)));
        }
        else {
            return NamespacedKey.minecraft(cleanseNamespaceID(input));
        }
    }

    public static AsciiMatcher namespaceMatcher = new AsciiMatcher("abcdefghijklmnopqrstuvwxyz" + ".-_/" + "0123456789");

    public static String cleanseNamespaceID(String input) {
        return namespaceMatcher.trimToMatches(CoreUtilities.toLowerCase(input));
    }

    public static String getRecipeType(Recipe recipe) {
        if (recipe == null) {
            return null;
        }
        if (recipe instanceof ShapedRecipe) {
            return "shaped";
        }
        else if (recipe instanceof ShapelessRecipe) {
            return "shapeless";
        }
        else if (recipe instanceof CookingRecipe) {
            if (recipe instanceof FurnaceRecipe) {
                return "furnace";
            }
            else if (recipe instanceof BlastingRecipe) {
                return "blasting";
            }
            else if (recipe instanceof CampfireRecipe) {
                return "campfire";
            }
            else if (recipe instanceof SmokingRecipe) {
                return "smoking";
            }
        }
        else if (recipe instanceof StonecuttingRecipe) {
            return "stonecutting";
        }
        Debug.echoError("Failed to determine recipe type for " + recipe.getClass().getName() + ": " + recipe);
        return null;
    }

    public static boolean isRecipeOfType(Recipe recipe, String type) {
        return type == null || (
                (type.equals("crafting") && (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)) ||
                        (type.equals("furnace") && recipe instanceof FurnaceRecipe) ||
                        (type.equals("cooking") && recipe instanceof CookingRecipe) ||
                        (type.equals("blasting") && recipe instanceof BlastingRecipe) ||
                        (type.equals("campfire") && recipe instanceof CampfireRecipe) ||
                        (type.equals("shaped") && recipe instanceof ShapedRecipe) ||
                        (type.equals("shapeless") && recipe instanceof ShapelessRecipe) ||
                        (type.equals("smoking") && recipe instanceof SmokingRecipe) ||
                        (type.equals("stonecutting") && recipe instanceof StonecuttingRecipe));
    }

    public static boolean canReadFile(File f) {
        if (Settings.allowStupids()) {
            return true;
        }
        try {
            if (!Settings.allowStrangeYAMLSaves() &&
                    !f.getCanonicalPath().startsWith(new File(".").getCanonicalPath())) {
                return false;
            }
            if (!CoreUtilities.equalsIgnoreCase(Settings.fileLimitPath(), "none")
                    && !f.getCanonicalPath().startsWith(new File("./" + Settings.fileLimitPath()).getCanonicalPath())) {
                return false;
            }
            return true;
        }
        catch (Exception ex) {
            Debug.echoError(ex);
            return false;
        }
    }

    public static boolean isFileCanonicalStringSafeToWrite(String lown) {
        if (lown.contains("denizen/config.yml")) {
            return false;
        }
        if (lown.contains("denizen/scripts/")) {
            return false;
        }
        if (lown.endsWith(".jar") || lown.endsWith(".java")) {
            return false;
        }
        if (lown.endsWith(".sh") || lown.endsWith(".bat")) {
            return false;
        }
        if (lown.endsWith("plugins/")) {
            return false;
        }
        return true;
    }

    public static boolean canWriteToFile(File f) {
        if (Settings.allowStupids()) {
            return true;
        }
        try {
            String lown = CoreUtilities.toLowerCase(f.getCanonicalPath()).replace('\\', '/');
            if (lown.endsWith("/")) {
                lown = lown.substring(0, lown.length() - 1);
            }
            if (Debug.verbose) {
                Debug.log("Checking file : " + lown);
            }
            if (!Settings.allowStrangeYAMLSaves() &&
                    !f.getCanonicalPath().startsWith(new File(".").getCanonicalPath())) {
                return false;
            }
            if (!CoreUtilities.toLowerCase(Settings.fileLimitPath()).equals("none")
                    && !f.getCanonicalPath().startsWith(new File("./" + Settings.fileLimitPath()).getCanonicalPath())) {
                return false;
            }
            return isFileCanonicalStringSafeToWrite(lown) && isFileCanonicalStringSafeToWrite(lown + "/");
        }
        catch (Exception ex) {
            Debug.echoError(ex);
            return false;
        }
    }

    public static BlockFace faceFor(Vector vec) {
        for (BlockFace face : BlockFace.values()) {
            if (face.getDirection().distanceSquared(vec) < 0.01) { // floating-point safe check
                return face;
            }
        }
        return null;
    }

    /**
     * Gets a Location within a range that an entity can walk in.
     *
     * @param location the Location to check with
     * @param range    the range around the Location
     * @return a random Location within range, or null if no Location within range is safe
     */
    public static Location getWalkableLocationNear(Location location, int range) {
        List<Location> locations = new ArrayList<>();
        location = location.getBlock().getLocation();

        // Loop through each location within the range
        for (double x = -(range); x <= range; x++) {
            for (double y = -(range); y <= range; y++) {
                for (double z = -(range); z <= range; z++) {
                    // Add each block location within range
                    Location loc = location.clone().add(x, y, z);
                    if (checkLocation(location, loc, range) && isWalkable(loc)) {
                        locations.add(loc);
                    }
                }
            }
        }

        // No safe Locations found
        if (locations.isEmpty()) {
            return null;
        }

        // Return a random Location from the list
        return locations.get(CoreUtilities.getRandom().nextInt(locations.size()));
    }

    public static boolean isWalkable(Location location) {
        if (location.getBlockY() < 1 || location.getBlockY() > 254) {
            return false;
        }
        BlockHelper blockHelper = NMSHandler.getBlockHelper();
        return location.clone().subtract(0, 1, 0).getBlock().getType().isSolid()
                && !location.getBlock().getType().isSolid()
                && !location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    /**
     * @param player the player doing the talking
     * @param npc    the npc being talked to
     * @param range  the range, in blocks, that 'bystanders' will hear he chat
     */
    public static void talkToNPC(String message, PlayerTag player, NPCTag npc, double range, ScriptTag script) {
        String replacer = String.valueOf((char) 0x04);
        // Get formats from Settings, and fill in <TEXT>
        String talkFormat = Settings.chatToNpcFormat()
                .replaceAll("(?i)<TEXT>", replacer);
        String bystanderFormat = Settings.chatToNpcOverheardFormat()
                .replaceAll("(?i)<TEXT>", replacer);

        // Fill in tags
        talkFormat = TagManager.tag(talkFormat, new BukkitTagContext(player, npc, script)).replace(replacer, message);
        bystanderFormat = TagManager.tag(bystanderFormat, new BukkitTagContext(player, npc, script)).replace(replacer, message);

        // Send message to player
        player.getPlayerEntity().sendMessage(talkFormat);

        // Send message to bystanders
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target != player.getPlayerEntity()) {
                if (target.getWorld().equals(player.getPlayerEntity().getWorld())
                        && target.getLocation().distance(player.getPlayerEntity().getLocation()) <= range) {
                    target.sendMessage(bystanderFormat);
                }
            }
        }
    }

    /**
     * Finds the closest NPC to a particular location.
     *
     * @param location The location to find the closest NPC to.
     * @param range    The maximum range to look for the NPC.
     * @return The closest NPC to the location, or null if no NPC was found
     * within the range specified.
     */
    public static NPCTag getClosestNPC_ChatTrigger(Location location, int range) {
        NPC closestNPC = null;
        double closestDistance = Math.pow(range, 2);
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (!npc.isSpawned()) {
                continue;
            }
            Location loc = npc.getStoredLocation();
            if (npc.hasTrait(TriggerTrait.class) && npc.getOrAddTrait(TriggerTrait.class).hasTrigger("CHAT") &&
                    loc.getWorld().equals(location.getWorld())
                    && loc.distanceSquared(location) < closestDistance) {
                closestNPC = npc;
                closestDistance = npc.getStoredLocation().distanceSquared(location);
            }
        }
        if (closestNPC == null) {
            return null;
        }
        return new NPCTag(closestNPC);
    }

    public static boolean checkLocationWithBoundingBox(Location baseLocation, Entity entity, double theLeeway) {
        if (!checkLocation(baseLocation, entity.getLocation(), theLeeway + 16)) {
            return false;
        }
        double distanceSq = NMSHandler.getEntityHelper().getBoundingBox(entity).distanceSquared(baseLocation.toVector());
        return distanceSq < theLeeway * theLeeway;
    }

    public static boolean checkLocation(LivingEntity entity, Location theLocation, double theLeeway) {
        return checkLocation(entity.getLocation(), theLocation, theLeeway);
    }

    public static boolean checkLocation(Location baseLocation, Location theLocation, double theLeeway) {
        if (baseLocation.getWorld() != theLocation.getWorld()) {
            return false;
        }
        return baseLocation.distanceSquared(theLocation) < theLeeway * theLeeway;
    }

    public static void setSignLines(Sign sign, String[] lines) {
        for (int n = 0; n < 4; n++) {
            AdvancedTextImpl.instance.setSignLine(sign, n, lines[n]);
        }
        sign.update();
    }

    public static BlockFace chooseSignRotation(Block signBlock) {
        BlockFace[] blockFaces = {BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};
        for (BlockFace blockFace : blockFaces) {
            Block block = signBlock.getRelative(blockFace);
            Material material = block.getType();
            if (material != Material.AIR && !MaterialCompat.isAnySign(material)) {
                return blockFace.getOppositeFace();
            }
        }
        return BlockFace.SOUTH;
    }

    public static BlockFace chooseSignRotation(String direction) {
        BlockFace[] blockFaces = {BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};
        String dirUpper = direction.toUpperCase();
        String firstChar = dirUpper.substring(0, 1);
        for (BlockFace blockFace : blockFaces) {
            if (blockFace.name().startsWith(firstChar)) {
                return blockFace;
            }
        }
        for (BlockFace blockFace : BlockFace.values()) { // Avoid valueOf which throws exceptions on failure
            if (blockFace.name().equals(dirUpper)) {
                return blockFace;
            }
        }
        return BlockFace.SOUTH;
    }

    public static void setSignRotation(BlockState signState, String direction) {
        direction = CoreUtilities.toLowerCase(direction);
        BlockFace bf;
        if (direction.startsWith("n")) {
            bf = BlockFace.NORTH;
        }
        else if (direction.startsWith("e")) {
            bf = BlockFace.EAST;
        }
        else if (direction.startsWith("s")) {
            bf = BlockFace.SOUTH;
        }
        else if (direction.startsWith("w")) {
            bf = BlockFace.WEST;
        }
        else {
            return;
        }
        MaterialTag signMaterial = new MaterialTag(signState.getBlock());
        MaterialDirectional.getFrom(signMaterial).setFacing(bf);
        signState.getBlock().setBlockData(signMaterial.getModernData());
    }

    /**
     * Extract a file from a zip or jar.
     *
     * @param jarFile  The zip/jar file to use
     * @param fileName Which file to extract
     * @param destDir  Where to extract it to
     */
    public static void extractFile(File jarFile, String fileName, String destDir) {
        java.util.jar.JarFile jar = null;
        try {
            jar = new java.util.jar.JarFile(jarFile);
            java.util.Enumeration myEnum = jar.entries();
            while (myEnum.hasMoreElements()) {
                java.util.jar.JarEntry file = (java.util.jar.JarEntry) myEnum.nextElement();
                if (CoreUtilities.equalsIgnoreCase(file.getName(), fileName)) {
                    java.io.File f = new java.io.File(destDir + "/" + file.getName());
                    if (file.isDirectory()) {
                        continue;
                    }
                    java.io.InputStream is = jar.getInputStream(file);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                    while (is.available() > 0) {
                        fos.write(is.read());
                    }
                    fos.close();
                    is.close();
                    return;
                }
            }
            Debug.echoError(fileName + " not found in the jar!");
        }
        catch (IOException e) {
            Debug.echoError(e);

        }
        finally {
            if (jar != null) {
                try {
                    jar.close();
                }
                catch (IOException e) {
                    Debug.echoError(e);
                }
            }
        }
    }

    private final static String colors = "0123456789abcdefklmnorABCDEFKLMNOR";

    public static String generateRandomColors(int count) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < count; i++) {
            ret.append(ChatColor.COLOR_CHAR).append(colors.charAt(CoreUtilities.getRandom().nextInt(colors.length())));
        }
        return ret.toString();
    }

    public static BukkitScriptEntryData getEntryData(ScriptEntry entry) {
        return (BukkitScriptEntryData) entry.entryData;
    }

    public static WorldTag entryDefaultWorld(ScriptEntry entry, boolean playerFirst) {
        EntityTag entity = entryDefaultEntity(entry, playerFirst);
        if (entity == null) {
            return new WorldTag(Bukkit.getWorlds().get(0));
        }
        return new WorldTag(entity.getWorld());
    }

    public static LocationTag entryDefaultLocation(ScriptEntry entry, boolean playerFirst) {
        EntityTag entity = entryDefaultEntity(entry, playerFirst);
        if (entity == null) {
            return null;
        }
        return entity.getLocation();
    }

    public static List<EntityTag> entryDefaultEntityList(ScriptEntry entry, boolean playerFirst) {
        EntityTag entity = entryDefaultEntity(entry, playerFirst);
        if (entity == null) {
            return null;
        }
        return Collections.singletonList(entity);
    }

    public static EntityTag entryDefaultEntity(ScriptEntry entry, boolean playerFirst) {
        BukkitScriptEntryData entryData = getEntryData(entry);
        if (playerFirst && entryData.hasPlayer() && entryData.getPlayer().isOnline()) {
            return entryData.getPlayer().getDenizenEntity();
        }
        if (entryData.hasNPC() && entryData.getNPC().isSpawned()) {
            return entryData.getNPC().getDenizenEntity();
        }
        if (entryData.hasPlayer() && entryData.getPlayer().isOnline()) {
            return entryData.getPlayer().getDenizenEntity();
        }
        return null;
    }

    public static boolean entryHasPlayer(ScriptEntry entry) {
        return getEntryData(entry).hasPlayer();
    }

    public static boolean entryHasNPC(ScriptEntry entry) {
        return getEntryData(entry).hasNPC();
    }

    public static PlayerTag getEntryPlayer(ScriptEntry entry) {
        return getEntryData(entry).getPlayer();
    }

    public static NPCTag getEntryNPC(ScriptEntry entry) {
        return getEntryData(entry).getNPC();
    }

    public static boolean isLocationYSafe(Location loc) {
        return isLocationYSafe(loc.getBlockY(), loc.getWorld());
    }

    public static boolean isLocationYSafe(double y, World world) {
        if (NMSHandler.getVersion().isAtMost(NMSVersion.v1_16)) {
            return y >= 0 && y <= 255;
        }
        if (world == null) {
            return true;
        }
        return y >= world.getMinHeight() && y <= world.getMaxHeight();
    }
}
