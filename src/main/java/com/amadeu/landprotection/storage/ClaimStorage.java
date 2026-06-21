package com.amadeu.landprotection.storage;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STORAGE_DIR = "landprotection";
    private static final String STORAGE_FILE = "claims.json";

    private ClaimStorage() {
    }

    public static void saveAll(MinecraftServer server) {
        Path file = getStorageFile(server);

        try {
            Files.createDirectories(file.getParent());

            StorageData data = new StorageData();

            for (Claim claim : ClaimManager.getClaims()) {
                ClaimData claimData = new ClaimData();
                claimData.owner = claim.getOwner().toString();
                claimData.dimension = claim.getDimension();
                claimData.pos1 = toPosData(claim.getPos1());
                claimData.pos2 = toPosData(claim.getPos2());
                claimData.center = toPosData(claim.getCenter());

                claimData.serverClaim = claim.isServerClaim();
                claimData.arena = claim.isArena();

                claimData.trustedPlayers = new LinkedHashMap<>();
                for (Map.Entry<UUID, String> entry : claim.getTrustedPlayers().entrySet()) {
                    claimData.trustedPlayers.put(entry.getKey().toString(), entry.getValue());
                }

                claimData.guestPlayers = new LinkedHashMap<>();
                for (Map.Entry<UUID, String> entry : claim.getGuestPlayers().entrySet()) {
                    claimData.guestPlayers.put(entry.getKey().toString(), entry.getValue());
                }

                claimData.builders = new LinkedHashMap<>();
                for (Map.Entry<UUID, String> entry : claim.getBuilders().entrySet()) {
                    claimData.builders.put(entry.getKey().toString(), entry.getValue());
                }

                data.claims.add(claimData);
            }

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }

        } catch (IOException e) {
            System.err.println("[landprotection] Erro ao salvar dados: " + e.getMessage());
        }
    }

    public static void loadAll(MinecraftServer server) {
        Path file = getStorageFile(server);

        ClaimManager.clearClaims();

        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<StorageData>() {
            }.getType();

            StorageData data = GSON.fromJson(reader, type);

            if (data == null || data.claims == null) {
                return;
            }

            for (ClaimData claimData : data.claims) {
                UUID owner = UUID.fromString(claimData.owner);

                String dimension = claimData.dimension != null
                        ? claimData.dimension
                        : "minecraft:overworld";

                Claim claim = new Claim(
                        owner,
                        fromPosData(claimData.pos1),
                        fromPosData(claimData.pos2),
                        fromPosData(claimData.center),
                        dimension);

                claim.setServerClaim(claimData.serverClaim);
                claim.setArena(claimData.arena);

                if (claimData.trustedPlayers != null) {
                    for (Map.Entry<String, String> entry : claimData.trustedPlayers.entrySet()) {
                        claim.trustPlayer(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                }

                if (claimData.guestPlayers != null) {
                    for (Map.Entry<String, String> entry : claimData.guestPlayers.entrySet()) {
                        claim.guestPlayer(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                }

                if (claimData.builders != null) {
                    for (Map.Entry<String, String> entry : claimData.builders.entrySet()) {
                        claim.addBuilder(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                }

                ClaimManager.addClaim(claim);
            }

        } catch (Exception e) {
            System.err.println("[landprotection] Erro ao carregar dados: " + e.getMessage());
        }
    }

    private static Path getStorageFile(MinecraftServer server) {
        return server.getServerDirectory()
                .resolve("world")
                .resolve(STORAGE_DIR)
                .resolve(STORAGE_FILE);
    }

    private static PosData toPosData(BlockPos pos) {
        PosData data = new PosData();
        data.x = pos.getX();
        data.y = pos.getY();
        data.z = pos.getZ();
        return data;
    }

    private static BlockPos fromPosData(PosData data) {
        return new BlockPos(data.x, data.y, data.z);
    }

    private static class StorageData {
        private List<ClaimData> claims = new ArrayList<>();
    }

    private static class ClaimData {
        private String owner;
        private String dimension;
        private PosData pos1;
        private PosData pos2;
        private PosData center;
        private Map<String, String> trustedPlayers;
        private Map<String, String> guestPlayers;
        private Map<String, String> builders;

        private boolean serverClaim;
        private boolean arena;
    }

    private static class PosData {
        private int x;
        private int y;
        private int z;
    }
}
