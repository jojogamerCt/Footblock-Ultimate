package net.footblock.footblockultimate.match;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.footblock.footblockultimate.FootblockUltimate;
import net.footblock.footblockultimate.block.ScoreManagerConsoleBlock;
import net.footblock.footblockultimate.entity.FootballEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class WorldCupMatchManager {
    private static final String SCORE_OBJECTIVE = "footblock_score";
    private static final String GOALS_OBJECTIVE = "footblock_goals";
    private static final ScoreHolder RED_SCORE = ScoreHolder.forNameOnly("Red");
    private static final ScoreHolder BLUE_SCORE = ScoreHolder.forNameOnly("Blue");
    public static final byte ACTION_RESET = 0;
    public static final byte ACTION_ADD = 1;
    public static final byte ACTION_SET = 2;
    public static final byte ACTION_END = 3;
    public static final byte TEAM_RED = 0;
    public static final byte TEAM_BLUE = 1;
    private static final int MAX_SCORE = 9999;
    private static final long TROPHY_COOLDOWN_TICKS = 200L;
    private static final Map<MinecraftServer, Map<UUID, Long>> TROPHY_COOLDOWNS = new WeakHashMap<>();

    public enum Side {
        RED(ChatFormatting.RED),
        BLUE(ChatFormatting.BLUE);

        private final ChatFormatting color;

        Side(ChatFormatting color) {
            this.color = color;
        }

        public ChatFormatting color() {
            return this.color;
        }
    }

    private WorldCupMatchManager() {
    }

    public static void registerGoal(ServerLevel level, BlockPos goalPos, FootballEntity football, Side scoringSide) {
        if (football.isRemoved() || !football.isGoalEligible()) {
            return;
        }

        football.markGoalHandled();
        ServerScoreboard scoreboard = level.getServer().getScoreboard();
        Objective scoreObjective = ensureScoreObjective(scoreboard);
        Objective goalsObjective = ensureGoalsObjective(scoreboard);
        ScoreHolder sideHolder = scoringSide == Side.RED ? RED_SCORE : BLUE_SCORE;
        scoreboard.getOrCreatePlayerScore(sideHolder, scoreObjective).increment();

        UUID lastTouchUuid = football.getLastTouchPlayerUUID();
        ServerPlayer scorer = lastTouchUuid == null ? null : level.getServer().getPlayerList().getPlayer(lastTouchUuid);
        Side scorerSide = getPlayerSide(scorer);
        boolean ownGoal = scorerSide != null && scorerSide != scoringSide;
        if (scorer != null && !ownGoal) {
            scoreboard.getOrCreatePlayerScore(scorer, goalsObjective).increment();
        }

        int redScore = scoreboard.getOrCreatePlayerScore(RED_SCORE, scoreObjective).get();
        int blueScore = scoreboard.getOrCreatePlayerScore(BLUE_SCORE, scoreObjective).get();
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, scoreObjective);

        Component scorerName = scorer == null
                ? Component.translatable("message.footblockultimate.unknown_scorer")
                : scorer.getDisplayName();
        Component goalMessage = Component.translatable(
                ownGoal ? "message.footblockultimate.own_goal" : "message.footblockultimate.goal",
                Component.translatable("message.footblockultimate.side." + scoringSide.name().toLowerCase())
                        .withStyle(scoringSide.color()),
                scorerName,
                redScore,
                blueScore
        ).withStyle(ChatFormatting.GOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(goalMessage, false);

        double x = goalPos.getX() + 0.5;
        double y = goalPos.getY() + 0.6;
        double z = goalPos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 45, 0.8, 0.7, 0.8, 0.12);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 20, 0.9, 0.5, 0.9, 0.08);
        level.playSound(null, goalPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

        ItemStack kickoffBall = football.getPickupStack();
        football.spawnAtLocation(kickoffBall, 0.2f);
        football.discard();
    }

    public static void stopPlay(ServerLevel level, Player referee, int stoppedBalls) {
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.footblockultimate.play_stopped", referee.getDisplayName(), stoppedBalls)
                        .withStyle(ChatFormatting.YELLOW),
                false
        );
    }

    public static void resetMatch(ServerLevel level, Player referee, int stoppedBalls) {
        ServerScoreboard scoreboard = level.getServer().getScoreboard();
        Objective scoreObjective = ensureScoreObjective(scoreboard);
        scoreboard.getOrCreatePlayerScore(RED_SCORE, scoreObjective).set(0);
        scoreboard.getOrCreatePlayerScore(BLUE_SCORE, scoreObjective).set(0);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, scoreObjective);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.footblockultimate.match_reset", referee.getDisplayName(), stoppedBalls)
                        .withStyle(ChatFormatting.AQUA),
                false
        );
    }

    public record ScoreState(int red, int blue, boolean active) {
    }

    public static void openScoreManager(ServerPlayer player, BlockPos pos) {
        sendScoreManagerState(player, pos, true);
    }

    public static void handleScoreManagerAction(ServerPlayer player, BlockPos pos, byte action, byte team, int value) {
        if (!(player.level() instanceof ServerLevel level)
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0
                || !(level.getBlockState(pos).getBlock() instanceof ScoreManagerConsoleBlock)) {
            return;
        }

        ServerScoreboard scoreboard = level.getServer().getScoreboard();
        if (action == ACTION_END) {
            Objective scoreObjective = scoreboard.getObjective(SCORE_OBJECTIVE);
            if (scoreObjective != null) {
                scoreboard.removeObjective(scoreObjective);
            }
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.footblockultimate.score_manager.ended", player.getDisplayName())
                            .withStyle(ChatFormatting.GOLD),
                    false
            );
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.9f);
            sendScoreManagerState(player, pos, false);
            return;
        }

        Objective scoreObjective = ensureScoreObjective(scoreboard);
        if (action == ACTION_RESET) {
            scoreboard.getOrCreatePlayerScore(RED_SCORE, scoreObjective).set(0);
            scoreboard.getOrCreatePlayerScore(BLUE_SCORE, scoreObjective).set(0);
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.footblockultimate.score_manager.reset", player.getDisplayName())
                            .withStyle(ChatFormatting.AQUA),
                    false
            );
        } else {
            if (team != TEAM_RED && team != TEAM_BLUE) {
                return;
            }
            ScoreHolder holder = team == TEAM_RED ? RED_SCORE : BLUE_SCORE;
            int current = scoreboard.getOrCreatePlayerScore(holder, scoreObjective).get();
            int next;
            if (action == ACTION_ADD && (value == -1 || value == 1)) {
                next = Math.clamp(current + value, 0, MAX_SCORE);
            } else if (action == ACTION_SET) {
                next = Math.clamp(value, 0, MAX_SCORE);
            } else {
                return;
            }
            scoreboard.getOrCreatePlayerScore(holder, scoreObjective).set(next);
            Component sideName = Component.translatable(team == TEAM_RED
                    ? "message.footblockultimate.side.red"
                    : "message.footblockultimate.side.blue");
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable(
                            "message.footblockultimate.score_manager.changed",
                            player.getDisplayName(),
                            sideName,
                            next
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, scoreObjective);
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.8f, 1.1f);
        sendScoreManagerState(player, pos, false);
    }

    public static ScoreState getScoreState(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(SCORE_OBJECTIVE);
        if (objective == null) {
            return new ScoreState(0, 0, false);
        }
        return new ScoreState(
                scoreboard.getOrCreatePlayerScore(RED_SCORE, objective).get(),
                scoreboard.getOrCreatePlayerScore(BLUE_SCORE, objective).get(),
                true
        );
    }

    public static void sendScoreManagerState(ServerPlayer player, BlockPos pos, boolean openScreen) {
        ScoreState state = getScoreState(player.getServer());
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.level().registryAccess()
        );
        buffer.writeBlockPos(pos);
        buffer.writeInt(state.red());
        buffer.writeInt(state.blue());
        buffer.writeBoolean(state.active());
        buffer.writeBoolean(openScreen);
        NetworkManager.sendToPlayer(player, FootblockUltimate.SCORE_MANAGER_STATE_PACKET_ID, buffer);
    }

    public static void denyReset(Player referee) {
        referee.displayClientMessage(
                Component.translatable("message.footblockultimate.match_reset_denied")
                        .withStyle(ChatFormatting.RED),
                true
        );
    }

    public static void celebrateTrophy(ServerLevel level, BlockPos pos, Player player) {
        Map<UUID, Long> playerCooldowns = TROPHY_COOLDOWNS.computeIfAbsent(
                level.getServer(),
                ignored -> new HashMap<>()
        );
        long gameTime = level.getGameTime();
        Long lastCelebration = playerCooldowns.get(player.getUUID());
        if (lastCelebration != null && gameTime >= lastCelebration
                && gameTime - lastCelebration < TROPHY_COOLDOWN_TICKS) {
            long secondsRemaining = (TROPHY_COOLDOWN_TICKS - (gameTime - lastCelebration) + 19L) / 20L;
            player.displayClientMessage(
                    Component.translatable("message.footblockultimate.trophy_cooldown", secondsRemaining)
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
            return;
        }
        playerCooldowns.put(player.getUUID(), gameTime);

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.2;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 70, 1.2, 1.0, 1.2, 0.16);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 35, 0.7, 0.9, 0.7, 0.08);
        level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 0.9f);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.footblockultimate.trophy_lifted", player.getDisplayName())
                        .withStyle(ChatFormatting.GOLD),
                false
        );
    }

    private static Side getPlayerSide(Player player) {
        if (player == null || player.getTeam() == null) {
            return null;
        }
        String teamName = player.getTeam().getName();
        if (teamName.equalsIgnoreCase("red")) {
            return Side.RED;
        }
        if (teamName.equalsIgnoreCase("blue")) {
            return Side.BLUE;
        }
        return null;
    }

    private static Objective ensureScoreObjective(ServerScoreboard scoreboard) {
        Objective objective = scoreboard.getObjective(SCORE_OBJECTIVE);
        if (objective == null) {
            objective = scoreboard.addObjective(
                    SCORE_OBJECTIVE,
                    ObjectiveCriteria.DUMMY,
                    Component.translatable("scoreboard.footblockultimate.world_cup_score"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
            );
            scoreboard.getOrCreatePlayerScore(RED_SCORE, objective).set(0);
            scoreboard.getOrCreatePlayerScore(BLUE_SCORE, objective).set(0);
        }
        scoreboard.getOrCreatePlayerScore(RED_SCORE, objective).display(
                Component.translatable("message.footblockultimate.side.red").withStyle(ChatFormatting.RED));
        scoreboard.getOrCreatePlayerScore(BLUE_SCORE, objective).display(
                Component.translatable("message.footblockultimate.side.blue").withStyle(ChatFormatting.BLUE));
        return objective;
    }

    private static Objective ensureGoalsObjective(ServerScoreboard scoreboard) {
        Objective objective = scoreboard.getObjective(GOALS_OBJECTIVE);
        if (objective == null) {
            objective = scoreboard.addObjective(
                    GOALS_OBJECTIVE,
                    ObjectiveCriteria.DUMMY,
                    Component.translatable("scoreboard.footblockultimate.player_goals"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
            );
        }
        return objective;
    }
}
