package com.amadeu.landprotection.claim;

import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Claim {

    private final UUID owner;
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final BlockPos center;
    private final Map<UUID, String> trustedPlayers = new LinkedHashMap<>();
    private final String dimension;

    public Claim(UUID owner, BlockPos pos1, BlockPos pos2, BlockPos center, String dimension) {
        this.owner = owner;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.center = center;
        this.dimension = dimension;
    }

    public UUID getOwner() {
        return owner;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public BlockPos getCenter() {
        return center;
    }

    public Map<UUID, String> getTrustedPlayers() {
        return trustedPlayers;
    }

    public String getDimension() {
        return dimension;
    }

    public boolean contains(BlockPos pos, String dimension) {
        if (!this.dimension.equals(dimension))
            return false;

        return pos.getX() >= Math.min(pos1.getX(), pos2.getX())
                && pos.getX() <= Math.max(pos1.getX(), pos2.getX())
                && pos.getY() >= Math.min(pos1.getY(), pos2.getY())
                && pos.getY() <= Math.max(pos1.getY(), pos2.getY())
                && pos.getZ() >= Math.min(pos1.getZ(), pos2.getZ())
                && pos.getZ() <= Math.max(pos1.getZ(), pos2.getZ());
    }

    public boolean isOwner(UUID playerUuid) {
        return owner.equals(playerUuid);
    }

    public boolean isTrusted(UUID playerUuid) {
        return trustedPlayers.containsKey(playerUuid);
    }

    public boolean canInteract(UUID playerUuid) {
        return isOwner(playerUuid) || isTrusted(playerUuid);
    }

    public void trustPlayer(UUID playerUuid, String playerName) {
        trustedPlayers.put(playerUuid, playerName);
    }

    public void untrustPlayer(UUID playerUuid) {
        trustedPlayers.remove(playerUuid);
    }
}
