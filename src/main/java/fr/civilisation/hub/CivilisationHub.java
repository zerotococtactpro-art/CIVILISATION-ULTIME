package fr.civilisation.hub;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;

public class CivilisationHub extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private World hub;
    private Location hubSpawn, civSpawn;
    private NamespacedKey builtKey;

    @Override public void onEnable() {
        saveDefaultConfig();
        builtKey = new NamespacedKey(this, "grand-city-v3");
        createHubWorld();
        loadLocations();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("civilisation")).setExecutor(this);
        Objects.requireNonNull(getCommand("civilisation")).setTabCompleter(this);
        getLogger().info("CivilisationHub V3 - Grand Hub activé.");
    }

    private void createHubWorld() {
        String name = getConfig().getString("hub-world", "hub");
        hub = Bukkit.getWorld(name);
        if (hub == null) hub = Bukkit.createWorld(new WorldCreator(name).generateStructures(false));
        if (getConfig().getBoolean("build-city-on-first-start", true)
                && !hub.getPersistentDataContainer().has(builtKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
            buildGrandHub();
            hub.getPersistentDataContainer().set(builtKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
            hub.setSpawnLocation(0, 82, 0);
            hub.save();
        }
    }

    private void loadLocations() {
        hubSpawn = new Location(hub,
                getConfig().getDouble("hub-spawn.x", 0),
                getConfig().getDouble("hub-spawn.y", 82),
                getConfig().getDouble("hub-spawn.z", 0),
                (float)getConfig().getDouble("hub-spawn.yaw", 180),
                (float)getConfig().getDouble("hub-spawn.pitch", 0));

        World w = Bukkit.getWorld(getConfig().getString("civilisation-spawn.world", "world"));
        if (w == null) w = Bukkit.getWorlds().getFirst();
        civSpawn = new Location(w,
                getConfig().getDouble("civilisation-spawn.x", 0),
                getConfig().getDouble("civilisation-spawn.y", 80),
                getConfig().getDouble("civilisation-spawn.z", 0),
                (float)getConfig().getDouble("civilisation-spawn.yaw", 0),
                (float)getConfig().getDouble("civilisation-spawn.pitch", 0));
    }

    private void buildGrandHub() {
        final int y = 80;
        clearArea(105, y, 105);
        foundation(y);
        grandBoulevard(y);
        centralPlaza(y);
        monumentalGate(y);
        civicDistrict(y);
        commercialDistrict(y);
        policeDistrict(y);
        marketDistrict(y);
        portalDistrict(y);
        gardens(y);
        streetFurniture(y);
        giantLogoWall(y);
        floatingText(new Location(hub,0,y+10,0),"§6§lCIVILISATION");
        floatingText(new Location(hub,0,y+8,0),"§fCONSTRUIRE • DÉVELOPPER • UNIR");
    }

    private void clearArea(int rx, int y, int rz) {
        for (int x=-rx; x<=rx; x++) for (int z=-rz; z<=rz; z++) {
            for (int yy=y; yy<y+25; yy++) hub.getBlockAt(x,yy,z).setType(Material.AIR);
            hub.getBlockAt(x,y-1,z).setType(Material.STONE_BRICKS);
        }
    }

    private void foundation(int y) {
        circle(0,y,0,104,Material.STONE_BRICKS);
        circle(0,y,0,98,Material.GRASS_BLOCK);
        // Large paved ring
        ring(0,y,0,78,82,Material.POLISHED_ANDESITE);
        ring(0,y,0,82,86,Material.DEEPSLATE_TILES);
        ring(0,y,0,86,90,Material.STONE_BRICKS);
    }

    private void grandBoulevard(int y) {
        // Main north/south and east/west avenues, 9 blocks wide
        road(-88,88,0,y,false);
        road(-88,88,0,y,true);
        road(-76,76,26,y,false);
        road(-76,76,26,y,true);
        // Decorative lane lines
        for(int i=-88;i<=88;i+=4){
            hub.getBlockAt(i,y,0).setType(Material.SMOOTH_QUARTZ);
            hub.getBlockAt(0,y,i).setType(Material.SMOOTH_QUARTZ);
        }
    }

    private void centralPlaza(int y) {
        circle(0,y,0,34,Material.SMOOTH_STONE);
        ring(0,y+1,0,29,32,Material.POLISHED_BLACKSTONE);
        ring(0,y+1,0,24,27,Material.POLISHED_ANDESITE);
        ring(0,y+1,0,17,20,Material.QUARTZ_BLOCK);
        fountain(0,y+1,0,9);
        for(int[] p: new int[][]{{-28,-28},{28,-28},{-28,28},{28,28}})
            monumentPillar(p[0],y+1,p[1]);
    }

    private void monumentalGate(int y) {
        // Huge central Civilisation entrance to the north
        int z=-62;
        tower(-23,y,z,11,26);
        tower(23,y,z,11,26);
        wall(-12,y,z,24,20,Material.STONE_BRICKS);
        arch(0,y+3,z,28,18);
        // Gold C emblem made from blocks
        goldC(0,y+22,z-1,7);
        // Entrance tunnel
        for(int x=-8;x<=8;x++) for(int yy=y;yy<=y+7;yy++) {
            if(Math.abs(x)>=7 || yy<=1+y) hub.getBlockAt(x,yy,z+5).setType(Material.DEEPSLATE_BRICKS);
        }
        for(int x=-6;x<=6;x++) for(int yy=y+1;yy<=y+6;yy++)
            hub.getBlockAt(x,yy,z+5).setType(Material.PURPLE_STAINED_GLASS);
        floatingText(new Location(hub,0,y+13,z+2),"§6§lCIVILISATION");
        floatingText(new Location(hub,0,y+11,z+2),"§fVOTRE HISTOIRE COMMENCE ICI");
    }

    private void civicDistrict(int y) {
        grandBuilding(-68,y,-42,30,24,18,Material.STONE_BRICKS,Material.DARK_OAK_PLANKS,"§6§lMAIRIE");
        grandBuilding(38,y,-42,30,24,18,Material.QUARTZ_BLOCK,Material.SPRUCE_PLANKS,"§b§lBANQUE");
    }

    private void commercialDistrict(int y) {
        shop(38,y,15,"§eBOUTIQUE");
        shop(62,y,15,"§eRESSOURCES");
        shop(38,y,39,"§eENTREPRISES");
        shop(62,y,39,"§eSERVICES");
    }

    private void policeDistrict(int y) {
        grandBuilding(-70,y,25,28,20,16,Material.POLISHED_ANDESITE,Material.SPRUCE_PLANKS,"§c§lPOLICE");
        // Blue/red signal columns
        for(int x=-64;x<=-48;x+=8){
            hub.getBlockAt(x,y+16,35).setType(Material.BLUE_STAINED_GLASS);
            hub.getBlockAt(x+2,y+16,35).setType(Material.RED_STAINED_GLASS);
        }
    }

    private void marketDistrict(int y) {
        marketStalls(y, 55, -15);
        marketStalls(y, 55, 5);
    }

    private void portalDistrict(int y) {
        int z=63;
        ring(0,y,63,12,15,Material.DEEPSLATE_TILES);
        arch(0,y+2,63,22,13);
        for(int x=-6;x<=6;x++) for(int yy=y+2;yy<=y+9;yy++)
            hub.getBlockAt(x,yy,64).setType(Material.NETHER_PORTAL);
        floatingText(new Location(hub,0,y+12,65),"§5§lMONDE RP");
        floatingText(new Location(hub,0,y+10,65),"§fCliquez / utilisez §e/civilisation rp");
    }

    private void gardens(int y) {
        int[][] spots={{-42,-18},{-18,-42},{18,-42},{42,-18},{-42,18},{-18,42},{18,42},{42,18}};
        for(int[] p:spots) {
            circle(p[0],y,p[1],8,Material.GRASS_BLOCK);
            circle(p[0],y+1,p[1],5,Material.MOSS_BLOCK);
            tree(p[0],y+1,p[1]);
            lantern(p[0]+4,y+3,p[1]+4);
        }
    }

    private void streetFurniture(int y) {
        for(int x=-84;x<=84;x+=12) {
            lamp(x,y, -7); lamp(x,y,7);
        }
        for(int z=-84;z<=84;z+=12) {
            lamp(-7,y,z); lamp(7,y,z);
        }
    }

    private void giantLogoWall(int y) {
        // A large visible logo facade behind the central plaza.
        int z=18;
        for(int x=-17;x<=17;x++) for(int yy=y+4;yy<=y+18;yy++)
            hub.getBlockAt(x,yy,z).setType(Material.DEEPSLATE_BRICKS);
        goldC(0,y+11,z-1,6);
        floatingText(new Location(hub,0,y+20,z),"§6§lCIVILISATION");
    }

    private void grandBuilding(int x,int y,int z,int w,int d,int h,Material wall,Material roof,String label) {
        for(int i=x;i<x+w;i++) for(int k=z;k<z+d;k++) {
            for(int j=y;j<y+h;j++) {
                boolean edge=i==x||i==x+w-1||k==z||k==z+d-1;
                hub.getBlockAt(i,j,k).setType(edge?wall:Material.AIR);
            }
            if((i-x)%2==0) hub.getBlockAt(i,y+h,k).setType(roof);
        }
        // Corner towers
        tower(x+3,y,z+3,5,h+6);
        tower(x+w-8,y,z+3,5,h+6);
        // Windows
        for(int i=x+6;i<x+w-5;i+=5){
            for(int j=y+5;j<y+h-3;j+=5){
                hub.getBlockAt(i,j,z).setType(Material.GLASS);
                hub.getBlockAt(i,j,z+d-1).setType(Material.GLASS);
            }
        }
        hub.getBlockAt(x+w/2,y+1,z).setType(Material.DARK_OAK_DOOR);
        hub.getBlockAt(x+w/2,y+2,z).setType(Material.GLASS);
        floatingText(new Location(hub,x+w/2,y+h+4,z+d/2),label);
    }

    private void shop(int x,int y,int z,String label) {
        grandBuilding(x,y,z,18,14,10,Material.BRICKS,Material.DARK_OAK_PLANKS,label);
        for(int i=x+2;i<x+16;i+=4) hub.getBlockAt(i,y+4,z).setType(Material.WHITE_STAINED_GLASS);
    }

    private void marketStalls(int y,int x,int z) {
        for(int n=0;n<4;n++) {
            int xx=x+n*9;
            for(int i=xx;i<xx+7;i++) for(int k=z;k<z+6;k++) {
                hub.getBlockAt(i,y,k).setType(Material.OAK_PLANKS);
                if(k==z || k==z+5 || i==xx || i==xx+6) hub.getBlockAt(i,y+1,k).setType(Material.SPRUCE_FENCE);
            }
            for(int i=xx;i<xx+7;i++) for(int k=z;k<z+6;k++) hub.getBlockAt(i,y+4,k).setType(Material.WHITE_WOOL);
            hub.getBlockAt(xx+3,y+1,z+2).setType(Material.CHEST);
        }
        floatingText(new Location(hub,x+13,y+7,z+2),"§e§lMARCHÉ");
    }

    private void tower(int x,int y,int z,int r,int h) {
        for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++) {
            if(dx*dx+dz*dz<=r*r) {
                for(int j=0;j<h;j++) hub.getBlockAt(x+dx,y+j,z+dz).setType(Material.STONE_BRICKS);
                if(dx*dx+dz*dz < (r-2)*(r-2))
                    for(int j=2;j<h-2;j++) hub.getBlockAt(x+dx,y+j,z+dz).setType(Material.AIR);
            }
        }
        ring(x,y+h,z,r-1,r+1,Material.DARK_OAK_PLANKS);
        ring(x,y+h+1,z,r-2,r,Material.GOLD_BLOCK);
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++)
            hub.getBlockAt(x+dx,y+h+2,z+dz).setType(Material.DARK_OAK_PLANKS);
        hub.getBlockAt(x,y+h+3,z).setType(Material.LIGHTNING_ROD);
    }

    private void wall(int x,int y,int z,int w,int h,Material m) {
        for(int i=x;i<=x+w;i++) for(int j=y;j<=y+h;j++) hub.getBlockAt(i,j,z).setType(m);
    }

    private void arch(int x,int y,int z,int w,int h) {
        int half=w/2;
        for(int dx=-half;dx<=half;dx++) for(int dy=0;dy<=h;dy++) {
            boolean edge=Math.abs(dx)>=half-2 || dy>=h-2;
            if(edge) hub.getBlockAt(x+dx,y+dy,z).setType(Material.STONE_BRICKS);
        }
    }

    private void goldC(int x,int y,int z,int r) {
        // Pixel/block C
        for(int dx=-r;dx<=r;dx++) for(int dy=-r;dy<=r;dy++) {
            int d=dx*dx+dy*dy;
            if(d<=r*r && d>=(r-2)*(r-2) && !(dx>0 && Math.abs(dy)>r-3))
                hub.getBlockAt(x+dx,y+dy,z).setType(Material.GOLD_BLOCK);
        }
        for(int dy=-1;dy<=1;dy++) {
            hub.getBlockAt(x+r-1,y+dy,z).setType(Material.GOLD_BLOCK);
        }
    }

    private void fountain(int x,int y,int z,int r) {
        ring(x,y,z,r,r+2,Material.QUARTZ_BLOCK);
        ring(x,y+1,z,r-2,r-1,Material.POLISHED_ANDESITE);
        circle(x,y+1,z,r-3,Material.WATER);
        for(int j=2;j<=6;j++) hub.getBlockAt(x,y+j,z).setType(Material.WATER);
        for(int[] p:new int[][]{{r-1,0},{-r+1,0},{0,r-1},{0,-r+1}})
            hub.getBlockAt(x+p[0],y+1,z+p[1]).setType(Material.SEA_LANTERN);
    }

    private void monumentPillar(int x,int y,int z) {
        for(int j=0;j<7;j++) hub.getBlockAt(x,y+j,z).setType(Material.QUARTZ_BLOCK);
        hub.getBlockAt(x,y+7,z).setType(Material.GOLD_BLOCK);
    }

    private void tree(int x,int y,int z) {
        for(int j=0;j<5;j++) hub.getBlockAt(x,y+j,z).setType(Material.OAK_LOG);
        for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) for(int dy=3;dy<=7;dy++) {
            if(Math.abs(dx)+Math.abs(dz)+(dy==7?2:0)<=5)
                hub.getBlockAt(x+dx,y+dy,z+dz).setType(Material.OAK_LEAVES);
        }
    }

    private void lamp(int x,int y,int z) {
        for(int j=0;j<4;j++) hub.getBlockAt(x,y+j,z).setType(Material.POLISHED_BLACKSTONE_WALL);
        hub.getBlockAt(x,y+4,z).setType(Material.LANTERN);
    }

    private void lantern(int x,int y,int z) {
        hub.getBlockAt(x,y,z).setType(Material.STONE_BRICKS);
        hub.getBlockAt(x,y+1,z).setType(Material.LANTERN);
    }

    private void road(int a,int b,int fixed,int y,boolean vertical) {
        for(int i=Math.min(a,b);i<=Math.max(a,b);i++) for(int d=-4;d<=4;d++) {
            int x=vertical?fixed+d:i, z=vertical?i:fixed+d;
            hub.getBlockAt(x,y,z).setType(Material.POLISHED_BLACKSTONE);
            hub.getBlockAt(x,y-1,z).setType(Material.STONE_BRICKS);
        }
    }

    private void circle(int cx,int y,int cz,int r,Material m) {
        for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++)
            if(x*x+z*z<=r*r) hub.getBlockAt(cx+x,y,cz+z).setType(m);
    }

    private void ring(int cx,int y,int cz,int inner,int outer,Material m) {
        for(int x=-outer;x<=outer;x++) for(int z=-outer;z<=outer;z++) {
            int d=x*x+z*z;
            if(d<=outer*outer && d>=inner*inner) hub.getBlockAt(cx+x,y,cz+z).setType(m);
        }
    }

    private void floatingText(Location l,String text) {
        TextDisplay e=hub.spawn(l,TextDisplay.class);
        e.text(net.kyori.adventure.text.Component.text(text.replace("§","")));
        e.setBillboard(Display.Billboard.CENTER);
        e.setShadowed(true);
        e.setSeeThrough(false);
    }

    private void sendHub(Player p) {
        p.teleport(hubSpawn);
        p.sendTitle("§6§lCIVILISATION","§fBienvenue dans la capitale",10,50,15);
        p.sendMessage("§8§m--------------------------------");
        p.sendMessage("§6§l CIVILISATION §f| §7Votre aventure commence ici");
        p.sendMessage("§e/civilisation rp §7→ rejoindre le monde RP");
        p.sendMessage("§8§m--------------------------------");
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        if(!getConfig().getBoolean("force-hub-on-join",true)) return;
        Bukkit.getScheduler().runTaskLater(this,()->sendHub(e.getPlayer()),2L);
    }

    @EventHandler public void onBreak(BlockBreakEvent e) {
        if(getConfig().getBoolean("protect-hub",true) && inside(e.getBlock().getLocation())
                && !e.getPlayer().hasPermission("civilisation.hub.admin")) e.setCancelled(true);
    }

    @EventHandler public void onPlace(BlockPlaceEvent e) {
        if(getConfig().getBoolean("protect-hub",true) && inside(e.getBlock().getLocation())
                && !e.getPlayer().hasPermission("civilisation.hub.admin")) e.setCancelled(true);
    }

    private boolean inside(Location l) {
        return l.getWorld()!=null && l.getWorld().equals(hub)
                && Math.abs(l.getX())<=getConfig().getDouble("hub-radius",100)
                && Math.abs(l.getZ())<=getConfig().getDouble("hub-radius",100);
    }

    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a) {
        if(!(s instanceof Player p)){s.sendMessage("Joueur uniquement.");return true;}
        if(a.length==0 || a[0].equalsIgnoreCase("hub") || a[0].equalsIgnoreCase("tp") || a[0].equalsIgnoreCase("ville")) {
            sendHub(p); return true;
        }
        if(a[0].equalsIgnoreCase("rp") || a[0].equalsIgnoreCase("world")) {
            p.teleport(civSpawn); p.sendMessage("§6§lCIVILISATION §7→ monde RP"); return true;
        }
        if(a[0].equalsIgnoreCase("sethub") || a[0].equalsIgnoreCase("setspawn")) {
            if(!p.hasPermission("civilisation.hub.admin")) {p.sendMessage("§cPermission insuffisante.");return true;}
            String path=a[0].equalsIgnoreCase("sethub")?"hub-spawn":"civilisation-spawn";
            Location l=p.getLocation();
            getConfig().set(path+".world",l.getWorld().getName());
            getConfig().set(path+".x",l.getX()); getConfig().set(path+".y",l.getY()); getConfig().set(path+".z",l.getZ());
            getConfig().set(path+".yaw",(double)l.getYaw()); getConfig().set(path+".pitch",(double)l.getPitch());
            saveConfig(); loadLocations(); p.sendMessage("§aPosition enregistrée."); return true;
        }
        if(a[0].equalsIgnoreCase("reload")) {
            if(!p.hasPermission("civilisation.hub.admin")) {p.sendMessage("§cPermission.");return true;}
            reloadConfig(); loadLocations(); p.sendMessage("§aConfiguration rechargée."); return true;
        }
        p.sendMessage("§6/civilisation §7Hub | §6/civilisation rp §7RP | §6/civilisation sethub | §6/civilisation setspawn | §6/civilisation reload");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a) {
        return a.length==1 ? List.of("hub","rp","tp","ville","sethub","setspawn","reload") : List.of();
    }
}
