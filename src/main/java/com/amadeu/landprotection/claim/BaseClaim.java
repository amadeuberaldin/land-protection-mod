package com.amadeu.landprotection.claim;

import net.minecraft.core.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class BaseClaim {

    private final UUID leader;
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final BlockPos center;
    private final Map<UUID, String> members = new LinkedHashMap<>();

    public BaseClaim(UUID leader, String leaderName, BlockPos pos1, BlockPos pos2, BlockPos center) {
        this.leader = leader;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.center = center;
        this.members.put(leader, leaderName);
    }

    public UUID getLeader() {
        return leader;
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

    public Map<UUID, String> getMembers() {
        return members;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= Math.min(pos1.getX(), pos2.getX())
                && pos.getX() <= Math.max(pos1.getX(), pos2.getX())
                && pos.getY() >= Math.min(pos1.getY(), pos2.getY())
                && pos.getY() <= Math.max(pos1.getY(), pos2.getY())
                && pos.getZ() >= Math.min(pos1.getZ(), pos2.getZ())
                && pos.getZ() <= Math.max(pos1.getZ(), pos2.getZ());
    }

    public boolean isLeader(UUID playerUuid) {
        return leader.equals(playerUuid);
    }

    public boolean isMember(UUID playerUuid) {
        return members.containsKey(playerUuid);
    }

    public boolean canInteract(UUID playerUuid) {
        return isMember(playerUuid);
    }

    public boolean addMember(UUID playerUuid, String playerName, int maxMembers) {
        if (members.containsKey(playerUuid)) {
            return false;
        }

        if (members.size() >= maxMembers) {
            return false;
        }

        members.put(playerUuid, playerName);
        return true;
    }

    public boolean removeMember(UUID playerUuid) {
        if (leader.equals(playerUuid)) {
            return false;
        }

        return members.remove(playerUuid) != null;
    }
}