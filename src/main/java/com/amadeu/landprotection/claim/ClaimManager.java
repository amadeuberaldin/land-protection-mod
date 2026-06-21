package com.amadeu.landprotection.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClaimManager {

    public static final int MAX_CLAIMS_PER_PLAYER = 10;

    private static final List<Claim> claims = new ArrayList<>();

    private ClaimManager() {
    }

    public static void addClaim(Claim claim) {
        claims.add(claim);
    }

    public static void removeClaim(Claim claim) {
        claims.remove(claim);
    }

    public static void removeClaimsByOwner(UUID playerUuid) {
        claims.removeIf(claim -> claim.getOwner().equals(playerUuid));
    }

    public static boolean playerHasClaim(UUID playerUuid) {
        return playerClaimCount(playerUuid) > 0;
    }

    public static int playerClaimCount(UUID playerUuid) {
        int count = 0;

        for (Claim claim : claims) {

            if (claim.isServerClaim()) {
                continue;
            }

            if (claim.isArena()) {
                continue;
            }

            if (claim.getOwner().equals(playerUuid)) {
                count++;
            }
        }

        return count;
    }

    public static boolean canCreateClaim(UUID playerUuid) {
        return playerClaimCount(playerUuid) < MAX_CLAIMS_PER_PLAYER;
    }

    public static List<Claim> getClaimsByPlayer(UUID playerUuid) {
        List<Claim> ownedClaims = new ArrayList<>();

        for (Claim claim : claims) {
            if (!claim.isServerClaim()
                    && !claim.isArena()
                    && claim.getOwner().equals(playerUuid)) {
                ownedClaims.add(claim);
            }
        }

        return ownedClaims;
    }

    public static Claim getClaimAt(BlockPos pos, String dimension) {
        for (Claim claim : claims) {
            if (claim.contains(pos, dimension)) {
                return claim;
            }
        }

        return null;
    }

    public static Claim getSpecialClaimAt(BlockPos pos, String dimension) {

        for (Claim claim : claims) {

            if (!claim.isServerClaim() && !claim.isArena()) {
                continue;
            }

            if (claim.contains3D(pos, dimension)) {
                return claim;
            }
        }

        return null;
    }

    public static Claim getArenaAt(BlockPos pos, String dimension) {

        for (Claim claim : claims) {

            if (!claim.isArena()) {
                continue;
            }

            if (claim.contains3D(pos, dimension)) {
                return claim;
            }
        }

        return null;
    }

    public static Claim getServerClaimAt(BlockPos pos, String dimension) {

        for (Claim claim : claims) {

            if (!claim.isServerClaim()) {
                continue;
            }

            if (claim.contains3D(pos, dimension)) {
                return claim;
            }
        }

        return null;
    }

    public static Claim getClaimAtOwnedBy(UUID playerUuid, BlockPos pos, String dimension) {
        for (Claim claim : claims) {

            if (claim.isServerClaim()) {
                continue;
            }

            if (claim.isArena()) {
                continue;
            }

            if (claim.getOwner().equals(playerUuid) && claim.contains(pos, dimension)) {
                return claim;
            }
        }

        return null;
    }

    public static Claim getClaimManageableBy(ServerPlayer player, BlockPos pos, String dimension) {
        Claim claim = getClaimAt(pos, dimension);

        if (claim != null && claim.canManage(player)) {
            return claim;
        }

        return null;
    }

    public static boolean hasClaimAt(BlockPos pos, String dimension) {
        return getClaimAt(pos, dimension) != null;
    }

    public static List<Claim> getClaims() {
        return Collections.unmodifiableList(claims);
    }

    public static void clearClaims() {
        claims.clear();
    }

    public static boolean canAccess(ServerPlayer player, BlockPos pos, String dimension) {

        Claim specialClaim = getSpecialClaimAt(pos, dimension);

        if (specialClaim != null) {
            return specialClaim.canAccess(player);
        }

        Claim claim = getClaimAt(pos, dimension);

        if (claim != null) {
            return claim.canAccess(player);
        }

        return true;
    }

    public static boolean canBuild(ServerPlayer player, BlockPos pos, String dimension) {

        Claim arena = getArenaAt(pos, dimension);

        if (arena != null) {
            return true;
        }

        Claim specialClaim = getSpecialClaimAt(pos, dimension);

        if (specialClaim != null) {
            return specialClaim.canBuild(player);
        }

        Claim claim = getClaimAt(pos, dimension);

        if (claim != null) {
            return claim.canBuild(player);
        }

        return true;
    }

    public static boolean canManage(ServerPlayer player, BlockPos pos, String dimension) {
        Claim claim = getClaimAt(pos, dimension);

        if (claim != null) {
            return claim.canManage(player);
        }

        return true;
    }

    public static boolean canInteract(UUID playerUuid, BlockPos pos, String dimension) {

        Claim specialClaim = getSpecialClaimAt(pos, dimension);

        if (specialClaim != null) {
            return specialClaim.canInteract(playerUuid);
        }

        Claim claim = getClaimAt(pos, dimension);

        if (claim != null) {
            return claim.canInteract(playerUuid);
        }

        return true;
    }

    public static boolean overlapsExistingArea(BlockPos pos1, BlockPos pos2, String dimension) {
        for (Claim claim : claims) {
            if (!claim.getDimension().equals(dimension)) {
                continue;
            }

            if (boxesOverlap(pos1, pos2, claim.getPos1(), claim.getPos2())) {
                return true;
            }
        }

        return false;
    }

    private static boolean boxesOverlap(BlockPos a1, BlockPos a2, BlockPos b1, BlockPos b2) {

        int aMinX = Math.min(a1.getX(), a2.getX());
        int aMaxX = Math.max(a1.getX(), a2.getX());
        int aMinZ = Math.min(a1.getZ(), a2.getZ());
        int aMaxZ = Math.max(a1.getZ(), a2.getZ());

        int bMinX = Math.min(b1.getX(), b2.getX());
        int bMaxX = Math.max(b1.getX(), b2.getX());
        int bMinZ = Math.min(b1.getZ(), b2.getZ());
        int bMaxZ = Math.max(b1.getZ(), b2.getZ());

        boolean overlapX = aMinX <= bMaxX && aMaxX >= bMinX;
        boolean overlapZ = aMinZ <= bMaxZ && aMaxZ >= bMinZ;

        return overlapX && overlapZ;
    }
}
