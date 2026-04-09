package com.amadeu.landprotection.storage;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
                claimData.pos1 = toPosData(claim.getPos1());
                claimData.pos2 = toPosData(claim.getPos2());
                claimData.center = toPosData(claim.getCenter());
                claimData.trustedPlayers = new LinkedHashMap<>();

                for (Map.Entry<UUID, String> entry : claim.getTrustedPlayers().entrySet()) {
                    claimData.trustedPlayers.put(entry.getKey().toString(), entry.getValue());
                }

                data.claims.add(claimData);
            }

            for (BaseClaim base : ClaimManager.getBases()) {
                BaseData baseData = new BaseData();
                baseData.leader = base.getLeader().toString();
                baseData.pos1 = toPosData(base.getPos1());
                baseData.pos2 = toPosData(base.getPos2());
                baseData.center = toPosData(base.getCenter());
                baseData.members = new LinkedHashMap<>();

                for (Map.Entry<UUID, String> entry : base.getMembers().entrySet()) {
                    baseData.members.put(entry.getKey().toString(), entry.getValue());
                }

                data.bases.add(baseData);
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
        ClaimManager.clearBases();

        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<StorageData>() {
            }.getType();
            StorageData data = GSON.fromJson(reader, type);

            if (data == null) {
                return;
            }

            if (data.claims != null) {
                for (ClaimData claimData : data.claims) {
                    UUID owner = UUID.fromString(claimData.owner);
                    Claim claim = new Claim(
                            owner,
                            fromPosData(claimData.pos1),
                            fromPosData(claimData.pos2),
                            fromPosData(claimData.center));

                    if (claimData.trustedPlayers != null) {
                        for (Map.Entry<String, String> entry : claimData.trustedPlayers.entrySet()) {
                            claim.trustPlayer(UUID.fromString(entry.getKey()), entry.getValue());
                        }
                    }

                    ClaimManager.addClaim(claim);
                }
            }

            if (data.bases != null) {
                for (BaseData baseData : data.bases) {
                    UUID leader = UUID.fromString(baseData.leader);
                    String leaderName = baseData.members != null ? baseData.members.get(baseData.leader) : "Líder";

                    BaseClaim base = new BaseClaim(
                            leader,
                            leaderName,
                            fromPosData(baseData.pos1),
                            fromPosData(baseData.pos2),
                            fromPosData(baseData.center));

                    if (baseData.members != null) {
                        for (Map.Entry<String, String> entry : baseData.members.entrySet()) {
                            UUID memberUuid = UUID.fromString(entry.getKey());

                            if (!memberUuid.equals(leader)) {
                                base.addMember(memberUuid, entry.getValue(), ClaimManager.MAX_BASE_MEMBERS);
                            }
                        }
                    }

                    ClaimManager.addBase(base);
                }
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
        private List<BaseData> bases = new ArrayList<>();
    }

    private static class ClaimData {
        private String owner;
        private PosData pos1;
        private PosData pos2;
        private PosData center;
        private Map<String, String> trustedPlayers;
    }

    private static class BaseData {
        private String leader;
        private PosData pos1;
        private PosData pos2;
        private PosData center;
        private Map<String, String> members;
    }

    private static class PosData {
        private int x;
        private int y;
        private int z;
    }
}