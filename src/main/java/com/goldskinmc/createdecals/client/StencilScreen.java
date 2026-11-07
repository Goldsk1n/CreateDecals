package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.goldskinmc.createdecals.stencil.StencilPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Decal Stencil's screen, in Create's style: a brass-framed panel ({@code textures/gui/stencil.png}, drawn
 * with its wells where the layout constants below put things), category tabs, the decals of one
 * category in a grid (scroll for more), the 16 dye colours and a hex field, Create's icon buttons to turn, flip, erase
 * and finish, and a preview of the decal as it will lie on a face. Closing it stores the choice on the stencil (one
 * serverbound payload).
 */
public class StencilScreen extends Screen {
    private static final ResourceLocation BACKGROUND = CreateDecals.id("textures/gui/stencil.png");
    private static final List<String> CATEGORY_ORDER = List.of("letters", "digits", "signs", "lines", "markers", "emblems");
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 200;
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int CELL = 22;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 42;
    private static final int TABS_Y = 22;
    private static final int TAB_HEIGHT = 14;
    private static final int PREVIEW_X = 215;
    private static final int PREVIEW_Y = 42;
    private static final int PREVIEW_SIZE = 54;
    private static final int SWATCH = 12;
    private static final int SWATCH_X = 8;
    private static final int SWATCH_Y = 152;

    private final InteractionHand hand;
    private StencilData data;
    private final Map<String, List<DecalLibrary.Entry>> categories = new LinkedHashMap<>();
    private final List<String> categoryNames = new ArrayList<>();
    private final List<int[]> tabBounds = new ArrayList<>();
    private int category;
    private int page;
    private EditBox hex;
    private int left;
    private int top;

    public StencilScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("createdecals.stencil.title"));
        this.hand = hand;
        this.data = DecalStencilItem.data(stack);
        List<DecalLibrary.Entry> all = new ArrayList<>(DecalLibrary.INSTANCE.all());
        all.sort(Comparator.comparing((DecalLibrary.Entry e) -> categoryRank(e.category())).thenComparing(e -> e.id().toString()));
        for (DecalLibrary.Entry entry : all) {
            categories.computeIfAbsent(entry.category(), k -> new ArrayList<>()).add(entry);
        }
        categoryNames.addAll(categories.keySet());
        data.decal().flatMap(id -> all.stream().filter(e -> e.id().equals(id)).findFirst())
                .ifPresent(e -> category = Math.max(0, categoryNames.indexOf(e.category())));
    }

    private static int categoryRank(String name) {
        int index = CATEGORY_ORDER.indexOf(name);
        return index >= 0 ? index : CATEGORY_ORDER.size();
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        tabBounds.clear();
        int x = left + 6;
        for (String name : categoryNames) {
            int w = font.width(categoryLabel(name)) + 8;
            tabBounds.add(new int[] {x, top + TABS_Y, w, TAB_HEIGHT});
            x += w + 2;
        }
        int buttonsY = top + PREVIEW_Y + PREVIEW_SIZE + 8;
        button(left + PREVIEW_X, buttonsY, AllIcons.I_TOOL_ROTATE, "rotate", () -> data = data.withTurn(data.rotation() + 1, data.flip()));
        button(left + PREVIEW_X + 20, buttonsY, AllIcons.I_TOOL_MIRROR, "flip", () -> data = data.withTurn(data.rotation(), !data.flip()));
        button(left + PREVIEW_X + 40, buttonsY, AllIcons.I_TRASH, "erase", () -> data = data.withErase(!data.erase()));
        button(left + PANEL_WIDTH - 30, top + PANEL_HEIGHT - 30, AllIcons.I_CONFIRM, "done", this::onClose);
        hex = new EditBox(font, left + 128, top + SWATCH_Y + 8, 62, 16, Component.translatable("createdecals.stencil.colour"));
        hex.setMaxLength(7);
        hex.setValue(String.format(Locale.ROOT, "#%06X", data.rgb()));
        hex.setResponder(text -> {
            String digits = text.startsWith("#") ? text.substring(1) : text;
            if (digits.length() == 6) {
                try {
                    data = data.withRgb(Integer.parseInt(digits, 16));
                } catch (NumberFormatException ignored) {
                    // not a colour yet: keep the last one
                }
            }
        });
        addRenderableWidget(hex);
    }

    private void button(int x, int y, AllIcons icon, String key, Runnable action) {
        IconButton button = new IconButton(x, y, icon);
        button.setToolTip(Component.translatable("createdecals.stencil." + key));
        button.withCallback(action);
        addRenderableWidget(button);
    }

    private static Component categoryLabel(String name) {
        return Component.translatableWithFallback("createdecals.category." + name, name);
    }

    private List<DecalLibrary.Entry> categoryEntries() {
        return categoryNames.isEmpty() ? List.of() : categories.get(categoryNames.get(category));
    }

    private List<DecalLibrary.Entry> shown() {
        List<DecalLibrary.Entry> entries = categoryEntries();
        int from = Math.min(page * COLUMNS * ROWS, entries.size());
        return entries.subList(from, Math.min(from + COLUMNS * ROWS, entries.size()));
    }

    private int pages() {
        return Math.max(1, (categoryEntries().size() + COLUMNS * ROWS - 1) / (COLUMNS * ROWS));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(BACKGROUND, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 512, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + 9, top + 7, 0xFFFFFF);
        for (int i = 0; i < tabBounds.size(); i++) {
            int[] b = tabBounds.get(i);
            boolean selected = i == category;
            graphics.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], selected ? 0xFF724731 : 0xFF353B39);
            graphics.fill(b[0] + 1, b[1] + 1, b[0] + b[2] - 1, b[1] + b[3] - 1, selected ? 0xFFCEA05A : 0xFF505351);
            graphics.drawString(font, categoryLabel(categoryNames.get(i)), b[0] + 4, b[1] + 3, selected ? 0x2A1A0E : 0xE0E0E0, false);
        }
        List<DecalLibrary.Entry> entries = shown();
        for (int i = 0; i < entries.size(); i++) {
            DecalLibrary.Entry entry = entries.get(i);
            int x = left + GRID_X + (i % COLUMNS) * CELL;
            int y = top + GRID_Y + (i / COLUMNS) * CELL;
            boolean selected = !data.erase() && data.decal().map(entry.id()::equals).orElse(false);
            boolean hovered = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2;
            graphics.fill(x, y, x + CELL - 2, y + CELL - 2, selected ? 0xFFF7CB6C : hovered ? 0xFFA8B3AB : 0xFF2A2D33);
            graphics.fill(x + 1, y + 1, x + CELL - 3, y + CELL - 3, 0xFF6B7078);
            drawDecal(graphics, entry, x + 2, y + 2, CELL - 6, 0, false);
        }
        if (pages() > 1) {
            graphics.drawString(font, (page + 1) + "/" + pages(), left + GRID_X, top + GRID_Y + ROWS * CELL + 1, 0xE0E0E0, false);
        }
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int x = left + SWATCH_X + (i % 8) * (SWATCH + 2);
            int y = top + SWATCH_Y + (i / 8) * (SWATCH + 2);
            int rgb = dyes[i].getTextureDiffuseColor() & 0xFFFFFF;
            graphics.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, rgb == data.rgb() ? 0xFFF7CB6C : 0xFF202226);
            graphics.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | rgb);
        }
        // The preview: the decal as it will lie on a face, turned and flipped
        int px = left + PREVIEW_X;
        int py = top + PREVIEW_Y;
        graphics.fill(px, py, px + PREVIEW_SIZE, py + PREVIEW_SIZE, 0xFF8D939B);
        if (data.erase()) {
            graphics.drawCenteredString(font, Component.translatable("createdecals.stencil.erase"), px + PREVIEW_SIZE / 2, py + 23, 0xFFFFFF);
        } else {
            data.decal().map(DecalLibrary.INSTANCE::get).ifPresent(entry -> {
                drawDecal(graphics, entry, px + 3, py + 3, PREVIEW_SIZE - 6, data.rotation(), data.flip());
                if (data.width() > 1 || data.height() > 1) {
                    boolean across = data.rotation() % 2 == 0;
                    graphics.drawString(font, (across ? data.width() : data.height()) + " x " + (across ? data.height() : data.width()),
                            px + PREVIEW_SIZE + 2, py + PREVIEW_SIZE - 9, 0xFFFFFF);
                }
            });
        }
    }

    /** The decal's whole sprite (all tiles of a large one), tinted, flipped and then turned {@code rotation} quarters. */
    private void drawDecal(GuiGraphics graphics, DecalLibrary.Entry entry, int x, int y, int size, int rotation, boolean flip) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(entry.sprite());
        float r = ((data.rgb() >> 16) & 0xFF) / 255f;
        float g = ((data.rgb() >> 8) & 0xFF) / 255f;
        float b = (data.rgb() & 0xFF) / 255f;
        graphics.pose().pushPose();
        graphics.pose().translate(x + size / 2f, y + size / 2f, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(90 * rotation));
        if (flip) {
            graphics.pose().scale(-1, 1, 1);
            // A mirrored quad winds the other way, and the GUI culls back faces: without this it vanished
            RenderSystem.disableCull();
        }
        graphics.blit(-size / 2, -size / 2, 0, size, size, sprite, r, g, b, 1f);
        if (flip) {
            RenderSystem.enableCull();
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < tabBounds.size(); i++) {
            int[] b = tabBounds.get(i);
            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
                category = i;
                page = 0;
                return true;
            }
        }
        List<DecalLibrary.Entry> entries = shown();
        for (int i = 0; i < entries.size(); i++) {
            double[] c = cellCentre(i);
            if (Math.abs(mouseX - c[0]) < CELL / 2.0 - 1 && Math.abs(mouseY - c[1]) < CELL / 2.0 - 1) {
                DecalLibrary.Entry entry = entries.get(i);
                data = data.withDecal(entry.id(), entry.width(), entry.height());
                return true;
            }
        }
        for (DyeColor dye : DyeColor.values()) {
            double[] c = swatchCentre(dye);
            if (Math.abs(mouseX - c[0]) <= SWATCH / 2.0 && Math.abs(mouseY - c[1]) <= SWATCH / 2.0) {
                data = data.withRgb(dye.getTextureDiffuseColor());
                hex.setValue(String.format(Locale.ROOT, "#%06X", data.rgb()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        page = Math.max(0, Math.min(pages() - 1, page - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new StencilPayload(hand, data));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For automated checks: what the screen holds now. */
    public StencilData data() {
        return data;
    }

    /** For automated checks, which click like a player: the centre of the {@code i}th decal cell shown. */
    public double[] cellCentre(int i) {
        return new double[] {left + GRID_X + (i % COLUMNS) * CELL + (CELL - 2) / 2.0, top + GRID_Y + (i / COLUMNS) * CELL + (CELL - 2) / 2.0};
    }

    /** For automated checks: the centre of the swatch of {@code dye}. */
    public double[] swatchCentre(DyeColor dye) {
        int i = dye.getId();
        return new double[] {left + SWATCH_X + (i % 8) * (SWATCH + 2) + SWATCH / 2.0, top + SWATCH_Y + (i / 8) * (SWATCH + 2) + SWATCH / 2.0};
    }
}
