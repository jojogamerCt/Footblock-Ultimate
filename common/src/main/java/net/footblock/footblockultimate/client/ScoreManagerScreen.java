package net.footblock.footblockultimate.client;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.footblock.footblockultimate.FootblockUltimate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

public final class ScoreManagerScreen extends Screen {
    public static final byte ACTION_RESET = 0;
    public static final byte ACTION_ADD = 1;
    public static final byte ACTION_SET = 2;
    public static final byte ACTION_END = 3;
    public static final byte TEAM_RED = 0;
    public static final byte TEAM_BLUE = 1;

    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 190;

    private final BlockPos consolePos;
    private int redScore;
    private int blueScore;
    private boolean matchActive;
    private byte selectedTeam = TEAM_RED;
    private EditBox scoreInput;
    private ScoreManagerButton redTeamButton;
    private ScoreManagerButton blueTeamButton;

    public ScoreManagerScreen(BlockPos consolePos, int redScore, int blueScore, boolean matchActive) {
        super(Component.translatable("screen.footblockultimate.score_manager.title"));
        this.consolePos = consolePos.immutable();
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.matchActive = matchActive;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        this.redTeamButton = this.addRenderableWidget(new ScoreManagerButton(
                left + 18, top + 58, 100, 20, teamLabel(TEAM_RED),
                0xFFFF4E5B, () -> selectedTeam == TEAM_RED, () -> selectTeam(TEAM_RED)
        ));
        this.blueTeamButton = this.addRenderableWidget(new ScoreManagerButton(
                left + 130, top + 58, 100, 20, teamLabel(TEAM_BLUE),
                0xFF4D84FF, () -> selectedTeam == TEAM_BLUE, () -> selectTeam(TEAM_BLUE)
        ));

        this.addRenderableWidget(new ScoreManagerButton(
                left + 18, top + 88, 48, 20, Component.literal("-1"),
                0xFF3EC7D3, () -> false, () -> sendAction(ACTION_ADD, selectedTeam, -1)
        ));
        this.addRenderableWidget(new ScoreManagerButton(
                left + 70, top + 88, 48, 20, Component.literal("+1"),
                0xFF3EC7D3, () -> false, () -> sendAction(ACTION_ADD, selectedTeam, 1)
        ));

        this.scoreInput = new ScoreManagerEditBox(
                this.font, left + 134, top + 88, 40, 20,
                Component.translatable("screen.footblockultimate.score_manager.value")
        );
        this.scoreInput.setMaxLength(4);
        this.scoreInput.setFilter(value -> value.matches("\\d{0,4}"));
        this.scoreInput.setValue(Integer.toString(selectedScore()));
        this.scoreInput.setBordered(false);
        this.scoreInput.setTextColor(0xFFF2FBFF);
        this.scoreInput.setTextColorUneditable(0xFF7B8A90);
        this.addRenderableWidget(this.scoreInput);

        this.addRenderableWidget(new ScoreManagerButton(
                left + 182, top + 88, 48, 20,
                Component.translatable("screen.footblockultimate.score_manager.set"),
                0xFF3EC7D3, () -> false, this::applyTypedScore
        ));
        this.addRenderableWidget(new ScoreManagerButton(
                left + 18, top + 126, 100, 20,
                Component.translatable("screen.footblockultimate.score_manager.reset"),
                0xFFF0B84B, () -> false, () -> sendAction(ACTION_RESET, selectedTeam, 0)
        ));
        this.addRenderableWidget(new ScoreManagerButton(
                left + 130, top + 126, 100, 20,
                Component.translatable("screen.footblockultimate.score_manager.end"),
                0xFFFF5A67, () -> false, () -> sendAction(ACTION_END, selectedTeam, 0)
        ));
        this.addRenderableWidget(new ScoreManagerButton(
                left + 74, top + 156, 100, 20, Component.translatable("gui.done"),
                0xFF3EC7D3, () -> false, this::onClose
        ));

        updateTeamButtons();
    }
    private void selectTeam(byte team) {
        this.selectedTeam = team;
        this.scoreInput.setValue(Integer.toString(selectedScore()));
        updateTeamButtons();
    }

    private void updateTeamButtons() {
        if (this.redTeamButton != null) {
            this.redTeamButton.setMessage(teamLabel(TEAM_RED));
        }
        if (this.blueTeamButton != null) {
            this.blueTeamButton.setMessage(teamLabel(TEAM_BLUE));
        }
    }

    private Component teamLabel(byte team) {
        int score = team == TEAM_RED ? redScore : blueScore;
        String marker = selectedTeam == team ? " > " : " ";
        return Component.literal(marker)
                .append(Component.translatable(team == TEAM_RED
                        ? "message.footblockultimate.side.red"
                        : "message.footblockultimate.side.blue"))
                .append(Component.literal(": " + score))
                .withStyle(team == TEAM_RED ? ChatFormatting.RED : ChatFormatting.BLUE);
    }

    private int selectedScore() {
        return selectedTeam == TEAM_RED ? redScore : blueScore;
    }

    private void applyTypedScore() {
        if (this.scoreInput.getValue().isEmpty()) {
            return;
        }
        try {
            sendAction(ACTION_SET, selectedTeam, Integer.parseInt(this.scoreInput.getValue()));
        } catch (NumberFormatException ignored) {
            this.scoreInput.setValue(Integer.toString(selectedScore()));
        }
    }

    private void sendAction(byte action, byte team, int value) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                minecraft.level.registryAccess()
        );
        buffer.writeBlockPos(this.consolePos);
        buffer.writeByte(action);
        buffer.writeByte(team);
        buffer.writeInt(value);
        NetworkManager.sendToServer(FootblockUltimate.SCORE_MANAGER_ACTION_PACKET_ID, buffer);
    }

    public boolean isFor(BlockPos pos) {
        return this.consolePos.equals(pos);
    }

    public void updateState(int redScore, int blueScore, boolean matchActive) {
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.matchActive = matchActive;
        if (this.scoreInput != null && !this.scoreInput.isFocused()) {
            this.scoreInput.setValue(Integer.toString(selectedScore()));
        }
        updateTeamButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(0, 0, this.width, this.height, 0xB0080E12);
        graphics.fill(left - 5, top - 5, left + PANEL_WIDTH + 5, top + PANEL_HEIGHT + 5, 0x70000000);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF101A20);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF2A6972);
        graphics.renderOutline(left + 2, top + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, 0xFF18363D);
        graphics.fill(left + 12, top + 48, left + PANEL_WIDTH - 12, top + 49, 0xFF20454D);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 13, 0xFFF2FBFF);
        Component status = Component.translatable(matchActive
                ? "screen.footblockultimate.score_manager.active"
                : "screen.footblockultimate.score_manager.inactive");
        graphics.drawCenteredString(this.font, status, this.width / 2, top + 34,
                matchActive ? 0xFF72E07A : 0xFF9AA8AE);
        graphics.fill(left + 130, top + 88, left + 178, top + 108, 0xFF091014);
        graphics.renderOutline(left + 130, top + 88, 48, 20,
                this.scoreInput != null && this.scoreInput.isFocused() ? 0xFF3EC7D3 : 0xFF42606A);

        for (GuiEventListener child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
