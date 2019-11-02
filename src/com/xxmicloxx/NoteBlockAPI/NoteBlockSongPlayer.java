package com.xxmicloxx.NoteBlockAPI;

import cn.nukkit.level.Sound;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.network.protocol.BlockEventPacket;

/**
 * Created with IntelliJ IDEA.
 * User: ml
 * Date: 07.12.13
 * Time: 12:56
 */
public class NoteBlockSongPlayer extends SongPlayer {

    private Block noteBlock;

    public NoteBlockSongPlayer(Song song) {
        super(song);
    }

    public Block getNoteBlock() {
        return noteBlock;
    }

    public void setNoteBlock(Block noteBlock) {
        this.noteBlock = noteBlock;
    }

    @Override
    public void playTick(Player p, int tick) {
        if (noteBlock.getId() != 25) {
            return;
        }

        if (!p.getLevel().getFolderName().equals(noteBlock.getLevel().getFolderName())) {
            return;
        }

        for (Layer l : song.getLayerHashMap().values()) {
            Note note = l.getNote(tick);
            if (note == null) {
                continue;
            }
            BlockEventPacket pk = new BlockEventPacket();
            pk.x = (int) noteBlock.x;
            pk.y = (int) noteBlock.y;
            pk.z = (int) noteBlock.z;
            pk.case1 = note.getInstrument();
            pk.case2 = note.getKey() - 33;
            p.dataPacket(pk);
            Sound sound = null;
            switch (note.getInstrument()) {
                case 0:
                    sound = Sound.NOTE_HARP;
                    break;
                case 1:
                    sound = Sound.NOTE_BASS;
                    break;
                case 2:
                    sound = Sound.NOTE_BD;
                    break;
                case 3:
                    sound = Sound.NOTE_SNARE;
                    break;
                case 4:
                    sound = Sound.NOTE_HAT;
                    break;
                case 5:
                    sound = Sound.NOTE_GUITAR;
                    break;
                case 6:
                    sound = Sound.NOTE_FLUTE;
                    break;
                case 7:
                    sound = Sound.NOTE_BELL;
                    break;
                case 8:
                    sound = Sound.NOTE_CHIME;
                    break;
                case 9:
                    sound = Sound.NOTE_XYLOPHONE;
                    break;
            }

            float fl = 0;
            switch (note.getKey() - 33) {
                case 0: fl = 0.5f; break;
                case 1: fl = 0.529732f; break;
                case 2: fl = 0.561231f; break;
                case 3: fl = 0.594604f; break;
                case 4: fl = 0.629961f; break;
                case 5: fl = 0.667420f; break;
                case 6: fl = 0.707107f; break;
                case 7: fl = 0.749154f; break;
                case 8: fl = 0.793701f; break;
                case 9: fl = 0.840896f; break;
                case 10: fl = 0.890899f; break;
                case 11: fl = 0.943874f; break;
                case 12: fl = 1.0f; break;
                case 13: fl = 1.059463f; break;
                case 14: fl = 1.122462f; break;
                case 15: fl = 1.189207f; break;
                case 16: fl = 1.259921f; break;
                case 17: fl = 1.334840f; break;
                case 18: fl = 1.414214f; break;
                case 19: fl = 1.498307f; break;
                case 20: fl = 1.587401f; break;
                case 21: fl = 1.681793f; break;
                case 22: fl = 1.781797f; break;
                case 23: fl = 1.887749f; break;
                case 24: fl = 2.0f; break;
            }

            if (sound != null) {
                p.getLevel().addSound(noteBlock, sound, 1, fl, p);
            }
        }
    }
}
