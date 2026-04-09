package com.amadeu.landprotection.visual;

import com.amadeu.landprotection.claim.Claim;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class ClaimVisualization {

    public static void showClaimBounds(ServerPlayer player, ServerLevel world, Claim claim) {
        BlockPos pos1 = claim.getPos1();
        BlockPos pos2 = claim.getPos2();

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        // 4 colunas verticais nos cantos
        drawVerticalLine(player, world, minX, minY, minZ, maxY);
        drawVerticalLine(player, world, maxX, minY, minZ, maxY);
        drawVerticalLine(player, world, minX, minY, maxZ, maxY);
        drawVerticalLine(player, world, maxX, minY, maxZ, maxY);

        // Bordas inferiores
        drawHorizontalXLine(player, world, minX, maxX, minY, minZ);
        drawHorizontalXLine(player, world, minX, maxX, minY, maxZ);
        drawHorizontalZLine(player, world, minZ, maxZ, minY, minX);
        drawHorizontalZLine(player, world, minZ, maxZ, minY, maxX);

        // Bordas superiores
        drawHorizontalXLine(player, world, minX, maxX, maxY, minZ);
        drawHorizontalXLine(player, world, minX, maxX, maxY, maxZ);
        drawHorizontalZLine(player, world, minZ, maxZ, maxY, minX);
        drawHorizontalZLine(player, world, minZ, maxZ, maxY, maxX);
    }

    private static void drawVerticalLine(ServerPlayer player, ServerLevel world, int x, int minY, int z,
            int maxY) {
        for (int y = minY; y <= maxY; y++) {
            world.sendParticles(
                    player,
                    ParticleTypes.END_ROD,
                    false,
                    false,
                    x + 0.5, y + 0.1, z + 0.5,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }
    }

    private static void drawHorizontalXLine(ServerPlayer player, ServerLevel world, int minX, int maxX, int y,
            int z) {
        for (int x = minX; x <= maxX; x++) {
            world.sendParticles(
                    player,
                    ParticleTypes.END_ROD,
                    false,
                    false,
                    x + 0.5, y + 0.1, z + 0.5,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }
    }

    private static void drawHorizontalZLine(ServerPlayer player, ServerLevel world, int minZ, int maxZ, int y,
            int x) {
        for (int z = minZ; z <= maxZ; z++) {
            world.sendParticles(
                    player,
                    ParticleTypes.END_ROD,
                    false,
                    false,
                    x + 0.5, y + 0.1, z + 0.5,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }
    }
}