package fr.civilisation.core;

import fr.civilisation.hub.CivilisationHub;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Monolithique "core" du serveur Civilisation : économie, banque, métiers,
 * immobilier, staff, administration et sécurité RP. Le constructeur de Hub
 * reste fourni par CivilisationHub et est réutilisé par héritage.
 */
public final class CivilisationUltimate extends CivilisationHub {
    private File coreFile;
    private FileConfiguration core;
    private NamespacedKey deviceKey;
    private final Map<UUID,String> staff = new HashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();

    private static final List<String> JOBS = List.of(
            "BANQUIER","POLICIER","MEDECIN","AVOCAT","AGENT_IMMOBILIER",
            "COMMERCANT","MINEUR","AGRICULTEUR","PECHEUR","MECANICIEN",
            "TAXI","JOURNALISTE"
    );

    @Override public void onEnable() {
        super.onEnable();
        deviceKey = new NamespacedKey(this,"civ-device");
        coreFile = new File(getDataFolder(),"core-data.yml");
        core = YamlConfiguration.loadConfiguration(coreFile);
        loadStaff();
        register("banque"); register("job"); register("parcelle"); register("staff");
        register("civadmin"); register("pay"); register("eco"); register("securite");
        Objects.requireNonNull(getCommand("civilisation")).setExecutor(this);
        Objects.requireNonNull(getCommand("civilisation")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getScheduler().runTaskTimer(this,this::salaryTick,20L*60L,20L*60L*10L);
        getLogger().info("CivilisationUltimate: economie + metiers + immobilier + staff + securite activés.");
    }

    private void register(String name){ Objects.requireNonNull(getCommand(name)).setExecutor(this); Objects.requireNonNull(getCommand(name)).setTabCompleter(this); }
    @Override public void onDisable(){ saveCore(); super.onDisable(); }

    private void loadStaff(){
        if(core.getConfigurationSection("staff") == null) return;
        for(String k: core.getConfigurationSection("staff").getKeys(false)) staff.put(UUID.fromString(k),core.getString("staff."+k,"JOUEUR"));
    }
    private void saveCore(){
        for(var e:staff.entrySet()) core.set("staff."+e.getKey(),e.getValue());
        try{core.save(coreFile);}catch(IOException e){getLogger().warning("core-data.yml: "+e.getMessage());}
    }
    private String akey(UUID u){return "accounts."+u;}
    private double cash(UUID u){return core.getDouble(akey(u)+".cash",getConfig().getDouble("economy.starting-cash",500));}
    private double bank(UUID u){return core.getDouble(akey(u)+".bank",getConfig().getDouble("economy.starting-bank",2500));}
    private void cash(UUID u,double v){core.set(akey(u)+".cash",Math.max(0,v));}
    private void bank(UUID u,double v){core.set(akey(u)+".bank",Math.max(0,v));}
    private String money(double v){return String.format(Locale.US,"%,.2f",v);}
    private void tx(UUID u,String text){
        List<String> l=core.getStringList(akey(u)+".history"); l.add(System.currentTimeMillis()+" | "+text);
        while(l.size()>10) l.remove(0); core.set(akey(u)+".history",l);
    }

    // ================= BANK =================
    private boolean inBank(Player p){
        Location l=p.getLocation();
        if(l.getWorld()==null || !l.getWorld().getName().equals(getConfig().getString("bank-zone.world","hub"))) return false;
        return l.getX()>=getConfig().getDouble("bank-zone.min-x") && l.getX()<=getConfig().getDouble("bank-zone.max-x")
                && l.getY()>=getConfig().getDouble("bank-zone.min-y") && l.getY()<=getConfig().getDouble("bank-zone.max-y")
                && l.getZ()>=getConfig().getDouble("bank-zone.min-z") && l.getZ()<=getConfig().getDouble("bank-zone.max-z");
    }
    private void bankMenu(Player p){
        if(!inBank(p)){p.sendMessage("§cLa banque est accessible uniquement dans la zone bancaire.");return;}
        Inventory inv=Bukkit.createInventory(null,27,"§1§lBANQUE • CIVILISATION");
        item(inv,10,Material.GOLD_INGOT,"§aDéposer 100$","§7Argent liquide → banque");
        item(inv,12,Material.EMERALD,"§cRetirer 100$","§7Banque → argent liquide");
        item(inv,14,Material.PAPER,"§bMon compte","§7Liquide: §f"+money(cash(p.getUniqueId()))+"$","§7Banque: §f"+money(bank(p.getUniqueId()))+"$");
        item(inv,16,Material.BOOK,"§eHistorique","§710 dernières transactions");
        p.openInventory(inv);
    }
    private void deposit(Player p,double n){
        if(!inBank(p)){p.sendMessage("§cZone bancaire requise.");return;}
        if(cash(p.getUniqueId())<n){p.sendMessage("§cPas assez de liquide.");return;}
        cash(p.getUniqueId(),cash(p.getUniqueId())-n);bank(p.getUniqueId(),bank(p.getUniqueId())+n);tx(p.getUniqueId(),"Dépôt +"+money(n)+"$");saveCore();p.sendMessage("§aDépôt effectué: §e"+money(n)+"$");
    }
    private void withdraw(Player p,double n){
        if(!inBank(p)){p.sendMessage("§cZone bancaire requise.");return;}
        if(bank(p.getUniqueId())<n){p.sendMessage("§cPas assez en banque.");return;}
        bank(p.getUniqueId(),bank(p.getUniqueId())-n);cash(p.getUniqueId(),cash(p.getUniqueId())+n);tx(p.getUniqueId(),"Retrait -"+money(n)+"$");saveCore();p.sendMessage("§aRetrait effectué: §e"+money(n)+"$");
    }

    // ================= JOBS =================
    private void jobMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,54,"§0§lMÉTIERS • CARRIÈRE");
        for(int i=0;i<JOBS.size();i++){
            String j=JOBS.get(i), status=core.getString("jobs."+p.getUniqueId()+".status."+j,"none");
            String c=status.equals("approved")?"§a":status.equals("pending")?"§e":"§7";
            item(inv,i,Material.BOOK,"§f"+j,c+"Statut: "+status,"§7Cliquez pour candidater / sélectionner");
        }
        item(inv,49,Material.NETHER_STAR,"§6Métier actuel","§7"+core.getString("jobs."+p.getUniqueId()+".current","AUCUN"));
        p.openInventory(inv);
    }
    private void applyJob(Player p,String job){
        String status=core.getString("jobs."+p.getUniqueId()+".status."+job,"none");
        if(status.equals("approved")){core.set("jobs."+p.getUniqueId()+".current",job);saveCore();p.sendMessage("§aMétier actif: §e"+job);return;}
        core.set("jobs."+p.getUniqueId()+".status."+job,"pending");saveCore();p.sendMessage("§eCandidature envoyée au staff pour validation: §f"+job);
    }
    private void approveJob(CommandSender s,String[] a){
        if(!isStaff(s,50)||a.length<3){s.sendMessage("§c/staff jobapprove <joueur> <métier>");return;}
        Player t=Bukkit.getPlayerExact(a[1]);String job=a[2].toUpperCase();
        if(t==null||!JOBS.contains(job)){s.sendMessage("§cJoueur ou métier invalide.");return;}
        core.set("jobs."+t.getUniqueId()+".status."+job,"approved");core.set("jobs."+t.getUniqueId()+".current",job);saveCore();
        t.sendMessage("§aVotre candidature a été acceptée: §e"+job);s.sendMessage("§aMétier validé.");
    }

    // ================= PLOTS 4x4 CHUNKS =================
    private String pk(int cx,int cz){return "plots."+cx+"."+cz;}
    private String ownerAt(Location l){return core.getString(pk(l.getChunk().getX(),l.getChunk().getZ())+".owner","");}
    private boolean canBuild(Player p,Location l){String o=ownerAt(l);return o.isEmpty()||o.equals(p.getUniqueId().toString())||isStaff(p,20);}
    private void plotMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,27,"§2§lIMMOBILIER • 64x64");
        double buy=getConfig().getDouble("plots.price-per-chunk",25000)*16;
        item(inv,11,Material.GRASS_BLOCK,"§aAcheter 4x4 chunks","§764x64 blocs","§7Prix: §e"+money(buy)+"$");
        item(inv,13,Material.PAPER,"§bMa parcelle","§7Voir la propriété du chunk actuel");
        item(inv,15,Material.CLOCK,"§eLouer 24h","§7Prix: §e"+money(getConfig().getDouble("plots.rent-per-day",1200))+"$");
        p.openInventory(inv);
    }
    private void buyPlot(Player p,boolean rent){
        Chunk c=p.getLocation().getChunk();int bx=Math.floorDiv(c.getX(),4)*4,bz=Math.floorDiv(c.getZ(),4)*4;
        double price=rent?getConfig().getDouble("plots.rent-per-day",1200):getConfig().getDouble("plots.price-per-chunk",25000)*16;
        if(cash(p.getUniqueId())<price){p.sendMessage("§cPas assez de liquide.");return;}
        for(int x=0;x<4;x++)for(int z=0;z<4;z++)if(!core.getString(pk(bx+x,bz+z)+".owner","").isEmpty()){p.sendMessage("§cLa zone est déjà occupée.");return;}
        for(int x=0;x<4;x++)for(int z=0;z<4;z++){core.set(pk(bx+x,bz+z)+".owner",p.getUniqueId().toString());core.set(pk(bx+x,bz+z)+".type",rent?"rent":"buy");if(rent)core.set(pk(bx+x,bz+z)+".until",System.currentTimeMillis()+86400000L);}
        cash(p.getUniqueId(),cash(p.getUniqueId())-price);tx(p.getUniqueId(),(rent?"Location":"Achat")+" parcelle 64x64 -"+money(price)+"$");saveCore();
        p.sendMessage("§aParcelle "+(rent?"louée":"achetée")+": §e64x64 blocs§a.");
    }

    // ================= STAFF =================
    private String rank(UUID u){return staff.getOrDefault(u,"JOUEUR");}
    private int power(String r){return switch(r){case "FONDA"->100;case "SUPER_ADMIN"->90;case "ADMIN"->80;case "MODO"->50;case "HELPER"->20;default->0;};}
    private boolean isStaff(CommandSender s,int p){return s instanceof Player pl && (power(rank(pl.getUniqueId()))>=p || pl.hasPermission("civilisation.admin"));}
    private void staffMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,54,"§4§lSTAFF • PANEL CENTRAL");
        item(inv,10,Material.NETHER_STAR,"§6Profil Staff","§7Grade: §f"+rank(p.getUniqueId()),"§7Puissance: §f"+power(rank(p.getUniqueId())));
        item(inv,12,Material.ENDER_EYE,"§bVanish","§7Cliquez pour basculer");
        item(inv,14,Material.ICE,"§3Freeze","§7Commande: /staff freeze <joueur>");
        item(inv,16,Material.BARRIER,"§cSanctions","§7Ban, mute, kick");
        item(inv,28,Material.COMPASS,"§eTéléportation","§7/staff tp <joueur>");
        item(inv,30,Material.GOLD_BLOCK,"§6Économie","§7/eco give|take");
        item(inv,32,Material.WRITABLE_BOOK,"§dMétiers","§7/staff jobapprove");
        item(inv,34,Material.PLAYER_HEAD,"§fGestion staff","§7Grades hiérarchiques");
        if(power(rank(p.getUniqueId()))>=80)item(inv,40,Material.COMMAND_BLOCK,"§5ADMIN CORE","§7/civadmin");
        p.openInventory(inv);
    }
    private void adminMenu(Player p){
        if(!isStaff(p,80)){p.sendMessage("§cAccès ADMIN requis.");return;}
        Inventory inv=Bukkit.createInventory(null,54,"§5§lADMIN CORE • CIVILISATION");
        item(inv,10,Material.GOLD_BLOCK,"§6Économie","§7Gestion des comptes");
        item(inv,12,Material.PLAYER_HEAD,"§bJoueurs","§7Actions staff");
        item(inv,14,Material.NETHER_STAR,"§dGrades","§7FONDA > SUPER_ADMIN > ADMIN > MODO > HELPER");
        item(inv,16,Material.GRASS_BLOCK,"§aImmobilier","§7Parcelles 64x64");
        item(inv,28,Material.REDSTONE,"§cSécurité","§7Caméras, alarmes, coffres");
        item(inv,30,Material.COMPASS,"§eMonde","§7Hub / RP");
        item(inv,32,Material.BOOK,"§fLogs","§7Historique économie");
        item(inv,34,Material.BEACON,"§6Système","§7Reload / sauvegarde");
        p.openInventory(inv);
    }
    private void setRank(CommandSender s,String[]a){
        if(!isStaff(s,90)||a.length<3){s.sendMessage("§c/staff setrank <joueur> <FONDA|SUPER_ADMIN|ADMIN|MODO|HELPER>");return;}
        Player t=Bukkit.getPlayerExact(a[1]);String r=a[2].toUpperCase();if(t==null||!List.of("FONDA","SUPER_ADMIN","ADMIN","MODO","HELPER").contains(r)){s.sendMessage("§cDonnées invalides.");return;}
        staff.put(t.getUniqueId(),r);saveCore();s.sendMessage("§aGrade de §e"+t.getName()+" §a= §f"+r);t.sendMessage("§6Votre grade staff est maintenant §e"+r);
    }
    private void freeze(CommandSender s,String[]a){
        if(!isStaff(s,50)||a.length<2){s.sendMessage("§c/staff freeze <joueur>");return;}Player t=Bukkit.getPlayerExact(a[1]);if(t==null)return;
        if(frozen.remove(t.getUniqueId())==null){frozen.put(t.getUniqueId(),p.getName());t.sendMessage("§cVous êtes gelé par le staff.");s.sendMessage("§cJoueur gelé.");}else{s.sendMessage("§aJoueur dégelé.");}
    }
    private void vanish(Player p){
        if(!isStaff(p,50)){p.sendMessage("§cMODO requis.");return;}
        if(vanished.add(p.getUniqueId())){for(Player q:Bukkit.getOnlinePlayers())if(q!=p&&!isStaff(q,50))q.hidePlayer(this,p);p.sendMessage("§aVanish activé.");}
        else{vanished.remove(p.getUniqueId());for(Player q:Bukkit.getOnlinePlayers())q.showPlayer(this,p);p.sendMessage("§cVanish désactivé.");}
    }

    // ================= SECURITY =================
    private ItemStack device(String type){
        Material m=switch(type){case "camera"->Material.SPYGLASS;case "alarm"->Material.NOTE_BLOCK;default->Material.ENDER_CHEST;};
        ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName("§6Sécurité: §f"+type.toUpperCase());meta.getPersistentDataContainer().set(deviceKey,PersistentDataType.STRING,type);meta.setLore(List.of("§7Dispositif RP Civilisation","§8Entreprise / commerce"));i.setItemMeta(meta);return i;
    }
    private void securityMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,27,"§8§lSÉCURITÉ • ENTREPRISE");
        item(inv,10,Material.SPYGLASS,"§bCaméra","§7Prix: §e"+money(getConfig().getDouble("security.camera-price",7500))+"$");
        item(inv,13,Material.NOTE_BLOCK,"§cAlarme","§7Prix: §e"+money(getConfig().getDouble("security.alarm-price",12000))+"$");
        item(inv,16,Material.ENDER_CHEST,"§6Coffre-fort","§7Prix: §e"+money(getConfig().getDouble("security.vault-price",50000))+"$");
        p.openInventory(inv);
    }
    private void buyDevice(Player p,String type){double price=getConfig().getDouble("security."+type+"-price",7500);if(cash(p.getUniqueId())<price){p.sendMessage("§cPas assez de liquide.");return;}cash(p.getUniqueId(),cash(p.getUniqueId())-price);p.getInventory().addItem(device(type));saveCore();p.sendMessage("§aDispositif acheté: §e"+type.toUpperCase());}

    // ================= COMMANDS =================
    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
        String n=c.getName().toLowerCase();
        if(n.equals("civilisation")){if(!(s instanceof Player p))return true;if(a.length==0||a[0].equalsIgnoreCase("hub")){p.teleport(getHubSpawn());return true;}if(a[0].equalsIgnoreCase("rp")){p.teleport(getRpSpawn());return true;}if(a[0].equalsIgnoreCase("menu")){mainMenu(p);return true;}p.sendMessage("§6/civilisation hub §7| §6/civilisation rp §7| §6/civilisation menu");return true;}
        if(n.equals("banque")){if(s instanceof Player p)bankMenu(p);return true;}
        if(n.equals("job")){if(s instanceof Player p){if(a.length>=3&&a[0].equalsIgnoreCase("approve"))approveJob(s,a);else jobMenu(p);}return true;}
        if(n.equals("parcelle")){if(s instanceof Player p){if(a.length>0&&a[0].equalsIgnoreCase("buy"))buyPlot(p,false);else if(a.length>0&&a[0].equalsIgnoreCase("rent"))buyPlot(p,true);else plotMenu(p);}return true;}
        if(n.equals("staff")){return staffCommand(s,a);}
        if(n.equals("civadmin")){if(s instanceof Player p)adminMenu(p);return true;}
        if(n.equals("securite")){if(s instanceof Player p){if(a.length>0&&a[0].equalsIgnoreCase("camera"))buyDevice(p,"camera");else if(a.length>0&&a[0].equalsIgnoreCase("alarm"))buyDevice(p,"alarm");else if(a.length>0&&a[0].equalsIgnoreCase("vault"))buyDevice(p,"vault");else securityMenu(p);}return true;}
        if(n.equals("pay")){return pay(s,a);}
        if(n.equals("eco")){return eco(s,a);}
        return true;
    }
    private Location getHubSpawn(){return new Location(Bukkit.getWorld(getConfig().getString("hub-world","hub")),getConfig().getDouble("hub-spawn.x"),getConfig().getDouble("hub-spawn.y"),getConfig().getDouble("hub-spawn.z"),(float)getConfig().getDouble("hub-spawn.yaw"),(float)getConfig().getDouble("hub-spawn.pitch"));}
    private Location getRpSpawn(){World w=Bukkit.getWorld(getConfig().getString("rp-world","world"));return new Location(w,getConfig().getDouble("rp-spawn.x"),getConfig().getDouble("rp-spawn.y"),getConfig().getDouble("rp-spawn.z"),(float)getConfig().getDouble("rp-spawn.yaw"),(float)getConfig().getDouble("rp-spawn.pitch"));}
    private void mainMenu(Player p){Inventory inv=Bukkit.createInventory(null,27,"§6§lCIVILISATION • MENU");item(inv,10,Material.GOLD_INGOT,"§6Banque","§7/banque dans la zone bancaire");item(inv,12,Material.BOOK,"§aMétiers","§7/job");item(inv,14,Material.GRASS_BLOCK,"§2Immobilier","§7/parcelle");item(inv,16,Material.NETHER_STAR,"§bStaff","§7/staff");p.openInventory(inv);}
    private boolean pay(CommandSender s,String[]a){
        if(!(s instanceof Player p)||a.length<2){s.sendMessage("/pay <joueur> <montant>");return true;}Player t=Bukkit.getPlayerExact(a[0]);if(t==null){p.sendMessage("§cJoueur introuvable.");return true;}double n=Double.parseDouble(a[1]);if(n<=0||cash(p.getUniqueId())<n){p.sendMessage("§cMontant invalide ou fonds insuffisants.");return true;}cash(p.getUniqueId(),cash(p.getUniqueId())-n);cash(t.getUniqueId(),cash(t.getUniqueId())+n);tx(p.getUniqueId(),"Paiement à "+t.getName()+" -"+money(n)+"$");tx(t.getUniqueId(),"Paiement de "+p.getName()+" +"+money(n)+"$");saveCore();p.sendMessage("§aPaiement envoyé.");t.sendMessage("§aPaiement reçu: §e"+money(n)+"$ §ade "+p.getName());return true;
    }
    private boolean eco(CommandSender s,String[]a){
        if(!isStaff(s,80)||a.length<3){s.sendMessage("§c/eco give|take <joueur> <montant>");return true;}Player t=Bukkit.getPlayerExact(a[1]);if(t==null){s.sendMessage("§cJoueur hors ligne.");return true;}double n=Double.parseDouble(a[2]);if(a[0].equalsIgnoreCase("give"))bank(t.getUniqueId(),bank(t.getUniqueId())+n);else bank(t.getUniqueId(),Math.max(0,bank(t.getUniqueId())-n));saveCore();s.sendMessage("§aCompte modifié.");return true;
    }
    private boolean staffCommand(CommandSender s,String[]a){
        if(!(s instanceof Player p)||!isStaff(s,20)){s.sendMessage("§cSTAFF requis.");return true;}if(a.length==0){staffMenu(p);return true;}
        switch(a[0].toLowerCase()){
            case "setrank"->setRank(s,a);
            case "freeze"->freeze(s,a);
            case "vanish"->vanish(p);
            case "tp"->{if(a.length>1){Player t=Bukkit.getPlayerExact(a[1]);if(t!=null)p.teleport(t);}}
            case "kick"->{if(isStaff(s,50)&&a.length>1){Player t=Bukkit.getPlayerExact(a[1]);if(t!=null)t.kickPlayer("§cExpulsé par le staff.");}}
            case "ban"->{if(isStaff(s,50)&&a.length>1){Player t=Bukkit.getPlayerExact(a[1]);if(t!=null)t.kickPlayer("§cBanni 24h par le staff.
§7Civilisation");}}
            case "jobapprove"->approveJob(s,new String[]{"approve",a.length>1?a[1]:"",a.length>2?a[2]:""});
            default->p.sendMessage("§e/staff setrank <joueur> <grade> §7| /staff freeze <joueur> | /staff vanish | /staff tp <joueur> | /staff kick <joueur> | /staff ban <joueur> | /staff jobapprove <joueur> <métier>");
        }return true;
    }

    // ================= EVENTS =================
    @EventHandler public void onJoinUltimate(PlayerJoinEvent e){
        Player p=e.getPlayer();
        getServer().getScheduler().runTaskLater(this,()->{
            p.teleport(getHubSpawn());
            if(vanished.contains(p.getUniqueId()))p.setInvisible(true);
            if(!core.contains(akey(p.getUniqueId())+".cash")){cash(p.getUniqueId(),getConfig().getDouble("economy.starting-cash",500));bank(p.getUniqueId(),getConfig().getDouble("economy.starting-bank",2500));saveCore();}
            p.sendMessage("§6§lCIVILISATION §7| §fLiquide: §e"+money(cash(p.getUniqueId()))+"$ §7| Banque: §e"+money(bank(p.getUniqueId()))+"$");
        },3L);
    }
    @EventHandler public void onMoveUltimate(PlayerMoveEvent e){if(frozen.contains(e.getPlayer().getUniqueId()))e.setTo(e.getFrom());}
    @EventHandler public void onBreakUltimate(BlockBreakEvent e){if(e.getBlock().getWorld().getName().equals(getConfig().getString("rp-world","world"))&&!canBuild(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler public void onPlaceUltimate(BlockPlaceEvent e){if(e.getBlock().getWorld().getName().equals(getConfig().getString("rp-world","world"))&&!canBuild(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler public void gui(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String t=e.getView().getTitle();if(!t.contains("BANQUE")&&!t.contains("MÉTIERS")&&!t.contains("IMMOBILIER")&&!t.contains("STAFF")&&!t.contains("SÉCURITÉ")&&!t.contains("ADMIN CORE")&&!t.contains("CIVILISATION • MENU"))return;e.setCancelled(true);int s=e.getRawSlot();
        if(t.contains("BANQUE")){if(s==10)deposit(p,100);else if(s==12)withdraw(p,100);}
        else if(t.contains("MÉTIERS")&&s<JOBS.size()){applyJob(p,JOBS.get(s));p.closeInventory();}
        else if(t.contains("IMMOBILIER")){if(s==11)buyPlot(p,false);else if(s==15)buyPlot(p,true);p.closeInventory();}
        else if(t.contains("STAFF")&&s==12)vanish(p);
        else if(t.contains("SÉCURITÉ")){if(s==10)buyDevice(p,"camera");else if(s==13)buyDevice(p,"alarm");else if(s==16)buyDevice(p,"vault");}
    }
    private void salaryTick(){
        long day=LocalDate.now().toEpochDay();
        for(Player p:Bukkit.getOnlinePlayers()){
            UUID u=p.getUniqueId();long last=core.getLong(akey(u)+".salaryDay",0);
            if(last<day){double s=getConfig().getDouble("economy.daily-salary",250);bank(u,bank(u)+s);core.set(akey(u)+".salaryDay",day);tx(u,"Salaire +"+money(s)+"$");p.sendMessage("§a💼 Salaire quotidien: §e+"+money(s)+"$ §aen banque.");}
        }saveCore();
    }
    private void item(Inventory inv,int slot,Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));i.setItemMeta(meta);inv.setItem(slot,i);}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[]a){return List.of();}
}
