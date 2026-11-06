package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.goldskinmc.createdecals.stencil.StencilPayload;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Decal Stencil's screen: decals by category in a grid, the 16 dye colours and a hex field, turn, flip and the
 * eraser, and a preview of the decal as it will lie on a face. Closing it stores the choice on the stencil (one
 * serverbound payload). Placeholder look until M4/M6 give it Create's style.
 */
public class StencilScreen extends Screen {
    private static final List<String> CATEGORY_ORDER = List.of("shapes", "digits", "letters", "emblems");
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int CELL = 22;
    private static final int SWATCH = 12;
    private static final int PANEL_WIDTH = COLUMNS * CELL + 92;
    private static final int PANEL_HEIGHT = 196;

    private final InteractionHand hand;
    private StencilData data;
    private final Map<String, List<DecalLibrary.Entry>> categories = new LinkedHashMap<>();
    private final List<String> categoryNames = new ArrayList<>();
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
        int x = left + 6;
        for (int i = 0; i < categoryNames.size(); i++) {
            int index = i;
            String name = categoryNames.get(i);
            Component label = Component.translatableWithFallback("createdecals.category." + name, name);
            int w = font.width(label) + 10;
            addRenderableWidget(Button.builder(label, b -> {
                category = index;
                page = 0;
            }).bounds(x, top + 18, w, 16).build());
            x += w + 2;
        }
        int controlsX = left + COLUMNS * CELL + 14;
        addRenderableWidget(Button.builder(Component.translatable("createdecals.stencil.rotate"),
                b -> data = data.withTurn(data.rotation() + 1, data.flip())).bounds(controlsX, top + 96, 72, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("createdecals.stencil.flip"),
                b -> data = data.withTurn(data.rotation(), !data.flip())).bounds(controlsX, top + 114, 72, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("createdecals.stencil.erase"),
                b -> data = data.withErase(!data.erase())).bounds(controlsX, top + 132, 72, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("createdecals.stencil.done"), b -> onClose())
                .bounds(controlsX, top + PANEL_HEIGHT - 22, 72, 16).build());
        hex = new EditBox(font, left + 6 + 8 * (SWATCH + 2) + 4, top + PANEL_HEIGHT - 22, 60, 16, Component.translatable("createdecals.stencil.colour"));
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

    private List<DecalLibrary.Entry> shown() {
        if (categoryNames.isEmpty()) {
            return List.of();
        }
        List<DecalLibrary.Entry> entries = categories.get(categoryNames.get(category));
        int from = Math.min(page * COLUMNS * ROWS, entries.size());
        return entries.subList(from, Math.min(from + COLUMNS * ROWS, entries.size()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + 6, top + 6, 0xFFFFFF);
        List<DecalLibrary.Entry> entries = shown();
        int gridTop = top + 40;
        for (int i = 0; i < entries.size(); i++) {
            DecalLibrary.Entry entry = entries.get(i);
            int x = left + 6 + (i % COLUMNS) * CELL;
            int y = gridTop + (i / COLUMNS) * CELL;
            boolean selected = !data.erase() && data.decal().map(entry.id()::equals).orElse(false);
            graphics.fill(x, y, x + CELL - 2, y + CELL - 2, selected ? 0xFFE6B24B : 0xFF3B3F46);
            graphics.fill(x + 1, y + 1, x + CELL - 3, y + CELL - 3, 0xFF6B7078);
            drawDecal(graphics, entry, x + 2, y + 2, CELL - 6, 0, false);
        }
        int swatchTop = top + PANEL_HEIGHT - 40;
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int x = left + 6 + (i % 8) * (SWATCH + 2);
            int y = swatchTop + (i / 8) * (SWATCH + 2);
            int rgb = dyes[i].getTextureDiffuseColor() & 0xFFFFFF;
            graphics.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, rgb == data.rgb() ? 0xFFFFFFFF : 0xFF202226);
            graphics.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | rgb);
        }
        // Preview: the decal as it will lie on a face, turned and flipped
        int previewX = left + COLUMNS * CELL + 14;
        int previewY = top + 40;
        graphics.fill(previewX, previewY, previewX + 52, previewY + 52, 0xFF3B3F46);
        graphics.fill(previewX + 2, previewY + 2, previewX + 50, previewY + 50, 0xFF8D939B);
        if (data.erase()) {
            graphics.drawCenteredString(font, Component.translatable("createdecals.stencil.erase"), previewX + 26, previewY + 22, 0xFFFFFF);
        } else {
            data.decal().map(DecalLibrary.INSTANCE::get).ifPresent(entry -> {
                drawDecal(graphics, entry, previewX + 4, previewY + 4, 44, data.rotation(), data.flip());
                if (data.width() > 1 || data.height() > 1) {
                    boolean across = data.rotation() % 2 == 0;
                    graphics.drawString(font, (across ? data.width() : data.height()) + " x " + (across ? data.height() : data.width()),
                            previewX + 2, previewY + 54, 0xFFFFFF);
                }
            });
        }
    }

    /** The decal's whole sprite (all tiles of a large one), tinted, turned {@code rotation} quarters after flipping. */
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
        }
        graphics.blit(-size / 2, -size / 2, 0, size, size, sprite, r, g, b, 1f);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<DecalLibrary.Entry> entries = shown();
        int gridTop = top + 40;
        for (int i = 0; i < entries.size(); i++) {
            int x = left + 6 + (i % COLUMNS) * CELL;
            int y = gridTop + (i / COLUMNS) * CELL;
            if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) {
                DecalLibrary.Entry entry = entries.get(i);
                data = data.withDecal(entry.id(), entry.width(), entry.height());
                return true;
            }
        }
        int swatchTop = top + PANEL_HEIGHT - 40;
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int x = left + 6 + (i % 8) * (SWATCH + 2);
            int y = swatchTop + (i / 8) * (SWATCH + 2);
            if (mouseX >= x && mouseX < x + SWATCH && mouseY >= y && mouseY < y + SWATCH) {
                data = data.withRgb(dyes[i].getTextureDiffuseColor());
                hex.setValue(String.format(Locale.ROOT, "#%06X", data.rgb()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!categoryNames.isEmpty()) {
            int pages = (categories.get(categoryNames.get(category)).size() + COLUMNS * ROWS - 1) / (COLUMNS * ROWS);
            page = Math.max(0, Math.min(pages - 1, page - (int) Math.signum(scrollY)));
        }
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
        return new double[] {left + 6 + (i % COLUMNS) * CELL + CELL / 2.0, top + 40 + (i / COLUMNS) * CELL + CELL / 2.0};
    }

    /** For automated checks: the centre of the swatch of {@code dye}. */
    public double[] swatchCentre(DyeColor dye) {
        int i = dye.getId();
        return new double[] {left + 6 + (i % 8) * (SWATCH + 2) + SWATCH / 2.0, top + PANEL_HEIGHT - 40 + (i / 8) * (SWATCH + 2) + SWATCH / 2.0};
    }
}
