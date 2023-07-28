package com.fcmcpe.nuclear.music;

import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.xxmicloxx.NoteBlockAPI.*;

import java.io.File;
import java.util.*;

public class NuclearMusicPlugin extends PluginBase {

    public static NuclearMusicPlugin instance;
    private final LinkedList<Song> songs = new LinkedList<>();
    private final Map<NodeIntegerPosition, SongPlayer> songPlayers = new HashMap<>();

    public static boolean playEverywhere;
    public static boolean useParticles;
    public static float volume;

    static List<File> getAllNBSFiles(File path) {
        List<File> result = new ArrayList<>();
        File[] subFile = path.listFiles();
        if (subFile == null) return result;
        for (File aSubFile : subFile) {
            if (aSubFile.isDirectory()) continue;
            if (!aSubFile.getName().trim().toLowerCase().endsWith(".nbs")) continue;
            result.add(aSubFile);
        }
        return result;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        playEverywhere = getConfig().getBoolean("playEverywhere", true);
        useParticles = getConfig().getBoolean("useParticles", true);
        volume = (float) getConfig().getDouble("volume", 1.0);

        loadAllSongs();
        Config noteblocks = new Config(getDataFolder() + "/noteblocks.yml", Config.YAML);
        List<String> positions = noteblocks.getStringList("positions");
        for (String position : positions) {
            String[] data = position.split(":");
            if (data.length != 4) {
                getLogger().warning("Corrupted save data found: " + position);
                continue;
            }
            Level level = getServer().getLevelByName(data[3]);
            if (level == null) {
                getLogger().warning("Unknown level: " + data[3]);
                continue;
            }
            int x, y, z;
            try {
                x = Integer.parseInt(data[0]);
                y = Integer.parseInt(data[1]);
                z = Integer.parseInt(data[2]);
            } catch (NumberFormatException ignore) {
                getLogger().warning("Corrupted save data found: " + position);
                continue;
            }
            Block block = level.getBlock(x, y, z, true);
            if (block.getId() != Item.NOTEBLOCK) {
                getLogger().warning("Noteblock does not exist at " + x + ' ' + y + ' ' + z);
                continue;
            }
            Song song = songs.getFirst();
            NoteBlockSongPlayer songPlayer = new NoteBlockSongPlayer(song);
            songPlayer.setNoteBlock(block);
            songPlayer.setAutoCycle(true);
            songPlayer.setAutoDestroy(false);
            getServer().getOnlinePlayers().forEach((s, p) -> songPlayer.addPlayer(p));
            songPlayer.setPlaying(true);
            songPlayers.put(new NodeIntegerPosition(block), songPlayer);
        }

        getServer().getPluginManager().registerEvents(new NuclearMusicListener(), this);

        new TickerRunnable().start();
    }

    @Override
    public void onDisable() {
        Config noteblocks = new Config(getDataFolder() + "/noteblocks.yml", Config.YAML);
        List<String> positions = new ArrayList<>();
        songPlayers.forEach((pos, sp) -> positions.add(pos.x + ":" + pos.y + ':' + pos.z  + ':' + pos.level.getName()));
        noteblocks.set("positions", positions);
        noteblocks.save();
    }

    private void loadAllSongs() {
        new File(getDataFolder() + "/tracks").mkdirs();
        List<File> files = getAllNBSFiles(new File(getDataFolder(), "tracks"));
        files.forEach(file -> {
            Song song = NBSDecoder.parse(file);
            if (song == null) return;
            songs.add(song);
        });
        Collections.shuffle(songs);
        getLogger().info("Loaded " + songs.size() + " songs");
    }

    public Song nextSong(Song now) {
        if (!songs.contains(now)) return songs.getFirst();
        if (songs.indexOf(now) >= songs.size() - 1) return songs.getFirst();
        return songs.get(songs.indexOf(now) + 1);
    }

    class NodeIntegerPosition {
        int x;
        int y;
        int z;
        Level level;

        NodeIntegerPosition(int x, int y, int z, Level level) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
        }

        NodeIntegerPosition(Position position) {
            this(position.getFloorX(), position.getFloorY(), position.getFloorZ(), position.getLevel());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NodeIntegerPosition)) return false;
            NodeIntegerPosition node = (NodeIntegerPosition) obj;
            return (x == node.x) && (y == node.y) && (z == node.z) && (level == node.level);
        }

        @Override
        public int hashCode() {
            return (x + ":" + y + ':' + z + ':' + level.getName()).hashCode();
        }
    }

    class NuclearMusicListener implements Listener {

        @EventHandler
        public void onBlockTouch(PlayerInteractEvent event) {
            if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
            if (event.getBlock().getId() != Item.NOTEBLOCK) return;
            Song song;
            NodeIntegerPosition node = new NodeIntegerPosition(event.getBlock());
            if (event.getItem().getId() != Item.DIAMOND_HOE || event.getItem().getDamage() != 9999 || !event.getPlayer().hasPermission("nuclearmusic.setup")) {
                if (songPlayers.containsKey(node)) {
                    SongPlayer sp = songPlayers.get(node);
                    song = sp.getSong();
                    event.getPlayer().sendActionBar("§aNow playing: §7" + song.getTitle());
                    event.setCancelled(true);
                }
            } else {
                if (songPlayers.containsKey(node)) {
                    SongPlayer sp = songPlayers.get(node);
                    Song now = sp.getSong();
                    songPlayers.get(node).setPlaying(false);
                    songPlayers.remove(node);
                    song = nextSong(now);
                    getServer().getOnlinePlayers().forEach((s, p) -> sp.removePlayer(p));
                } else {
                    try {
                        song = songs.getFirst();
                    } catch (NoSuchElementException ignore) {
                        event.getPlayer().sendMessage("§cError! No songs loaded!");
                        return;
                    }
                }

                NoteBlockSongPlayer songPlayer = new NoteBlockSongPlayer(song);
                songPlayer.setNoteBlock(event.getBlock());
                songPlayer.setAutoCycle(true);
                songPlayer.setAutoDestroy(false);
                getServer().getOnlinePlayers().forEach((s, p) -> songPlayer.addPlayer(p));
                songPlayer.setPlaying(true);
                songPlayers.put(node, songPlayer);
                event.getPlayer().sendActionBar("§aNow playing: §7" + song.getTitle());
            }
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            songPlayers.forEach((p, s) -> s.addPlayer(event.getPlayer()));
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            NoteBlockAPI.getInstance().stopPlaying(event.getPlayer());
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getId() == Item.NOTEBLOCK) {
                NodeIntegerPosition node = new NodeIntegerPosition(event.getBlock());
                SongPlayer sp = songPlayers.get(node);
                if (sp != null) {
                    sp.setPlaying(false);
                    sp.destroy();
                }
                songPlayers.remove(node);
            }
        }
    }

    class TickerRunnable extends Thread {

        TickerRunnable() {
            setName("NuclearMusic");
        }

        public void run() {
            while (isEnabled()) {
                try {
                    NoteBlockAPI.getInstance().playingSongs.forEach((s, a) -> a.forEach((SongPlayer::tryPlay)));
                } catch (Exception ignore) {}
                try {
                    Thread.sleep(20);
                } catch (Exception ignore) {}
            }
        }
    }
}
