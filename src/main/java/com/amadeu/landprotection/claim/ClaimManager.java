package com.amadeu.landprotection.claim;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClaimManager {

    public static final int MAX_BASE_MEMBERS = 5;

    private static final List<Claim> claims = new ArrayList<>();
    private static final List<BaseClaim> bases = new ArrayList<>();

    // =========================
    // Claims individuais
    // =========================

    public static void addClaim(Claim claim) {
        claims.add(claim);
    }

    public static void removeClaim(Claim claim) {
        claims.remove(claim);
    }

    public static void removeClaimByOwner(UUID playerUuid) {
        claims.removeIf(claim -> claim.getOwner().equals(playerUuid));
    }

    public static boolean playerHasClaim(UUID playerUuid) {
        return getClaimByPlayer(playerUuid) != null;
    }

    public static Claim getClaimByPlayer(UUID playerUuid) {
        for (Claim claim : claims) {
            if (claim.getOwner().equals(playerUuid)) {
                return claim;
            }
        }
        return null;
    }

    public static Claim getClaimAt(BlockPos pos) {
        for (Claim claim : claims) {
            if (claim.contains(pos)) {
                return claim;
            }
        }
        return null;
    }

    public static boolean hasClaimAt(BlockPos pos) {
        return getClaimAt(pos) != null;
    }

    public static List<Claim> getClaims() {
        return Collections.unmodifiableList(claims);
    }

    public static void clearClaims() {
        claims.clear();
    }

    // =========================
    // Bases em grupo
    // =========================

    public static void addBase(BaseClaim base) {
        bases.add(base);
    }

    public static void removeBase(BaseClaim base) {
        bases.remove(base);
    }

    public static BaseClaim getBaseByLeader(UUID playerUuid) {
        for (BaseClaim base : bases) {
            if (base.getLeader().equals(playerUuid)) {
                return base;
            }
        }
        return null;
    }

    public static BaseClaim getBaseByMember(UUID playerUuid) {
        for (BaseClaim base : bases) {
            if (base.isMember(playerUuid)) {
                return base;
            }
        }
        return null;
    }

    public static BaseClaim getBaseAt(BlockPos pos) {
        for (BaseClaim base : bases) {
            if (base.contains(pos)) {
                return base;
            }
        }
        return null;
    }

    public static boolean hasBaseAt(BlockPos pos) {
        return getBaseAt(pos) != null;
    }

    public static boolean playerHasBase(UUID playerUuid) {
        return getBaseByMember(playerUuid) != null;
    }

    public static List<BaseClaim> getBases() {
        return Collections.unmodifiableList(bases);
    }

    public static void clearBases() {
        bases.clear();
    }

    // =========================
    // Permissões
    // =========================

    public static boolean canInteract(UUID playerUuid, BlockPos pos) {
        Claim claim = getClaimAt(pos);
        if (claim != null) {
            return claim.canInteract(playerUuid);
        }

        BaseClaim base = getBaseAt(pos);
        if (base != null) {
            return base.canInteract(playerUuid);
        }

        return true;
    }

    // =========================
    // Anti-overlap
    // =========================

    public static boolean overlapsExistingArea(BlockPos pos1, BlockPos pos2) {
        for (Claim claim : claims) {
            if (boxesOverlap(pos1, pos2, claim.getPos1(), claim.getPos2())) {
                return true;
            }
        }

        for (BaseClaim base : bases) {
            if (boxesOverlap(pos1, pos2, base.getPos1(), base.getPos2())) {
                return true;
            }
        }

        return false;
    }

    private static boolean boxesOverlap(BlockPos a1, BlockPos a2, BlockPos b1, BlockPos b2) {
        int aMinX = Math.min(a1.getX(), a2.getX());
        int aMaxX = Math.max(a1.getX(), a2.getX());
        int aMinY = Math.min(a1.getY(), a2.getY());
        int aMaxY = Math.max(a1.getY(), a2.getY());
        int aMinZ = Math.min(a1.getZ(), a2.getZ());
        int aMaxZ = Math.max(a1.getZ(), a2.getZ());

        int bMinX = Math.min(b1.getX(), b2.getX());
        int bMaxX = Math.max(b1.getX(), b2.getX());
        int bMinY = Math.min(b1.getY(), b2.getY());
        int bMaxY = Math.max(b1.getY(), b2.getY());
        int bMinZ = Math.min(b1.getZ(), b2.getZ());
        int bMaxZ = Math.max(b1.getZ(), b2.getZ());

        boolean overlapX = aMinX <= bMaxX && aMaxX >= bMinX;
        boolean overlapY = aMinY <= bMaxY && aMaxY >= bMinY;
        boolean overlapZ = aMinZ <= bMaxZ && aMaxZ >= bMinZ;

        return overlapX && overlapY && overlapZ;
    }
}