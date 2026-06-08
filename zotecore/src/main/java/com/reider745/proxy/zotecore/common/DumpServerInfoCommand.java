package com.reider745.proxy.zotecore.common;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.google.gson.Gson;
import com.reider745.api.ReflectHelper;
import com.reider745.proxy.data.ModInfo;
import com.zhekasmirnov.apparatus.modloader.ApparatusMod;
import com.zhekasmirnov.apparatus.multiplayer.mod.IdConversionMap;
import com.zhekasmirnov.apparatus.multiplayer.mod.MultiplayerModList;
import com.zhekasmirnov.innercore.api.biomes.CustomBiome;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DumpServerInfoCommand extends Command {
    private static final Gson GSON = new Gson();

    public DumpServerInfoCommand() {
        super("dumpserverinfo");

        this.setPermission("zotecore.dumpserverinfo");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(this.testPermission(sender)) {
            try {
                List<ModInfo> mods = new ArrayList<>();

                ReflectHelper.<List<ApparatusMod>>getField(MultiplayerModList.getSingleton(), "modList").forEach(mod -> {
                    mods.add(new ModInfo(mod.getInfo().getString("name"), mod.getInfo().getString("version")));
                });

                Files.writeString(new File("mods.json").toPath(), GSON.toJson(mods));
                Files.writeString(new File("biomes.json").toPath(), GSON.toJson(CustomBiome.getAllCustomBiomes()));
                Files.writeString(new File("id_map.json").toPath(), GSON.toJson((Map<String, Integer>) ReflectHelper.getField(IdConversionMap.getSingleton(), "localIdMap")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }
}
