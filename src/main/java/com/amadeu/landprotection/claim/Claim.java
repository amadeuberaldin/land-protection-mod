package com.amadeu.landprotection.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Claim {

    private final UUID owner;
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final BlockPos center;
    private final Map<UUID, String> trustedPlayers = new LinkedHashMap<>();
    private final Map<UUID, String> guestPlayers = new LinkedHashMap<>();
    private final Map<UUID, String> builders = new LinkedHashMap<>();
    private final String dimension;

    private boolean serverClaim = false;
    private boolean arena = false;

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

    public Map<UUID, String> getGuestPlayers() {
        return guestPlayers;
    }

    public Map<UUID, String> getBuilders() {
        return builders;
    }

    public String getDimension() {
        return dimension;
    }

    public boolean isServerClaim() {
        return serverClaim;
    }

    public void setServerClaim(boolean serverClaim) {
        this.serverClaim = serverClaim;
    }

    public boolean isArena() {
        return arena;
    }

    public void setArena(boolean arena) {
        this.arena = arena;
    }

    public boolean contains(BlockPos pos, String dimension) {
        if (!this.dimension.equals(dimension))
            return false;

        return pos.getX() >= Math.min(pos1.getX(), pos2.getX())
                && pos.getX() <= Math.max(pos1.getX(), pos2.getX())
                && pos.getZ() >= Math.min(pos1.getZ(), pos2.getZ())
                && pos.getZ() <= Math.max(pos1.getZ(), pos2.getZ());
    }

    public boolean contains3D(BlockPos pos, String dimension) {

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

    public boolean isGuest(UUID playerUuid) {
        return guestPlayers.containsKey(playerUuid);
    }

    public boolean isBuilder(UUID playerUuid) {
        return builders.containsKey(playerUuid);
    }

    public void trustPlayer(UUID playerUuid, String playerName) {
        trustedPlayers.put(playerUuid, playerName);
    }

    public void guestPlayer(UUID playerUuid, String playerName) {
        guestPlayers.put(playerUuid, playerName);
    }

    public void untrustPlayer(UUID playerUuid) {
        trustedPlayers.remove(playerUuid);
    }

    public void unguestPlayer(UUID playerUuid) {
        guestPlayers.remove(playerUuid);
    }

    public void addBuilder(UUID playerUuid, String playerName) {
        builders.put(playerUuid, playerName);
    }

    public void removeBuilder(UUID playerUuid) {
        builders.remove(playerUuid);
    }

    public boolean canAccess(ServerPlayer player) {
        if (serverClaim) {
            return true;
        }

        return isOwner(player.getUUID()) || isTrusted(player.getUUID());
    }

    public boolean canBuild(ServerPlayer player) {

        if (serverClaim && !arena) {
            return isBuilder(player.getUUID());
        }

        if (serverClaim && arena) {
            return true;
        }

        return isOwner(player.getUUID()) || isTrusted(player.getUUID());
    }

    public boolean canManage(ServerPlayer player) {
        return isOwner(player.getUUID());
    }

    public boolean canInteract(UUID playerUuid) {
        if (serverClaim) {
            return true;
        }

        return isOwner(playerUuid)
                || isTrusted(playerUuid)
                || isGuest(playerUuid);
    }
}
